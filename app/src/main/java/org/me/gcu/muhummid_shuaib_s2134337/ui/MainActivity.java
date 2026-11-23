package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.muhummid_shuaib_s2134337.R;
import org.me.gcu.muhummid_shuaib_s2134337.data.FeedResult;
import org.me.gcu.muhummid_shuaib_s2134337.model.DurationCategory;
import org.me.gcu.muhummid_shuaib_s2134337.model.SituationType;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int TAB_LIST = 0;
    public static final int TAB_NEARBY = 1;
    public static final int TAB_MAP = 2;
    private static final String[] TAB_TAGS = {"tab_list", "tab_nearby", "tab_map"};

    private TrafficViewModel vm;
    private NavigationBarView nav;
    private View detailContainer;
    private TextView banner;
    private boolean twoPane;
    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        twoPane = getResources().getBoolean(R.bool.two_pane);
        detailContainer = findViewById(R.id.detail_container);
        banner = findViewById(R.id.status_banner);
        LinearProgressIndicator progress = findViewById(R.id.progress);
        nav = findViewById(twoPane ? R.id.nav_rail : R.id.bottom_nav);

        vm = new ViewModelProvider(this).get(TrafficViewModel.class);
        vm.start(readMetaData("TS_CLIENT_ID"), readMetaData("TS_CLIENT_KEY"));

        if (savedInstanceState == null || getSupportFragmentManager().findFragmentById(R.id.detail_container) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new SituationDetailFragment()).commit();
        }

        int tab = vm.getCurrentTab();
        nav.setSelectedItemId(menuIdForTab(tab));
        showTab(tab);
        nav.setOnItemSelectedListener(item -> {
            showTab(tabForMenuId(item.getItemId()));
            return true;
        });
        nav.setOnItemReselectedListener(item -> { });

        OnBackPressedCallback closeDetail = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { vm.clearSelection(); }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDetail);
        vm.getSelectedKey().observe(this, key -> {
            boolean hasSelection = key != null;
            if (!twoPane) detailContainer.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
            closeDetail.setEnabled(hasSelection && !twoPane);
        });

        vm.getTabRequest().observe(this, event -> {
            Integer requested = event.getContentIfNotHandled();
            if (requested == null) return;
            if (!twoPane && requested == TAB_MAP) vm.clearSelection();
            nav.setSelectedItemId(menuIdForTab(requested));
        });

        vm.getLoading().observe(this, isLoading -> {
            progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
            updateBanner();
        });
        vm.getProgress().observe(this, p -> progress.setProgressCompat(p == null ? 0 : p, true));
        vm.getLastResult().observe(this, r -> updateBanner());
        vm.getOnline().observe(this, o -> updateBanner());
        vm.getIntervalMinutes().observe(this, m -> { updateBanner(); updateIntervalCheck(); });
        vm.getListResults().observe(this, l -> updateBanner());
    }

    private String readMetaData(String name) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData == null) return "";
            String value = ai.metaData.getString(getPackageName() + "." + name);
            return value == null ? "" : value;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read " + name, e);
            return "";
        }
    }

    private void showTab(int tab) {
        vm.setCurrentTab(tab);
        String tag = TAB_TAGS[tab];
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (current != null && tag.equals(current.getTag())) return;
        Fragment f = tab == TAB_NEARBY ? new NearbyFragment()
                : tab == TAB_MAP ? new TrafficMapFragment() : new SituationListFragment();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.main_container, f, tag)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(tab == TAB_NEARBY ? R.string.tab_nearby
                    : tab == TAB_MAP ? R.string.tab_map : R.string.app_name);
        }
    }

    private static int tabForMenuId(int id) {
        if (id == R.id.nav_nearby) return TAB_NEARBY;
        if (id == R.id.nav_map) return TAB_MAP;
        return TAB_LIST;
    }

    private static int menuIdForTab(int tab) {
        return tab == TAB_NEARBY ? R.id.nav_nearby : tab == TAB_MAP ? R.id.nav_map : R.id.nav_list;
    }

    private void updateBanner() {
        FeedResult r = vm.getLastResult().getValue();
        boolean online = !Boolean.FALSE.equals(vm.getOnline().getValue());
        boolean loading = Boolean.TRUE.equals(vm.getLoading().getValue());
        Long interval = vm.getIntervalMinutes().getValue();

        StringBuilder sb = new StringBuilder();
        if (!online) sb.append("OFFLINE - showing saved data. ");
        if (loading) sb.append("Updating from Traffic Scotland... ");
        if (r != null) {
            if (r.downloadedAt > 0) {
                sb.append("Updated ").append(DateUtils.getRelativeTimeSpanString(r.downloadedAt,
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)).append(". ");
            }
            sb.append(String.format(Locale.UK, "%d incidents, %d roadworks.",
                    r.incidents.size(), r.roadworks.size()));
            if (r.error != null && online) sb.append(" Last update failed: ").append(r.error).append('.');
            else if (r.fromCache && online && !loading) sb.append(" (saved copy)");
        } else if (!loading) {
            sb.append("No data yet.");
        }
        if (interval != null) {
            sb.append(interval == 1 ? " Auto-update every minute."
                    : interval < 60 ? " Auto-update every " + interval + " min."
                    : " Auto-update hourly.");
        }
        banner.setText(sb.toString().trim());
        boolean bad = !online || (r != null && r.error != null);
        banner.setBackgroundColor(ContextCompat.getColor(this, bad ? R.color.banner_offline : R.color.banner_ok));
        banner.setTextColor(ContextCompat.getColor(this, bad ? R.color.banner_offline_text : R.color.banner_ok_text));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        optionsMenu = menu;
        updateIntervalCheck();
        return true;
    }

    private void updateIntervalCheck() {
        if (optionsMenu == null) return;
        Long m = vm.getIntervalMinutes().getValue();
        int id = m != null && m == 1 ? R.id.interval_1 : m != null && m == 15 ? R.id.interval_15 : R.id.interval_60;
        MenuItem item = optionsMenu.findItem(id);
        if (item != null) item.setChecked(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            vm.refresh();
            return true;
        } else if (id == R.id.interval_1) {
            vm.setUpdateInterval(1);
            return true;
        } else if (id == R.id.interval_15) {
            vm.setUpdateInterval(15);
            return true;
        } else if (id == R.id.interval_60) {
            vm.setUpdateInterval(60);
            return true;
        } else if (id == R.id.action_legend) {
            showLegend();
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLegend() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);
        box.addView(heading("Situation types"));
        for (SituationType t : SituationType.values()) {
            box.addView(legendRow(t.getIconRes(), t.getColour(), t.getLabel()));
        }
        box.addView(heading("Roadworks duration (colour of bar, badge and map marker)"));
        for (DurationCategory d : DurationCategory.values()) {
            String text = d == DurationCategory.SHORT ? "Short: " + DurationCategory.SHORT_MAX_DAYS + " days or less"
                    : d == DurationCategory.MEDIUM ? "Medium: up to " + DurationCategory.MEDIUM_MAX_DAYS + " days"
                    : d == DurationCategory.LONG ? "Long: more than " + DurationCategory.MEDIUM_MAX_DAYS + " days"
                    : "Unknown: no end date";
            box.addView(legendRow(R.drawable.ic_type_roadworks, d.getColour(), text));
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(box);
        new AlertDialog.Builder(this).setTitle(R.string.legend).setView(scroll)
                .setPositiveButton(R.string.close, null).show();
    }

    private TextView heading(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(12), 0, dp(6));
        return tv;
    }

    private View legendRow(int iconRes, int colour, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setBackgroundResource(R.drawable.bg_circle);
        icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colour));
        int p = dp(5);
        icon.setPadding(p, p, p, p);
        row.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setPadding(dp(12), 0, 0, 0);
        row.addView(tv);
        return row;
    }

    private void showAbout() {
        String text = "Scot Roads Live shows current incidents and roadworks on Scotland's trunk roads.\n\n"
                + "Contains Traffic Scotland data (DATEX II feeds: Unplanned Events and Current Roadworks). "
                + "Data is downloaded in the background, saved on the phone for offline use, "
                + "and updated automatically.\n\n"
                + "Icons: Material Icons by Google (Apache License 2.0, fonts.google.com/icons). "
                + "Roadworks cone and app icons drawn for this app.\n\n"
                + "Maps: Google Maps SDK for Android.\n\n"
                + "Shuaib Muhummid, S2134337 - Mobile Platform Development, GCU.";
        new AlertDialog.Builder(this).setTitle(R.string.about).setMessage(text)
                .setPositiveButton(R.string.close, null).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
