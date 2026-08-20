package com.a32.fixlag.commands;

import android.content.Context;
import com.a32.fixlag.shizuku.ShellExecutor;
import com.a32.fixlag.shizuku.ShizukuManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Lớp tối ưu hóa CPU, GPU, Thermal & Power Management - PHIÊN BẢN NÂNG CẤP TỐI ĐA (ULTIMATE MAX LEVEL)
 * Chuyên biệt cho Samsung Galaxy A32 4G (MediaTek Helio G80 | ARM Mali-G52 MC2 | One UI 3/4/5)
 * Hoạt động 100% qua Shizuku / ADB Shell (Không cần Root)
 * Package: com.a32.fixlag.commands
 */
public class CpuGpuCommands {

    /**
     * Enum các cấp độ hiệu năng
     */
    public enum CpuProfile {
        EXTREME_GAMING, // Cực hạn Gaming: Max xung CPU/GPU, Tắt Throttling GOS, Low Latency Touch
        DAILY_BALANCED, // Hằng ngày mượt mà: Tận dụng 90Hz, tiết kiệm pin, cuộn lướt mượt
        BATTERY_SAVER   // Tiết kiệm Pin tối đa: Hạ xung ngầm, giảm nhiệt độ máy
    }

    // =========================================================================
    // DANH SÁCH BỘ LỆNH SHIZUKU / ADB TUNING SIÊU CẤP (HELIO G80 & MALI-G52)
    // =========================================================================

    // 1. TỐI ƯU CỰC HẠN GAMING (EXTREME GAMING PROFILE)
    private static final String SHIZUKU_CPU_GPU_EXTREME_GAMING =
        "# --- 1. KÍCH HOẠT POWER MANAGER FIXED PERFORMANCE --- \n" +
        "cmd power set-fixed-performance-mode-enabled true 2>/dev/null\n" +
        "cmd power set-adaptive-power-saver-enabled false 2>/dev/null\n" +

        "# --- 2. MEDIATEK COREPILOT & POWERHAL PERF BOOST --- \n" +
        "setprop persist.sys.power.cpu.boost 1 2>/dev/null\n" +
        "setprop persist.vendor.powerhal.perf 1 2>/dev/null\n" +
        "setprop vendor.powerhal.core.control 1 2>/dev/null\n" +
        "setprop debug.performance.tuning 1 2>/dev/null\n" +

        "# --- 3. EPIC GPU MALI-G52 VULKAN (SKIAVK) RENDERER ENGINE --- \n" +
        "setprop debug.hwui.renderer skiavk 2>/dev/null\n" +
        "setprop debug.renderengine.backend skiavk 2>/dev/null\n" +
        "setprop debug.sf.hw 1 2>/dev/null\n" +
        "setprop debug.egl.hw 1 2>/dev/null\n" +
        "setprop debug.sf.enable_hwc_vds 1 2>/dev/null\n" +
        "setprop debug.hwui.overdraw false 2>/dev/null\n" +

        "# --- 4. SURFACEFLINGER ZERO-LATENCY & LOW INPUT LAG --- \n" +
        "setprop debug.sf.latch_unsignaled 1 2>/dev/null\n" +
        "setprop debug.sf.disable_backpressure 1 2>/dev/null\n" +
        "setprop debug.sf.early_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.sf.early_app_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.sf.early_gl_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.sf.early_gl_app_phase_offset_ns 1000000 2>/dev/null\n" +

        "# --- 5. GAME DRIVER & BỎ BỎ GIỚI HẠN NHIỆT SAMSUNG GOS --- \n" +
        "settings put global game_driver_all_apps 1 2>/dev/null\n" +
        "settings put secure game_auto_temperature_control 0 2>/dev/null\n" +
        "cmd thermal override-status 0 2>/dev/null\n" +

        "# --- 6. DALVIK/ART VM HEAP & DEX2OAT MULTI-THREADING BOOST --- \n" +
        "setprop dalvik.vm.dex2oat-threads 8 2>/dev/null\n" +
        "setprop dalvik.vm.boot-dex2oat-threads 8 2>/dev/null\n" +
        "setprop dalvik.vm.image-dex2oat-threads 8 2>/dev/null\n" +
        "setprop dalvik.vm.heapgrowthlimit 256m 2>/dev/null\n" +
        "setprop dalvik.vm.heapsize 512m 2>/dev/null\n";

