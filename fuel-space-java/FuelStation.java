public class FuelStation {
    private int nitrogen;
    private int quantum;
    private int freeDocks;

    private final int MAX_NITROGEN;
    private final int MAX_QUANTUM;
    private final int MAX_DOCKS;

    public FuelStation(int maxN, int maxQ, int maxV) {
        this.MAX_NITROGEN = maxN;
        this.MAX_QUANTUM = maxQ;
        this.MAX_DOCKS = maxV;
        this.nitrogen = maxN;
        this.quantum = maxQ;
        this.freeDocks = maxV;
    }

    // Regular vehicle: atomically wait for dock + sufficient fuel
    public synchronized void requestDockAndRefuel(int n, int q) throws InterruptedException {
        // CRITICAL: Check dock AND fuel atomically in while loop
        // This prevents: vehicle taking dock then blocking for fuel
        while (freeDocks == 0 || nitrogen < n || quantum < q) {
            wait(); // Release lock and wait
        }

        // Atomically consume resources
        freeDocks--;
        nitrogen -= n;
        quantum -= q;
    }

    // Supply vehicle: atomically wait for dock + sufficient storage space, then
    // deposit
    public synchronized void requestDockAndDeposit(int n, int q) throws InterruptedException {
        // CRITICAL: Check dock AND storage space atomically
        while (freeDocks == 0 || nitrogen + n > MAX_NITROGEN || quantum + q > MAX_QUANTUM) {
            wait();
        }

        // Take dock and deposit fuel
        freeDocks--;
        nitrogen += n;
        quantum += q;

        // Wake all waiters since fuel is now available
        notifyAll();
    }

    // Supply vehicle already has dock, just waiting for return fuel
    public synchronized void requestReturnFuel(int n, int q) throws InterruptedException {
        // Vehicle already holds dock, just wait for fuel
        while (nitrogen < n || quantum < q) {
            wait();
        }

        nitrogen -= n;
        quantum -= q;
    }

    // Release dock when leaving
    public synchronized void releaseDock() {
        freeDocks++;

        // Wake all waiters since dock (and possibly fuel) now available
        notifyAll();
    }

    // Thread-safe status printing
    public synchronized void printStatus() {
        System.out.printf("    [Station: N=%d/%d, Q=%d/%d, Docks=%d/%d]%n",
                nitrogen, MAX_NITROGEN, quantum, MAX_QUANTUM, freeDocks, MAX_DOCKS);
    }

    // For debugging - check if deposit would fit
    public synchronized boolean canAccommodateDeposit(int n, int q) {
        return nitrogen + n <= MAX_NITROGEN && quantum + q <= MAX_QUANTUM;
    }
}
