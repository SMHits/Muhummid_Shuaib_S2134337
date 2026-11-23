package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.muhummid_shuaib_s2134337.R;
import org.me.gcu.muhummid_shuaib_s2134337.model.DurationCategory;
import org.me.gcu.muhummid_shuaib_s2134337.model.LocationPoint;
import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;
import org.me.gcu.muhummid_shuaib_s2134337.model.SituationLocation;

import java.util.Locale;
import java.util.Map;

public class SituationDetailFragment extends Fragment {
    private TrafficViewModel vm;
    private View emptyText, scroll, header;
    private ImageView icon;
    private TextView typeText, roadText, commentText, durationBadge;
    private LinearLayout fields;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        emptyText = view.findViewById(R.id.empty_text);
        scroll = view.findViewById(R.id.detail_scroll);
        header = view.findViewById(R.id.header);
        icon = view.findViewById(R.id.type_icon);
        typeText = view.findViewById(R.id.type_text);
        roadText = view.findViewById(R.id.road_text);
        commentText = view.findViewById(R.id.comment_text);
        durationBadge = view.findViewById(R.id.duration_badge);
        fields = view.findViewById(R.id.fields);
        Button mapButton = view.findViewById(R.id.map_button);
        Button closeButton = view.findViewById(R.id.close_button);

        mapButton.setOnClickListener(v -> {
            Situation s = vm.getSelected().getValue();
            if (s != null) vm.showOnMap(TrafficViewModel.MapSource.ALL, s.getKey());
        });
        closeButton.setOnClickListener(v -> vm.clearSelection());

        vm.getSelected().observe(getViewLifecycleOwner(), this::show);
    }

    private void show(Situation s) {
        if (s == null) {
            emptyText.setVisibility(View.VISIBLE);
            scroll.setVisibility(View.GONE);
            return;
        }
        emptyText.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
        if (scroll instanceof ScrollView) ((ScrollView) scroll).scrollTo(0, 0);

        int typeColour = s.getType().getColour();
        header.setBackgroundColor(typeColour);
        icon.setImageResource(s.isRoadworks() ? R.drawable.ic_type_roadworks : s.getType().getIconRes());
        icon.setBackgroundTintList(ColorStateList.valueOf(0x33FFFFFF));
        typeText.setText(s.getTypeLabel());
        roadText.setText(s.getRoadName());
        commentText.setText(s.getComment().isEmpty() ? "No description given." : s.getComment());

        if (s.isRoadworks()) {
            DurationCategory dc = s.getDurationCategory();
            durationBadge.setVisibility(View.VISIBLE);
            durationBadge.setText(String.format(Locale.UK, "%s roadworks: %s", dc.getLabel(), s.getDurationText()));
            durationBadge.setBackgroundTintList(ColorStateList.valueOf(dc.getColour()));
        } else {
            durationBadge.setVisibility(View.GONE);
        }

        fields.removeAllViews();
        addRow("Category", s.isRoadworks() ? "Planned roadworks" : "Unplanned event (incident)");
        String sub = Situation.readable(s.getSubType());
        addRow("Type", s.getType().getLabel()
                + (sub.isEmpty() || sub.equalsIgnoreCase(s.getType().getLabel()) ? "" : " - " + sub));
        addRow("Road", s.getRoadName());

        SituationLocation loc = s.getLocation();
        if (loc != null) {
            addRow("Direction", loc.getReadableDirection());
            addPoint(loc.isLine() ? "From" : "Location", loc.getFrom() != null ? loc.getFrom() : loc.getSingle());
            if (loc.isLine()) addPoint("To", loc.getTo());
        }

        addRow("Starts", Situation.formatTime(s.getStartTime()));
        addRow("Ends", Situation.formatTime(s.getEndTime()));
        if (s.getDurationDays() >= 0 || s.isRoadworks()) addRow("Planned duration", s.getDurationText());
        addRow("Impact", s.getImpactText());
        if (s.getOperationalLanes() >= 0) addRow("Lanes open", String.valueOf(s.getOperationalLanes()));
        if (s.getLanesRestricted() >= 0) addRow("Lanes restricted", String.valueOf(s.getLanesRestricted()));
        addRow("Likelihood", Situation.readable(s.getProbability()));
        addRow("Status", Situation.readable(s.getValidityStatus()));
        addRow("Instruction", Situation.readable(s.getComplianceOption()));
        if (s.getUrgentRoadworks() != null) addRow("Urgent", s.getUrgentRoadworks() ? "Yes" : "No");
        addRow("Source", s.getSourceName());
        addRow("First reported", Situation.formatTime(s.getCreationTime()));
        addRow("Last updated", Situation.formatTime(s.getVersionTime()));
        addRow("Record ID", s.getRecordId() + (s.getVersion().isEmpty() ? "" : " (version " + s.getVersion() + ")"));
    }

    private void addPoint(String label, LocationPoint p) {
        if (p == null) return;
        StringBuilder sb = new StringBuilder();
        String place = p.getPlaceName();
        if (place != null && !place.isEmpty()) sb.append(place);
        for (Map.Entry<String, String> e : p.getDescriptors().entrySet()) {
            String value = e.getValue();
            if (value == null || value.equals(place)) continue;
            String key = e.getKey();

            if ((key.equals("linkName") || key.equals("tpegIlcName1")) && value.equals(p.getRoadName())) continue;
            String name;
            if (key.startsWith("tpegIlcName1") || key.startsWith("linkName")) name = "Road";
            else if (key.startsWith("tpegIlcName2")) name = "Meets";
            else if (key.startsWith("tpegIlcName3")) name = "Area";
            else name = Situation.readable(key);
            if (sb.length() > 0) sb.append("\n");
            sb.append(name).append(": ").append(value);
        }
        if (p.hasCoordinates()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(String.format(Locale.UK, "%.5f, %.5f", p.getLatitude(), p.getLongitude()));
        }
        addRow(label, sb.toString());
    }

    private void addRow(String label, String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) return;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(6);
        row.setPadding(0, pad, 0, pad);

        TextView l = new TextView(requireContext());
        l.setText(label.toUpperCase(Locale.UK));
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        l.setTypeface(Typeface.DEFAULT_BOLD);
        l.setLetterSpacing(0.05f);
        l.setTextColor(0xFF5F6B7A);

        TextView v = new TextView(requireContext());
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        v.setTextColor(0xFF1B1F24);
        v.setTextIsSelectable(true);

        row.addView(l);
        row.addView(v);
        fields.addView(row);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