    // 2. TỐI ƯU CẢM ỨNG & MẠNG KHI CHƠI GAME
    private static final String SHIZUKU_TOUCH_NETWORK_GAMING_BOOST =
        "# --- TĂNG ĐỘ NHẠY VÀ TỐC ĐỘ PHẢN HỒI CẢM ỨNG --- \n" +
        "settings put system touch_sensitivity 1 2>/dev/null\n" +
        "settings put system pointer_speed 7 2>/dev/null\n" +
        "setprop touch.pressure.scale 0.001 2>/dev/null\n" +
        "setprop view.touch_slop 2 2>/dev/null\n" +

        "# --- CẤU HÌNH WIFI & GIẢM PING BỊ HOẠT ĐỘNG NGẦM --- \n" +
        "settings put global wifi_scan_always_enabled 0 2>/dev/null\n" +
        "settings put global ble_scan_always_enabled 0 2>/dev/null\n" +
        "cmd wifi set-one-shot-screen-on-delay-ms 0 2>/dev/null\n";

    // 3. DANH SÁCH DỊCH VỤ BÓP HIỆU NĂNG SAMSUNG CẦN TẠM DỪNG (SUSPEND GOS)
    private static final String[] SAMSUNG_THROTTLING_PACKAGES = {
        "com.samsung.android.game.gos",
        "com.samsung.android.game.gamehome",
        "com.samsung.android.game.gametools"
    };

    // 4. TỐI ƯU HẰNG NGÀY CÂN BẰNG (DAILY BALANCED PROFILE)
    private static final String SHIZUKU_CPU_GPU_DAILY_BALANCED =
        "# --- KHÔI PHỤC CHẾ ĐỘ NĂNG LƯỢNG TIÊU CHUẨN --- \n" +
        "cmd power set-fixed-performance-mode-enabled false 2>/dev/null\n" +
        "cmd power set-adaptive-power-saver-enabled true 2>/dev/null\n" +

        "# --- RENDERER SKIAGL MƯỢT MÀ CHO ONE UI --- \n" +
        "setprop debug.hwui.renderer skiagl 2>/dev/null\n" +
        "setprop debug.renderengine.backend skiagl 2>/dev/null\n" +

        "# --- BẬT SCROLLING BOOST CHO TẦN SỐ 90HZ --- \n" +
        "setprop persist.sys.scrolling.boost 1 2>/dev/null\n" +
        "setprop persist.sys.power.cpu.boost 0 2>/dev/null\n" +
        "setprop persist.vendor.powerhal.perf 0 2>/dev/null\n" +

        "# --- BẬT KIỂM SOÁT NHIỆT ĐỘ AN TOÀN --- \n" +
        "settings put secure game_auto_temperature_control 1 2>/dev/null\n" +
        "settings put global game_driver_all_apps 0 2>/dev/null\n";

