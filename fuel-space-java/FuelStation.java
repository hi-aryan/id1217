public class FuelStation {
    private int nitrogen;
    private int quantum;
    private int freeDocks;
    private int activeRegularVehicles;
    private int activeSupplyVehicles;

    private final int MAX_NITROGEN;
    private final int MAX_QUANTUM;
    private final int MAX_DOCKS;

    public FuelStation(int maxN, int maxQ, int maxV, int numVehicles, int numSupply) {
        this.MAX_NITROGEN = maxN;
        this.MAX_QUANTUM = maxQ;
        this.MAX_DOCKS = maxV;
        this.nitrogen = maxN;
        this.quantum = maxQ;
        this.freeDocks = maxV;
        this.activeRegularVehicles = numVehicles;
        this.activeSupplyVehicles = numSupply;
    }

    // Regular vehicle (or supply return trip): wait for dock + sufficient fuel
    public synchronized void requestDockAndRefuel(int n, int q) throws InterruptedException {
        while (freeDocks == 0 || nitrogen < n || quantum < q) {
            if (activeSupplyVehicles == 0 && freeDocks > 0 && (nitrogen < n || quantum < q)) {
                break; // No more supply coming — take what's available
            }
            wait();
        }
        freeDocks--;
        nitrogen -= Math.min(n, nitrogen);
        quantum -= Math.min(q, quantum);
        notifyAll();
    }

    // Supply vehicle: deposit cargo and reserve return fuel atomically, then wait for dock.
    public synchronized void depositAndRefuel(int depositN, int depositQ,
            int returnN, int returnQ) throws InterruptedException {
        // Wait for space to deposit
        while (nitrogen >= MAX_NITROGEN && quantum >= MAX_QUANTUM) {
            if (activeRegularVehicles == 0) break;
            wait();
        }
        // Deposit cargo and reserve return fuel atomically
        nitrogen = Math.min(nitrogen + depositN, MAX_NITROGEN) - returnN;
        quantum = Math.min(quantum + depositQ, MAX_QUANTUM) - returnQ;
        notifyAll();

        // Wait for dock only (return fuel already reserved)
        while (freeDocks == 0) {
            wait();
        }
        freeDocks--;
        notifyAll();
    }

    public synchronized void regularVehicleDone() {
        activeRegularVehicles--;
        notifyAll();
    }

    public synchronized void supplyVehicleDone() {
        activeSupplyVehicles--;
        notifyAll();
    }

    // Release dock when leaving
    public synchronized void releaseDock() {
        freeDocks++;
        notifyAll();
    }

    public synchronized void printStatus() {
        System.out.printf("    [Station: N=%d/%d, Q=%d/%d, Docks=%d/%d]%n",
                nitrogen, MAX_NITROGEN, quantum, MAX_QUANTUM, freeDocks, MAX_DOCKS);
    }
}
