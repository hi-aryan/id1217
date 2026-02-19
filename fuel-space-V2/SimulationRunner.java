import java.util.LinkedList;

void main(String[] args) {
    int numOrdinaries = 5;
    int numSupplies = 2;
    int tripsPerVehicle = 3;
    int stationCapN = 2000;
    int stationCapQ = 2000;
    int numDocks = 3;

    if (args.length > 0) {
        try {
            tripsPerVehicle = Integer.parseInt(args[0]);
            if (args.length > 1)
                numOrdinaries = Integer.parseInt(args[1]);
            if (args.length > 2)
                numSupplies = Integer.parseInt(args[2]);
            if (args.length > 3)
                stationCapN = Integer.parseInt(args[3]);
            if (args.length > 4)
                stationCapQ = Integer.parseInt(args[4]);
            if (args.length > 5)
                numDocks = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            System.err
                    .println("Usage: java SimulationRunner.java [trips] [ordinaries] [supplies] [capN] [capQ] [docks]");
            System.exit(1);
        }
    }

    SpaceFuelStation station = new SpaceFuelStation(numDocks, stationCapN, stationCapQ, numOrdinaries, numSupplies);
    LinkedList<Thread> fleet = new LinkedList<>();

    Logger.logTrace(0, "System", "--- STARTING SPACE STATION SIMULATION ---");
    Logger.logTrace(0, "Config", String.format("Trips:%d, Ord:%d, Sup:%d, CapN:%d, CapQ:%d, Docks:%d",
            tripsPerVehicle, numOrdinaries, numSupplies, stationCapN, stationCapQ, numDocks));

    // Spawn Ordinary Vehicles
    for (int i = 1; i <= numOrdinaries; i++) {
        Thread t = new Thread(new OrdinaryVehicle(i, tripsPerVehicle, station));
        fleet.add(t);
        t.start();
    }

    // Spawn Supply Vehicles
    for (int i = 1; i <= numSupplies; i++) {
        Thread t = new Thread(new SupplyVehicle(i, tripsPerVehicle, station));
        fleet.add(t);
        t.start();
    }

    // Wait for all threads to finish
    for (Thread t : fleet) {
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    Logger.logTrace(0, "System", "--- SIMULATION COMPLETE. ALL VEHICLES GROUNDED ---");
}