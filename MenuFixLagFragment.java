package com.a32.fixlag.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.a32.fixlag.R;
import com.a32.fixlag.commands.CommandRegistry;
import com.a32.fixlag.commands.DisplayCommands;

/**
 * MenuFixLagFragment - Menu điều khiển tinh chỉnh VIP Pro Max (Đã sửa lỗi)
 * Package: com.a32.fixlag.ui
 */
public class MenuFixLagFragment extends Fragment {

    private RadioGroup rgSystemProfiles;
    private RadioButton rbUltimateGaming, rbDailyBalanced, rbBatterySaver;
    private Button btnApplyProfile, btnForce90Hz, btnForce60Hz;
    private Button btnRes1080p, btnRes900p, btnRes810p, btnRes720p, btnRes640p, btnRes540p;
    private Button btnToggleGos, btnRestoreDefault;

    private boolean isGosDisabled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        initViews(view);
        setupActions();
        return view;
    }

    private void initViews(View view) {
        rgSystemProfiles = view.findViewById(R.id.rgSystemProfiles);
        rbUltimateGaming = view.findViewById(R.id.rbUltimateGaming);
        rbDailyBalanced = view.findViewById(R.id.rbDailyBalanced);
        rbBatterySaver = view.findViewById(R.id.rbBatterySaver);

        btnApplyProfile = view.findViewById(R.id.btnApplyProfile);
        btnForce90Hz = view.findViewById(R.id.btnForce90Hz);
        btnForce60Hz = view.findViewById(R.id.btnForce60Hz);

        btnRes1080p = view.findViewById(R.id.btnRes1080p);
        btnRes900p = view.findViewById(R.id.btnRes900p);
        btnRes810p = view.findViewById(R.id.btnRes810p);
        btnRes720p = view.findViewById(R.id.btnRes720p);
        btnRes640p = view.findViewById(R.id.btnRes640p);
        btnRes540p = view.findViewById(R.id.btnRes540p);

        btnToggleGos = view.findViewById(R.id.btnToggleGos);
        btnRestoreDefault = view.findViewById(R.id.btnRestoreDefault);
    }

    private void setupActions() {
        // 1. Áp dụng Profile hệ thống
        btnApplyProfile.setOnClickListener(v -> new Thread(() -> {
            int selectedId = rgSystemProfiles.getCheckedRadioButtonId();
            String result;
            if (selectedId == R.id.rbUltimateGaming) {
                result = CommandRegistry.applyUltimateGamingMode(requireContext(), null);
            } else if (selectedId == R.id.rbBatterySaver) {
                result = CommandRegistry.applyBatterySaverMode(requireContext());
            } else {
                result = CommandRegistry.applyDailyBalancedMode(requireContext());
            }
            showToast(result);
        }).start());

        // 2. Ép tần số quét 90Hz / 60Hz
        btnForce90Hz.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.setRefreshRate(requireContext(), DisplayCommands.RefreshRateMode.HZ_90_LOCKED);
            showToast(res);
        }).start());

        btnForce60Hz.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.setRefreshRate(requireContext(), DisplayCommands.RefreshRateMode.HZ_60_LOCKED);
            showToast(res);
        }).start());

        // 3. Ma trận độ phân giải (Tùy chỉnh trực tiếp qua DisplayCommands)
        btnRes1080p.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.setResolution(requireContext(), DisplayCommands.ResolutionProfile.FHD_1080P_NATIVE);
            showToast(res);
        }).start());

        btnRes900p.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.setResolution(requireContext(), DisplayCommands.ResolutionProfile.BALANCED_900P);
            showToast(res);
        }).start());

        btnRes810p.setOnClickListener(v -> new Thread(() -> {
            String res = DisplayCommands.setCustomResolution(requireContext(), 810, 1800, 315);
            showToast(res);
        }).start());

        btnRes720p.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.setResolution(requireContext(), DisplayCommands.ResolutionProfile.HD_720P_GAMING);
            showToast(res);
        }).start());

        btnRes640p.setOnClickListener(v -> new Thread(() -> {
            String res = DisplayCommands.setCustomResolution(requireContext(), 640, 1422, 250);
            showToast(res);
        }).start());

        btnRes540p.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.setResolution(requireContext(), DisplayCommands.ResolutionProfile.EXTREME_540P);
            showToast(res);
        }).start());

        // 4. Bật / Tắt Samsung GOS Throttling
        btnToggleGos.setOnClickListener(v -> new Thread(() -> {
            isGosDisabled = !isGosDisabled;
            String res = CommandRegistry.toggleSamsungGos(requireContext(), isGosDisabled);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> 
                    btnToggleGos.setText(isGosDisabled ? "Khôi Phục Dịch Vụ GOS" : "Vô Hiệu Hóa Samsung GOS Throttling")
                );
            }
            showToast(res);
        }).start());

        // 5. Khôi phục mặc định
        btnRestoreDefault.setOnClickListener(v -> new Thread(() -> {
            String res = CommandRegistry.restoreSystemToDefault(requireContext());
            showToast(res);
        }).start());
    }

    private void showToast(String text) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show());
    }
}