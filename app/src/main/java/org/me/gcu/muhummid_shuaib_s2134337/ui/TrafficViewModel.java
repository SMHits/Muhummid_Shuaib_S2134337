package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.muhummid_shuaib_s2134337.data.FeedResult;
import org.me.gcu.muhummid_shuaib_s2134337.data.TrafficRepository;
import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;
import org.me.gcu.muhummid_shuaib_s2134337.util.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrafficViewModel extends AndroidViewModel implements TrafficRepository.Listener {
    public enum ListFilter { ALL, INCIDENTS, ROADWORKS }
    public enum MapSource { ALL, LIST_RESULTS, NEARBY }

    public static class Place {
        public final String name;
        public final double lat, lon;
        Place(String name, double lat, double lon) { this.name = name; this.lat = lat; this.lon = lon; }
        @NonNull @Override public String toString() { return name; }
    }

    public static final Place[] FIXED_PLACES = {
            new Place("GCU, Glasgow", 55.8666, -4.2506),
            new Place("Edinburgh", 55.9533, -3.1883),
            new Place("Stirling", 56.1165, -3.9369),
            new Place("Perth", 56.3950, -3.4308),
            new Place("Dundee", 56.4620, -2.9707),
            new Place("Aberdeen", 57.1497, -2.0943),
            new Place("Inverness", 57.4778, -4.2247),
    };

    private static final String PREFS = "settings";
    private static final String PREF_INTERVAL = "update_interval";
    public static final long DEFAULT_INTERVAL_MIN = 60;

    private TrafficRepository repository;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final MutableLiveData<List<Situation>> incidents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Situation>> roadworks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<FeedResult> lastResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> online = new MutableLiveData<>(true);
    private final MutableLiveData<Long> intervalMinutes = new MutableLiveData<>(DEFAULT_INTERVAL_MIN);

    private final MutableLiveData<ListFilter> listFilter = new MutableLiveData<>(ListFilter.ALL);
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MediatorLiveData<List<Situation>> listResults = new MediatorLiveData<>();

    private final MutableLiveData<Integer> radiusKm = new MutableLiveData<>(25);
    private final MutableLiveData<double[]> searchCentre =
            new MutableLiveData<>(new double[]{FIXED_PLACES[0].lat, FIXED_PLACES[0].lon});
    private final MutableLiveData<String> searchCentreName = new MutableLiveData<>(FIXED_PLACES[0].name);
    private final MutableLiveData<Boolean> useDeviceLocation = new MutableLiveData<>(false);
    private int fixedPlaceIndex = 0;
    private final MediatorLiveData<List<Situation>> nearbyResults = new MediatorLiveData<>();

    private final MutableLiveData<String> selectedKey = new MutableLiveData<>(null);
    private final MediatorLiveData<Situation> selected = new MediatorLiveData<>();
    private final MutableLiveData<MapSource> mapSource = new MutableLiveData<>(MapSource.ALL);
    private final MutableLiveData<String> mapFocusKey = new MutableLiveData<>(null);
    private final MutableLiveData<Event<Integer>> tabRequest = new MutableLiveData<>();
    private int currentTab = 0;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        listResults.addSource(incidents, x -> updateListResults());
        listResults.addSource(roadworks, x -> updateListResults());
        listResults.addSource(listFilter, x -> updateListResults());
        listResults.addSource(roadQuery, x -> updateListResults());

        nearbyResults.addSource(incidents, x -> updateNearby());
        nearbyResults.addSource(roadworks, x -> updateNearby());
        nearbyResults.addSource(radiusKm, x -> updateNearby());
        nearbyResults.addSource(searchCentre, x -> updateNearby());

        selected.addSource(selectedKey, x -> selected.setValue(findByKey(x)));
        selected.addSource(incidents, x -> selected.setValue(findByKey(selectedKey.getValue())));
        selected.addSource(roadworks, x -> selected.setValue(findByKey(selectedKey.getValue())));
    }

    public void start(String clientId, String clientKey) {
        if (repository != null) return;
        repository = new TrafficRepository(getApplication(), clientId, clientKey, this);
        long interval = settings().getLong(PREF_INTERVAL, DEFAULT_INTERVAL_MIN);
        intervalMinutes.setValue(interval);
        repository.loadCached();
        repository.startAutoUpdate(interval);
        watchConnection();
    }

    @Override public void onLoadingChanged(boolean isLoading) { loading.postValue(isLoading); }

    @Override public void onProgress(int percent) { progress.postValue(percent); }

    @Override
    public void onResult(FeedResult result) {
        if (result.hasData() || !hasData()) {
            incidents.postValue(result.incidents);
            roadworks.postValue(result.roadworks);
        }
        lastResult.postValue(result);
    }

    private boolean hasData() {
        List<Situation> i = incidents.getValue(), r = roadworks.getValue();
        return (i != null && !i.isEmpty()) || (r != null && !r.isEmpty());
    }

    private void watchConnection() {
        ConnectivityManager cm = (ConnectivityManager)
                getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        Network active = cm.getActiveNetwork();
        NetworkCapabilities caps = active != null ? cm.getNetworkCapabilities(active) : null;
        online.setValue(caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network network) {
                boolean wasOffline = Boolean.FALSE.equals(online.getValue());
                online.postValue(true);
                if (wasOffline && repository != null) repository.refreshNow();
            }
            @Override public void onLost(@NonNull Network network) {
                online.postValue(false);
            }
        };
        try {
            cm.registerDefaultNetworkCallback(networkCallback);
        } catch (SecurityException e) {
            networkCallback = null;
        }
    }

    @Override
    protected void onCleared() {
        if (repository != null) repository.shutdown();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager)
                    getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
        }
        super.onCleared();
    }

    public void refresh() { if (repository != null) repository.refreshNow(); }

    public void setUpdateInterval(long minutes) {
        intervalMinutes.setValue(minutes);
        settings().edit().putLong(PREF_INTERVAL, minutes).apply();
        if (repository != null) repository.startAutoUpdate(minutes);
    }

    public void setListFilter(ListFilter f) { if (f != listFilter.getValue()) listFilter.setValue(f); }

    public void setRoadQuery(String q) {
        String clean = q == null ? "" : q.trim();
        if (!clean.equals(roadQuery.getValue())) roadQuery.setValue(clean);
    }

    public void select(Situation s) { selectedKey.setValue(s == null ? null : s.getKey()); }
    public void selectKey(String key) { selectedKey.setValue(key); }
    public void clearSelection() { selectedKey.setValue(null); }

    public void setRadiusKm(int km) { radiusKm.setValue(km); }

    public void useFixedPlace(int index) {
        fixedPlaceIndex = index;
        Place p = FIXED_PLACES[index];
        useDeviceLocation.setValue(false);
        searchCentreName.setValue(p.name);
        searchCentre.setValue(new double[]{p.lat, p.lon});
    }

    public void useDeviceLocation(double lat, double lon) {
        useDeviceLocation.setValue(true);
        searchCentreName.setValue("Your location");
        searchCentre.setValue(new double[]{lat, lon});
    }

    public int getFixedPlaceIndex() { return fixedPlaceIndex; }

    public void showOnMap(MapSource source, String focusKey) {
        mapSource.setValue(source);
        mapFocusKey.setValue(focusKey);
        tabRequest.setValue(new Event<>(MainActivity.TAB_MAP));
    }

    public void setMapSource(MapSource source) { mapSource.setValue(source); mapFocusKey.setValue(null); }

    public int getCurrentTab() { return currentTab; }
    public void setCurrentTab(int tab) { currentTab = tab; }

    private void updateListResults() {
        List<Situation> out = new ArrayList<>();
        ListFilter f = listFilter.getValue();
        String q = roadQuery.getValue();
        if (f != ListFilter.ROADWORKS) addMatching(out, incidents.getValue(), q);
        if (f != ListFilter.INCIDENTS) addMatching(out, roadworks.getValue(), q);
        listResults.setValue(out);
    }

    private static void addMatching(List<Situation> out, List<Situation> source, String query) {
        if (source == null) return;
        for (Situation s : source) {
            if (query == null || query.isEmpty() || s.isOnRoad(query)) out.add(s);
        }
    }

    private void updateNearby() {
        double[] c = searchCentre.getValue();
        Integer r = radiusKm.getValue();
        List<Situation> out = new ArrayList<>();
        if (c != null && r != null) {
            for (List<Situation> list : java.util.Arrays.asList(incidents.getValue(), roadworks.getValue())) {
                if (list == null) continue;
                for (Situation s : list) {
                    double d = s.distanceKmFrom(c[0], c[1]);
                    if (d >= 0 && d <= r) out.add(s);
                }
            }
            Collections.sort(out, Comparator.comparingDouble(s -> s.distanceKmFrom(c[0], c[1])));
        }
        nearbyResults.setValue(out);
    }

    public Situation findByKey(String key) {
        if (key == null) return null;
        for (List<Situation> list : java.util.Arrays.asList(incidents.getValue(), roadworks.getValue())) {
            if (list == null) continue;
            for (Situation s : list) if (s.getKey().equals(key)) return s;
        }
        return null;
    }

    public List<Situation> getMapSituations() {
        MapSource src = mapSource.getValue();
        if (src == MapSource.LIST_RESULTS && listResults.getValue() != null) return listResults.getValue();
        if (src == MapSource.NEARBY && nearbyResults.getValue() != null) return nearbyResults.getValue();
        List<Situation> all = new ArrayList<>();
        if (incidents.getValue() != null) all.addAll(incidents.getValue());
        if (roadworks.getValue() != null) all.addAll(roadworks.getValue());
        return all;
    }

    private SharedPreferences settings() {
        return getApplication().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public LiveData<List<Situation>> getIncidents() { return incidents; }
    public LiveData<List<Situation>> getRoadworks() { return roadworks; }
    public LiveData<FeedResult> getLastResult() { return lastResult; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<Boolean> getOnline() { return online; }
    public LiveData<Long> getIntervalMinutes() { return intervalMinutes; }
    public LiveData<ListFilter> getListFilter() { return listFilter; }
    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<List<Situation>> getListResults() { return listResults; }
    public LiveData<Integer> getRadiusKm() { return radiusKm; }
    public LiveData<double[]> getSearchCentre() { return searchCentre; }
    public LiveData<String> getSearchCentreName() { return searchCentreName; }
    public LiveData<Boolean> getUsingDeviceLocation() { return useDeviceLocation; }
    public LiveData<List<Situation>> getNearbyResults() { return nearbyResults; }
    public LiveData<String> getSelectedKey() { return selectedKey; }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<MapSource> getMapSource() { return mapSource; }
    public LiveData<String> getMapFocusKey() { return mapFocusKey; }
    public LiveData<Event<Integer>> getTabRequest() { return tabRequest; }
}
