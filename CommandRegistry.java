package com.a32.fixlag.commands;

import android.content.Context;
import com.a32.fixlag.shizuku.ShizukuManager;

/**
 * Lớp Bộ Điều Phối & Nâng Cấp PRO MAX AI (AI Smart Command Registry & Adaptive Dispatcher)
 * Chuyên biệt cho Samsung Galaxy A32 4G (Helio G80 | Mali-G52 MC2 | AMOLED 90Hz | One UI)
 * Package: com.a32.fixlag.commands
 */
public class CommandRegistry {

    public enum SystemMode {
        PRO_MAX_AI_AUTO,
        ULTIMATE_GAMING,
        DAILY_BALANCED,
        BATTERY_SAVER,
        DEFAULT_RESET
    }

    public enum AppCategory {
        HEAVY_GAME,
        LIGHT_GAME,
        SOCIAL_MEDIA,
        VIDEO_STREAMING,
        SYSTEM_IDLE
    }

    private static boolean isAiAutoPilotRunning = false;
    private static Thread aiWorkerThread = null;

    public static String runProMaxAiOptimization(Context context, String foregroundPackage, float batteryTempNhietDo, long freeRamMB) {
        StringBuilder aiLog = new StringBuilder();
        aiLog.append("===============================================\n");
        aiLog.append("   FIXLAG A32 4G - PRO MAX AI ENGINE ACTIVE    \n");
        aiLog.append("===============================================\n\n");

        if (!checkShizukuStatus(context, aiLog)) {
            aiLog.append(RamCommands.trimMemoryStandard(context));
            return aiLog.toString();
        }

        AppCategory category = detectAppCategory(foregroundPackage);
        aiLog.append(" [AI DETECT] App: ").append(foregroundPackage != null ? foregroundPackage : "Hệ thống")
             .append(" | Phân loại: ").append(category.name()).append("\n");

        aiLog.append(" [AI THERMAL] Nhiệt độ hiện tại: ").append(batteryTempNhietDo).append("°C -> ");
        if (batteryTempNhietDo >= 45.0f) {
            aiLog.append("CẢNH BÁO NÓNG MÁY! Chuyển kịch bản bảo vệ SOC.\n");
        } else if (batteryTempNhietDo >= 41.0f) {
            aiLog.append("ẤM MÁY. Kích hoạt AI Cân bằng Nhiệt/FPS.\n");
        } else {
            aiLog.append("NHIỆT ĐỘ LÝ TƯỞNG. Cho phép Max Performance.\n");
        }

        aiLog.append("\n>>> [AI DISPATCHER] THỰC THI QUYẾT ĐỊNH TỐI ƯU:\n");

        if (category == AppCategory.HEAVY_GAME) {
            if (batteryTempNhietDo >= 46.0f) {
                aiLog.append("-> AI Decision: Game nặng + Quá nhiệt -> Ép 900p @ 60Hz & Nén RAM Cấp tốc\n\n");
                aiLog.append(DisplayCommands.setBalancedResolution900p(context)).append("\n");
                aiLog.append(DisplayCommands.setForce60Hz(context)).append("\n");
                aiLog.append(RamCommands.compactMemoryAggressive(context)).append("\n");
            } else {
                aiLog.append("-> AI Decision: Game nặng + Mát máy -> Kích hoạt PRO MAX GAMING 720p @ 90Hz\n\n");
                aiLog.append(applyUltimateGamingMode(context, foregroundPackage)).append("\n");
            }
        } else if (category == AppCategory.SOCIAL_MEDIA || category == AppCategory.VIDEO_STREAMING) {
            aiLog.append("-> AI Decision: Lướt Web/Media -> Ép 1080p @ 90Hz Cuộn mượt & Cân bằng Pin\n\n");
            aiLog.append(DisplayCommands.setNativeResolution1080p(context)).append("\n");
            aiLog.append(DisplayCommands.setForce90Hz(context)).append("\n");
            aiLog.append(CpuGpuCommands.applyDailyBalancedProfile(context)).append("\n");
            if (freeRamMB < 1200) {
                aiLog.append(RamCommands.clearAppAndSystemCache(context)).append("\n");
            }
        } else {
            aiLog.append("-> AI Decision: Tác vụ thông thường -> Chế độ Daily Balanced Standard\n\n");
            aiLog.append(applyDailyBalancedMode(context)).append("\n");
        }

        aiLog.append("===============================================\n");
        aiLog.append(" [PRO MAX AI] ĐÃ ĐIỀU CHỈNH HỆ THỐNG THÀNH CÔNG!\n");
        aiLog.append("===============================================\n");

        return aiLog.toString();
    }

