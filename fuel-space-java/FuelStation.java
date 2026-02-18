import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;
import java.util.Set;

/**
 * Monitor representing the fuel space station.
 * Controls access to fuel resources and docking spots using synchronized
 * methods.
 * Implements a scannable FIFO queue for practical fairness + progress.
 */
public class FuelStation {
    private int nitrogenLevel;
    private int quantumLevel;
    private int occupiedDocks;

    private final int MAX_NITROGEN;
    private final int MAX_QUANTUM;
    private final int MAX_DOCKS;

    private final Queue<FuelRequest> waitingQueue;

    public FuelStation(int maxDocks, int maxNitrogen, int maxQuantum) {
        this(maxDocks, maxNitrogen, maxQuantum, maxNitrogen, maxQuantum);
    }

    public FuelStation(int maxDocks, int maxNitrogen, int maxQuantum, int initialNitrogen, int initialQuantum) {
        this.MAX_DOCKS = maxDocks;
        this.MAX_NITROGEN = maxNitrogen;
        this.MAX_QUANTUM = maxQuantum;

        if (initialNitrogen < 0 || initialNitrogen > maxNitrogen
                || initialQuantum < 0 || initialQuantum > maxQuantum) {
            throw new IllegalArgumentException("Initial fuel levels must be within station capacity");
        }

        this.nitrogenLevel = initialNitrogen;
        this.quantumLevel = initialQuantum;
        this.occupiedDocks = 0;

        this.waitingQueue = new LinkedList<>();
        this.dockedVehicles = new HashSet<>();
    }

    private final Set<String> dockedVehicles;

    /**
     * Supply vehicle deposits fuel.
     * Returns true if docking/deposit succeeded, false if interrupted while
     * waiting.
     */
    public synchronized boolean depositFuel(int nitrogen, int quantum, String vehicleId, int returnNitrogen,
            int returnQuantum) {
        validateAmounts(nitrogen, quantum);
        if (nitrogen > MAX_NITROGEN || quantum > MAX_QUANTUM) {
            throw new IllegalArgumentException("Deposit exceeds station capacity: " + vehicleId);
        }

        long startTime = System.currentTimeMillis();
        FuelRequest request = new FuelRequest(vehicleId, nitrogen, quantum, FuelRequest.RequestType.FUEL_DEPOSIT,
                returnNitrogen, returnQuantum);
        waitingQueue.add(request);

        System.out.printf("[%d ms] %s arrives to DEPOSIT %dL N2, %dL QF (waiting in queue)\n",
                System.currentTimeMillis(), vehicleId, nitrogen, quantum);

        // Wait until dock is free, full deposit fits, AND return fuel is guaranteed.
        while (!canSatisfyDeposit(nitrogen, quantum, returnNitrogen, returnQuantum) || !isFirstSatisfiable(request)) {
            try {
                wait();
            } catch (InterruptedException e) {
                waitingQueue.remove(request);
                notifyAll();
                Thread.currentThread().interrupt();
                return false;
            }
        }

        // Full deposit only (assignment requirement).
        nitrogenLevel += nitrogen;
        quantumLevel += quantum;
        occupiedDocks++;
        dockedVehicles.add(vehicleId);
        request.setServed(true);
        waitingQueue.remove(request);

        long waitTime = System.currentTimeMillis() - startTime;
        System.out.printf("[%d ms] %s DEPOSITING (waited %d ms) | N2: %d/%d, QF: %d/%d, Docks: %d/%d\n",
                System.currentTimeMillis(), vehicleId, waitTime,
                nitrogenLevel, MAX_NITROGEN, quantumLevel, MAX_QUANTUM,
                occupiedDocks, MAX_DOCKS);

        notifyAll();
        printDebugState("DEPOSIT COMPLETE: " + vehicleId);
        return true;
    }

    /**
     * Vehicle requests fuel.
     * Returns true if docking/refuel succeeded, false if interrupted while waiting.
     */
    public synchronized boolean requestFuel(int nitrogen, int quantum, String vehicleId) {
        validateAmounts(nitrogen, quantum);
        if (nitrogen > MAX_NITROGEN || quantum > MAX_QUANTUM) {
            throw new IllegalArgumentException("Request exceeds station capacity: " + vehicleId);
        }

        long startTime = System.currentTimeMillis();
        FuelRequest request = new FuelRequest(vehicleId, nitrogen, quantum, FuelRequest.RequestType.FUEL_REQUEST);
        waitingQueue.add(request);

        System.out.printf("[%d ms] %s arrives to REQUEST %dL N2, %dL QF (waiting in queue)\n",
                System.currentTimeMillis(), vehicleId, nitrogen, quantum);

        while (!canSatisfyFuelRequest(nitrogen, quantum, vehicleId) || !isFirstSatisfiable(request)) {
            try {
                wait();
            } catch (InterruptedException e) {
                waitingQueue.remove(request);
                notifyAll();
                Thread.currentThread().interrupt();
                return false;
            }
        }

        nitrogenLevel -= nitrogen;
        quantumLevel -= quantum;

        // Only increment dock count if we didn't already have one
        if (!dockedVehicles.contains(vehicleId)) {
            occupiedDocks++;
            dockedVehicles.add(vehicleId);
        }

        request.setServed(true);
        waitingQueue.remove(request);

        long waitTime = System.currentTimeMillis() - startTime;
        System.out.printf("[%d ms] %s REFUELING (waited %d ms) | N2: %d/%d, QF: %d/%d, Docks: %d/%d\n",
                System.currentTimeMillis(), vehicleId, waitTime,
                nitrogenLevel, MAX_NITROGEN, quantumLevel, MAX_QUANTUM,
                occupiedDocks, MAX_DOCKS);

        notifyAll();
        printDebugState("REFUEL COMPLETE: " + vehicleId);
        return true;
    }

