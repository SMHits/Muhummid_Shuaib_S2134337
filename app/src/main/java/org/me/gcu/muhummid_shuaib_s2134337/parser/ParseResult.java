package org.me.gcu.muhummid_shuaib_s2134337.parser;

import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {
    private final List<Situation> situations = new ArrayList<>();
    private String publicationTime = "";
    private int unknownTagCount = 0;

    public List<Situation> getSituations() { return situations; }
    public String getPublicationTime() { return publicationTime; }
    void setPublicationTime(String t) { publicationTime = t == null ? "" : t; }
    public int getUnknownTagCount() { return unknownTagCount; }
    void incrementUnknownTags() { unknownTagCount++; }
}