    public static synchronized String startAiAutoPilot(Context context) {
        if (isAiAutoPilotRunning) {
            return "[PRO MAX AI] Auto-Pilot đã và đang hoạt động ngầm!";
        }

        isAiAutoPilotRunning = true;
        aiWorkerThread = new Thread(() -> {
            while (isAiAutoPilotRunning) {
                try {
                    Thread.sleep(45000);

                    if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
                        continue;
                    }

                    Runtime runtime = Runtime.getRuntime();
                    long freeRam = runtime.freeMemory() / (1024 * 1024);
                    if (freeRam < 800) {
                        RamCommands.compactMemoryAggressive(context);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {
                }
            }
        });
        aiWorkerThread.start();

        return "[PRO MAX AI] Đã kích hoạt Chế độ AI Auto-Pilot Chạy Ngầm Thông Minh!";
    }

    public static synchronized String stopAiAutoPilot() {
        isAiAutoPilotRunning = false;
        if (aiWorkerThread != null) {
            aiWorkerThread.interrupt();
            aiWorkerThread = null;
        }
        return "[PRO MAX AI] Đã tắt Chế độ AI Auto-Pilot.";
    }

    public static String applyUltimateGamingMode(Context context, String gamePackage) {
        StringBuilder masterLog = new StringBuilder();
        masterLog.append("===============================================\n");
        masterLog.append("   FIXLAG A32 4G - ULTIMATE GAMING DISPATCHER  \n");
        masterLog.append("===============================================\n\n");

        if (!checkShizukuStatus(context, masterLog)) {
            masterLog.append(RamCommands.trimMemoryStandard(context));
            return masterLog.toString();
        }

        masterLog.append(">>> [BƯỚC 1/3] ĐIỀU PHỐI MÀN HÌNH & HIỂN THỊ:\n");
        masterLog.append(DisplayCommands.applyGamingDisplayProfile(context)).append("\n");

        masterLog.append(">>> [BƯỚC 2/3] ĐIỀU PHỐI CPU, GPU & SAMSUNG GOS:\n");
        masterLog.append(CpuGpuCommands.applyExtremeGamingProfile(context, gamePackage)).append("\n");

        masterLog.append(">>> [BƯỚC 3/3] ĐIỀU PHỐI RAM & MEMORY COMPACTION:\n");
        masterLog.append(RamCommands.applyGamingProfile(context, gamePackage)).append("\n");

        masterLog.append("===============================================\n");
        masterLog.append(" [HOÀN TẤT] HỆ THỐNG ĐÃ SẴN SÀNG CHIẾN GAME!\n");
        masterLog.append("===============================================\n");

        return masterLog.toString();
    }

    public static String applyDailyBalancedMode(Context context) {
        StringBuilder masterLog = new StringBuilder();
        masterLog.append("===============================================\n");
        masterLog.append("   FIXLAG A32 4G - DAILY BALANCED DISPATCHER   \n");
        masterLog.append("===============================================\n\n");

        if (!checkShizukuStatus(context, masterLog)) {
            masterLog.append(RamCommands.trimMemoryStandard(context));
            return masterLog.toString();
        }

        masterLog.append(">>> [BƯỚC 1/3] THIẾT LẬP MÀN HÌNH 1080P & 90HZ:\n");
        masterLog.append(DisplayCommands.applyDailyDisplayProfile(context)).append("\n");

        masterLog.append(">>> [BƯỚC 2/3] CÂN BẰNG CPU/GPU & AN TOÀN NHIỆT:\n");
        masterLog.append(CpuGpuCommands.applyDailyBalancedProfile(context)).append("\n");

        masterLog.append(">>> [BƯỚC 3/3] TỐI ƯU RAM ĐA NHIỆM & CACHE:\n");
        masterLog.append(RamCommands.applyDailyProfile(context)).append("\n");

        masterLog.append("===============================================\n");
        masterLog.append(" [HOÀN TẤT] ĐÃ TỐI ƯU GIAO DIỆN & TÁC VỤ HẰNG NGÀY!\n");
        masterLog.append("===============================================\n");

        return masterLog.toString();
    }

    public static String applyBatterySaverMode(Context context) {
        StringBuilder masterLog = new StringBuilder();
        masterLog.append("===============================================\n");
        masterLog.append("   FIXLAG A32 4G - BATTERY SAVER DISPATCHER    \n");
        masterLog.append("===============================================\n\n");

        if (!checkShizukuStatus(context, masterLog)) {
            return masterLog.toString();
        }

        masterLog.append(">>> [BƯỚC 1/3] HẠ TẦN SỐ QUÉT MÀN HÌNH 60HZ:\n");
        masterLog.append(DisplayCommands.applyBatterySaverDisplayProfile(context)).append("\n");

        masterLog.append(">>> [BƯỚC 2/3] KÍCH HOẠT TIẾT KIỆM NĂNG LƯỢNG CPU/GPU:\n");
        masterLog.append(CpuGpuCommands.applyBatterySaverProfile(context)).append("\n");

        masterLog.append(">>> [BƯỚC 3/3] GIẢI PHÓNG TÀI NGUYÊN NGẦM:\n");
        masterLog.append(RamCommands.compactMemoryAggressive(context)).append("\n");

        masterLog.append("===============================================\n");
        masterLog.append(" [HOÀN TẤT] ĐÃ KÍCH HOẠT TIẾT KIỆM PIN!\n");
        masterLog.append("===============================================\n");

        return masterLog.toString();
    }

    public static String restoreSystemToDefault(Context context) {
        StringBuilder masterLog = new StringBuilder();
        masterLog.append("===============================================\n");
        masterLog.append("   RESTORE SYSTEM TO DEFAULT - SAMSUNG A32 4G  \n");
        masterLog.append("===============================================\n\n");

        if (!checkShizukuStatus(context, masterLog)) {
            return masterLog.toString();
        }

        masterLog.append("1. ").append(DisplayCommands.resetDisplayToDefault(context)).append("\n");
        masterLog.append("2. ").append(CpuGpuCommands.restoreSamsungGosServices(context)).append("\n");
        masterLog.append("3. ").append(CpuGpuCommands.setGpuRenderer(context, "skiagl")).append("\n");

        masterLog.append("\nĐã hoàn tất khôi phục cấu hình mặc định.");
        return masterLog.toString();
    }

    public static String quickRamClean(Context context) {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.hasPermission()) {
            String compact = RamCommands.compactMemoryAggressive(context);
            String cache = RamCommands.clearAppAndSystemCache(context);
            return compact + "\n" + cache;
        } else {
            return RamCommands.trimMemoryStandard(context);
        }
    }

