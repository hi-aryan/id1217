# homework 1: 8-queens (bag of tasks)
**student name:** aryan johansson begdeli & rafael fernandes

## solution description

we implemented a bag of tasks (with producer-consumer pattern) using posix threads (`pthreads`) in c.

### design
*   **producer (main thread):** generates partial board configurations ("tasks") by placing queens in the first 2 rows (depth 2). these partial states are pushed into a shared `task_queue`.
*   **bag of tasks (queue):** a thread-safe queue protected by a mutex (`queue.mutex`) and condition variables (`cond_not_empty`, `cond_not_full`) to handle synchronization.
*   **consumers (worker threads):** multiple worker threads pull tasks from the queue and solve the remaining rows (3-8) using a recursive backtracking algorithm.
*   **synchronization:** a separate mutex (`mutex_sol`) protects the global `total_solutions` counter to ensure accurate results.

### implementation details
*   **data structures:** a `queue` struct holding an array of `task` objects (partial boards).
*   **base case:** when 8 queens are placed safely, we increment the solution counter.
*   **termination:** the producer sets a `done_producing` flag and broadcasts to all consumers once all initial partial tasks are generated.

## performance evaluation

we measured the execution time for finding all 92 solutions of the 8-queens problem using varying numbers of threads. to ensure accurate timing, printing of board solutions was disabled during these tests.

### results

| threads | execution time (s) | speedup (vs 1 thread) |
|---------|--------------------|-----------------------|
| 1       | 0.000483           | 1.0x                  |
| 2       | 0.000260           | 1.86x                 |
| 3       | 0.000255           | 1.89x                 |
| 4       | 0.000182           | 2.65x                 |
| 5       | 0.000278           | 1.73x                 |
| 6       | 0.000228           | 2.11x                 |
| 7       | 0.000232           | 2.08x                 |
| 8       | 0.000433 (avg)     | 1.11x                 |

### analysis
*   **peak performance:** the best performance was observed at **4 threads** (~0.000182s), achieving a speedup of roughly 2.65x.
*   **diminishing returns:** beyond 4 threads, the overhead of context switching and contention for the shared queue lock likely started to outweigh the benefits of parallelism for this relatively small problem size.
*   **overhead:** at 8 threads, performance degraded significantly, likely due to high contention on the mutex relative to the very short work duration of each task.
