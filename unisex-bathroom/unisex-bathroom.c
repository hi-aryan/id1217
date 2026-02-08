#include <fcntl.h> // For O_CREAT flag
#include <pthread.h>
#include <semaphore.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

// shared variables
int men_inside = 0;
int women_inside = 0;

// named semaphores for MacOS (pointers, not structs)
sem_t *mutex;
sem_t *bathroom;
sem_t *turnstile;

// semaphore names (unique!!)
const char SEM_MUTEX[] = "/bathroom_mutex";
const char SEM_BATHROOM[] = "/bathroom_access";
const char SEM_TURNSTILE[] = "/bathroom_turnstile";

void random_sleep(int min_ms, int max_ms) {
  int sleep_time = min_ms + rand() % (max_ms - min_ms);
  usleep(sleep_time * 1000);
}

void *woman_thread(void *arg) {
  int id = *(int *)arg;

  while (1) {
    printf("Woman %d is working\n", id);
    random_sleep(500, 2000);

    printf("Woman %d wants to use bathroom\n", id);

    // ENTRY PROTOCOL (deadlock-free)
    sem_wait(turnstile);
    sem_wait(mutex);

    int first_woman = (women_inside == 0);
    women_inside++; // Increment BEFORE releasing mutex

    sem_post(mutex); // Release mutex BEFORE waiting on bathroom!

    if (first_woman) {
      sem_wait(bathroom); // Safe: not holding mutex
    }

    sem_post(turnstile);

    // USE BATHROOM
    printf("  Woman %d ENTERED bathroom [%d women inside]\n", id, women_inside);
    random_sleep(100, 500);
    printf("  Woman %d LEAVING bathroom\n", id);

    // EXIT PROTOCOL
    sem_wait(mutex);
    women_inside--;

    if (women_inside == 0) {
      sem_post(bathroom);
    }

    sem_post(mutex);
  }

  return NULL;
}

void *man_thread(void *arg) {
  int id = *(int *)arg;

  while (1) {
    printf("Man %d is working\n", id);
    random_sleep(500, 2000);

    printf("Man %d wants to use bathroom\n", id);

    // ENTRY PROTOCOL (deadlock-free)
    sem_wait(turnstile);
    sem_wait(mutex);

    int first_man = (men_inside == 0); // C returns 0 = false, 1 = true
    men_inside++; // Increment BEFORE releasing mutex

    sem_post(mutex); // Release mutex BEFORE waiting on bathroom!

    if (first_man) {
      sem_wait(bathroom); // Safe: not holding mutex
    }

    sem_post(turnstile);

    // USE BATHROOM
    printf("  Man %d ENTERED bathroom [%d men inside]\n", id, men_inside);
    random_sleep(100, 500);
    printf("  Man %d LEAVING bathroom\n", id);

    // EXIT PROTOCOL
    sem_wait(mutex);
    men_inside--;

    if (men_inside == 0) {
      sem_post(bathroom);
    }

    sem_post(mutex);
  }

  return NULL;
}

int main(int argc, char *argv[]) {
  srand(time(NULL));

  int num_men = 3;
  int num_women = 3;

  if (argc == 3) {
    num_men = atoi(argv[1]);
    num_women = atoi(argv[2]);
  }

  // unlink any existing semaphores first
  sem_unlink(SEM_MUTEX);
  sem_unlink(SEM_BATHROOM);
  sem_unlink(SEM_TURNSTILE);

  // named semaphores for MacOS
  mutex = sem_open(SEM_MUTEX, O_CREAT, 0644, 1); // protects men_inside and women_inside
  bathroom = sem_open(SEM_BATHROOM, O_CREAT, 0644, 1); // gender exclusion
  turnstile = sem_open(SEM_TURNSTILE, O_CREAT, 0644, 1); // fairness

  // check for errors
  if (mutex == SEM_FAILED || bathroom == SEM_FAILED ||
      turnstile == SEM_FAILED) {
    perror("sem_open failure");
    exit(1);
  }

  pthread_t men[num_men];
  pthread_t women[num_women];
  int men_ids[num_men];
  int women_ids[num_women];

  printf("Bathroom Simulation (MacOS)\n");
  printf("Men: %d, Women: %d\n\n", num_men, num_women);

  for (int i = 0; i < num_men; i++) {
    men_ids[i] = i + 1;
    pthread_create(&men[i], NULL, man_thread, &men_ids[i]);
  }

  for (int i = 0; i < num_women; i++) {
    women_ids[i] = i + 1;
    pthread_create(&women[i], NULL, woman_thread, &women_ids[i]);
  }

  // wait for threads (they run forever)
  for (int i = 0; i < num_men; i++) {
    pthread_join(men[i], NULL);
  }
  for (int i = 0; i < num_women; i++) {
    pthread_join(women[i], NULL);
  }

  // cleanup named semaphores
  sem_close(mutex);
  sem_close(bathroom);
  sem_close(turnstile);
  sem_unlink(SEM_MUTEX);
  sem_unlink(SEM_BATHROOM);
  sem_unlink(SEM_TURNSTILE);

  return 0;
}

// compile
// gcc unisex-bathroom.c -lpthread -o unisex-bathroom
// ./unisex-bathroom 3 3
