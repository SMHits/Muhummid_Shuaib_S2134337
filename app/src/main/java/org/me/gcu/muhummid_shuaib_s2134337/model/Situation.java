package org.me.gcu.muhummid_shuaib_s2134337.model;

import android.location.Location;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Situation {
    public enum Category { INCIDENT, ROADWORKS }

    private static final Pattern ROAD_PATTERN =
            Pattern.compile("\\b([MAB][0-9]{1,4}(?:\\(M\\))?)\\b");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);
    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");

    private final Category category;
    private String id = "";
    private String recordId = "";
    private String version = "";
    private String xsiType = "";
    private SituationType type = SituationType.OTHER;
    private String subType = "";
    private String comment = "";
    private String probability = "";
    private String sourceName = "";
    private String validityStatus = "";
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String delaysType = "";
    private int lanesRestricted = -1;
    private int operationalLanes = -1;
    private String complianceOption = "";
    private Boolean urgentRoadworks;
    private SituationLocation location = new SituationLocation();

    public Situation(Category category) { this.category = category; }

    public Category getCategory() { return category; }
    public boolean isRoadworks() { return category == Category.ROADWORKS; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id == null ? "" : id; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId == null ? "" : recordId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version == null ? "" : version; }
    public String getXsiType() { return xsiType; }
    public void setXsiType(String xsiType) {
        this.xsiType = xsiType == null ? "" : xsiType;
        this.type = SituationType.fromXsiType(xsiType);
    }
    public SituationType getType() { return type; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType == null ? "" : subType; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment == null ? "" : comment.trim().replaceAll("\\s+", " "); }
    public String getProbability() { return probability; }
    public void setProbability(String probability) { this.probability = probability; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String validityStatus) { this.validityStatus = validityStatus; }
    public OffsetDateTime getCreationTime() { return creationTime; }
    public void setCreationTime(String s) { creationTime = parseTime(s); }
    public OffsetDateTime getVersionTime() { return versionTime; }
    public void setVersionTime(String s) { versionTime = parseTime(s); }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(String s) { startTime = parseTime(s); }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(String s) { endTime = parseTime(s); }
    public String getDelaysType() { return delaysType; }
    public void setDelaysType(String delaysType) { this.delaysType = delaysType; }
    public int getLanesRestricted() { return lanesRestricted; }
    public void setLanesRestricted(int lanesRestricted) { this.lanesRestricted = lanesRestricted; }
    public int getOperationalLanes() { return operationalLanes; }
    public void setOperationalLanes(int operationalLanes) { this.operationalLanes = operationalLanes; }
    public String getComplianceOption() { return complianceOption; }
    public void setComplianceOption(String complianceOption) { this.complianceOption = complianceOption; }
    public Boolean getUrgentRoadworks() { return urgentRoadworks; }
    public void setUrgentRoadworks(Boolean urgentRoadworks) { this.urgentRoadworks = urgentRoadworks; }
    public SituationLocation getLocation() { return location; }
    public void setLocation(SituationLocation location) { this.location = location; }

    private static OffsetDateTime parseTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return OffsetDateTime.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatTime(OffsetDateTime t) {
        if (t == null) return "Not given";
        return t.atZoneSameInstant(UK_ZONE).format(DISPLAY_FORMAT);
    }

    public String getTypeLabel() {
        if (isRoadworks()) return urgentRoadworks != null && urgentRoadworks ? "Urgent roadworks" : "Roadworks";
        return type.getLabel();
    }

    public String getRoadName() {
        if (location != null) {
            if (location.getFrom() != null && location.getFrom().getRoadName() != null)
                return location.getFrom().getRoadName();
            if (location.getTo() != null && location.getTo().getRoadName() != null)
                return location.getTo().getRoadName();
            if (location.getSingle() != null && location.getSingle().getRoadName() != null)
                return location.getSingle().getRoadName();
        }
        Matcher m = ROAD_PATTERN.matcher(comment);
        return m.find() ? m.group(1) : "Unknown road";
    }

    public String[] getCommentParts() {
        return comment.split("\\s+-\\s+");
    }

    public static String readable(String camel) {
        if (camel == null || camel.isEmpty()) return "";
        String spaced = camel.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.UK);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    public String getImpactText() {
        StringBuilder sb = new StringBuilder();
        if (!delaysType.isEmpty()) sb.append(readable(delaysType));
        if (lanesRestricted >= 0) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(lanesRestricted).append(lanesRestricted == 1 ? " lane restricted" : " lanes restricted");
        }
        if (operationalLanes >= 0) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(operationalLanes == 1 ? "1 lane open" : operationalLanes + " lanes open");
        }
        return sb.length() == 0 ? "Not given" : sb.toString();
    }

    public long getDurationDays() {
        if (startTime == null || endTime == null) return -1;
        long hours = Duration.between(startTime, endTime).toHours();
        if (hours < 0) return -1;
        return (long) Math.ceil(hours / 24.0);
    }

    public DurationCategory getDurationCategory() {
        return DurationCategory.fromDays(getDurationDays());
    }

    public String getDurationText() {
        long d = getDurationDays();
        if (d < 0) return "No end date given";
        if (d <= 1) return "1 day or less";
        if (d < 60) return d + " days";
        return String.format(Locale.UK, "%d days (about %d months)", d, Math.round(d / 30.0));
    }

    public boolean isOnRoad(String query) {
        if (query == null) return false;
        String q = query.trim().toUpperCase(Locale.UK);
        if (q.isEmpty()) return false;
        if (getRoadName().toUpperCase(Locale.UK).equals(q)) return true;

        Matcher m = ROAD_PATTERN.matcher(comment.toUpperCase(Locale.UK));
        while (m.find()) {
            if (m.group(1).equals(q)) return true;
        }

        return q.length() >= 3 && comment.toUpperCase(Locale.UK).contains(q);
    }

    public double distanceKmFrom(double lat, double lon) {
        double best = -1;
        float[] result = new float[1];
        for (LocationPoint p : location.getPoints()) {
            Location.distanceBetween(lat, lon, p.getLatitude(), p.getLongitude(), result);
            double km = result[0] / 1000.0;
            if (best < 0 || km < best) best = km;
        }
        return best;
    }

    public String getKey() {
        return category.name() + ":" + id;
    }
}
