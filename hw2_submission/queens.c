/**
 * 8-Queens Problem - OpenMP Solution
 *
 * Homework 2: Parallel programming with OpenMP
 *
 * This solution uses OpenMP tasks to parallelize the N-Queens solver.
 * The master thread recursively generates partial queen placements,
 * and spawns tasks for worker threads to complete the solutions.
 *
 * Compile: gcc -fopenmp queens.c -o queens -DN=8
 * Run: ./queens <number_of_threads> <number_of_runs>
 * Or set: export OMP_NUM_THREADS=4 && ./queens
 */

#include <omp.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef N
#define N 8 // Board size / Number of queens
#endif

// Global solution counter
static int totalSolutions = 0;

/**
 * Recursive solver using backtracking.
 * Uses O(1) lookup arrays for attack detection.
 *
 * @param r         Current row being processed
 * @param col       Column occupancy lookup
 * @param posDiag   Positive diagonal (r+c) occupancy lookup
 * @param negDiag   Negative diagonal (r-c+N) occupancy lookup
 * @param count     Pointer to local solution counter
 */
void solve(const int r, bool col[], bool posDiag[], bool negDiag[],
           int *count) {
  // Base case: all queens placed successfully
  if (r == N) {
    (*count)++;
    return;
  }

  // Try placing queen in each column of current row
  for (int c = 0; c < N; c++) {
    // O(1) safety check using lookup arrays
    if (!col[c] && !posDiag[r + c] && !negDiag[r - c + N]) {
      // Place queen (update lookups)
      col[c] = true;
      posDiag[r + c] = true;
      negDiag[r - c + N] = true;

      // Recurse to next row
      solve(r + 1, col, posDiag, negDiag, count);

      // Backtrack (undo lookups)
      col[c] = false;
      posDiag[r + c] = false;
      negDiag[r - c + N] = false;
    }
  }
}

/**
 * Recursive task generator.
 * Explores the search tree up to a certain depth, then spawns OpenMP tasks.
 * Each task independently completes the remaining solution search.
 *
 * @param r         Current row
 * @param col       Column occupancy lookup
 * @param posDiag   Positive diagonal occupancy lookup
 * @param negDiag   Negative diagonal occupancy lookup
 * @param depth     Depth at which to spawn tasks
 */
void generateTasks(const int r, bool col[], bool posDiag[], bool negDiag[],
                   int depth) {
  // When we reach the target depth, spawn a task to complete this subtree
  if (r == depth) {
    // Capture current state for the task (each task needs its own copy)
    bool taskCol[N];
    bool taskPosDiag[2 * N];
    bool taskNegDiag[2 * N];
    memcpy(taskCol, col, sizeof(bool) * N);
    memcpy(taskPosDiag, posDiag, sizeof(bool) * 2 * N);
    memcpy(taskNegDiag, negDiag, sizeof(bool) * 2 * N);

#pragma omp task firstprivate(taskCol, taskPosDiag, taskNegDiag)
    {
      int localCount = 0;
      solve(r, taskCol, taskPosDiag, taskNegDiag, &localCount);

// Atomically add to global counter
#pragma omp atomic
      totalSolutions += localCount;
    }
    return;
  }

  // Recursively explore and generate tasks
  for (int c = 0; c < N; c++) {
    if (!col[c] && !posDiag[r + c] && !negDiag[r - c + N]) {
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

/**
 * Calculate optimal task generation depth based on board size.
 * More tasks = better load balancing but more overhead.
 */
int getOptimalDepth(int n) {
  if (n < 12)
    return 2; // N=8-11: ~42-70 tasks
  if (n < 15)
    return 3; // N=12-14: ~800-2000 tasks
  return 4;   // N=15+: ~3000+ tasks
}

/**
 * Run the solver once and return execution time.
 */
double runOnce(int numThreads) {
  // Reset global counter
  totalSolutions = 0;

  // Initialize empty board state
  bool col[N] = {false};
  bool posDiag[2 * N] = {false};
  bool negDiag[2 * N] = {false};

  int depth = getOptimalDepth(N);

  double startTime = omp_get_wtime();

#pragma omp parallel num_threads(numThreads)
  {
#pragma omp single
    {
      generateTasks(0, col, posDiag, negDiag, depth);
    }
    // Implicit barrier and taskwait at end of parallel region
  }

  double endTime = omp_get_wtime();

  return endTime - startTime;
}

/**
 * Compare function for qsort (used to find median).
 */
int compareDoubles(const void *a, const void *b) {
  double da = *(const double *)a;
  double db = *(const double *)b;
  if (da < db)
    return -1;
  if (da > db)
    return 1;
  return 0;
}

/**
 * Run multiple trials and return median execution time.
 */
double runTrials(int numThreads, int numRuns) {
  double *times = malloc(numRuns * sizeof(double));

  for (int i = 0; i < numRuns; i++) {
    times[i] = runOnce(numThreads);
  }

  // Sort to find median
  qsort(times, numRuns, sizeof(double), compareDoubles);

  double median = times[numRuns / 2];
  free(times);

  return median;
}

/**
 * Main entry point.
 *
 * Usage: ./queens [max_threads] [num_runs]
 *   max_threads: Maximum number of threads to test (default: 4)
 *   num_runs: Number of runs per thread count for median (default: 5)
 */
int main(int argc, char *argv[]) {
  int maxThreads = 4;
  int numRuns = 5;

  if (argc >= 2) {
    maxThreads = atoi(argv[1]);
    if (maxThreads <= 0) {
      printf("Error: max_threads must be positive\n");
      return 1;
    }
  }
  if (argc >= 3) {
    numRuns = atoi(argv[2]);
    if (numRuns <= 0) {
      printf("Error: num_runs must be positive\n");
      return 1;
    }
  }

  printf("=== N-Queens OpenMP Solver ===\n");
  printf("Board Size (N): %d\n", N);
  printf("Task Depth: %d\n", getOptimalDepth(N));
  printf("Runs per thread count: %d (using median)\n", numRuns);
  printf("\n");

  // Run sequential version first to establish baseline
  printf("Running sequential (1 thread) baseline...\n");
  double seqTime = runTrials(1, numRuns);
  printf("Sequential time: %.6f seconds\n", seqTime);
  printf("Solutions found: %d\n\n", totalSolutions);

  // Print header for results table
  printf("%-10s %-15s %-10s\n", "Threads", "Time (s)", "Speedup");
  printf("%-10s %-15s %-10s\n", "-------", "--------", "-------");
  printf("%-10d %-15.6f %-10.2f\n", 1, seqTime, 1.0);

  // Test with increasing thread counts
  for (int t = 2; t <= maxThreads; t++) {
    double parTime = runTrials(t, numRuns);
    double speedup = seqTime / parTime;
    printf("%-10d %-15.6f %-10.2f\n", t, parTime, speedup);
  }

  printf("\n");
  printf("Verification: Found %d solutions (expected 92 for N=8)\n",
         totalSolutions);

  return 0;
}
