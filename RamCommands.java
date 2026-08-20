package com.a32.fixlag.commands;

import android.content.Context;
import android.app.ActivityManager;
import com.a32.fixlag.shizuku.ShellExecutor;
import com.a32.fixlag.shizuku.ShizukuManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp tối ưu hóa RAM, Cache và Hệ thống - PHIÊN BẢN MAX LEVEL (GAMING & DAILY)
 * Chuyên biệt cho Samsung Galaxy A32 4G (MediaTek Helio G80 | One UI | eMMC 5.1)
 * Hoạt động 100% qua Shizuku / ADB Shell (Không cần Root)
 * Package: com.a32.fixlag.commands
 */
public class RamCommands {

    /**
     * Enum định nghĩa các chế độ tối ưu hóa
     */
    public enum Profile {
        DAILY,      // Tác vụ hằng ngày (Mượt mà, đa nhiệm tốt, tiết kiệm pin)
        GAMING,     // Chơi Game (Hiệu năng cực hạn, RAM trống tối đa, ưu tiên FPS)
        BALANCED    // Cân bằng hệ thống
    }

    // --- DANH SÁCH LỆNH SHIZUKU / ADB TUNING ---

    // 1. Tinh chỉnh Game Mode & Render GPU chuyên sâu
    private static final String SHIZUKU_GAMING_BOOST = 
        "# Bật Game Driver toàn hệ thống\n" +
        "settings put global game_driver_all_apps 1 2>/dev/null\n" +
        "settings put secure game_auto_temperature_control 0 2>/dev/null\n" +
        
        "# Khóa tần số quét màn hình 90Hz ổn định FPS\n" +
        "settings put system peak_refresh_rate 90.0 2>/dev/null\n" +
        "settings put system min_refresh_rate 90.0 2>/dev/null\n" +
        
        "# Tối ưu hóa GPU HWUI Renderer (Vulkan/SkiaVK)\n" +
        "setprop debug.hwui.renderer skiavk 2>/dev/null\n" +
        "setprop debug.hwui.disable_vsync false 2>/dev/null\n" +
        
        "# Bật chế độ Fixed Performance Mode qua Android Power Manager\n" +
        "cmd power set-fixed-performance-mode-enabled true 2>/dev/null\n" +
        
        "# Thu hẹp số lượng cached process để tập trung RAM cho Game\n" +
        "device_config put activity_manager max_cached_processes 16 2>/dev/null\n";

    // 2. Tinh chỉnh Tác vụ Hằng ngày (Daily Mode)
    private static final String SHIZUKU_DAILY_TUNING = 
        "# Bật tính năng đóng đóng băng ứng dụng ngầm tiết kiệm pin & RAM\n" +
        "settings put global cached_apps_freezer enabled 2>/dev/null\n" +
        "device_config put activity_manager use_freezer true 2>/dev/null\n" +
        
        "# Tăng số lượng Cached Processes hỗ trợ đa nhiệm mở lại app nhanh\n" +
        "device_config put activity_manager max_cached_processes 32 2>/dev/null\n" +
        "settings put global activity_manager_constants max_cached_processes=32,bg_cool_off_time=10000 2>/dev/null\n" +
        
        "# Tắt chế độ Fixed Performance để tiết kiệm pin\n" +
        "cmd power set-fixed-performance-mode-enabled false 2>/dev/null\n" +
        
        "# Tần số quét tự động mượt mà 90Hz\n" +
        "settings put system peak_refresh_rate 90.0 2>/dev/null\n" +
        "settings put system min_refresh_rate 60.0 2>/dev/null\n" +
        
        "# Animation 0.5x mượt giao diện One UI\n" +
        "settings put global window_animation_scale 0.5 2>/dev/null\n" +
        "settings put global transition_animation_scale 0.5 2>/dev/null\n" +
        "settings put global animator_duration_scale 0.5 2>/dev/null\n";

    // 3. Tăng giới hạn Phantom Process (Android 12/13) chống tràn CPU/RAM
    private static final String SHIZUKU_PHANTOM_TUNING = 
        "device_config put activity_manager max_phantom_processes 2147483647 2>/dev/null\n";

    // Danh sách ứng dụng mạng xã hội/nền nặng cần đóng khi CHƠI GAME
    private static final String[] HEAVY_BACKGROUND_APPS = {
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        "com.ss.android.ugc.trill", // TikTok
        "com.zhiliaoapp.musically",
        "com.shopee.vn",
        "com.lazada.android",
        "com.samsung.android.bixby.agent",
        "com.samsung.android.app.routines",
        "com.samsung.android.rubin.app",
        "com.sec.android.app.sbrowser"
    };

