public class Logger {
    private static final Object printLock = new Object();

    public static void logTrace(int id, String type, String action) {
        synchronized (printLock) {
            if (id == 0) {
                System.out.println("[" + type + "] " + action);
            } else {
                System.out.println("[" + type + " " + id + "] " + action);
            }
        }
    }
}