import java.util.Random;

public class SupplyVehicle implements Runnable {
    private final int id;
    private final int trips;
    private final int nitrogenSupply;
    private final int quantumSupply;
    private final int nitrogenReturn;
    private final int quantumReturn;
    private final FuelStation station;
    private final Random random;

    public SupplyVehicle(int id, int trips, int nitrogenSupply, int quantumSupply,
            int nitrogenReturn, int quantumReturn,
            FuelStation station, Random random) {
        this.id = id;
        this.trips = trips;
        this.nitrogenSupply = nitrogenSupply;
        this.quantumSupply = quantumSupply;
        this.nitrogenReturn = nitrogenReturn;
        this.quantumReturn = quantumReturn;
        this.station = station;
        this.random = random;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < trips; i++) {
                // Travel to station with supplies
                Thread.sleep(random.nextInt(1000) + 500);

                System.out.printf("[%d] Supply %d: Arriving with %d nitrogen, %d quantum (trip %d/%d)%n",
                        System.currentTimeMillis(), id, nitrogenSupply, quantumSupply, i + 1, trips);

                // Deposit net fuel (gross minus return; supply keeps return fuel in own tanks)
                int netN = nitrogenSupply - nitrogenReturn;
                int netQ = quantumSupply - quantumReturn;
                station.depositFuel(netN, netQ);

                System.out.printf("[%d] Supply %d: Deposited net fuel (%d N, %d Q) — kept return (%d N, %d Q)%n",
                        System.currentTimeMillis(), id, netN, netQ, nitrogenReturn, quantumReturn);
                station.printStatus();

                // Service time
                Thread.sleep(random.nextInt(300) + 100);

                System.out.printf("[%d] Supply %d: Departing%n",
                        System.currentTimeMillis(), id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("Supply %d interrupted%n", id);
        }
    }
}
