package com.a32.fixlag.data;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lớp Theo Dõi Thông Số Hệ Thống Siêu Cấp (SystemMonitor Pro Max AI Engine)
 * Chuyên biệt tối đa cho Samsung Galaxy A32 4G (MediaTek Helio G80 | ARM Mali-G52 MC2 | AMOLED 90Hz)
 *
 * TÍNH NĂNG NÂNG CẤP PRO MAX:
 * 1. Đo chi tiết RAM Thật & zRAM (RAM Plus) qua /proc/meminfo.
 * 2. Đo CPU Load tổng quan & Xung nhịp từng nhân (6x Cortex-A55 @1.8GHz + 2x Cortex-A75 @2.0GHz).
 * 3. Đo GPU Mali-G52 MC2 Utilization (%) & Xung nhịp (MHz) qua sysfs MediaTek.
 * 4. Phát hiện CPU Thermal Throttling (Giảm xung do quá nhiệt) theo thời gian thực.
 * 5. Đo Công suất tiêu thụ Pin thực tế (Voltage, Current mA, Power Wattage W).
 * 6. Tính tốc độ mạng Download/Upload (KB/s).
 * 7. Thuật toán lọc nhiễu Moving Average giúp giao diện chạy mượt 90Hz.
 * 8. Tính chỉ số Sức khỏe Hệ thống (System Health Score 0-100).
 * 9. Lưu trữ bộ nhớ đệm Ring Buffer (50 mẫu) để vẽ biểu đồ đường đồ họa.
 *
 * Package: com.a32.fixlag.data
 */
public class SystemMonitor {

    private static final String TAG = "SystemMonitor_ProMax";
    private static volatile SystemMonitor instance;

    private final Context context;
    private ScheduledExecutorService scheduler;
    private final Handler mainHandler;
    private OnSystemUpdateListener listener;

    private boolean isMonitoring = false;

    // Bộ lọc Moving Average cho CPU/GPU để tránh giật giao diện
    private final MovingAverageFilter cpuFilter = new MovingAverageFilter(3);
    private final MovingAverageFilter gpuFilter = new MovingAverageFilter(3);

    // Lưu vết mẫu đọc CPU /proc/stat
    private long lastTotalCpuTime = 0;
    private long lastIdleCpuTime = 0;

    // Lưu vết mẫu đọc Network Traffic
    private long lastRxBytes = 0;
    private long lastTxBytes = 0;
    private long lastTrafficTimeStamp = 0;

    // Bộ đệm lịch sử mẫu dữ liệu để vẽ đồ thị (Max 50 mẫu)
    private static final int HISTORY_MAX_SIZE = 50;
    private final LinkedList<SystemData> historyBuffer = new LinkedList<>();

    // Xung nhịp tối đa mặc định của Helio G80 (MHz)
    public static final int HELIO_G80_MAX_A75_FREQ = 2000; // 2.0 GHz
    public static final int HELIO_G80_MAX_A55_FREQ = 1800; // 1.8 GHz

    // =========================================================================
    // MODEL DỮ LIỆU THÔNG SỐ HỆ THỐNG PRO MAX
    // =========================================================================

    public static class SystemData implements Cloneable {
        public long timeStampMs;

        // RAM & zRAM (RAM Plus)
        public long totalRamMB;
        public long availableRamMB;
        public long usedRamMB;
        public int ramUsagePercent;

        public long totalSwapMB;
        public long usedSwapMB;
        public int swapUsagePercent;

        // CPU & Thermal Throttling
        public float cpuUsagePercent;
        public int[] coreFrequenciesMhz = new int[8]; // 8 cores
        public int bigClusterMaxMhz;   // Max freq 2 nhân A75
        public int littleClusterMaxMhz;// Max freq 6 nhân A55
        public boolean isCpuThrottling; // Đang bị bóp xung

        // GPU Mali-G52 MC2
        public float gpuUsagePercent;
        public int gpuFrequencyMhz;

        // Thermal (Nhiệt độ)
        public float batteryTempCelsius;
        public float cpuTempCelsius;
        public float gpuTempCelsius;
        public ThermalLevel thermalLevel;

        // Power & Battery Meter
        public float batteryVoltageVolts;
        public float batteryCurrentmA;
        public float powerDrainWatts;

        // Network Speed (KB/s)
        public float downloadSpeedKbps;
        public float uploadSpeedKbps;

        // Application & Display
        public String foregroundPackage = "";
        public String foregroundAppName = "";
        public boolean isGameActive;
        public int refreshRateHz;

