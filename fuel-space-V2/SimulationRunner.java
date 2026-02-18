void main() {
    int numOrdinaries = 5;
    int numSupplies = 2;
    int tripsPerVehicle = 3;

    SpaceFuelStation station = new SpaceFuelStation(3, 2000, 2000, numOrdinaries, numSupplies);
    LinkedList<Thread> fleet = new LinkedList<>();

    Logger.logTrace(0, "System", "--- STARTING SPACE STATION SIMULATION ---");

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