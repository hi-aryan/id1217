/*
 * Homework 1: Problem 7 (8-Queens)
 * Author: [Your Name]
 * Date: [Today's Date]
 *
 * Description: Solves the 8-Queens problem using Pthreads and a
 * Bag-of-Tasks approach for domain decomposition.
 */

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>

#define SIZE 8 // board size (8x8)

// shared global variables
int total_solutions = 0;
pthread_mutex_t mutex_sol;

// task definition: a partial board state
typedef struct {
  int board[SIZE];
  int start_row;
} Task;

// queue definition
#define QUEUE_SIZE 256 // Enough to hold tasks for depth 2 or 3
typedef struct {
  Task tasks[QUEUE_SIZE];
  int front;
  int rear;
  int count;
  int done_producing; // flag to signal no more tasks will be added
  pthread_mutex_t mutex;
  pthread_cond_t cond_not_empty;
  pthread_cond_t cond_not_full;
} Queue;

Queue task_queue;
pthread_mutex_t mutex_sol; // for updating total_solutions

// helper to initialize queue
void init_queue(Queue *q) {
  q->front = 0;
  q->rear = 0;
  q->count = 0;
  q->done_producing = 0;
  pthread_mutex_init(&q->mutex, NULL);
  pthread_cond_init(&q->cond_not_empty, NULL);
  pthread_cond_init(&q->cond_not_full, NULL);
}

// helper to push task (PRODUCER TASK)
void push_task(Queue *q, Task t) {
  pthread_mutex_lock(&q->mutex);
  while (q->count == QUEUE_SIZE) {
    pthread_cond_wait(&q->cond_not_full, &q->mutex);
  }
  q->tasks[q->rear] = t;
  q->rear = (q->rear + 1) % QUEUE_SIZE;
  q->count++;
  pthread_cond_signal(&q->cond_not_empty);
  pthread_mutex_unlock(&q->mutex);
}

// helper to pop task (CONSUMER TASK)
// returns 1 if managed to pop, 0 if empty and done_producing
int pop_task(Queue *q, Task *t) {
  pthread_mutex_lock(&q->mutex);
  while (q->count == 0 && !q->done_producing) {
    pthread_cond_wait(&q->cond_not_empty, &q->mutex);
  }
  if (q->count == 0 && q->done_producing) {
    pthread_mutex_unlock(&q->mutex);
    return 0;
  }
  *t = q->tasks[q->front];
  q->front = (q->front + 1) % QUEUE_SIZE;
  q->count--;
  pthread_cond_signal(&q->cond_not_full);
  pthread_mutex_unlock(&q->mutex);
  return 1;
}

// set done flag
void set_done(Queue *q) {
  pthread_mutex_lock(&q->mutex);
  q->done_producing = 1;
  pthread_cond_broadcast(&q->cond_not_empty); // wake up all consumers
  pthread_mutex_unlock(&q->mutex);
}

// called from inside critical section to verify output
void print_board(int board[]) {
  printf("[");
  for (int i = 0; i < SIZE; i++) {
    printf("%d ", board[i]);
  }
  printf("]\n");
}

// check if placing a queen at (row, col) is safe against previous queens
int is_safe(int board[], int row, int col) {
  for (int i = 0; i < row; i++) {
    int other_col = board[i];

    // 1. check vertical (same column)
    if (other_col == col) {
      return 0;
    }

    // 2. check diagonals
    // the vertical distance (row - i) must not equal the horizontal distance
    // |col - other_col|
    if (abs(other_col - col) == abs(i - row)) {
      return 0;
    }
  }
  return 1;
}

void solve(int board[], int row) {
  // base case: all queens placed
  if (row == SIZE) {
    pthread_mutex_lock(&mutex_sol);
    total_solutions++;
    print_board(board); // uncomment AND RECOMPILE to see solutions (slows down
                        // timing!!)
    pthread_mutex_unlock(&mutex_sol);
    return;
  }

  // recursive step: try all columns in current row
  for (int col = 0; col < SIZE; col++) {
    if (is_safe(board, row, col)) {
      board[row] = col;      // place queen
      solve(board, row + 1); // recurse
    }
  }
}

// worker: consumes tasks from the queue
void *worker(void *arg) {
  Task t;
  while (pop_task(&task_queue, &t)) {
    solve(t.board, t.start_row);
  }
  return NULL;
}

// producer: generates partial tasks up to a certain depth
void generate_tasks(int board[], int row) {
  // split depth: how deep the producer goes before handing off
  // depth 2 usually generates enough tasks (~40-50 for 8 queens)
  const int SPLIT_DEPTH = 2;

  if (row == SPLIT_DEPTH) {
    Task t;
    for (int i = 0; i < SIZE; i++)
      t.board[i] = board[i];
    t.start_row = row;
    push_task(&task_queue, t);
    return;
  }

  for (int col = 0; col < SIZE; col++) {
    if (is_safe(board, row, col)) {
      board[row] = col;
      generate_tasks(board, row + 1);
    }
  }
}

int main(int argc, char *argv[]) {
  if (argc != 2) {
    printf("Usage: %s <number_of_threads>\n", argv[0]);
    return 1;
  }

  int num_threads = atoi(argv[1]);
  if (num_threads <= 0) {
    printf("Error: Number of threads must be > 0\n");
    return 1;
  }

  pthread_t *threads = malloc(num_threads * sizeof(pthread_t));
  if (threads == NULL) {
    perror("Malloc failed");
    return 1;
  }

  // mutexes
  if (pthread_mutex_init(&mutex_sol, NULL) != 0) {
    perror("Mutex init failed");
    return 1;
  }
  init_queue(&task_queue);

  // start timing
  struct timeval start, end;
  gettimeofday(&start, NULL);

  // create threads (consumers)
  for (int i = 0; i < num_threads; i++) {
    if (pthread_create(&threads[i], NULL, worker, NULL) != 0) {
      perror("Thread creation failed");
      exit(1);
    }
  }

  // producer runs in main thread (main thread IS the producer)
  int initial_board[SIZE];
  generate_tasks(initial_board, 0);

  // signal done
  set_done(&task_queue);

  // join threads
  for (int i = 0; i < num_threads; i++) {
    pthread_join(threads[i], NULL);
  }

  // stop timing
  gettimeofday(&end, NULL);

  // calculate elapsed time
  double elapsed =
      (end.tv_sec - start.tv_sec) + (end.tv_usec - start.tv_usec) / 1000000.0;

  printf("Number of threads: %d\n", num_threads);
  printf("Total solutions found: %d\n", total_solutions);
  printf("Execution time: %.6f seconds\n", elapsed);

  // cleanup
  pthread_mutex_destroy(&mutex_sol);
  // destroy queue mutex/conds
  pthread_mutex_destroy(&task_queue.mutex);
  pthread_cond_destroy(&task_queue.cond_not_empty);
  pthread_cond_destroy(&task_queue.cond_not_full);
  free(threads);

  return 0;
}