#ifndef FUEL_STATION_H
#define FUEL_STATION_H

#include <pthread.h>

class FuelStation {
private:
    pthread_mutex_t mutex;
    pthread_cond_t condition;
    
    int nitrogen;
    int quantum;
    int freeDocks;
    
    const int MAX_NITROGEN;
    const int MAX_QUANTUM;
    const int MAX_DOCKS;

public:
    FuelStation(int maxN, int maxQ, int maxV);
    ~FuelStation();
    
    // Regular vehicle: atomically wait for dock + fuel
    void requestDockAndFuel(int n, int q);
    
    // Supply vehicle: atomically wait for dock + storage space, then deposit
    void requestDockAndDeposit(int n, int q);
    
    // Supply vehicle: wait for return fuel (already has dock)
    void requestFuel(int n, int q);
    
    // All vehicles: release dock when leaving
    void releaseDock();
    
    void printStatus();
};

#endif
