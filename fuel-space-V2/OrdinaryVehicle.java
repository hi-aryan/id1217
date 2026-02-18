import java.util.Random;

public class OrdinaryVehicle implements Runnable {
    private static final int MIN_TRAVEL_TIME = 400;
    private static final int MAX_TRAVEL_TIME = 600;
    private static final int MIN_DOCK_TIME = 50;
    private static final int MAX_DOCK_TIME = 150;
    private static final int MIN_FUEL_REQUEST = 50;
    private static final int MAX_FUEL_REQUEST = 150;

    private final int id;
    private final int trips;
    private final SpaceFuelStation station;
    private final Random rng = new Random();

    public OrdinaryVehicle(int id, int trips, SpaceFuelStation station) {
        this.id = id;
        this.trips = trips;
        this.station = station;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < trips; i++) {
                int travelTime = MIN_TRAVEL_TIME + rng.nextInt(MAX_TRAVEL_TIME);
                int dockTime = MIN_DOCK_TIME + rng.nextInt(MAX_DOCK_TIME);
                int fuelReq = MIN_FUEL_REQUEST + rng.nextInt(MAX_FUEL_REQUEST);

                Thread.sleep(travelTime);
                boolean success = station.refuelVehicle(id, "Ordinary", fuelReq, fuelReq, dockTime);
                if (!success) break;
            }
            Logger.logTrace(id, "Ordinary", "Completed operations. Shutting down.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            station.unregisterConsumer();
        }
    }
}