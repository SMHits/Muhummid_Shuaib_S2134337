package org.me.gcu.muhummid_shuaib_s2134337.model;

public enum DurationCategory {
    SHORT("Short (up to 3 days)", 0xFF2E7D32),
    MEDIUM("Medium (up to 2 weeks)", 0xFFF9A825),
    LONG("Long (over 2 weeks)", 0xFFC62828),
    UNKNOWN("Duration unknown", 0xFF9E9E9E);

    public static final long SHORT_MAX_DAYS = 3;
    public static final long MEDIUM_MAX_DAYS = 14;

    private final String label;
    private final int colour;

    DurationCategory(String label, int colour) {
        this.label = label;
        this.colour = colour;
    }

    public String getLabel() { return label; }
    public int getColour() { return colour; }

    public static DurationCategory fromDays(long days) {
        if (days < 0) return UNKNOWN;
        if (days <= SHORT_MAX_DAYS) return SHORT;
        if (days <= MEDIUM_MAX_DAYS) return MEDIUM;
        return LONG;
    }
}
