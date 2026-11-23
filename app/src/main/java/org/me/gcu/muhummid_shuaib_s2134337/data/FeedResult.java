package org.me.gcu.muhummid_shuaib_s2134337.data;

import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;

import java.util.ArrayList;
import java.util.List;

public class FeedResult {
    public final List<Situation> incidents;
    public final List<Situation> roadworks;

    public final long downloadedAt;

    public final boolean fromCache;

    public final String error;

    public FeedResult(List<Situation> incidents, List<Situation> roadworks,
                      long downloadedAt, boolean fromCache, String error) {
        this.incidents = incidents != null ? incidents : new ArrayList<>();
        this.roadworks = roadworks != null ? roadworks : new ArrayList<>();
        this.downloadedAt = downloadedAt;
        this.fromCache = fromCache;
        this.error = error;
    }

    public boolean hasData() {
        return !incidents.isEmpty() || !roadworks.isEmpty();
    }
}
