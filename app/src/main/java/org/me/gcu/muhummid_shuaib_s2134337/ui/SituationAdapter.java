package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.muhummid_shuaib_s2134337.R;
import org.me.gcu.muhummid_shuaib_s2134337.model.DurationCategory;
import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.Holder> {
    public interface OnSituationClick { void onClick(Situation s); }

    private final List<Situation> items = new ArrayList<>();
    private final OnSituationClick listener;
    private double[] distanceFrom;
    private String selectedKey;

    public SituationAdapter(OnSituationClick listener) { this.listener = listener; }

    public void submit(List<Situation> list, double[] distanceFrom) {
        items.clear();
        if (list != null) items.addAll(list);
        this.distanceFrom = distanceFrom;
        notifyDataSetChanged();
    }

    public void setSelectedKey(String key) {
        selectedKey = key;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Situation s = items.get(position);
        h.icon.setImageResource(s.isRoadworks() ? R.drawable.ic_type_roadworks : s.getType().getIconRes());
        h.icon.setBackgroundTintList(ColorStateList.valueOf(s.getType().getColour()));
        h.type.setText(s.getTypeLabel());
        h.road.setText(s.getRoadName());
        h.comment.setText(s.getComment());

        String extra;
        int colour;
        if (s.isRoadworks()) {
            DurationCategory dc = s.getDurationCategory();
            colour = dc.getColour();
            extra = dc.getLabel() + " · " + s.getDurationText();
        } else {
            colour = s.getType().getColour();
            extra = s.getImpactText().equals("Not given") ? Situation.readable(s.getSubType()) : s.getImpactText();
        }
        if (distanceFrom != null) {
            double km = s.distanceKmFrom(distanceFrom[0], distanceFrom[1]);
            extra = String.format(Locale.UK, "%.1f km away · %s", km, extra);
        }
        h.colourBar.setBackgroundColor(colour);
        h.extra.setText(extra);
        h.extra.setBackgroundTintList(ColorStateList.valueOf(colour));
        h.extra.setVisibility(extra.isEmpty() ? View.GONE : View.VISIBLE);

        boolean selected = s.getKey().equals(selectedKey);
        h.itemView.setActivated(selected);
        h.itemView.setAlpha(1f);
        ((com.google.android.material.card.MaterialCardView) h.itemView).setStrokeWidth(selected ? 4 : 0);
        h.itemView.setContentDescription(s.getTypeLabel() + " on " + s.getRoadName() + ". " + s.getComment());
        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final View colourBar;
        final ImageView icon;
        final TextView type, road, comment, extra;

        Holder(View v) {
            super(v);
            colourBar = v.findViewById(R.id.colour_bar);
            icon = v.findViewById(R.id.type_icon);
            type = v.findViewById(R.id.type_text);
            road = v.findViewById(R.id.road_text);
            comment = v.findViewById(R.id.comment_text);
            extra = v.findViewById(R.id.extra_text);
        }
    }
}
