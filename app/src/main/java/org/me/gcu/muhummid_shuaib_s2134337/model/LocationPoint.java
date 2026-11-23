package org.me.gcu.muhummid_shuaib_s2134337.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class LocationPoint {
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String pointType = "";
    private final Map<String, String> descriptors = new LinkedHashMap<>();

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getPointType() { return pointType; }
    public void setPointType(String pointType) { this.pointType = pointType == null ? "" : pointType; }
    public Map<String, String> getDescriptors() { return descriptors; }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public void addDescriptor(String type, String value) {
        if (type == null || value == null || value.trim().isEmpty()) return;

        String key = type;
        int n = 2;
        while (descriptors.containsKey(key)) key = type + " " + n++;
        descriptors.put(key, value.trim());
    }

    public String getRoadName() {
        String raw = descriptors.containsKey("linkName") ? descriptors.get("linkName")
                : descriptors.get("tpegIlcName1");
        if (raw == null) return null;

        java.util.regex.Matcher m = LEADING_ROAD.matcher(raw.trim().toUpperCase(java.util.Locale.UK));
        return m.find() ? m.group(1) : raw.trim();
    }

    private static final java.util.regex.Pattern LEADING_ROAD =
            java.util.regex.Pattern.compile("^([MAB][0-9]{1,4}(?:\\(M\\))?)(?![0-9])");

    public String getPlaceName() {
        if (descriptors.containsKey("nonLinkedPointName")) return descriptors.get("nonLinkedPointName");
        if (descriptors.containsKey("intersectionName")) return descriptors.get("intersectionName");
        if (descriptors.containsKey("junctionName")) return descriptors.get("junctionName");
        if (descriptors.containsKey("tpegIlcName2")) return descriptors.get("tpegIlcName2");
        String road = getRoadName();
        return road != null ? road : "";
    }
}
