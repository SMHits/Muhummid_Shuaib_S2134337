package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;

import org.me.gcu.muhummid_shuaib_s2134337.R;

import java.util.Locale;
import java.util.Map;

public class NearbyFragment extends Fragment {
    private TrafficViewModel vm;
    private SituationAdapter adapter;
    private Spinner placeSpinner;
    private boolean ignoreSpinner = true;
    private LocationManager locationManager;
    private LocationListener pendingListener;
    private TextView centreText;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    (Map<String, Boolean> result) -> {
                        if (hasLocationPermission()) {
                            findLocation();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Location permission refused - choose a place instead",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        centreText = view.findViewById(R.id.centre_text);
        Button locationButton = view.findViewById(R.id.location_button);
        placeSpinner = view.findViewById(R.id.place_spinner);
        TextView radiusText = view.findViewById(R.id.radius_text);
        Slider slider = view.findViewById(R.id.radius_slider);
        TextView count = view.findViewById(R.id.result_count);
        Button mapButton = view.findViewById(R.id.map_results_button);
        RecyclerView recycler = view.findViewById(R.id.recycler);
        TextView empty = view.findViewById(R.id.empty_text);

        adapter = new SituationAdapter(vm::select);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        ArrayAdapter<TrafficViewModel.Place> places = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, TrafficViewModel.FIXED_PLACES);
        places.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        placeSpinner.setAdapter(places);
        placeSpinner.setSelection(vm.getFixedPlaceIndex(), false);
        placeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (ignoreSpinner) { ignoreSpinner = false; return; }
                vm.useFixedPlace(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        placeSpinner.setOnTouchListener((v, e) -> { ignoreSpinner = false; return false; });

        locationButton.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                findLocation();
            } else {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION});
            }
        });

        Integer r = vm.getRadiusKm().getValue();
        slider.setValue(r == null ? 25 : r);
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) vm.setRadiusKm(Math.round(value));
        });
        vm.getRadiusKm().observe(getViewLifecycleOwner(),
                km -> radiusText.setText(String.format(Locale.UK, "Within %d km", km)));

        vm.getSearchCentreName().observe(getViewLifecycleOwner(), name -> showCentre());
        vm.getSearchCentre().observe(getViewLifecycleOwner(), c -> showCentre());

        mapButton.setOnClickListener(v -> vm.showOnMap(TrafficViewModel.MapSource.NEARBY, null));

        vm.getNearbyResults().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list, vm.getSearchCentre().getValue());
            int n = list == null ? 0 : list.size();
            count.setText(String.format(Locale.UK, "%d situation%s within %d km of %s", n, n == 1 ? "" : "s",
                    vm.getRadiusKm().getValue(), vm.getSearchCentreName().getValue()));
            empty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
            mapButton.setEnabled(n > 0);
        });
        vm.getSelectedKey().observe(getViewLifecycleOwner(), adapter::setSelectedKey);
    }

    private boolean hasLocationPermission() {
        Context c = requireContext();
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void findLocation() {
        if (locationManager == null) return;
        Location best = null;
        for (String provider : locationManager.getProviders(true)) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
        }
        if (best != null) {
            vm.useDeviceLocation(best.getLatitude(), best.getLongitude());
            return;
        }

        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps && !network) {
            Toast.makeText(requireContext(), "Location is turned off - choose a place instead",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(requireContext(), "Finding your location...", Toast.LENGTH_SHORT).show();
        stopListening();
        pendingListener = location -> {
            vm.useDeviceLocation(location.getLatitude(), location.getLongitude());
            stopListening();
        };
        Looper main = Looper.getMainLooper();
        if (gps) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, pendingListener, main);
        if (network) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, pendingListener, main);
        timeoutHandler.postDelayed(timeout, 20000);
    }

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> {
        if (pendingListener == null || !isAdded()) return;
        stopListening();
        Toast.makeText(requireContext(), "Could not find your location - choose a place instead",
                Toast.LENGTH_LONG).show();
    };

    private void showCentre() {
        String name = vm.getSearchCentreName().getValue();
        double[] c = vm.getSearchCentre().getValue();
        centreText.setText(c == null ? name
                : String.format(Locale.UK, "Searching from: %s (%.4f, %.4f)", name, c[0], c[1]));
    }

    private void stopListening() {
        timeoutHandler.removeCallbacks(timeout);
        if (pendingListener != null && locationManager != null) {
            locationManager.removeUpdates(pendingListener);
            pendingListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        stopListening();
        super.onDestroyView();
    }
}
