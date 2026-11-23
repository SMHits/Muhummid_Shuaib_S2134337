package org.me.gcu.muhummid_shuaib_s2134337.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;
import org.me.gcu.muhummid_shuaib_s2134337.parser.ParseResult;
import org.me.gcu.muhummid_shuaib_s2134337.parser.SituationParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TrafficRepository {
    private static final String TAG = "TrafficRepository";

    public static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    public static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private static final String CACHE_UNPLANNED = "unplanned_cache.xml";
    private static final String CACHE_ROADWORKS = "roadworks_cache.xml";
    private static final String PREFS = "traffic_prefs";
    private static final String PREF_DOWNLOADED_AT = "downloaded_at";
    private static final int TIMEOUT_MS = 15000;

    public interface Listener {
        void onLoadingChanged(boolean loading);
        void onProgress(int percent);
        void onResult(FeedResult result);
    }

    private final Context appContext;
    private final String authHeader;
    private final Listener listener;
    private final ExecutorService downloadPool = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoUpdateTask;

    public TrafficRepository(Context context, String clientId, String clientKey, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;

        this.authHeader = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientKey).getBytes(StandardCharsets.UTF_8));
    }

    public void loadCached() {
        scheduler.execute(() -> {
            FeedResult cached = readCache(null);
            if (cached.hasData()) listener.onResult(cached);
        });
    }

    public synchronized void startAutoUpdate(long intervalMinutes) {
        if (autoUpdateTask != null) autoUpdateTask.cancel(false);
        autoUpdateTask = scheduler.scheduleWithFixedDelay(
                this::update, 0, intervalMinutes, TimeUnit.MINUTES);
        Log.d(TAG, "Auto update every " + intervalMinutes + " min");
    }

    public void refreshNow() {
        scheduler.execute(this::update);
    }

    public void shutdown() {
        scheduler.shutdownNow();
        downloadPool.shutdownNow();
    }

    private void update() {
        listener.onLoadingChanged(true);
        listener.onProgress(0);
        try {
            Future<String> unplannedJob = downloadPool.submit(() -> download(UNPLANNED_URL));
            Future<String> roadworksJob = downloadPool.submit(() -> download(ROADWORKS_URL));
            String unplannedXml = unplannedJob.get();
            String roadworksXml = roadworksJob.get();

            List<Situation> incidents = parse(unplannedXml, Situation.Category.INCIDENT, 0, 20);
            List<Situation> roadworks = parse(roadworksXml, Situation.Category.ROADWORKS, 20, 80);

            writeFile(CACHE_UNPLANNED, unplannedXml);
            writeFile(CACHE_ROADWORKS, roadworksXml);
            long now = System.currentTimeMillis();
            prefs().edit().putLong(PREF_DOWNLOADED_AT, now).apply();

            listener.onResult(new FeedResult(incidents, roadworks, now, false, null));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            Log.e(TAG, "Update failed: " + cause);
            listener.onResult(readCache(describe(cause)));
        } finally {
            listener.onProgress(100);
            listener.onLoadingChanged(false);
        }
    }

    private List<Situation> parse(String xml, Situation.Category category, int base, int span)
            throws Exception {
        SituationParser parser = new SituationParser(category);
        ParseResult result = parser.parse(xml, p -> listener.onProgress(base + p * span / 100));
        return result.getSituations();
    }

    private String download(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Authorization", authHeader);
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + code
                        + (code == 401 || code == 403 ? " (check your Client ID / Key)" : ""));
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) sb.append(line).append('\n');
            }
            String xml = sb.toString();
            int start = xml.indexOf("<?");
            if (start > 0) xml = xml.substring(start);
            if (!xml.contains("<situation") && !xml.contains("payloadPublication")) {
                throw new IOException("Server did not send traffic data (check the Client ID / Key)");
            }
            return xml;
        } finally {
            connection.disconnect();
        }
    }

    private FeedResult readCache(String error) {
        List<Situation> incidents = new ArrayList<>();
        List<Situation> roadworks = new ArrayList<>();
        try {
            String u = readFile(CACHE_UNPLANNED);
            String r = readFile(CACHE_ROADWORKS);
            if (u != null) incidents = new SituationParser(Situation.Category.INCIDENT).parse(u, null).getSituations();
            if (r != null) roadworks = new SituationParser(Situation.Category.ROADWORKS).parse(r, null).getSituations();
        } catch (Exception e) {
            Log.e(TAG, "Could not read saved data: " + e);
        }
        return new FeedResult(incidents, roadworks, prefs().getLong(PREF_DOWNLOADED_AT, 0), true, error);
    }

    private static String describe(Throwable t) {
        if (t instanceof java.net.UnknownHostException) return "No internet connection";
        if (t instanceof java.net.SocketTimeoutException) return "Connection timed out";
        String msg = t.getMessage();
        return msg != null ? msg : t.getClass().getSimpleName();
    }

    private SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void writeFile(String name, String content) throws IOException {
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(new File(appContext.getFilesDir(), name)), StandardCharsets.UTF_8)) {
            w.write(content);
        }
    }

    private String readFile(String name) throws IOException {
        File f = new File(appContext.getFilesDir(), name);
        if (!f.exists()) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