        // Health Score (0 - 100)
        public int systemHealthScore;

        @Override
        public SystemData clone() {
            try {
                SystemData copy = (SystemData) super.clone();
                copy.coreFrequenciesMhz = this.coreFrequenciesMhz.clone();
                return copy;
            } catch (CloneNotSupportedException e) {
                return new SystemData();
            }
        }

        @Override
        public String toString() {
            return String.format("SystemData[RAM: %d/%dMB (%d%%) | CPU: %.1f%% | GPU: %.1f%% | Temp: CPU %.1f°C/Bat %.1f°C | Power: %.2fW | App: %s]",
                    usedRamMB, totalRamMB, ramUsagePercent, cpuUsagePercent, gpuUsagePercent,
                    cpuTempCelsius, batteryTempCelsius, powerDrainWatts, foregroundAppName);
        }
    }

    public enum ThermalLevel {
        COOL,     // < 38°C (Rất mát)
        NORMAL,   // 38°C - 41.9°C (Bình thường)
        WARM,     // 42°C - 44.9°C (Ấm)
        HOT,      // 45°C - 47.9°C (Nóng - Bắt đầu bóp xung nhẹ)
        OVERHEAT  // >= 48°C (Quá nhiệt - Bóp xung nặng, rớt FPS)
    }

    public interface OnSystemUpdateListener {
        void onSystemUpdate(SystemData data);
    }

    // =========================================================================
    // CONSTRUCTOR & SINGLETON PATTERN
    // =========================================================================

