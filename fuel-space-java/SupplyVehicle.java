import java.util.Random;

/**
 * Supply vehicle that delivers fuel, then requests fuel for return trip.
 */
public class SupplyVehicle extends Thread {
    private final FuelStation station;
    private final String vehicleId;
    private final int nitrogenDelivery;
    private final int quantumDelivery;
    private final int nitrogenForReturn;
    private final int quantumForReturn;
    private final int numTrips;
    private final Random random;
    private final int maxTravelTime;
    private final int maxServiceTime;

    public SupplyVehicle(FuelStation station, int id, int nitrogenDelivery,
            int quantumDelivery, int nitrogenForReturn,
            int quantumForReturn, int numTrips,
            int maxTravelTime, int maxServiceTime) {
        this.station = station;
        this.vehicleId = "SupplyVehicle-" + id;
        this.nitrogenDelivery = nitrogenDelivery;
        this.quantumDelivery = quantumDelivery;
        this.nitrogenForReturn = nitrogenForReturn;
        this.quantumForReturn = quantumForReturn;
        this.numTrips = numTrips;
        this.random = new Random();
        this.maxTravelTime = maxTravelTime;
        this.maxServiceTime = maxServiceTime;
    }

    @Override
    public void run() {
        for (int trip = 1; trip <= numTrips; trip++) {
            try {
                int travelTime = random.nextInt(maxTravelTime) + 200;
                Thread.sleep(travelTime);

                boolean depositDockAcquired = station.depositFuel(nitrogenDelivery, quantumDelivery, vehicleId);
                if (!depositDockAcquired) {
                    return;
                }

                // Simulate pumping time for deposit
                try {
                    int serviceTime = random.nextInt(maxServiceTime) + 100;
                    Thread.sleep(serviceTime);
                } catch (InterruptedException e) {
                    station.releaseDock(vehicleId); // Ensure release if interrupted
                    throw e;
                }

                // Turnaround time (staying at dock)
                Thread.sleep(50);

                // Express Refuel (Atomic: already at dock)
                station.expressRefuel(nitrogenForReturn, quantumForReturn, vehicleId);

                // Simulate pumping time for refuel
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
