#include "FuelStation.h"
#include <stdio.h>
#include <time.h>

FuelStation::FuelStation(int maxN, int maxQ, int maxV)
    : nitrogen(maxN), quantum(maxQ), freeDocks(maxV),
      MAX_NITROGEN(maxN), MAX_QUANTUM(maxQ), MAX_DOCKS(maxV) {
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&condition, NULL);
}

FuelStation::~FuelStation() {
    pthread_mutex_destroy(&mutex);
    pthread_cond_destroy(&condition);
}

void FuelStation::requestDockAndFuel(int n, int q) {
    pthread_mutex_lock(&mutex);
    
    // Wait atomically for dock AND sufficient fuel
    // This prevents blocking: vehicle only takes dock when it can proceed
    while (freeDocks == 0 || nitrogen < n || quantum < q) {
        pthread_cond_wait(&condition, &mutex);
    }
    
    freeDocks--;
    nitrogen -= n;
    quantum -= q;
    
    pthread_mutex_unlock(&mutex);
}

void FuelStation::requestDockAndDeposit(int n, int q) {
    pthread_mutex_lock(&mutex);
    
    // Wait atomically for dock AND sufficient storage space
    while (freeDocks == 0 || nitrogen + n > MAX_NITROGEN || quantum + q > MAX_QUANTUM) {
        pthread_cond_wait(&condition, &mutex);
    }
    
    freeDocks--;
    nitrogen += n;
    quantum += q;
    
    // Wake all waiters since fuel is now available
    pthread_cond_broadcast(&condition);
    
    pthread_mutex_unlock(&mutex);
}

void FuelStation::requestFuel(int n, int q) {
    pthread_mutex_lock(&mutex);
    
    // Supply vehicle already has dock, just waiting for fuel
    while (nitrogen < n || quantum < q) {
        pthread_cond_wait(&condition, &mutex);
    }
    
    nitrogen -= n;
    quantum -= q;
    
    pthread_mutex_unlock(&mutex);
}

void FuelStation::releaseDock() {
    pthread_mutex_lock(&mutex);
    
    freeDocks++;
    
    // Wake all waiters since dock is now available
    pthread_cond_broadcast(&condition);
    
    pthread_mutex_unlock(&mutex);
}

void FuelStation::printStatus() {
    pthread_mutex_lock(&mutex);
    printf("    [Station: N=%d/%d, Q=%d/%d, Docks=%d/%d]\n",
           nitrogen, MAX_NITROGEN, quantum, MAX_QUANTUM, freeDocks, MAX_DOCKS);
    pthread_mutex_unlock(&mutex);
}
