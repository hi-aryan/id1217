import java.util.Random;

/**
 * Regular vehicle that periodically visits the station to request fuel.
 */
public class RegularVehicle extends Thread {
    private final FuelStation station;
    private final String vehicleId;
    private final int nitrogenNeeded;
    private final int quantumNeeded;
    private final int numTrips;
    private final Random random;
    private final int maxTravelTime;
    private final int maxServiceTime;

    public RegularVehicle(FuelStation station, int id, int nitrogenNeeded,
            int quantumNeeded, int numTrips,
            int maxTravelTime, int maxServiceTime) {
        this.station = station;
        this.vehicleId = "Vehicle-" + id;
        this.nitrogenNeeded = nitrogenNeeded;
        this.quantumNeeded = quantumNeeded;
        this.numTrips = numTrips;
        this.random = new Random();
        this.maxTravelTime = maxTravelTime;
        this.maxServiceTime = maxServiceTime;
    }

    @Override
    public void run() {
        for (int trip = 1; trip <= numTrips; trip++) {
            try {
                int travelTime = random.nextInt(maxTravelTime) + 100;
                Thread.sleep(travelTime);

                boolean dockAcquired = station.requestFuel(nitrogenNeeded, quantumNeeded, vehicleId);
                if (!dockAcquired) {
                    return;
                }

                try {
                    int serviceTime = random.nextInt(maxServiceTime) + 50;
                    Thread.sleep(serviceTime);
                } finally {
                    station.releaseDock(vehicleId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
