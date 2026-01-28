#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <stdbool.h>
#include <sys/time.h>
#include <unistd.h>

#ifndef N
#define N 8  // Number of queens (Default value if none is provided)
#endif    

#define QUEUE_SIZE 256

// DATA STRUCTURES

// Task: Represents a snapshot of the board logic, not visual board
typedef struct
{
    int startRow; // Workers start solving from here
    bool col[N]; // Lookup: is column 'i' occupied?
    bool posDiag[2 * N]; // Lookup: is r + c (positive diagonal) occupied?
    bool negDiag[2 * N]; // Lookup: is r - c (negative diagonal) occupied?
} Task;

// Circular buffer queue for tasks (thread safe)
typedef struct
{
    Task tasks[QUEUE_SIZE];
    int readIndex; // Controlled by producer
    int writeIndex; // Controlled by worker
    int count;
    bool productionComplete;
    pthread_mutex_t mutex;
    pthread_cond_t condNotEmpty;
    pthread_cond_t condNotFull;
} TaskQueue;

TaskQueue taskQueue;

// TASK QUEUE HELPER FUNCTIONS

void initQueue(TaskQueue* q)
{
    q->readIndex = 0;
    q->writeIndex = 0;
    q->count = 0;
    q->productionComplete = false;
    pthread_mutex_init(&q->mutex, NULL);
    pthread_cond_init(&q->condNotEmpty, NULL);
    pthread_cond_init(&q->condNotFull, NULL);
}

// Producer calls this
void pushTask(TaskQueue* q, Task t)
{
    pthread_mutex_lock(&q->mutex);

    // Wait while queue is full
    while (q->count == QUEUE_SIZE)
    {
        pthread_cond_wait(&q->condNotFull, &q->mutex);
    }

    q->tasks[q->writeIndex] = t;
    q->writeIndex = (q->writeIndex + 1) % QUEUE_SIZE;
    q->count++;

    // Wake up sleeping worker
    pthread_cond_signal(&q->condNotEmpty);
    pthread_mutex_unlock(&q->mutex);
}

// Consumer calls this
int popTask(TaskQueue* q, Task* t)
{
    pthread_mutex_lock(&q->mutex);

    // Wait while queue is empty, unless producer is done
    while (q->count == 0 && !q->productionComplete)
    {
        pthread_cond_wait(&q->condNotEmpty, &q->mutex);
    }

    // If empty and done, return 0
    if (q->count == 0 && q->productionComplete)
    {
        pthread_mutex_unlock(&q->mutex);
        return 0;
    }

    *t = q->tasks[q->readIndex];
    q->readIndex = (q->readIndex + 1) % QUEUE_SIZE;
    q->count--;

    // Signal producer that there is space
    pthread_cond_signal(&q->condNotFull);
    pthread_mutex_unlock(&q->mutex);
    return 1;
}

// Signal workers that no new tasks are coming
void setDone(TaskQueue* q)
{
    pthread_mutex_lock(&q->mutex);
    q->productionComplete = true;
    pthread_cond_broadcast(&q->condNotEmpty); // Wake everyone up to exit
    pthread_mutex_unlock(&q->mutex);
}

// Solving algorithms, Recursion + Backtracking (consumer logic)
void solve(const int r,
           bool col[], bool posDiag[], bool negDiag[],
           int* localCount)
{
    // Base Case: all queens placed
    if (r == N)
    {
        (*localCount)++;
        return;
    }

    // Try all columns
    for (int c = 0; c < N; c++)
    {
        // O(1) lookup instead of scanning loop
        if (!col[c] && !posDiag[r + c] && !negDiag[r - c + N])
        {
            // Place Queen (Update lookups)
            col[c] = true;
            posDiag[r + c] = true;
            negDiag[r - c + N] = true; // Offset by N to prevent negative index

            // Recurse
            solve(r + 1, col, posDiag, negDiag, localCount);

            // Backtrack (Undo Lookups)
            col[c] = false;
            posDiag[r + c] = false;
            negDiag[r - c + N] = false;
        }
    }
}

