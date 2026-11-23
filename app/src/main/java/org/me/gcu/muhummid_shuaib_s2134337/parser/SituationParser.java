package org.me.gcu.muhummid_shuaib_s2134337.parser;

import android.util.Log;

import org.me.gcu.muhummid_shuaib_s2134337.model.LocationPoint;
import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;
import org.me.gcu.muhummid_shuaib_s2134337.model.SituationLocation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class SituationParser {
    private static final String TAG = "SituationParser";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    private static final Set<String> LEAF_TAGS = new HashSet<>(Arrays.asList(
            "publicationTime", "situationRecordCreationTime", "situationRecordVersionTime",
            "probabilityOfOccurrence", "validityStatus", "overallStartTime", "overallEndTime",
            "numberOfOperationalLanes", "numberOfLanesRestricted", "delaysType",
            "tpegDirection", "tpegLinearLocationType", "latitude", "longitude", "value",
            "tpegOtherPointDescriptorType", "tpegIlcPointDescriptorType",
            "tpegJunctionPointDescriptorType", "complianceOption", "urgentRoadworks"));

    public interface ProgressListener {
        void onProgress(int percent);
    }

    private final Situation.Category category;

    private final Deque<String> path = new ArrayDeque<>();
    private Situation current;
    private SituationLocation currentLocation;
    private LocationPoint currentPoint;
    private String lastDescriptorValue;

    public SituationParser(Situation.Category category) {
        this.category = category;
    }

    public ParseResult parse(String xml, ProgressListener listener)
            throws XmlPullParserException, IOException {
        ParseResult result = new ParseResult();
        if (xml == null || xml.isEmpty()) return result;

        int total = countOccurrences(xml, "<situation ");
        int done = 0;

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xml));

        path.clear();
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = xpp.getName();

                boolean isSubType = name.endsWith("Type") && "situationRecord".equals(path.peek());
                if (LEAF_TAGS.contains(name) || isSubType) {
                    handleText(name, safeNextText(xpp), result);
                } else {
                    handleStartTag(xpp, name);
                    path.push(name);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String name = xpp.getName();
                if (!path.isEmpty()) path.pop();
                if (handleEndTag(name, result)) {
                    done++;
                    if (listener != null && total > 0) listener.onProgress(done * 100 / total);
                }
            }
            eventType = xpp.next();
        }
        Log.d(TAG, category + ": parsed " + result.getSituations().size() + " situations");
        return result;
    }

    private void handleStartTag(XmlPullParser xpp, String name) {
        switch (name) {
            case "situation":
                current = new Situation(category);
                current.setId(xpp.getAttributeValue(null, "id"));
                currentLocation = new SituationLocation();
                current.setLocation(currentLocation);
                break;
            case "situationRecord":
                if (current == null) break;
                current.setXsiType(getXsiType(xpp));
                current.setRecordId(xpp.getAttributeValue(null, "id"));
                current.setVersion(xpp.getAttributeValue(null, "version"));
                break;
            case "groupOfLocations":
                if (currentLocation != null) currentLocation.setLocationType(getXsiType(xpp));
                break;
            case "from":
            case "to":
                currentPoint = new LocationPoint();
                currentPoint.setPointType(getXsiType(xpp));
                break;
            case "pointCoordinates":

                if (currentPoint == null && currentLocation != null) {
                    currentPoint = new LocationPoint();
                    currentLocation.setSingle(currentPoint);
                }
                break;
            default:
                break;
        }
    }

    private boolean handleEndTag(String name, ParseResult result) {
        switch (name) {
            case "from":
                if (currentLocation != null) currentLocation.setFrom(currentPoint);
                currentPoint = null;
                break;
            case "to":
                if (currentLocation != null) currentLocation.setTo(currentPoint);
                currentPoint = null;
                break;
            case "situation":
                if (current != null) result.getSituations().add(current);
                current = null;
                currentLocation = null;
                currentPoint = null;
                return true;
            default:
                break;
        }
        return false;
    }

    private void handleText(String name, String text, ParseResult result) {
        if (text == null) return;
        text = text.trim();
        if (name.equals("publicationTime")) {
            result.setPublicationTime(text);
            return;
        }
        if (current == null) return;
        String parent = path.peek();

        switch (name) {
            case "situationRecordCreationTime": current.setCreationTime(text); break;
            case "situationRecordVersionTime": current.setVersionTime(text); break;
            case "probabilityOfOccurrence": current.setProbability(text); break;
            case "validityStatus": current.setValidityStatus(text); break;
            case "overallStartTime": current.setStartTime(text); break;
            case "overallEndTime": current.setEndTime(text); break;
            case "numberOfOperationalLanes": current.setOperationalLanes(parseInt(text)); break;
            case "numberOfLanesRestricted": current.setLanesRestricted(parseInt(text)); break;
            case "delaysType": current.setDelaysType(text); break;
            case "complianceOption": current.setComplianceOption(text); break;
            case "urgentRoadworks": current.setUrgentRoadworks(Boolean.parseBoolean(text)); break;
            case "tpegDirection": if (currentLocation != null) currentLocation.setDirection(text); break;
            case "tpegLinearLocationType": if (currentLocation != null) currentLocation.setLinearType(text); break;
            case "latitude": if (currentPoint != null) currentPoint.setLatitude(parseDouble(text)); break;
            case "longitude": if (currentPoint != null) currentPoint.setLongitude(parseDouble(text)); break;
            case "value":

                if (path.contains("generalPublicComment")) {
                    current.setComment(current.getComment().isEmpty() ? text : current.getComment() + " " + text);
                } else if (path.contains("descriptor")) {
                    lastDescriptorValue = text;
                } else if (path.contains("sourceName")) {
                    current.setSourceName(text);
                }
                break;
            case "tpegOtherPointDescriptorType":
            case "tpegIlcPointDescriptorType":
            case "tpegJunctionPointDescriptorType":
                if (currentPoint != null) currentPoint.addDescriptor(text, lastDescriptorValue);
                lastDescriptorValue = null;
                break;
            default:
                break;
        }

        if ("situationRecord".equals(parent) && name.endsWith("Type") && !name.equals("delaysType")) {
            current.setSubType(text);
        }
    }

    @Override
    public String toString() { return "SituationParser(" + category + ")"; }

    private static String getXsiType(XmlPullParser xpp) {
        String type = xpp.getAttributeValue(XSI_NS, "type");
        if (type == null) type = xpp.getAttributeValue(null, "xsi:type");
        if (type == null && xpp.getAttributeCount() > 0) type = xpp.getAttributeValue(0);
        return type == null ? "" : type;
    }

    private static String safeNextText(XmlPullParser xpp) throws IOException, XmlPullParserException {
        return xpp.nextText();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return Double.NaN; }
    }

    private static int countOccurrences(String text, String token) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) { count++; idx += token.length(); }
        return count;
    }
}
