#include "FuelStation.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <sys/time.h>

FuelStation* station;

// Helper to get timestamp in milliseconds
long long currentTimeMillis() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000LL + tv.tv_usec / 1000;
}

struct VehicleArgs {
    int id;
    int trips;
    int nitrogenNeed;
    int quantumNeed;
};

struct SupplyArgs {
    int id;
    int trips;
    int nitrogenSupply;
    int quantumSupply;
    int nitrogenReturn;
    int quantumReturn;
};

void* regularVehicle(void* arg) {
    VehicleArgs* args = (VehicleArgs*)arg;
    
    for (int i = 0; i < args->trips; i++) {
        // Travel to station
        usleep((rand() % 1000 + 500) * 1000);  // 0.5-1.5 seconds
        
        printf("[%lld] Vehicle %d: Requesting %d nitrogen, %d quantum (trip %d/%d)\n",
               currentTimeMillis(), args->id, args->nitrogenNeed, args->quantumNeed, i+1, args->trips);
        
        station->requestDockAndFuel(args->nitrogenNeed, args->quantumNeed);
        
        printf("[%lld] Vehicle %d: Docked and refueling\n", currentTimeMillis(), args->id);
        station->printStatus();
        
        // Refueling time
        usleep((rand() % 300 + 100) * 1000);  // 0.1-0.4 seconds
        
        printf("[%lld] Vehicle %d: Departing\n", currentTimeMillis(), args->id);
        
        station->releaseDock();
    }
    
    delete args;
    return NULL;
}

void* supplyVehicle(void* arg) {
    SupplyArgs* args = (SupplyArgs*)arg;
    
    for (int i = 0; i < args->trips; i++) {
        // Travel to station with supplies
        usleep((rand() % 1000 + 500) * 1000);
        
        printf("[%lld] Supply %d: Arriving with %d nitrogen, %d quantum (trip %d/%d)\n",
               currentTimeMillis(), args->id, args->nitrogenSupply, args->quantumSupply, i+1, args->trips);
        
        station->requestDockAndDeposit(args->nitrogenSupply, args->quantumSupply);
        
        printf("[%lld] Supply %d: Docked and depositing fuel\n", currentTimeMillis(), args->id);
        station->printStatus();
        
        // Depositing time
        usleep((rand() % 300 + 100) * 1000);
        
        printf("[%lld] Supply %d: Requesting return fuel: %d nitrogen, %d quantum\n",
               currentTimeMillis(), args->id, args->nitrogenReturn, args->quantumReturn);
        
        station->requestFuel(args->nitrogenReturn, args->quantumReturn);
        
        printf("[%lld] Supply %d: Refueling for return trip\n", currentTimeMillis(), args->id);
        station->printStatus();
        
        // Refueling time
        usleep((rand() % 300 + 100) * 1000);
        
        printf("[%lld] Supply %d: Departing\n", currentTimeMillis(), args->id);
        
        station->releaseDock();
    }
    
    delete args;
    return NULL;
}

int main(int argc, char* argv[]) {
    if (argc < 6) {
        printf("Usage: %s <max_nitrogen> <max_quantum> <max_docks> <num_vehicles> <num_supply> [trips] [seed]\n", argv[0]);
        printf("Example: %s 100 100 3 5 2 3 42\n", argv[0]);
        return 1;
    }
    
    int maxNitrogen = atoi(argv[1]);
    int maxQuantum = atoi(argv[2]);
    int maxDocks = atoi(argv[3]);
    int numVehicles = atoi(argv[4]);
    int numSupply = atoi(argv[5]);
    int trips = argc > 6 ? atoi(argv[6]) : 3;
    int seed = argc > 7 ? atoi(argv[7]) : (int)time(NULL);
    
    srand(seed);
    
    station = new FuelStation(maxNitrogen, maxQuantum, maxDocks);
    
    printf("=== Fuel Space Station Simulation ===\n");
    printf("Max Nitrogen: %d, Max Quantum: %d, Max Docks: %d\n", maxNitrogen, maxQuantum, maxDocks);
    printf("Regular Vehicles: %d, Supply Vehicles: %d, Trips: %d\n\n", numVehicles, numSupply, trips);
    
    pthread_t* threads = new pthread_t[numVehicles + numSupply];
    
    // Create regular vehicles
    for (int i = 0; i < numVehicles; i++) {
        VehicleArgs* args = new VehicleArgs();
        args->id = i + 1;
        args->trips = trips;
        args->nitrogenNeed = rand() % 20 + 5;   // 5-24 units
        args->quantumNeed = rand() % 20 + 5;    // 5-24 units
        pthread_create(&threads[i], NULL, regularVehicle, args);
    }
    
    // Create supply vehicles
    for (int i = 0; i < numSupply; i++) {
        SupplyArgs* args = new SupplyArgs();
        args->id = i + 1;
        args->trips = trips;
        args->nitrogenSupply = rand() % 30 + 40;    // 40-69 units (large supply) 
        // ^ possible race condition, lower to % 20 + 20
        args->quantumSupply = rand() % 30 + 40;     // 40-69 units
        // ^ possible race condition, lower to % 20 + 20
        args->nitrogenReturn = rand() % 15 + 5;     // 5-19 units (return fuel)
        args->quantumReturn = rand() % 15 + 5;      // 5-19 units
        pthread_create(&threads[numVehicles + i], NULL, supplyVehicle, args);
    }
    
    // Wait for all threads to complete
    for (int i = 0; i < numVehicles + numSupply; i++) {
        pthread_join(threads[i], NULL);
    }
    
    printf("\n=== Simulation Complete ===\n");
    station->printStatus();
    
    delete station;
    delete[] threads;
    
    return 0;
}
