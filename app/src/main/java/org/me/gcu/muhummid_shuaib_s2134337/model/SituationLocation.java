package org.me.gcu.muhummid_shuaib_s2134337.model;

import java.util.ArrayList;
import java.util.List;

public class SituationLocation {
    private String locationType = "";
    private String direction = "";
    private String linearType = "";
    private LocationPoint from;
    private LocationPoint to;
    private LocationPoint single;

    public String getLocationType() { return locationType; }
    public void setLocationType(String t) { locationType = t == null ? "" : t; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getLinearType() { return linearType; }
    public void setLinearType(String linearType) { this.linearType = linearType; }
    public LocationPoint getFrom() { return from; }
    public void setFrom(LocationPoint from) { this.from = from; }
    public LocationPoint getTo() { return to; }
    public void setTo(LocationPoint to) { this.to = to; }
    public LocationPoint getSingle() { return single; }
    public void setSingle(LocationPoint single) { this.single = single; }

    public List<LocationPoint> getPoints() {
        List<LocationPoint> list = new ArrayList<>();
        if (from != null && from.hasCoordinates()) list.add(from);
        if (to != null && to.hasCoordinates()) list.add(to);
        if (single != null && single.hasCoordinates()) list.add(single);
        return list;
    }

    public boolean isLine() {
        return from != null && to != null && from.hasCoordinates() && to.hasCoordinates()
                && (from.getLatitude() != to.getLatitude() || from.getLongitude() != to.getLongitude());
    }

    public String getReadableDirection() {
        if (direction == null || direction.isEmpty()) return "";
        String d = direction.replace("Bound", "bound").replace("bound", "bound");
        return Character.toUpperCase(d.charAt(0)) + d.substring(1).toLowerCase();
    }

    public double[] getCentre() {
        List<LocationPoint> pts = getPoints();
        if (pts.isEmpty()) return null;
        double lat = 0, lon = 0;
        for (LocationPoint p : pts) { lat += p.getLatitude(); lon += p.getLongitude(); }
        return new double[]{lat / pts.size(), lon / pts.size()};
    }
}
