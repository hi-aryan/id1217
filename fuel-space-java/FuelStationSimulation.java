/**
 * Main simulation program for the Fuel Space Station.
 */
public class FuelStationSimulation {
    public static void main(String[] args) {
        int maxDocks = 3;
        int maxNitrogen = 1000;
        int maxQuantum = 1000;
        int numRegularVehicles = 5;
        int numSupplyVehicles = 2;
        int numTrips = 3;

        try {
            if (args.length > 0) {
                numTrips = Integer.parseInt(args[0]);
            }
            if (args.length > 1) {
                maxDocks = Integer.parseInt(args[1]);
            }
            if (args.length > 2) {
                maxNitrogen = Integer.parseInt(args[2]);
            }
            if (args.length > 3) {
                maxQuantum = Integer.parseInt(args[3]);
            }
            if (args.length > 4) {
                numRegularVehicles = Integer.parseInt(args[4]);
            }
            if (args.length > 5) {
                numSupplyVehicles = Integer.parseInt(args[5]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid numeric argument.");
            System.err.println("Usage: java FuelStationSimulation [numTrips] [maxDocks] [maxNitrogen] [maxQuantum] [numRegularVehicles] [numSupplyVehicles]");
            return;
        }

        if (numTrips < 0 || maxDocks <= 0 || maxNitrogen <= 0 || maxQuantum <= 0
                || numRegularVehicles < 0 || numSupplyVehicles < 0) {
            System.err.println("Invalid configuration: use positive capacities/docks and non-negative counts/trips.");
            return;
        }

        int regularNitrogen = Math.max(1, maxNitrogen / 20);
        int regularQuantum = Math.max(1, maxQuantum / 20);

        int supplyReturnNitrogen = Math.max(1, regularNitrogen / 2);
        int supplyReturnQuantum = Math.max(1, regularQuantum / 2);

        int supplyNitrogen = Math.max(1, maxNitrogen / 3);
        int supplyQuantum = Math.max(1, maxQuantum / 3);
        int totalDepositEvents = numSupplyVehicles * numTrips;

        if (totalDepositEvents > 0) {
            // Keep deliveries feasible for strict full-space deposits.
            int totalRegularDemandN = numRegularVehicles * numTrips * regularNitrogen;
            int totalRegularDemandQ = numRegularVehicles * numTrips * regularQuantum;
            int maxPerDepositN = Math.max(1, totalRegularDemandN / totalDepositEvents);
            int maxPerDepositQ = Math.max(1, totalRegularDemandQ / totalDepositEvents);

            supplyNitrogen = Math.min(supplyNitrogen, maxPerDepositN);
            supplyQuantum = Math.min(supplyQuantum, maxPerDepositQ);
        }

        int initialNitrogen = maxNitrogen;
        int initialQuantum = maxQuantum;
        if (totalDepositEvents > 0) {
            // Guarantee at least one delivery can fit from the start.
            initialNitrogen = Math.max(0, maxNitrogen - supplyNitrogen);
            initialQuantum = Math.max(0, maxQuantum - supplyQuantum);
        }

        System.out.println("=== FUEL SPACE STATION SIMULATION ===");
        System.out.printf("Station capacity: %d docks, %dL nitrogen, %dL quantum%n",
                maxDocks, maxNitrogen, maxQuantum);
        System.out.printf("Initial fuel: %dL nitrogen, %dL quantum%n",
                initialNitrogen, initialQuantum);
        System.out.printf("Vehicles: %d regular, %d supply%n",
                numRegularVehicles, numSupplyVehicles);
        System.out.printf("Trips per vehicle: %d%n%n", numTrips);

        FuelStation station = new FuelStation(maxDocks, maxNitrogen, maxQuantum, initialNitrogen, initialQuantum);

        int maxTravelTime = 1000;
        int maxServiceTime = 300;

        Thread[] regularVehicles = new Thread[numRegularVehicles];
        for (int i = 0; i < numRegularVehicles; i++) {
            regularVehicles[i] = new RegularVehicle(
                    station, i + 1, regularNitrogen, regularQuantum,
                    numTrips, maxTravelTime, maxServiceTime);
            regularVehicles[i].start();
        }

        Thread[] supplyVehicles = new Thread[numSupplyVehicles];
        for (int i = 0; i < numSupplyVehicles; i++) {
            supplyVehicles[i] = new SupplyVehicle(
                    station, i + 1, supplyNitrogen, supplyQuantum,
                    supplyReturnNitrogen, supplyReturnQuantum,
                    numTrips, maxTravelTime, maxServiceTime);
            supplyVehicles[i].start();
        }

        try {
            for (Thread vehicle : regularVehicles) {
                vehicle.join();
            }
            for (Thread vehicle : supplyVehicles) {
                vehicle.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("=== SIMULATION COMPLETE ===");
    }
}