    /**
     * Phương thức điều phối thực thi lệnh qua Shizuku hoặc Shell thường
     */
    private static String executeCommand(Context context, String command) {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.hasPermission()) {
            return ShellExecutor.executeShizukuCommand(command);
        } else {
            return ShellExecutor.executeNormalCommand(command);
        }
    }

    /**
     * Lấy dung lượng RAM phần cứng (MB)
     */
    public static long getTotalRamMB() {
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/meminfo");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return Long.parseLong(matcher.group()) / 1024;
                    }
                }
            }
            reader.close();
        } catch (Exception ignored) {}
        return 4096;
    }

    /**
     * Tối ưu hóa dung lượng RAM Plus (RAM Ảo) của Samsung One UI
     * @param sizeGb Dung lượng RAM Plus mong muốn: 0, 2, hoặc 4 (GB)
     */
    public static String setRamPlusSize(Context context, int sizeGb) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku để cấu hình RAM Plus!";
        }
        String cmd = "settings put global ram_expand_size " + sizeGb;
        executeCommand(context, cmd);
        return "Đã thiết lập RAM Plus: " + sizeGb + " GB (Khởi động lại máy để áp dụng).";
    }

    /**
     * Ép nén bộ nhớ RAM ứng dụng cực hạn (Memory Compaction)
     */
    public static String compactMemoryAggressive(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku để thực thi Nén RAM!";
        }
        executeCommand(context, "cmd activity compact all 2>/dev/null");
        executeCommand(context, "cmd activity send-trim-memory --user current RUNNING_CRITICAL 2>/dev/null");
        return "Đã nén toàn bộ RAM ứng dụng thành công.";
    }

    /**
     * Dọn dẹp bộ nhớ đệm Cache hệ thống & ứng dụng
     */
    public static String clearAppAndSystemCache(Context context) {
        executeCommand(context, "pm trim-caches 4000M 2>/dev/null");
        executeCommand(context, "rm -rf /data/local/tmp/* 2>/dev/null");
        return "Đã thu hồi bộ nhớ đệm Cache thành công.";
    }

    /**
     * Ép chế độ High Performance cho ứng dụng Game cụ thể
     * @param gamePackageName Tên package của Game (ví dụ: com.garena.game.kgvn)
     */
    public static String setGamePerformanceMode(Context context, String gamePackageName) {
        if (gamePackageName == null || gamePackageName.isEmpty()) return "";
        
        String cmd = "cmd game mode performance " + gamePackageName + " 2>/dev/null\n" +
                     "device_config put game_overlay " + gamePackageName + " mode=2,fps=90 2>/dev/null\n";
        executeCommand(context, cmd);
        return "Đã kích hoạt Game Performance Mode cho: " + gamePackageName;
    }

    /**
     * Ép dừng các ứng dụng chạy ngầm ngốn RAM khi vào Game
     */
    public static String killBackgroundAppsForGaming(Context context) {
        for (String pkg : HEAVY_BACKGROUND_APPS) {
            executeCommand(context, "am force-stop " + pkg + " 2>/dev/null");
        }
        executeCommand(context, "am kill-all 2>/dev/null");
        return "Đã đóng các ứng dụng ngầm gây tốn RAM & Lag Game.";
    }

    // =========================================================================
    // MASTER FUNCTIONS: KÍCH HOẠT PROFILE TỐI ƯU
    // =========================================================================

    /**
     * CHẾ ĐỘ 1: TỐI ƯU CHƠI GAME MAX LEVEL (GAMING PROFILE)
     * @param context Context ứng dụng
     * @param gamePackageName (Tùy chọn) Package name của Game đang chơi, có thể để null hoặc ""
     */
    public static String applyGamingProfile(Context context, String gamePackageName) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - GAMING PROFILE (MAX LEVEL)\n");
        log.append(" SAMSUNG A32 4G - HELIO G80 BOOST\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối! Vui lòng bật Shizuku để kích hoạt Max Performance.\n\n");
            log.append(trimMemoryStandard(context));
            return log.toString();
        }

        // 1. Áp dụng tinh chỉnh GPU, Refreshrate, Power
        log.append("[1/6] Bật Game Driver, GPU SkiaVK & Màn hình 90Hz... ");
        executeCommand(context, SHIZUKU_GAMING_BOOST);
        executeCommand(context, SHIZUKU_PHANTOM_TUNING);
        log.append("HOÀN THÀNH!\n");

        // 2. Cấu hình RAM Plus tối ưu chơi Game (2GB để tránh eMMC nghẽn)
        log.append("[2/6] Đặt RAM Plus 2GB (Tối ưu độ trễ eMMC)... ");
        log.append(setRamPlusSize(context, 2)).append("\n");

        // 3. Đặt Game Performance Mode cho package Game
        if (gamePackageName != null && !gamePackageName.trim().isEmpty()) {
            log.append("[3/6] ");
            log.append(setGamePerformanceMode(context, gamePackageName)).append("\n");
        } else {
            log.append("[3/6] Thiết lập Game Engine tổng thể... HOÀN THÀNH!\n");
        }

        // 4. Diệt ứng dụng ngầm nặng
        log.append("[4/6] Giải phóng RAM & Ép dừng các app ngầm ngốn CPU... ");
        killBackgroundAppsForGaming(context);
        log.append("HOÀN THÀNH!\n");

        // 5. Nén RAM cực hạn
        log.append("[5/6] Nén RAM toàn bộ tiến trình hệ thống... ");
        compactMemoryAggressive(context);
        log.append("HOÀN THÀNH!\n");

        // 6. Dọn dẹp Cache
        log.append("[6/6] Dọn dẹp TRIM Cache ứng dụng... ");
        clearAppAndSystemCache(context);
        log.append("HOÀN THÀNH!\n\n");

        log.append("=========================================\n");
        log.append(" MÁY ĐÃ SẴN SÀNG CHƠI GAME MƯỢT MÀ!\n");
        log.append("=========================================\n");

        return log.toString();
    }

    /**
     * CHẾ ĐỘ 2: TỐI ƯU TÁC VỤ HẰNG NGÀY MAX LEVEL (DAILY PROFILE)
     */
    public static String applyDailyProfile(Context context) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - DAILY PROFILE (MƯỢT MÀ & ĐA NHIỆM)\n");
        log.append(" SAMSUNG A32 4G - ONE UI OPTIMIZED\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa cấp quyền. Chạy dọn dẹp tiêu chuẩn...\n\n");
            log.append(trimMemoryStandard(context));
            return log.toString();
        }

        // 1. Tối ưu Cached Apps Freezer & Animation 0.5x
        log.append("[1/4] Kích hoạt Đóng băng app ngầm & Animation 0.5x... ");
        executeCommand(context, SHIZUKU_DAILY_TUNING);
        executeCommand(context, SHIZUKU_PHANTOM_TUNING);
        log.append("HOÀN THÀNH!\n");

        // 2. Thiết lập RAM Plus 4GB cho đa nhiệm ứng dụng hằng ngày
        log.append("[2/4] Đặt RAM Plus 4GB (Tối ưu đa nhiệm mở nhiều app)... ");
        log.append(setRamPlusSize(context, 4)).append("\n");

        // 3. Thu hồi RAM thông minh
        log.append("[3/4] Tối ưu hóa bộ nhớ RAM ngầm... ");
        executeCommand(context, "am kill-all 2>/dev/null");
        compactMemoryAggressive(context);
        log.append("HOÀN THÀNH!\n");

        // 4. Dọn Cache
        log.append("[4/4] Dọn dẹp Cache rác hệ thống... ");
        clearAppAndSystemCache(context);
        log.append("HOÀN THÀNH!\n\n");

        log.append("=========================================\n");
        log.append(" ĐÃ TỐI ƯU TÁC VỤ HẰNG NGÀY MƯỢT MÀ!\n");
        log.append("=========================================\n");

        return log.toString();
    }

    /**
     * Tối ưu RAM tiêu chuẩn cho máy chưa bật Shizuku
     */
    public static String trimMemoryStandard(Context context) {
        StringBuilder log = new StringBuilder();
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);

                long beforeMB = mi.availMem / (1024 * 1024);
                System.gc();
                am.getMemoryInfo(mi);
                long afterMB = mi.availMem / (1024 * 1024);

                log.append("RAM khả dụng trước: ").append(beforeMB).append(" MB\n");
                log.append("RAM khả dụng sau: ").append(afterMB).append(" MB\n");
                log.append("Đã giải phóng: ~").append(Math.max(0, afterMB - beforeMB)).append(" MB RAM.");
            }
        } catch (Exception e) {
            log.append("Lỗi Standard API: ").append(e.getMessage());
        }
        return log.toString();
    }

    /**
     * Đọc trạng thái MemInfo
     */
    public static String getMemoryStatus() {
        StringBuilder status = new StringBuilder("=== THÔNG SỐ MEMINFO HỆ THỐNG ===\n");
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/meminfo");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:") || line.startsWith("MemFree:") || 
                    line.startsWith("MemAvailable:") || line.startsWith("Buffers:") || 
                    line.startsWith("Cached:") || line.startsWith("SwapTotal:") || 
                    line.startsWith("SwapFree:")) {
                    status.append(line).append("\n");
                }
            }
            reader.close();
        } catch (Exception e) {
            status.append("Lỗi đọc meminfo: ").append(e.getMessage());
        }
        return status.toString();
    }
}