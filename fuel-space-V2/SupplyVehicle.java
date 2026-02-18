import java.util.Random;

public class SupplyVehicle implements Runnable {
    private static final int MIN_TRAVEL_TIME = 800;
    private static final int MAX_TRAVEL_TIME = 700;
    private static final int MIN_DOCK_TIME = 100;
    private static final int MAX_DOCK_TIME = 200;
    private static final int DEPOSIT_NITROGEN = 600;
    private static final int DEPOSIT_QUANTUM = 600;
    private static final int RETURN_NITROGEN = 50;
    private static final int RETURN_QUANTUM = 50;

    private final int id;
    private final int trips;
    private final SpaceFuelStation station;
    private final Random rng = new Random();

    public SupplyVehicle(int id, int trips, SpaceFuelStation station) {
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

                Thread.sleep(travelTime);
                boolean success = station.supplyStation(id, DEPOSIT_NITROGEN, DEPOSIT_QUANTUM,
                                                        RETURN_NITROGEN, RETURN_QUANTUM, dockTime);
                if (!success) break;
            }
            Logger.logTrace(id, "Supply", "Completed operations. Shutting down.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            station.unregisterProducer();
        }
    }
}