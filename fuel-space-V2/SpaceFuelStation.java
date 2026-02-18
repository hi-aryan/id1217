import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SpaceFuelStation {

    // --- ENCAPSULATED DATA STRUCTURE ---
    private static class Request {
        int reqN, reqQ;
        boolean isAllocated = false;
        boolean isAborted = false;
        Condition cv;

        Request(int n, int q, ReentrantLock lock) {
            this.reqN = n;
            this.reqQ = q;
            this.cv = lock.newCondition();
        }
    }

    // --- STATION STATE ---
    private final int MAX_N, MAX_Q;
    private int vFree, nCurr, qCurr;
    private int nResOut = 0, qResOut = 0;
    private int nResIn = 0, qResIn = 0;

    private int activeConsumers;
    private int activeProducers;

    private final ReentrantLock lock = new ReentrantLock();
    private final LinkedList<Request> refuelQueue = new LinkedList<>();
    private final LinkedList<Request> supplyQueue = new LinkedList<>();

    public SpaceFuelStation(int v, int n, int q, int numCons, int numProd) {
        this.MAX_N = n;
        this.MAX_Q = q;
        this.vFree = v;
        this.nCurr = n / 2;
        this.qCurr = q / 2;
        this.activeConsumers = numCons;
        this.activeProducers = numProd;
    }

    private int nAvail() { return nCurr - nResOut; }
    private int qAvail() { return qCurr - qResOut; }
    private int nSpace() { return MAX_N - nCurr - nResIn; }
    private int qSpace() { return MAX_Q - qCurr - qResIn; }

    private boolean canSupply(Request req) {
        return nSpace() >= req.reqN && qSpace() >= req.reqQ;
    }

    private boolean canRefuel(Request req) {
        return nAvail() >= req.reqN && qAvail() >= req.reqQ;
    }

    private void reserveSupply(Request req) {
        vFree--;
        nResIn += req.reqN;
        qResIn += req.reqQ;
        req.isAllocated = true;
        req.cv.signal();
    }

    private void reserveRefuel(Request req) {
        vFree--;
        nResOut += req.reqN;
        qResOut += req.reqQ;
        req.isAllocated = true;
        req.cv.signal();
    }

    private void processQueue(LinkedList<Request> queue, java.util.function.Predicate<Request> canProcess,
                              java.util.function.Consumer<Request> reserve) {
        Iterator<Request> it = queue.iterator();
        while (it.hasNext() && vFree > 0) {
            Request req = it.next();
            if (canProcess.test(req)) {
                reserve.accept(req);
                it.remove();
            }
        }
    }

    private void wakeUpNext() {
        if (vFree > 0) {
            processQueue(supplyQueue, this::canSupply, this::reserveSupply);
        }
        if (vFree > 0) {
            processQueue(refuelQueue, this::canRefuel, this::reserveRefuel);
        }
    }

    public void unregisterConsumer() {
        lock.lock();
        try {
            activeConsumers--;
            if (activeConsumers == 0 && !supplyQueue.isEmpty()) {
                Logger.logTrace(0, "Station", "ALERT: All consumers left. Aborting waiting supply ships.");
                for (Request req : supplyQueue) {
                    req.isAborted = true;
                    req.cv.signal();
                }
                supplyQueue.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    public void unregisterProducer() {
        lock.lock();
        try {
            activeProducers--;
            if (activeProducers == 0 && !refuelQueue.isEmpty()) {
                Logger.logTrace(0, "Station", "ALERT: All supply ships left. Aborting waiting ordinary ships.");
                for (Request req : refuelQueue) {
                    req.isAborted = true;
                    req.cv.signal();
                }
                refuelQueue.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean refuelVehicle(int id, String type, int reqN, int reqQ, int dockTimeMs) throws InterruptedException {
        lock.lock();
        try {
            Logger.logTrace(id, type, "Arrived, requesting " + reqN + "N, " + reqQ + "Q.");

            if (refuelQueue.isEmpty() && vFree > 0 && nAvail() >= reqN && qAvail() >= reqQ) {
                vFree--;
                nResOut += reqN;
                qResOut += reqQ;
            } else {
                if (activeProducers == 0 && (nAvail() < reqN || qAvail() < reqQ)) {
                    Logger.logTrace(id, type, "Mission Aborted: Station lacks fuel and no producers remain.");
                    return false;
                }

                Logger.logTrace(id, type, "Insufficient resources/bays. Waiting in orbit...");
                Request myReq = new Request(reqN, reqQ, lock);
                refuelQueue.add(myReq);

                while (!myReq.isAllocated && !myReq.isAborted) {
                    myReq.cv.await();
                }

                if (myReq.isAborted) {
                    Logger.logTrace(id, type, "Forced to abort from orbit: Station is closing.");
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }

        Logger.logTrace(id, type, "Docked. Pumping fuel...");
        Thread.sleep(dockTimeMs);

        lock.lock();
        try {
            nCurr -= reqN;
            qCurr -= reqQ;
            nResOut -= reqN;
            qResOut -= reqQ;
            vFree++;
            Logger.logTrace(id, type, "Finished refueling and departed. (Station N:" + nCurr + " Q:" + qCurr + ")");
            wakeUpNext();
        } finally {
            lock.unlock();
        }
        return true;
    }

    public boolean supplyStation(int id, int depN, int depQ, int retN, int retQ, int dockTimeMs) throws InterruptedException {
        lock.lock();
        try {
            Logger.logTrace(id, "Supply", "Arrived to deposit " + depN + "N, " + depQ + "Q.");

            if (supplyQueue.isEmpty() && vFree > 0 && nSpace() >= depN && qSpace() >= depQ) {
                vFree--;
                nResIn += depN;
                qResIn += depQ;
            } else {
                if (activeConsumers == 0 && (nSpace() < depN || qSpace() < depQ)) {
                    Logger.logTrace(id, "Supply", "Mission Aborted: Station full and no consumers remain.");
                    return false;
                }

                Logger.logTrace(id, "Supply", "Insufficient space/bays. Waiting in orbit...");
                Request myReq = new Request(depN, depQ, lock);
                supplyQueue.add(myReq);

                while (!myReq.isAllocated && !myReq.isAborted) {
                    myReq.cv.await();
                }

                if (myReq.isAborted) {
                    Logger.logTrace(id, "Supply", "Forced to abort from orbit: Station is closing.");
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }

        Logger.logTrace(id, "Supply", "Docked. Depositing fuel...");
        Thread.sleep(dockTimeMs);

        lock.lock();
        try {
            nCurr += depN;
            qCurr += depQ;
            nResIn -= depN;
            qResIn -= depQ;
            vFree++;
            Logger.logTrace(id, "Supply", "Finished deposit. Requesting return fuel...");
            wakeUpNext();
        } finally {
            lock.unlock();
        }

        return refuelVehicle(id, "Supply", retN, retQ, dockTimeMs);
    }
}