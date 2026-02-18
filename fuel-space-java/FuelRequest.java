/**
 * Represents a fuel request or deposit in the FIFO queue.
 * Used to ensure fair ordering of service.
 */
public class FuelRequest {
    public enum RequestType {
        FUEL_REQUEST,  // Vehicle requesting fuel
        FUEL_DEPOSIT   // Supply vehicle depositing fuel
    }

    private final String vehicleId;
    private final int nitrogenAmount;
    private final int quantumAmount;
    private final RequestType type;
    private boolean served;

    public FuelRequest(String vehicleId, int nitrogen, int quantum, RequestType type) {
        this.vehicleId = vehicleId;
        this.nitrogenAmount = nitrogen;
        this.quantumAmount = quantum;
        this.type = type;
        this.served = false;
    }

    public String getVehicleId() { return vehicleId; }
    public int getNitrogenAmount() { return nitrogenAmount; }
    public int getQuantumAmount() { return quantumAmount; }
    public RequestType getType() { return type; }
    public boolean isServed() { return served; }
    public void setServed(boolean served) { this.served = served; }
}
