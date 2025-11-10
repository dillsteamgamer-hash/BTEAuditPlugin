package mplugin.net.bTEAuditPlugin.resources;

public class BlockPoint {
    private int xPos, yPos, zPos;

    public BlockPoint(int xPos, int yPos, int zPos){
        this.xPos = xPos;
        this.yPos = yPos;
        this.zPos = zPos;
    }


    public int getxPos() {return xPos;}
    public int getyPos() {return yPos;}
    public int getzPos() {return zPos;}
}