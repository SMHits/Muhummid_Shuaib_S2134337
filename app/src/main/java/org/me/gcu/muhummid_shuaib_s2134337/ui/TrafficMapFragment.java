package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.muhummid_shuaib_s2134337.R;
import org.me.gcu.muhummid_shuaib_s2134337.model.LocationPoint;
import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrafficMapFragment extends Fragment {
    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private TrafficViewModel vm;
    private GoogleMap map;
    private TextView info;
    private final Map<String, Marker> markers = new HashMap<>();
    private final Map<String, BitmapDescriptor> iconCache = new HashMap<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        info = view.findViewById(R.id.map_info);
        ChipGroup sourceGroup = view.findViewById(R.id.source_group);

        TrafficViewModel.MapSource src = vm.getMapSource().getValue();
        sourceGroup.check(src == TrafficViewModel.MapSource.LIST_RESULTS ? R.id.chip_map_list
                : src == TrafficViewModel.MapSource.NEARBY ? R.id.chip_map_nearby : R.id.chip_map_all);
        sourceGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            TrafficViewModel.MapSource chosen = id == R.id.chip_map_list ? TrafficViewModel.MapSource.LIST_RESULTS
                    : id == R.id.chip_map_nearby ? TrafficViewModel.MapSource.NEARBY
                    : TrafficViewModel.MapSource.ALL;
            if (chosen != vm.getMapSource().getValue()) vm.setMapSource(chosen);
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentByTag("google_map");
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_holder, mapFragment, "google_map").commitNow();
        }
        mapFragment.getMapAsync(this::onMapReady);
    }

    private void onMapReady(GoogleMap googleMap) {
        if (!isAdded()) return;
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
        map.setOnInfoWindowClickListener(m -> {
            Object key = m.getTag();
            if (key instanceof String) vm.selectKey((String) key);
        });

        vm.getMapSource().observe(getViewLifecycleOwner(), s -> redraw(true));
        vm.getListResults().observe(getViewLifecycleOwner(), l -> redraw(false));
        vm.getNearbyResults().observe(getViewLifecycleOwner(), l -> redraw(false));
        vm.getMapFocusKey().observe(getViewLifecycleOwner(), key -> focus(key));
        vm.getSelectedKey().observe(getViewLifecycleOwner(), key -> {
            Marker m = key == null ? null : markers.get(key);
            if (m != null) m.showInfoWindow();
        });
    }

    private void redraw(boolean moveCamera) {
        if (map == null) return;
        map.clear();
        markers.clear();
        TrafficViewModel.MapSource src = vm.getMapSource().getValue();
        List<Situation> list = vm.getMapSituations();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int shown = 0;

        if (src == TrafficViewModel.MapSource.NEARBY) {
            double[] c = vm.getSearchCentre().getValue();
            Integer r = vm.getRadiusKm().getValue();
            if (c != null && r != null) {
                LatLng centre = new LatLng(c[0], c[1]);
                map.addCircle(new CircleOptions().center(centre).radius(r * 1000.0)
                        .strokeColor(0xAA0B4F8A).strokeWidth(3f).fillColor(0x220B4F8A));
                map.addMarker(new MarkerOptions().position(centre).title(vm.getSearchCentreName().getValue())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                bounds.include(centre);
            }
        }

        for (Situation s : list) {
            double[] centre = s.getLocation().getCentre();
            if (centre == null) continue;
            int colour = s.isRoadworks() ? s.getDurationCategory().getColour() : s.getType().getColour();
            LatLng pos = new LatLng(centre[0], centre[1]);

            if (s.getLocation().isLine()) {
                LocationPoint from = s.getLocation().getFrom(), to = s.getLocation().getTo();
                map.addPolyline(new PolylineOptions()
                        .add(new LatLng(from.getLatitude(), from.getLongitude()),
                                new LatLng(to.getLatitude(), to.getLongitude()))
                        .color(colour).width(8f));
            }
            Marker m = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(s.getTypeLabel() + " - " + s.getRoadName())
                    .snippet(shorten(s.getComment()) + "  (tap for details)")
                    .icon(markerIcon(s, colour)));
            if (m != null) {
                m.setTag(s.getKey());
                markers.put(s.getKey(), m);
            }
            bounds.include(pos);
            shown++;
        }

        String label = src == TrafficViewModel.MapSource.LIST_RESULTS ? "situations from the search"
                : src == TrafficViewModel.MapSource.NEARBY ? "nearby situations" : "situations (all)";
        if (src == TrafficViewModel.MapSource.LIST_RESULTS) {
            String q = vm.getRoadQuery().getValue();
            if (q != null && !q.isEmpty()) label += " for \"" + q + "\"";
        }
        info.setText(String.format(Locale.UK, "Showing %d %s. Tap a marker, then its label, for details.",
                shown, label));

        String focusKey = vm.getMapFocusKey().getValue();
        if (focusKey != null && markers.containsKey(focusKey)) {
            focus(focusKey);
        } else if (moveCamera && shown > 0) {
            try {
                int padding = Math.round(48 * getResources().getDisplayMetrics().density);
                if (shown == 1) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.build().getCenter(), 11f));
                } else {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding));
                }
            } catch (IllegalStateException e) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
            }
        }
    }

    private void focus(String key) {
        if (map == null || key == null) return;
        Marker m = markers.get(key);
        if (m == null) return;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(), 12f));
        m.showInfoWindow();
    }

    private static String shorten(String text) {
        return text.length() > 90 ? text.substring(0, 87) + "..." : text;
    }

    private BitmapDescriptor markerIcon(Situation s, int colour) {
        int iconRes = s.isRoadworks() ? R.drawable.ic_type_roadworks : s.getType().getIconRes();
        String cacheKey = iconRes + ":" + colour;
        BitmapDescriptor cached = iconCache.get(cacheKey);
        if (cached != null) return cached;

        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(26 * density);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setColor(colour);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2 * density, paint);
        Drawable d = ContextCompat.getDrawable(requireContext(), iconRes);
        if (d != null) {
            int inset = Math.round(5 * density);
            d.setBounds(inset, inset, size - inset, size - inset);
            d.draw(canvas);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bmp);
        iconCache.put(cacheKey, descriptor);
        return descriptor;
    }
}