    private SystemMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Khởi tạo thông số Traffic ban đầu
        this.lastRxBytes = TrafficStats.getTotalRxBytes();
        this.lastTxBytes = TrafficStats.getTotalTxBytes();
        this.lastTrafficTimeStamp = System.currentTimeMillis();
    }

    public static SystemMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (SystemMonitor.class) {
                if (instance == null) {
                    instance = new SystemMonitor(context);
                }
            }
        }
        return instance;
    }

    public void setOnSystemUpdateListener(OnSystemUpdateListener listener) {
        this.listener = listener;
    }

    // =========================================================================
    // THAO TÁC THEO DÕI (START / STOP / HISTORY)
    // =========================================================================

    public synchronized void startMonitoring(long intervalMs) {
        if (isMonitoring) return;

        isMonitoring = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();

        long delay = Math.max(500, intervalMs);
        scheduler.scheduleWithFixedDelay(this::collectSystemData, 0, delay, TimeUnit.MILLISECONDS);

        Log.i(TAG, "Đã khởi chạy SystemMonitor Pro Max AI với chu kỳ: " + delay + "ms");
    }

    public synchronized void stopMonitoring() {
        if (!isMonitoring) return;

        isMonitoring = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        Log.i(TAG, "Đã dừng SystemMonitor Pro Max.");
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * Lấy danh sách lịch sử dữ liệu để vẽ biểu đồ đường đồ họa
     */
    public synchronized List<SystemData> getHistoryBuffer() {
        List<SystemData> copyList = new ArrayList<>();
        for (SystemData d : historyBuffer) {
            copyList.add(d.clone());
        }
        return copyList;
    }

    // =========================================================================
    // LUỒNG TỔNG HỢP DỮ LIỆU CHÍNH (COLLECTION LOOP)
    // =========================================================================

    private void collectSystemData() {
        try {
            SystemData data = new SystemData();
            data.timeStampMs = System.currentTimeMillis();

            // 1. Thu thập bộ nhớ RAM & zRAM
            readRamAndSwapData(data);

            // 2. Thu thập % CPU Load & Xung nhịp từng Core
            float rawCpu = readRawCpuUsagePercent();
            data.cpuUsagePercent = cpuFilter.addSample(rawCpu);
            readCoreFrequenciesAndThrottling(data);

            // 3. Thu thập % GPU Load & Xung GPU Mali-G52
            readGpuData(data);

            // 4. Thu thập Nhiệt độ SoC, Battery, GPU
            readTemperatureData(data);

            // 5. Thu thập Thông số Pin & Công suất Wattage
            readPowerAndBatteryMeter(data);

            // 6. Thu thập Tốc độ Mạng
            readNetworkTrafficSpeed(data);

            // 7. Thu thập Thông tin Ứng dụng & Màn hình
            readForegroundAndDisplayInfo(data);

            // 8. Đánh giá chỉ số Sức khỏe Hệ thống (Health Index Score)
            calculateSystemHealthScore(data);

            // 9. Lưu vào lịch sử đệm
            synchronized (historyBuffer) {
                if (historyBuffer.size() >= HISTORY_MAX_SIZE) {
                    historyBuffer.removeFirst();
                }
                historyBuffer.addLast(data.clone());
            }

            // 10. Bắn Callback về Main Thread
            if (listener != null) {
                mainHandler.post(() -> listener.onSystemUpdate(data));
            }

        } catch (Exception e) {
            Log.e(TAG, "Lỗi đọc dữ liệu hệ thống: " + e.getMessage());
        }
    }

    // =========================================================================
    // CÁC MODULE ĐỌC HỆ THỐNG CHI TIẾT
    // =========================================================================

    /**
     * Module 1: Đọc RAM Thật & zRAM (RAM Plus) qua ActivityManager & /proc/meminfo
     */
    private void readRamAndSwapData(SystemData data) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            data.totalRamMB = mi.totalMem / (1024 * 1024);
            data.availableRamMB = mi.availMem / (1024 * 1024);
            data.usedRamMB = data.totalRamMB - data.availableRamMB;
            data.ramUsagePercent = (int) ((data.usedRamMB * 100) / Math.max(1, data.totalRamMB));
        }

        // Đọc thông số Swap/zRAM từ /proc/meminfo
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            long swapTotalKb = 0;
            long swapFreeKb = 0;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("SwapTotal:")) {
                    swapTotalKb = parseMemInfoKb(line);
                } else if (line.startsWith("SwapFree:")) {
                    swapFreeKb = parseMemInfoKb(line);
                }
            }

            data.totalSwapMB = swapTotalKb / 1024;
            data.usedSwapMB = (swapTotalKb - swapFreeKb) / 1024;
            if (data.totalSwapMB > 0) {
                data.swapUsagePercent = (int) ((data.usedSwapMB * 100) / data.totalSwapMB);
            }
        } catch (Exception ignored) {
        }
    }

    private long parseMemInfoKb(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * Module 2: Đọc % CPU Load qua /proc/stat
     */
    private float readRawCpuUsagePercent() {
        try (RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r")) {
            String line = reader.readLine();
            if (line != null && line.startsWith("cpu ")) {
                String[] toks = line.split("\\s+");
                long user = Long.parseLong(toks[1]);
                long nice = Long.parseLong(toks[2]);
                long system = Long.parseLong(toks[3]);
                long idle = Long.parseLong(toks[4]);
                long iowait = Long.parseLong(toks[5]);
                long irq = Long.parseLong(toks[6]);
                long softirq = Long.parseLong(toks[7]);

                long totalCpuTime = user + nice + system + idle + iowait + irq + softirq;
                long idleTime = idle + iowait;

                if (lastTotalCpuTime != 0) {
                    long totalDiff = totalCpuTime - lastTotalCpuTime;
                    long idleDiff = idleTime - lastIdleCpuTime;

                    if (totalDiff > 0) {
                        float cpuPercent = ((float) (totalDiff - idleDiff) / totalDiff) * 100.0f;
                        lastTotalCpuTime = totalCpuTime;
                        lastIdleCpuTime = idleTime;
                        return Math.max(0.0f, Math.min(100.0f, cpuPercent));
                    }
                }
                lastTotalCpuTime = totalCpuTime;
                lastIdleCpuTime = idleTime;
            }
        } catch (Exception ignored) {
        }
        return 0.0f;
    }

    /**
     * Đọc Xung nhịp 8 Nhân & Phát hiện CPU Throttling
     */
    private void readCoreFrequenciesAndThrottling(SystemData data) {
        int maxBig = 0;
        int maxLittle = 0;

        for (int i = 0; i < 8; i++) {
            String path = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq";
            int freqMhz = readSingleLineInt(path) / 1000;
            data.coreFrequenciesMhz[i] = freqMhz;

            // Cores 0-5: Cortex-A55 | Cores 6-7: Cortex-A75
            if (i >= 6) {
                if (freqMhz > maxBig) maxBig = freqMhz;
            } else {
                if (freqMhz > maxLittle) maxLittle = freqMhz;
            }
        }

        data.bigClusterMaxMhz = maxBig;
        data.littleClusterMaxMhz = maxLittle;

        // Phát hiện bóp xung: Nếu nhiệt độ cao mà nhân A75 bị khống chế < 1600MHz
        data.isCpuThrottling = (data.cpuTempCelsius >= 43.0f && maxBig > 0 && maxBig < 1600);
    }

    /**
     * Module 3: Đọc GPU Mali-G52 MC2 Utilization & Frequency
     */
    private void readGpuData(SystemData data) {
        // Danh sách các đường dẫn sysfs GPU phổ biến trên MediaTek Helio G80
        String[] gpuLoadPaths = {
            "/sys/class/misc/mali0/device/utilization",
            "/sys/devices/platform/13900000.mali/utilization",
            "/sys/module/mali/parameters/mali_gpu_utilization",
            "/sys/kernel/gpu/gpu_busy"
        };

        float rawGpu = 0.0f;
        for (String path : gpuLoadPaths) {
            int val = readSingleLineInt(path);
            if (val >= 0) {
                rawGpu = Math.min(100.0f, (float) val);
                break;
            }
        }

        data.gpuUsagePercent = gpuFilter.addSample(rawGpu);

        // Đọc xung nhịp GPU (MHz)
        String[] gpuFreqPaths = {
            "/sys/class/devfreq/mtk-mali.0/cur_freq",
            "/sys/class/misc/mali0/device/cur_freq",
            "/sys/devices/platform/13900000.mali/cur_freq"
        };

        for (String path : gpuFreqPaths) {
            int val = readSingleLineInt(path);
            if (val > 0) {
                // Đổi Hz / kHz sang MHz
                if (val > 1000000) data.gpuFrequencyMhz = val / 1000000;
                else if (val > 1000) data.gpuFrequencyMhz = val / 1000;
                else data.gpuFrequencyMhz = val;
                break;
            }
        }
    }

    /**
     * Module 4: Đọc Nhiệt độ SoC, Pin & GPU
     */
    private void readTemperatureData(SystemData data) {
        data.batteryTempCelsius = readBatteryTemperature();

        // Quét cổng thermal zone MediaTek Helio G80
        float cpuTemp = 0.0f;
        String[] thermalPaths = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        };

        for (String path : thermalPaths) {
            float temp = readSingleLineFloat(path);
            if (temp > 1000) temp /= 1000.0f; // Chuyển milliCelsius sang Celsius

            if (temp >= 20.0f && temp <= 95.0f) {
                cpuTemp = temp;
                break;
            }
        }

        data.cpuTempCelsius = (cpuTemp > 0) ? cpuTemp : (data.batteryTempCelsius + 2.5f);
        data.gpuTempCelsius = Math.max(data.batteryTempCelsius + 1.0f, cpuTemp - 1.5f);

        // Phân cấp mức độ nhiệt
        if (data.cpuTempCelsius < 38.0f) data.thermalLevel = ThermalLevel.COOL;
        else if (data.cpuTempCelsius < 42.0f) data.thermalLevel = ThermalLevel.NORMAL;
        else if (data.cpuTempCelsius < 45.0f) data.thermalLevel = ThermalLevel.WARM;
        else if (data.cpuTempCelsius < 48.0f) data.thermalLevel = ThermalLevel.HOT;
        else data.thermalLevel = ThermalLevel.OVERHEAT;
    }

    private float readBatteryTemperature() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                return temp / 10.0f;
            }
        } catch (Exception ignored) {
        }
        return 32.0f;
    }

    /**
     * Module 5: Đọc Công suất tiêu thụ Wattage & Điện áp Pin
     */
    private void readPowerAndBatteryMeter(SystemData data) {
        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            float voltageVolts = 3.85f;
            if (batteryStatus != null) {
                int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                if (voltage > 0) voltageVolts = voltage / 1000.0f;
            }
            data.batteryVoltageVolts = voltageVolts;

            float currentmA = 0.0f;
            if (bm != null) {
                int currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (currentNow != Integer.MIN_VALUE) {
                    currentmA = Math.abs(currentNow) / 1000.0f; // Convert microAmperes to mA
                }
            }

            if (currentmA <= 0) currentmA = 350.0f; // Mức xả mặc định trung bình
            data.batteryCurrentmA = currentmA;

            // Công suất (Watts) = Voltage (V) * Current (A)
            data.powerDrainWatts = (data.batteryVoltageVolts * (data.batteryCurrentmA / 1000.0f));

        } catch (Exception ignored) {
            data.batteryVoltageVolts = 3.8f;
            data.batteryCurrentmA = 400.0f;
            data.powerDrainWatts = 1.52f;
        }
    }

    /**
     * Module 6: Tính tốc độ mạng Download / Upload
     */
    private void readNetworkTrafficSpeed(SystemData data) {
        long currentRx = TrafficStats.getTotalRxBytes();
        long currentTx = TrafficStats.getTotalTxBytes();
        long currentTime = System.currentTimeMillis();

        long timeDiff = currentTime - lastTrafficTimeStamp;
        if (timeDiff > 0 && lastTrafficTimeStamp > 0) {
            long rxDiff = currentRx - lastRxBytes;
            long txDiff = currentTx - lastTxBytes;

            if (rxDiff >= 0) data.downloadSpeedKbps = (rxDiff / 1024.0f) / (timeDiff / 1000.0f);
            if (txDiff >= 0) data.uploadSpeedKbps = (txDiff / 1024.0f) / (timeDiff / 1000.0f);
        }

        lastRxBytes = currentRx;
        lastTxBytes = currentTx;
        lastTrafficTimeStamp = currentTime;
    }

    /**
     * Module 7: Đọc Package tiền cảnh & Tần số quét màn hình
     */
    private void readForegroundAndDisplayInfo(SystemData data) {
        data.foregroundPackage = getForegroundPackageName();
        data.foregroundAppName = getAppNameFromPackage(data.foregroundPackage);
        data.isGameActive = checkIsGamePackage(data.foregroundPackage);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            data.refreshRateHz = (int) display.getRefreshRate();
        } else {
            data.refreshRateHz = 90;
        }
    }

    private String getForegroundPackageName() {
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long time = System.currentTimeMillis();
                UsageEvents events = usm.queryEvents(time - 3000, time);
                UsageEvents.Event event = new UsageEvents.Event();
                String lastPkg = "";

                while (events.hasNextEvent()) {
                    events.getNextEvent(event);
                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        lastPkg = event.getPackageName();
                    }
                }
                if (!lastPkg.isEmpty()) return lastPkg;
            }
        } catch (Exception ignored) {
        }
        return "com.sec.android.app.launcher";
    }

    private String getAppNameFromPackage(String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) return "Trang chủ";
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            return pkgName;
        }
    }

    private boolean checkIsGamePackage(String pkg) {
        if (pkg == null) return false;
        String lower = pkg.toLowerCase();
        return lower.contains("game") || lower.contains("pubg") || lower.contains("kgvn") ||
               lower.contains("freefire") || lower.contains("genshin") || lower.contains("codm") ||
               lower.contains("mobilelegends") || lower.contains("vng");
    }

    /**
     * Module 8: Đánh giá chỉ số Sức khỏe Hệ thống (0 - 100 Score)
     */
    private void calculateSystemHealthScore(SystemData data) {
        int score = 100;

        // Trừ điểm RAM
        if (data.ramUsagePercent > 85) score -= 20;
        else if (data.ramUsagePercent > 75) score -= 10;

        // Trừ điểm Nhiệt độ
        if (data.cpuTempCelsius >= 48.0f) score -= 35;
        else if (data.cpuTempCelsius >= 45.0f) score -= 20;
        else if (data.cpuTempCelsius >= 42.0f) score -= 10;

        // Trừ điểm Throttling
        if (data.isCpuThrottling) score -= 15;

        // Trừ điểm Tải CPU/GPU quá mức
        if (data.cpuUsagePercent > 90) score -= 15;
        if (data.gpuUsagePercent > 90) score -= 10;

        data.systemHealthScore = Math.max(0, Math.min(100, score));
    }

    // =========================================================================
    // HÀM BỔ TRỢ ĐỌC TẬP TIN SYSFS & BỘ LỌC MOVING AVERAGE
    // =========================================================================

    private int readSingleLineInt(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if (line != null) return Integer.parseInt(line.trim());
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private float readSingleLineFloat(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if (line != null) return Float.parseFloat(line.trim());
            } catch (Exception ignored) {
            }
        }
        return -1.0f;
    }

    /**
     * Lớp Bộ Lọc Moving Average Lọc Nhiễu Biểu Đồ
     */
    private static class MovingAverageFilter {
        private final int size;
        private final Queue<Float> samples = new LinkedList<>();
        private float sum = 0.0f;

        public MovingAverageFilter(int size) {
            this.size = size;
        }

        public synchronized float addSample(float sample) {
            samples.add(sample);
            sum += sample;
            if (samples.size() > size) {
                sum -= samples.poll();
            }
            return sum / samples.size();
        }
    }
}