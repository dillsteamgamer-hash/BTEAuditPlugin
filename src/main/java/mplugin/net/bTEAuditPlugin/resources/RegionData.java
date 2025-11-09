package mplugin.net.bTEAuditPlugin.resources;

public class RegionData {
    private final String name;
    private final int x;
    private final int z;
    private String status;
    private  String deleted1, deleted2;


    public RegionData(String name, int x, int z, String status) {
        this.name = name;
        this.x = x;
        this.z = z;
        this.status = status;
    }

    public String getName() { return name; }
    public int getX() { return x; }
    public int getZ() { return z; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public void setDeleted1(String t) {deleted1 = t;}
    public void setDeleted2(String t) {deleted2 = t;}
    public String getDeleted1(){return deleted1;}
    public String getDeleted2(){return deleted2;}
}