    public static String setRefreshRate(Context context, DisplayCommands.RefreshRateMode mode) {
        switch (mode) {
            case HZ_90_LOCKED:
                return DisplayCommands.setForce90Hz(context);
            case HZ_60_LOCKED:
                return DisplayCommands.setForce60Hz(context);
            case HZ_AUTO:
            default:
                return DisplayCommands.setAutoRefreshRate(context);
        }
    }

    public static String setResolution(Context context, DisplayCommands.ResolutionProfile profile) {
        switch (profile) {
            case BALANCED_900P:
                return DisplayCommands.setBalancedResolution900p(context);
            case SMOOTH_810P:
                return DisplayCommands.setSmoothResolution810p(context);
            case HD_720P_GAMING:
                return DisplayCommands.setGamingResolution720p(context);
            case SUPER_FPS_640P:
                return DisplayCommands.setSuperFpsResolution640p(context);
            case EXTREME_540P:
                return DisplayCommands.setExtremeResolution540p(context);
            case FHD_1080P_NATIVE:
            default:
                return DisplayCommands.setNativeResolution1080p(context);
        }
    }

    public static String setRamPlus(Context context, int sizeGb) {
        return RamCommands.setRamPlusSize(context, sizeGb);
    }

    public static String toggleSamsungGos(Context context, boolean disable) {
        if (disable) {
            return CpuGpuCommands.disableSamsungGosServices(context);
        } else {
            return CpuGpuCommands.restoreSamsungGosServices(context);
        }
    }

    public static String getFullSystemReport(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("=========================================\n");
        report.append("  FIXLAG A32 4G - PRO MAX AI DIAGNOSTICS \n");
        report.append("=========================================\n\n");

        report.append(CpuGpuCommands.getCpuGpuDetailedInfo()).append("\n");
        report.append(DisplayCommands.getDisplayDetailedInfo(context)).append("\n");
        report.append(RamCommands.getMemoryStatus()).append("\n");

        return report.toString();
    }

    private static AppCategory detectAppCategory(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return AppCategory.SYSTEM_IDLE;
        }

        String pkg = packageName.toLowerCase();

        if (pkg.contains("kgvn") || pkg.contains("pubg") || pkg.contains("freefire") ||
            pkg.contains("genshin") || pkg.contains("mobilelegends") || pkg.contains("vng") ||
            pkg.contains("game") || pkg.contains("garena") || pkg.contains("codm")) {
            return AppCategory.HEAVY_GAME;
        }

        if (pkg.contains("facebook") || pkg.contains("instagram") || pkg.contains("tiktok") ||
            pkg.contains("messenger") || pkg.contains("zalo") || pkg.contains("twitter") ||
            pkg.contains("threads")) {
            return AppCategory.SOCIAL_MEDIA;
        }

        if (pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("vtv") ||
            pkg.contains("vieon") || pkg.contains("chrome")) {
            return AppCategory.VIDEO_STREAMING;
        }

        return AppCategory.LIGHT_GAME;
    }

    private static boolean checkShizukuStatus(Context context, StringBuilder log) {
        if (!ShizukuManager.isShizukuAvailable()) {
            log.append("[CẢNH BÁO] Shizuku chưa khởi chạy!\n");
            log.append("Vui lòng kích hoạt Shizuku qua ADB/Wi-Fi Debugging.\n\n");
            return false;
        }

        if (!ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Chưa cấp quyền Shizuku cho ứng dụng!\n\n");
            return false;
        }

        return true;
    }
}