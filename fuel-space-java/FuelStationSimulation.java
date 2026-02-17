import java.util.Random;

public class FuelStationSimulation {
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println(
                    "Usage: java FuelStationSimulation <maxNitrogen> <maxQuantum> <maxDocks> <numVehicles> <numSupply> <trips> [seed]");
            System.err.println("Example: java FuelStationSimulation 100 100 3 5 2 3 42");
            System.exit(1);
        }

        int maxNitrogen = Integer.parseInt(args[0]);
        int maxQuantum = Integer.parseInt(args[1]);
        int maxDocks = Integer.parseInt(args[2]);
        int numVehicles = Integer.parseInt(args[3]);
        int numSupply = Integer.parseInt(args[4]);
        int trips = Integer.parseInt(args[5]);
        long seed = args.length > 6 ? Long.parseLong(args[6]) : System.currentTimeMillis();

        Random random = new Random(seed);
        FuelStation station = new FuelStation(maxNitrogen, maxQuantum, maxDocks, numVehicles, numSupply);

        System.out.println("=== Fuel Space Station Simulation ===");
        System.out.printf("Max Nitrogen: %d, Max Quantum: %d, Max Docks: %d%n",
                maxNitrogen, maxQuantum, maxDocks);
        System.out.printf("Regular Vehicles: %d, Supply Vehicles: %d, Trips: %d, Seed: %d%n%n",
                numVehicles, numSupply, trips, seed);

        Thread[] threads = new Thread[numVehicles + numSupply];

        // Create regular vehicles
        for (int i = 0; i < numVehicles; i++) {
            int nitrogenNeed = random.nextInt(15) + 5; // 5-19 units
            int quantumNeed = random.nextInt(15) + 5; // 5-19 units
            threads[i] = new Thread(new RegularVehicle(i + 1, trips, nitrogenNeed,
                    quantumNeed, station, new Random(random.nextLong())));
            threads[i].setName("Vehicle-" + (i + 1));
        }

        // Create supply vehicles
        for (int i = 0; i < numSupply; i++) {
            // Supply 50-65% of capacity (far exceeds vehicle needs)
            int nitrogenSupply = random.nextInt((int) (maxNitrogen * 0.15)) + (int) (maxNitrogen * 0.5);
            int quantumSupply = random.nextInt((int) (maxQuantum * 0.15)) + (int) (maxQuantum * 0.5);
            int nitrogenReturn = random.nextInt(15) + 5; // 5-19 units (like a regular vehicle)
            int quantumReturn = random.nextInt(15) + 5; // 5-19 units (like a regular vehicle)

            threads[numVehicles + i] = new Thread(new SupplyVehicle(i + 1, trips,
                    nitrogenSupply, quantumSupply, nitrogenReturn, quantumReturn,
                    station, new Random(random.nextLong())));
            threads[numVehicles + i].setName("Supply-" + (i + 1));
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all threads to complete
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted");
            Thread.currentThread().interrupt();
        }

        System.out.println("\n=== Simulation Complete ===");
        station.printStatus();
    }
}

// Compile all files
// javac FuelStation.java RegularVehicle.java SupplyVehicle.java
// FuelStationSimulation.java

// Run simulation
// java FuelStationSimulation 100 100 3 5 2 3 42

// Parameters: <nitrogen> <quantum> <docks> <vehicles> <supplies> <trips> [seed]