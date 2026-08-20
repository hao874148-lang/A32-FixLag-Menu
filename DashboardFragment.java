package com.a32.fixlag.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.a32.fixlag.R;
import com.a32.fixlag.commands.CommandRegistry;
import com.a32.fixlag.data.AppPreferences;
import com.a32.fixlag.data.SystemMonitor;

/**
 * DashboardFragment - Hiển thị Telemetry Realtime & Thao tác VIP Pro Max
 * Package: com.a32.fixlag.ui
 */
public class DashboardFragment extends Fragment implements SystemMonitor.OnSystemUpdateListener {

    private TextView tvHealthScore, tvCpuUsage, tvCpuFreq, tvGpuUsage, tvGpuFreq;
    private TextView tvRamUsage, tvSwapUsage, tvTemperature, tvPowerWatts, tvConsoleLog;
    private Switch switchAiAutoPilot;
    private Button btnQuickCleanRam, btnQuickTurboGame;

    private SystemMonitor systemMonitor;
    private AppPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initViews(view);
        preferences = AppPreferences.getInstance(requireContext());
        systemMonitor = SystemMonitor.getInstance(requireContext());

        setupListeners();
        return view;
    }

    private void initViews(View view) {
        tvHealthScore = view.findViewById(R.id.tvHealthScore);
        tvCpuUsage = view.findViewById(R.id.tvCpuUsage);
        tvCpuFreq = view.findViewById(R.id.tvCpuFreq);
        tvGpuUsage = view.findViewById(R.id.tvGpuUsage);
        tvGpuFreq = view.findViewById(R.id.tvGpuFreq);
        tvRamUsage = view.findViewById(R.id.tvRamUsage);
        tvSwapUsage = view.findViewById(R.id.tvSwapUsage);
        tvTemperature = view.findViewById(R.id.tvTemperature);
        tvPowerWatts = view.findViewById(R.id.tvPowerWatts);
        tvConsoleLog = view.findViewById(R.id.tvConsoleLog);

        switchAiAutoPilot = view.findViewById(R.id.switchAiAutoPilot);
        btnQuickCleanRam = view.findViewById(R.id.btnQuickCleanRam);
        btnQuickTurboGame = view.findViewById(R.id.btnQuickTurboGame);
    }

    private void setupListeners() {
        switchAiAutoPilot.setChecked(preferences.isAiAutoPilotEnabled());
        switchAiAutoPilot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setAiAutoPilotEnabled(isChecked);
            if (isChecked) {
                String result = CommandRegistry.startAiAutoPilot(requireContext());
                appendLog(result);
            } else {
                String result = CommandRegistry.stopAiAutoPilot();
                appendLog(result);
            }
        });

        btnQuickCleanRam.setOnClickListener(v -> {
            String result = CommandRegistry.quickRamClean(requireContext());
            appendLog(result);
        });

        btnQuickTurboGame.setOnClickListener(v -> {
            String result = CommandRegistry.applyUltimateGamingMode(requireContext(), null);
            appendLog(result);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        systemMonitor.setOnSystemUpdateListener(this);
        systemMonitor.startMonitoring(1000); // Cập nhật Telemetry mỗi 1000ms
    }

    @Override
    public void onPause() {
        super.onPause();
        systemMonitor.stopMonitoring();
    }

    @Override
    public void onSystemUpdate(SystemMonitor.SystemData data) {
        if (!isAdded()) return;

        tvHealthScore.setText(String.valueOf(data.systemHealthScore));
        tvCpuUsage.setText(String.format("%.1f%%", data.cpuUsagePercent));
        tvCpuFreq.setText(String.format("A75 Max: %d MHz", data.bigClusterMaxMhz));

        tvGpuUsage.setText(String.format("%.1f%%", data.gpuUsagePercent));
        tvGpuFreq.setText(String.format("Freq: %d MHz", data.gpuFrequencyMhz));

        tvRamUsage.setText(String.format("%d / %d MB", data.usedRamMB, data.totalRamMB));
        tvSwapUsage.setText(String.format("zRAM: %d MB", data.usedSwapMB));

        tvTemperature.setText(String.format("%.1f°C", data.cpuTempCelsius));
        tvPowerWatts.setText(String.format("Power: %.2f W", data.powerDrainWatts));
    }

    private void appendLog(String message) {
        if (tvConsoleLog != null) {
            tvConsoleLog.append("\n\n" + message);
        }
    }
}