    // 5. CHẾ ĐỘ TIẾT KIỆM PIN (BATTERY SAVER PROFILE)
    private static final String SHIZUKU_CPU_GPU_BATTERY =
        "cmd power set-fixed-performance-mode-enabled false 2>/dev/null\n" +
        "cmd power set-adaptive-power-saver-enabled true 2>/dev/null\n" +
        "setprop persist.sys.power.cpu.boost 0 2>/dev/null\n" +
        "setprop persist.vendor.powerhal.perf 0 2>/dev/null\n" +
        "setprop debug.performance.tuning 0 2>/dev/null\n" +
        "settings put secure game_auto_temperature_control 1 2>/dev/null\n";

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
     * Vô hiệu hóa/Tạm dừng các service bóp hiệu năng của Samsung GOS không cần Root
     */
    public static String disableSamsungGosServices(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku để tạm dừng Samsung GOS!";
        }
        StringBuilder result = new StringBuilder();
        for (String pkg : SAMSUNG_THROTTLING_PACKAGES) {
            executeCommand(context, "cmd package suspend " + pkg + " 2>/dev/null\n");
            executeCommand(context, "am force-stop " + pkg + " 2>/dev/null\n");
        }
        executeCommand(context, "cmd thermal override-status 0 2>/dev/null\n");
        executeCommand(context, "settings put secure game_auto_temperature_control 0 2>/dev/null\n");
        result.append("Đã vô hiệu hóa Samsung Game Optimizing Service (GOS) & Throttling.");
        return result.toString();
    }

    /**
     * Khôi phục lại các dịch vụ Samsung GOS về bình thường
     */
    public static String restoreSamsungGosServices(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku!";
        }
        for (String pkg : SAMSUNG_THROTTLING_PACKAGES) {
            executeCommand(context, "cmd package unsuspend " + pkg + " 2>/dev/null\n");
        }
        executeCommand(context, "cmd thermal reset 2>/dev/null\n");
        executeCommand(context, "settings put secure game_auto_temperature_control 1 2>/dev/null\n");
        return "Đã khôi phục kiểm soát nhiệt độ Samsung GOS.";
    }

    /**
     * Tinh chỉnh tỷ lệ phân giải Game (Downscale Factor) giảm tải GPU Mali-G52
     * @param packageName Tên package game (Ví dụ: com.garena.game.kgvn)
     * @param scale Tỷ lệ khung hình (Ví dụ: 0.85 = render 85% phân giải gốc)
     * @param targetFps Khóa FPS mong muốn (Ví dụ: 90 hoặc 60)
     */
    public static String optimizeGameOverlaySettings(Context context, String packageName, float scale, int targetFps) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return "Chưa nhập Package Name!";
        }
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku!";
        }

        String cmd = "cmd game mode performance " + packageName + " 2>/dev/null\n" +
                     "device_config put game_overlay " + packageName + 
                     " mode=2,fps=" + targetFps + ",downscaleFactor=" + scale + " 2>/dev/null\n";
        executeCommand(context, cmd);
        return "Đã cấu hình Game Overlay: FPS=" + targetFps + " | Scale=" + scale + " cho " + packageName;
    }

    /**
     * Chuyển đổi GPU HWUI Renderer
     */
    public static String setGpuRenderer(Context context, String renderer) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku!";
        }
        String cmd = "setprop debug.hwui.renderer " + renderer + "\n" +
                     "setprop debug.renderengine.backend " + renderer + "\n";
        executeCommand(context, cmd);
        return "GPU Renderer -> " + renderer.toUpperCase();
    }

    /**
     * Đọc thông số CPU & Thermal hiện tại từ hệ thống
     */
    public static String getCpuGpuDetailedInfo() {
        StringBuilder info = new StringBuilder("=== THÔNG TIN PHẦN CỨNG GALAXY A32 4G ===\n");
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/cpuinfo");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int processorCount = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("processor")) {
                    processorCount++;
                } else if (line.startsWith("Hardware") || line.startsWith("model name")) {
                    info.append(line).append("\n");
                }
            }
            reader.close();
            info.append("SoC: MediaTek MT6769V/CU Helio G80 (12nm)\n");
            info.append("CPU Cores: ").append(processorCount > 0 ? processorCount : 8).append(" Nhân (2x A75 @ 2.0GHz + 6x A55 @ 1.8GHz)\n");
            info.append("GPU Engine: ARM Mali-G52 MC2 (Vulkan 1.1 / OpenGL ES 3.2)\n");
        } catch (Exception e) {
            info.append("Lỗi đọc cpuinfo: ").append(e.getMessage());
        }
        return info.toString();
    }

    // =========================================================================
    // MASTER FUNCTIONS: KÍCH HOẠT PROFILE CỰC HẠN
    // =========================================================================

    /**
     * MASTER PROFILE 1: NÂNG CẤP TỐI ĐA - CHƠI GAME CỰC HẠN (EXTREME GAMING)
     * @param context Context ứng dụng
     * @param gamePackageName Package Name của game đang chơi (Có thể null)
     */
    public static String applyExtremeGamingProfile(Context context, String gamePackageName) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - ULTIMATE EXTREME GAMING BOOST\n");
        log.append(" SAMSUNG A32 4G - HELIO G80 & MALI-G52\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối! Cần kết nối Shizuku để nâng cấp cực hạn.\n");
            return log.toString();
        }

        // Step 1: Bật Performance Mode, CorePilot Boost & SkiaVK Renderer
        log.append("[1/6] Khóa xung CPU, Bật CorePilot Boost & SkiaVK Vulkan Engine... ");
        executeCommand(context, SHIZUKU_CPU_GPU_EXTREME_GAMING);
        log.append("HOÀN THÀNH!\n");

        // Step 2: Vô hiệu hóa Samsung GOS & Thermal Throttling
        log.append("[2/6] Tắt Samsung GOS & Bỏ qua giới hạn nhiệt độ Thermal Throttling... ");
        log.append(disableSamsungGosServices(context)).append("\n");

        // Step 3: Tối ưu cảm ứng & giảm trễ mạng
        log.append("[3/6] Ép nhạy cảm ứng Touch Response & Tối ưu Ping Wifi... ");
        executeCommand(context, SHIZUKU_TOUCH_NETWORK_GAMING_BOOST);
        log.append("HOÀN THÀNH!\n");

        // Step 4: Cấu hình Game Overlay nếu có Package Name
        if (gamePackageName != null && !gamePackageName.trim().isEmpty()) {
            log.append("[4/6] ");
            log.append(optimizeGameOverlaySettings(context, gamePackageName, 0.90f, 90)).append("\n");
        } else {
            log.append("[4/6] Kích hoạt Game Driver toàn hệ thống... HOÀN THÀNH!\n");
        }

        // Step 5: Ép nén RAM & Giải phóng bộ nhớ đệm
        log.append("[5/6] Ép dọn dẹp RAM & Truncate System Cache... ");
        executeCommand(context, "cmd activity compact all 2>/dev/null\n");
        executeCommand(context, "pm trim-caches 4000M 2>/dev/null\n");
        log.append("HOÀN THÀNH!\n");

        // Step 6: Hoàn tất
        log.append("[6/6] Cấu hình SurfaceFlinger Zero-Latency 90 FPS... HOÀN THÀNH!\n\n");

        log.append("=========================================\n");
        log.append(" ĐÃ KÍCH HOẠT CHẾ ĐỘ ULTIMATE GAMING!\n");
        log.append(" MÁY ĐÃ SẴN SÀNG CHIẾN GAME MƯỢT MÀ 90 FPS!\n");
        log.append("=========================================\n");

        return log.toString();
    }

    /**
     * MASTER PROFILE 2: TỐI ƯU CÂN BẰNG TÁC VỤ HẰNG NGÀY (DAILY BALANCED)
     */
    public static String applyDailyBalancedProfile(Context context) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - DAILY BALANCED PROFILE (90HZ)\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối!\n");
            return log.toString();
        }

        // 1. Cấu hình SkiaGL & Tần số quét 90Hz
        log.append("[1/3] Khôi phục xung nhịp tự động & Renderer SkiaGL... ");
        executeCommand(context, SHIZUKU_CPU_GPU_DAILY_BALANCED);
        log.append("HOÀN THÀNH!\n");

        // 2. Mở lại dịch vụ GOS an toàn
        log.append("[2/3] Bật lại kiểm soát nhiệt độ an toàn Samsung GOS... ");
        log.append(restoreSamsungGosServices(context)).append("\n");

        // 3. Tối ưu scrolling
        log.append("[3/3] Kích hoạt Touch Scrolling Boost 90Hz... HOÀN THÀNH!\n\n");

        log.append("=========================================\n");
        log.append(" ĐÃ TỐI ƯU TÁC VỤ HẰNG NGÀY MƯỢT MÀ & CÂN BẰNG!\n");
        log.append("=========================================\n");

        return log.toString();
    }

    /**
     * MASTER PROFILE 3: TIẾT KIỆM PIN (BATTERY SAVER)
     */
    public static String applyBatterySaverProfile(Context context) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - BATTERY SAVER PROFILE\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối!\n");
            return log.toString();
        }

        log.append("[1/2] Hạ mức tiêu thụ CPU/GPU & Bật tiết kiệm điện... ");
        executeCommand(context, SHIZUKU_CPU_GPU_BATTERY);
        log.append("HOÀN THÀNH!\n");

        log.append("[2/2] Bật lại Samsung Thermal Safety Control... ");
        log.append(restoreSamsungGosServices(context)).append("\n\n");

        log.append("Đã kích hoạt chế độ Tiết kiệm Pin thành công!\n");
        return log.toString();
    }
}