package org.me.gcu.muhummid_shuaib_s2134337.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import org.me.gcu.muhummid_shuaib_s2134337.R;
import org.me.gcu.muhummid_shuaib_s2134337.model.Situation;

import java.util.List;
import java.util.Locale;

public class SituationListFragment extends Fragment {
    private TrafficViewModel vm;
    private SituationAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        RecyclerView recycler = view.findViewById(R.id.recycler);
        TextView count = view.findViewById(R.id.result_count);
        TextView empty = view.findViewById(R.id.empty_text);
        EditText search = view.findViewById(R.id.search_input);
        ChipGroup filters = view.findViewById(R.id.filter_group);
        Button mapButton = view.findViewById(R.id.map_results_button);

        adapter = new SituationAdapter(vm::select);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        String q = vm.getRoadQuery().getValue();
        if (q != null && !q.equals(search.getText().toString())) search.setText(q);
        TrafficViewModel.ListFilter f = vm.getListFilter().getValue();
        filters.check(f == TrafficViewModel.ListFilter.INCIDENTS ? R.id.chip_incidents
                : f == TrafficViewModel.ListFilter.ROADWORKS ? R.id.chip_roadworks : R.id.chip_all);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { vm.setRoadQuery(s.toString()); }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        filters.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            vm.setListFilter(id == R.id.chip_incidents ? TrafficViewModel.ListFilter.INCIDENTS
                    : id == R.id.chip_roadworks ? TrafficViewModel.ListFilter.ROADWORKS
                    : TrafficViewModel.ListFilter.ALL);
        });

        mapButton.setOnClickListener(v -> vm.showOnMap(TrafficViewModel.MapSource.LIST_RESULTS, null));

        vm.getListResults().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list, null);
            int n = list == null ? 0 : list.size();
            String query = vm.getRoadQuery().getValue();
            count.setText(query == null || query.isEmpty()
                    ? String.format(Locale.UK, "%d situations%s", n, breakdown(list))
                    : String.format(Locale.UK, "%d on \"%s\"%s", n, query, breakdown(list)));
            empty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
            mapButton.setEnabled(n > 0);
        });
        vm.getSelectedKey().observe(getViewLifecycleOwner(), adapter::setSelectedKey);
    }

    private static String breakdown(List<Situation> list) {
        if (list == null || list.isEmpty()) return "";
        int rw = 0;
        for (Situation s : list) if (s.isRoadworks()) rw++;
        return String.format(Locale.UK, " (%d incidents, %d roadworks)", list.size() - rw, rw);
    }
}
