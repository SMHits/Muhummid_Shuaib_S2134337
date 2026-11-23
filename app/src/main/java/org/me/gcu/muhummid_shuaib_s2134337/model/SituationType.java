package org.me.gcu.muhummid_shuaib_s2134337.model;

import org.me.gcu.muhummid_shuaib_s2134337.R;

public enum SituationType {
    ROADWORKS("MaintenanceWorks", "Roadworks", R.drawable.ic_type_roadworks, 0xFFEF6C00),
    ROAD_MANAGEMENT("RoadOrCarriagewayOrLaneManagement", "Road / lane closure", R.drawable.ic_type_closure, 0xFFC62828),
    ABNORMAL_TRAFFIC("AbnormalTraffic", "Queuing traffic", R.drawable.ic_type_traffic, 0xFFF9A825),
    ACCIDENT("Accident", "Accident", R.drawable.ic_type_accident, 0xFFB71C1C),
    OBSTRUCTION("GeneralObstruction", "Obstruction", R.drawable.ic_type_obstruction, 0xFF6A1B9A),
    VEHICLE_OBSTRUCTION("VehicleObstruction", "Vehicle obstruction", R.drawable.ic_type_vehicle, 0xFF1565C0),
    WEATHER("WeatherRelatedRoadConditions", "Weather conditions", R.drawable.ic_type_weather, 0xFF00838F),
    OTHER("", "Other incident", R.drawable.ic_type_other, 0xFF546E7A);

    private final String xsiType;
    private final String label;
    private final int iconRes;
    private final int colour;

    SituationType(String xsiType, String label, int iconRes, int colour) {
        this.xsiType = xsiType;
        this.label = label;
        this.iconRes = iconRes;
        this.colour = colour;
    }

    public String getLabel() { return label; }
    public int getIconRes() { return iconRes; }
    public int getColour() { return colour; }

    public static SituationType fromXsiType(String xsiType) {
        if (xsiType == null) return OTHER;

        String clean = xsiType.contains(":") ? xsiType.substring(xsiType.indexOf(':') + 1) : xsiType;
        for (SituationType t : values()) {
            if (!t.xsiType.isEmpty() && t.xsiType.equalsIgnoreCase(clean)) return t;
        }
        if (clean.toLowerCase().contains("weather") || clean.toLowerCase().contains("environment")) return WEATHER;
        if (clean.toLowerCase().contains("obstruction")) return OBSTRUCTION;
        return OTHER;
    }
}
