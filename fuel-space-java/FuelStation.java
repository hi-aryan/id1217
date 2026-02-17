public class FuelStation {
    private int nitrogen;
    private int quantum;
    private int freeDocks;
    private int activeRegularVehicles;

    private final int MAX_NITROGEN;
    private final int MAX_QUANTUM;
    private final int MAX_DOCKS;

    public FuelStation(int maxN, int maxQ, int maxV, int numVehicles) {
        this.MAX_NITROGEN = maxN;
        this.MAX_QUANTUM = maxQ;
        this.MAX_DOCKS = maxV;
        this.nitrogen = maxN;
        this.quantum = maxQ;
        this.freeDocks = maxV;
        this.activeRegularVehicles = numVehicles;
    }

    // Regular vehicle (or supply return trip): wait for dock + sufficient fuel
    public synchronized void requestDockAndRefuel(int n, int q) throws InterruptedException {
        while (freeDocks == 0 || nitrogen < n || quantum < q) {
            wait();
        }
        freeDocks--;
        nitrogen -= n;
        quantum -= q;
        notifyAll();
    }

    // Supply vehicle: deposit fuel without occupying a dock.
    // Deposits each type independently when space is available (zero fuel loss).
    // When all regular vehicles are done, clamps remaining fuel to avoid deadlock.
    public synchronized void depositFuel(int depositN, int depositQ) throws InterruptedException {
        int remainN = depositN;
        int remainQ = depositQ;
        while (remainN > 0 || remainQ > 0) {
            boolean deposited = false;
            if (remainN > 0 && nitrogen + remainN <= MAX_NITROGEN) {
                nitrogen += remainN;
                remainN = 0;
                deposited = true;
            }
            if (remainQ > 0 && quantum + remainQ <= MAX_QUANTUM) {
                quantum += remainQ;
                remainQ = 0;
                deposited = true;
            }
            if (remainN > 0 || remainQ > 0) {
                if (activeRegularVehicles == 0) {
                    // No consumers left — deposit what fits and proceed
                    nitrogen = Math.min(nitrogen + remainN, MAX_NITROGEN);
                    quantum = Math.min(quantum + remainQ, MAX_QUANTUM);
                    remainN = 0;
                    remainQ = 0;
                } else {
                    if (deposited) notifyAll();
                    wait();
                }
            }
        }
        notifyAll();
    }

    public synchronized void regularVehicleDone() {
        activeRegularVehicles--;
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
