package com.a32.fixlag.commands;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.a32.fixlag.shizuku.ShellExecutor;
import com.a32.fixlag.shizuku.ShizukuManager;

/**
 * Lớp điều khiển & tối ưu hóa Màn hình, Độ phân giải, Tần số quét (90Hz/60Hz) - NÂNG CẤP TỐI THƯỢNG
 * Chuyên biệt cho Samsung Galaxy A32 4G (Super AMOLED 90Hz | Full HD+ 1080x2400 | Density 420 DPI)
 * Hoạt động 100% qua Shizuku / ADB Shell (Không cần Root)
 * Package: com.a32.fixlag.commands
 */
public class DisplayCommands {

    // =========================================================================
    // THÔNG SỐ ĐỘ PHÂN GIẢI MÀN HÌNH CHUẨN SAMSUNG GALAXY A32 4G (20:9 RATIO)
    // =========================================================================

    // 1. Độ phân giải gốc Full HD+ (Default)
    public static final int NATIVE_WIDTH = 1080;
    public static final int NATIVE_HEIGHT = 2400;
    public static final int NATIVE_DENSITY = 420;

    // 2. Độ phân giải Cân bằng 900p
    public static final int BALANCED_WIDTH = 900;
    public static final int BALANCED_HEIGHT = 2000;
    public static final int BALANCED_DENSITY = 350;

    // 3. Độ phân giải Mượt mà 810p
    public static final int SMOOTH_WIDTH = 810;
    public static final int SMOOTH_HEIGHT = 1800;
    public static final int SMOOTH_DENSITY = 315;

    // 4. Độ phân giải Chơi game HD+ (720p)
    public static final int GAME_WIDTH = 720;
    public static final int GAME_HEIGHT = 1600;
    public static final int GAME_DENSITY = 280;

    // 5. Độ phân giải FPS Cao 640p
    public static final int SUPER_FPS_WIDTH = 640;
    public static final int SUPER_FPS_HEIGHT = 1422;
    public static final int SUPER_FPS_DENSITY = 250;

    // 6. Độ phân giải Siêu hạ FPS (540p)
    public static final int EXTREME_LOW_WIDTH = 540;
    public static final int EXTREME_LOW_HEIGHT = 1200;
    public static final int EXTREME_LOW_DENSITY = 210;

    /**
     * Enum định nghĩa các Chế độ Tần số quét
     */
    public enum RefreshRateMode {
        HZ_90_LOCKED, // Khóa cố định 90Hz
        HZ_60_LOCKED, // Khóa 60Hz
        HZ_AUTO       // Tự động thích ứng
    }

    /**
     * Enum định nghĩa các Cấp độ Độ phân giải
     */
    public enum ResolutionProfile {
        FHD_1080P_NATIVE, // 1080x2400 (Sắc nét)
        BALANCED_900P,    // 900x2000 (Cân bằng)
        SMOOTH_810P,      // 810x1800 (Mượt)
        HD_720P_GAMING,   // 720x1600 (Siêu mượt)
        SUPER_FPS_640P,   // 640x1422 (FPS Cao)
        EXTREME_540P      // 540x1200 (Tối đa FPS)
    }

    // =========================================================================
    // DANH SÁCH BỘ LỆNH SHIZUKU / ADB TUNING MÀN HÌNH TỐI THƯỢNG
    // =========================================================================

    private static final String SHIZUKU_FORCE_90HZ =
        "settings put system peak_refresh_rate 90.0 2>/dev/null\n" +
        "settings put system min_refresh_rate 90.0 2>/dev/null\n" +
        "settings put user user_refresh_rate 2 2>/dev/null\n" +
        "settings put global refresh_rate_mode 2 2>/dev/null\n" +
        "setprop persist.sys.sf.high_fps 90 2>/dev/null\n";

    private static final String SHIZUKU_FORCE_60HZ =
        "settings put system peak_refresh_rate 60.0 2>/dev/null\n" +
        "settings put system min_refresh_rate 60.0 2>/dev/null\n" +
        "settings put user user_refresh_rate 1 2>/dev/null\n" +
        "settings put global refresh_rate_mode 0 2>/dev/null\n" +
        "setprop persist.sys.sf.high_fps 60 2>/dev/null\n";

    private static final String SHIZUKU_AUTO_REFRESH_RATE =
        "settings put system peak_refresh_rate 90.0 2>/dev/null\n" +
        "settings put system min_refresh_rate 60.0 2>/dev/null\n" +
        "settings put user user_refresh_rate 2 2>/dev/null\n" +
        "settings put global refresh_rate_mode 1 2>/dev/null\n";

