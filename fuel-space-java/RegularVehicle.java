import java.util.Random;

public class RegularVehicle implements Runnable {
    private final int id;
    private final int trips;
    private final int nitrogenNeed;
    private final int quantumNeed;
    private final FuelStation station;
    private final Random random;

    public RegularVehicle(int id, int trips, int nitrogenNeed, int quantumNeed,
            FuelStation station, Random random) {
        this.id = id;
        this.trips = trips;
        this.nitrogenNeed = nitrogenNeed;
        this.quantumNeed = quantumNeed;
        this.station = station;
        this.random = random;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < trips; i++) {
                // Travel to station (random delay)
                Thread.sleep(random.nextInt(1000) + 500);

                System.out.printf("[%d] Vehicle %d: Requesting %d nitrogen, %d quantum (trip %d/%d)%n",
                        System.currentTimeMillis(), id, nitrogenNeed, quantumNeed, i + 1, trips);

                // Request dock and fuel atomically
                station.requestDockAndRefuel(nitrogenNeed, quantumNeed);

                System.out.printf("[%d] Vehicle %d: Docked and refueling%n",
                        System.currentTimeMillis(), id);
                station.printStatus();

                // Refueling time
                Thread.sleep(random.nextInt(300) + 100);

                System.out.printf("[%d] Vehicle %d: Departing%n",
                        System.currentTimeMillis(), id);

                // Release dock
                station.releaseDock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("Vehicle %d interrupted%n", id);
        }
    }
}