// Worker thread
void* worker(void* arg)
{
    int* solutionsFound = malloc(sizeof(int));
    *solutionsFound = 0;

    Task t;

    // Consuming Loop
    while (popTask(&taskQueue, &t))
    {
        solve(t.startRow, t.col, t.posDiag, t.negDiag, solutionsFound);
    }

    return solutionsFound;
}

// Producer Logic (recursive generator)
void generateTasks(const int r, bool col[], bool posDiag[], bool negDiag[], int depth)
{
    // If we reached the split depth, send to queue
    if (r == depth)
    {
        Task t;
        t.startRow = r;
        memcpy(t.col, col, sizeof(bool) * N);
        memcpy(t.posDiag, posDiag, sizeof(bool) * 2 * N);
        memcpy(t.negDiag, negDiag, sizeof(bool) * 2 * N);
        pushTask(&taskQueue, t);
        return;
    }

    for (int c = 0; c < N; c++)
    {
        if (!col[c] && !posDiag[r + c] && !negDiag[r - c + N])
        {
            col[c] = true;
            posDiag[r + c] = true;
            negDiag[r - c + N] = true;

            generateTasks(r + 1, col, posDiag, negDiag, depth);

            col[c] = false;
            posDiag[r + c] = false;
            negDiag[r - c + N] = false;
        }
    }
}

// Depth calculation helper function
int getOptimalDepth(int n) {
    if (n < 10) return 1;       // For N=8, creates ~8 tasks.
    if (n < 12) return 2;       // For N=10, creates ~70 tasks.
    if (n < 15) return 3;       // For N=12-14, creates ~800 to ~2,000 tasks.
    return 4;                   // For N=15+, creates ~3,000+ tasks.
}

int main(int argc, char* argv[])
{
    if (argc != 2)
    {
        printf("Usage: %s <number of threads>\n", argv[0]);
        return 1;
    }

    int NUM_THREADS = atoi(argv[1]);
    if (NUM_THREADS <= 0)
    {
        printf("Number of threads must be greater than 0\n");
        return 1;
    }

    pthread_t threads[NUM_THREADS];

    initQueue(&taskQueue);

    // Initial board arrays (empty)
    bool col[N] = {false};
    bool posDiag[2 * N] = {false};
    bool negDiag[2 * N] = {false};

    int optimalDepth = getOptimalDepth(N);

    printf("Solving N=%d with depth %d and threads %d\n", N, optimalDepth, NUM_THREADS);

    struct timeval start, end;
    gettimeofday(&start, NULL);
    
    // Launch workers
    for (int i = 0; i < NUM_THREADS; i++)
    {
        pthread_create(&threads[i], NULL, worker, NULL);
    }

    // Producer generates partial boards
    generateTasks(0, col, posDiag, negDiag, optimalDepth);

    // Signal completion
    setDone(&taskQueue);

    // Join and sum
    // 1. Wait: pthread_join blocks until the specific thread terminates.
    // 2. Capture: We get the return value as a void* (raw memory address).
    // 3. Cast & Dereference: Convert void* to int*, then read the value.
    // 4. Sum: Add the thread's local count to the global total.
    // 5. Cleanup: Free the heap memory allocated by the worker.
    int totalSolutions = 0;
    for (int i = 0; i < NUM_THREADS; i++)
    {
        void* ret;
        pthread_join(threads[i], &ret);
        totalSolutions += *(int*)ret;
        free(ret);
    }

    gettimeofday(&end, NULL);
    const double time = (double)(end.tv_sec - start.tv_sec) + (double)(end.tv_usec - start.tv_usec) / 1e6;

    printf("Total Solutions: %d\n", totalSolutions);
    printf("Time: %f seconds\n", time);

    // Cleanup
    pthread_mutex_destroy(&taskQueue.mutex);
    pthread_cond_destroy(&taskQueue.condNotEmpty);
    pthread_cond_destroy(&taskQueue.condNotFull);

    return 0;
}