    private static final String SHIZUKU_SURFACEFLINGER_ZERO_LATENCY =
        "setprop debug.sf.latch_unsignaled 1 2>/dev/null\n" +
        "setprop debug.sf.disable_backpressure 1 2>/dev/null\n" +
        "setprop debug.sf.early_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.sf.early_app_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.sf.early_gl_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.sf.early_gl_app_phase_offset_ns 1000000 2>/dev/null\n" +
        "setprop debug.graphics.game_default_frame_rate.enabled 1 2>/dev/null\n";

    private static final String SHIZUKU_TOUCH_RESPONSE_BOOST =
        "settings put system touch_sensitivity 1 2>/dev/null\n" +
        "settings put system pointer_speed 7 2>/dev/null\n" +
        "setprop touch.pressure.scale 0.001 2>/dev/null\n" +
        "setprop view.touch_slop 2 2>/dev/null\n" +
        "setprop view.scroll_friction 0.005 2>/dev/null\n";

    private static String executeCommand(Context context, String command) {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.hasPermission()) {
            return ShellExecutor.executeShizukuCommand(command);
        } else {
            return ShellExecutor.executeNormalCommand(command);
        }
    }

    public static String setForce90Hz(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku để thay đổi Tần số quét!";
        }
        executeCommand(context, SHIZUKU_FORCE_90HZ);
        return "Đã khóa cố định Tần số quét: 90Hz.";
    }

    public static String setForce60Hz(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku để thay đổi Tần số quét!";
        }
        executeCommand(context, SHIZUKU_FORCE_60HZ);
        return "Đã khóa cố định Tần số quét: 60Hz (Tiết kiệm pin).";
    }

    public static String setAutoRefreshRate(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku!";
        }
        executeCommand(context, SHIZUKU_AUTO_REFRESH_RATE);
        return "Đã bật Tần số quét Tự động Dynamic (60Hz - 90Hz).";
    }

    public static String setCustomResolution(Context context, int width, int height, int density) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku để chỉnh độ phân giải!";
        }
        String cmd = "wm size " + width + "x" + height + " 2>/dev/null\n" +
                     "wm density " + density + " 2>/dev/null\n";
        executeCommand(context, cmd);
        return "Đã thiết lập Độ phân giải: " + width + "x" + height + " | DPI: " + density;
    }

    public static String setNativeResolution1080p(Context context) {
        return setCustomResolution(context, NATIVE_WIDTH, NATIVE_HEIGHT, NATIVE_DENSITY);
    }

    public static String setBalancedResolution900p(Context context) {
        return setCustomResolution(context, BALANCED_WIDTH, BALANCED_HEIGHT, BALANCED_DENSITY);
    }

    public static String setSmoothResolution810p(Context context) {
        return setCustomResolution(context, SMOOTH_WIDTH, SMOOTH_HEIGHT, SMOOTH_DENSITY);
    }

    public static String setGamingResolution720p(Context context) {
        return setCustomResolution(context, GAME_WIDTH, GAME_HEIGHT, GAME_DENSITY);
    }

    public static String setSuperFpsResolution640p(Context context) {
        return setCustomResolution(context, SUPER_FPS_WIDTH, SUPER_FPS_HEIGHT, SUPER_FPS_DENSITY);
    }

    public static String setExtremeResolution540p(Context context) {
        return setCustomResolution(context, EXTREME_LOW_WIDTH, EXTREME_LOW_HEIGHT, EXTREME_LOW_DENSITY);
    }

    public static String resetDisplayToDefault(Context context) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku!";
        }
        String cmd = "wm size reset 2>/dev/null\n" +
                     "wm density reset 2>/dev/null\n" +
                     SHIZUKU_AUTO_REFRESH_RATE +
                     "settings put global window_animation_scale 1.0 2>/dev/null\n" +
                     "settings put global transition_animation_scale 1.0 2>/dev/null\n" +
                     "settings put global animator_duration_scale 1.0 2>/dev/null\n";
        executeCommand(context, cmd);
        return "Đã khôi phục thiết lập Màn hình về Mặc định nhà sản xuất.";
    }

    public static String setAnimationScale(Context context, float scale) {
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            return "Cần quyền Shizuku!";
        }
        String cmd = "settings put global window_animation_scale " + scale + " 2>/dev/null\n" +
                     "settings put global transition_animation_scale " + scale + " 2>/dev/null\n" +
                     "settings put global animator_duration_scale " + scale + " 2>/dev/null\n";
        executeCommand(context, cmd);
        return "Tỉ lệ hoạt ảnh (Animation) -> " + scale + "x";
    }

    public static String applyGamingDisplayProfile(Context context) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - ULTIMATE GAMING DISPLAY BOOST\n");
        log.append(" SAMSUNG A32 4G - HELIO G80 & MALI-G52\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối!\n");
            return log.toString();
        }

        log.append("[1/4] Chuyển độ phân giải Game HD+ (720x1600 | 280 DPI)... ");
        log.append(setGamingResolution720p(context)).append("\n");

        log.append("[2/4] Khóa cố định Tần số quét 90Hz... ");
        executeCommand(context, SHIZUKU_FORCE_90HZ);
        log.append("HOÀN THÀNH!\n");

        log.append("[3/4] Tối ưu SurfaceFlinger & Giảm trễ dựng khung hình... ");
        executeCommand(context, SHIZUKU_SURFACEFLINGER_ZERO_LATENCY);
        executeCommand(context, SHIZUKU_TOUCH_RESPONSE_BOOST);
        log.append("HOÀN THÀNH!\n");

        log.append("[4/4] Tắt Animation (0.0x) tăng tốc cảm ứng... ");
        setAnimationScale(context, 0.0f);
        log.append("HOÀN THÀNH!\n\n");

        log.append("=========================================\n");
        log.append(" ĐÃ TỐI ƯU MÀN HÌNH CHƠI GAME CỰC HẠN!\n");
        log.append("=========================================\n");

        return log.toString();
    }

    public static String applyDailyDisplayProfile(Context context) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - DAILY SMOOTH DISPLAY PROFILE\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối!\n");
            return log.toString();
        }

        log.append("[1/3] Trả về độ phân giải chuẩn FHD+ (1080x2400 | 420 DPI)... ");
        log.append(setNativeResolution1080p(context)).append("\n");

        log.append("[2/3] Bật Tần số quét 90Hz cho One UI... ");
        executeCommand(context, SHIZUKU_FORCE_90HZ);
        log.append("HOÀN THÀNH!\n");

        log.append("[3/3] Thiết lập Tỉ lệ hoạt ảnh 0.5x mượt siêu tốc... ");
        setAnimationScale(context, 0.5f);
        log.append("HOÀN THÀNH!\n\n");

        log.append("=========================================\n");
        log.append(" ĐÃ TỐI ƯU MÀN HÌNH HẰNG NGÀY SẮC NÉT & MƯỢT MÀ!\n");
        log.append("=========================================\n");

        return log.toString();
    }

    public static String applyBatterySaverDisplayProfile(Context context) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================\n");
        log.append(" FIXLAG - BATTERY SAVER DISPLAY PROFILE\n");
        log.append("=========================================\n\n");

        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            log.append("[CẢNH BÁO] Shizuku chưa kết nối!\n");
            return log.toString();
        }

        log.append("[1/2] Hạ Tần số quét xuống 60Hz tiết kiệm pin... ");
        executeCommand(context, SHIZUKU_FORCE_60HZ);
        log.append("HOÀN THÀNH!\n");

        log.append("[2/2] Khôi phục độ phân giải FHD+ 1080p... ");
        log.append(setNativeResolution1080p(context)).append("\n\n");

        log.append("Đã kích hoạt chế độ Màn hình Tiết Kiệm Pin thành công!\n");
        return log.toString();
    }

    public static String getDisplayDetailedInfo(Context context) {
        StringBuilder info = new StringBuilder("=== THÔNG SỐ MÀN HÌNH GALAXY A32 4G ===\n");
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealMetrics(metrics);

                info.append("Độ phân giải thực tế: ").append(metrics.widthPixels).append(" x ").append(metrics.heightPixels).append(" px\n");
                info.append("Mật độ điểm ảnh: ").append(metrics.densityDpi).append(" DPI (Scale: ").append(metrics.density).append("x)\n");
                info.append("Tần số quét hiện tại: ").append((int) display.getRefreshRate()).append(" Hz\n");
            }

            String shellSize = ShellExecutor.executeNormalCommand("wm size");
            String shellDensity = ShellExecutor.executeNormalCommand("wm density");
            if (shellSize != null && !shellSize.trim().isEmpty()) {
                info.append("Cấu hình Shell Size: ").append(shellSize.trim()).append("\n");
            }
            if (shellDensity != null && !shellDensity.trim().isEmpty()) {
                info.append("Cấu hình Shell Density: ").append(shellDensity.trim()).append("\n");
            }

            info.append("Tấm nền: Super AMOLED | Mặc định: 1080x2400 @ 90Hz\n");
        } catch (Exception e) {
            info.append("Lỗi đọc thông số display: ").append(e.getMessage());
        }
        return info.toString();
    }
}