    /**
     * Vehicle releases docking spot and departs.
     */
    public synchronized void releaseDock(String vehicleId) {
        if (occupiedDocks <= 0) {
            System.err.printf("[%d ms] WARN: %s attempted to release with no occupied docks\n",
                    System.currentTimeMillis(), vehicleId);
            notifyAll();
            return;
        }

        occupiedDocks--;
        dockedVehicles.remove(vehicleId);
        System.out.printf("[%d ms] %s DEPARTED | N2: %d/%d, QF: %d/%d, Docks: %d/%d\n",
                System.currentTimeMillis(), vehicleId,
                nitrogenLevel, MAX_NITROGEN, quantumLevel, MAX_QUANTUM,
                occupiedDocks, MAX_DOCKS);

        notifyAll();
    }

    private void validateAmounts(int nitrogen, int quantum) {
        if (nitrogen < 0 || quantum < 0) {
            throw new IllegalArgumentException("Fuel amounts must be non-negative");
        }
    }

    private boolean canSatisfyDeposit(int nitrogen, int quantum, int requiredReturnNitrogen,
            int requiredReturnQuantum) {
        // SAFE ENTRY CHECK:
        // Do not enter if the station cannot provide the return fuel immediately after
        // deposit.
        // We calculate the POST-DEPOSIT levels and check if they satisfy the return
        // requirement.

        boolean spaceForDeposit = nitrogenLevel + nitrogen <= MAX_NITROGEN
                && quantumLevel + quantum <= MAX_QUANTUM;

        boolean fuelForReturn = (nitrogenLevel + nitrogen) >= requiredReturnNitrogen
                && (quantumLevel + quantum) >= requiredReturnQuantum;

        return occupiedDocks < MAX_DOCKS && spaceForDeposit && fuelForReturn;
    }

    private boolean canSatisfyFuelRequest(int nitrogen, int quantum, String vehicleId) {
        // If vehicle is already docked, it doesn't need a new dock.
        boolean hasDock = dockedVehicles.contains(vehicleId);
        boolean waitingForDock = !hasDock && occupiedDocks >= MAX_DOCKS;

        if (waitingForDock) {
            return false;
        }

        return nitrogenLevel >= nitrogen && quantumLevel >= quantum;
    }

    /**
     * Scannable FIFO: target can proceed only if no earlier request is currently
     * satisfiable.
     */
    private boolean isFirstSatisfiable(FuelRequest targetRequest) {
        for (FuelRequest req : waitingQueue) {
            if (req == targetRequest) {
                return true;
            }

            if (req.getType() == FuelRequest.RequestType.FUEL_DEPOSIT) {
                if (canSatisfyDeposit(req.getNitrogenAmount(), req.getQuantumAmount(),
                        req.getReturnNitrogen(), req.getReturnQuantum())) {
                    return false;
                }
            } else {
                if (canSatisfyFuelRequest(req.getNitrogenAmount(), req.getQuantumAmount(), req.getVehicleId())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Helper to print detailed debug state of the station.
     */
    private void printDebugState(String reason) {
        System.out.println("\n--- DEBUG STATE [" + reason + "] ---");
        System.out.printf("Values: N2=%d/%d, QF=%d/%d, Docks=%d/%d\n",
                nitrogenLevel, MAX_NITROGEN, quantumLevel, MAX_QUANTUM, occupiedDocks, MAX_DOCKS);
        System.out.println("Wait Queue:");
        if (waitingQueue.isEmpty()) {
            System.out.println("  (Empty)");
        } else {
            for (FuelRequest req : waitingQueue) {
                String typeStr = (req.getType() == FuelRequest.RequestType.FUEL_DEPOSIT) ? "DEPOSIT" : "REQUEST";
                System.out.printf("  - %s [%s]: N2=%d, QF=%d (Served=%s)\n",
                        req.getVehicleId(), typeStr, req.getNitrogenAmount(), req.getQuantumAmount(), req.isServed());
            }
        }
        System.out.println("----------------------------------------\n");
    }
}
