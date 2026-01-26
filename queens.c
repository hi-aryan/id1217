/*
 * Homework 1: Problem 7 (8-Queens)
 * Author: [Your Name]
 * Date: [Today's Date]
 * 
 * Description: Solves the 8-Queens problem using Pthreads and a 
 * Bag-of-Tasks approach for domain decomposition.
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/time.h>

#define SIZE 8  // board size (8x8)

// shared global variables
int total_solutions = 0;
pthread_mutex_t mutex_sol;

// BAG OF TASKS: tracks which column (0-7) of row 0 needs to be processed next
int current_start_col = 0;
pthread_mutex_t mutex_task;

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
        // the vertical distance (row - i) must not equal the horizontal distance |col - other_col|
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
        print_board(board); // uncomment AND RECOMPILE to see solutions (slows down timing!!)
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

// worker
void* worker(void* arg) {
    int board[SIZE]; // thread-local

    while (1) {
        int my_col;

        // 1. CS: get a task from the bag
        pthread_mutex_lock(&mutex_task);
        my_col = current_start_col;
        current_start_col++;
        pthread_mutex_unlock(&mutex_task);

        // 2. check termination condition
        if (my_col >= SIZE) {
            break; // bag is empty
        }

        // 3. execute the task
        // place first queen at (row 0, my_col)
        board[0] = my_col;
   
        // start solving from row 1 cuz row 0 is already placed
        solve(board, 1);
    }
    return NULL;
}

int main(int argc, char* argv[]) {
    if (argc != 2) {
        printf("Usage: %s <number_of_threads>\n", argv[0]);
        return 1;
    }

    int num_threads = atoi(argv[1]);
    if (num_threads <= 0) {
        printf("Error: Number of threads must be > 0\n");
        return 1;
    }

    pthread_t* threads = malloc(num_threads * sizeof(pthread_t));
    if (threads == NULL) {
        perror("Malloc failed");
        return 1;
    }

    // mutexes
    if (pthread_mutex_init(&mutex_sol, NULL) != 0 || 
        pthread_mutex_init(&mutex_task, NULL) != 0) {
        perror("Mutex init failed");
        return 1;
    }

    // start timing
    struct timeval start, end;
    gettimeofday(&start, NULL);

    // create threads
    for (int i = 0; i < num_threads; i++) {
        if (pthread_create(&threads[i], NULL, worker, NULL) != 0) {
            perror("Thread creation failed");
            exit(1);
        }
    }

    // join threads
    for (int i = 0; i < num_threads; i++) {
        pthread_join(threads[i], NULL);
    }

    // stop timing
    gettimeofday(&end, NULL);

    // calculate elapsed time
    double elapsed = (end.tv_sec - start.tv_sec) + 
                     (end.tv_usec - start.tv_usec) / 1000000.0;

    printf("Number of threads: %d\n", num_threads);
    printf("Total solutions found: %d\n", total_solutions);
    printf("Execution time: %.6f seconds\n", elapsed);

    // cleanup
    pthread_mutex_destroy(&mutex_sol);
    pthread_mutex_destroy(&mutex_task);
    free(threads);

    return 0;
}