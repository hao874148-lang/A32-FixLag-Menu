package com.a32.fixlag.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.a32.fixlag.commands.CommandRegistry;
import com.a32.fixlag.commands.CpuGpuCommands;
import com.a32.fixlag.commands.DisplayCommands;
import com.a32.fixlag.commands.RamCommands;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Lớp Quản Lý Lưu Trữ Cấu Hình & Trạng Thái Hệ Thống (AppPreferences Pro Max AI)
 * Chuyên biệt cho Samsung Galaxy A32 4G (MediaTek Helio G80 | Mali-G52 MC2 | AMOLED 90Hz | One UI)
 *
 * Tính Năng Nâng Cấp Pro Max:
 * 1. Đồng bộ hoàn hảo với bộ lệnh CpuGpuCommands, DisplayCommands, RamCommands & CommandRegistry.
 * 2. Lưu trữ cài đặt nâng cao: LMK Profile, Thermal Safety Threshold, Dynamic Hz, Whitelist App.
 * 3. Hỗ trợ Sao lưu & Khôi phục cấu hình qua định dạng JSON (Backup & Restore Config).
 * 4. Quản lý danh sách Game tùy chỉnh (Custom Game Library) & Danh sách ứng dụng bảo vệ (Whitelist).
 * 5. Đã tích hợp Thread-safe Singleton & Callback Lắng nghe thay đổi Preference theo thời gian thực.
 *
 * Package: com.a32.fixlag.data
 */
public class AppPreferences {

    private static final String TAG = "AppPreferences_A32";
    private static final String PREF_NAME = "fixlag_a32_pro_max_preferences";
    private static AppPreferences instance;
    private final SharedPreferences sharedPreferences;

    // =========================================================================
    // CONSTANT KEYS FOR SHARED PREFERENCES
    // =========================================================================

    // 1. Chế độ hệ thống & AI Auto-Pilot
    private static final String KEY_SYSTEM_MODE = "key_system_mode";
    private static final String KEY_AI_AUTOPILOT_ENABLED = "key_ai_autopilot_enabled";
    private static final String KEY_AI_SCAN_INTERVAL_SEC = "key_ai_scan_interval_sec";
    private static final String KEY_THERMAL_SAFETY_THRESHOLD = "key_thermal_safety_threshold";

    // 2. Màn hình, Tần số quét & FPS HUD
    private static final String KEY_REFRESH_RATE_MODE = "key_refresh_rate_mode";
    private static final String KEY_RESOLUTION_PROFILE = "key_resolution_profile";
    private static final String KEY_ANIMATION_SCALE = "key_animation_scale";
    private static final String KEY_DYNAMIC_HZ_ENABLED = "key_dynamic_hz_enabled";
    private static final String KEY_SHOW_FPS_HUD = "key_show_fps_hud";

    // 3. CPU, GPU & Samsung GOS Tuning
    private static final String KEY_CPU_PROFILE = "key_cpu_profile";
    private static final String KEY_GOS_DISABLED = "key_gos_disabled";
    private static final String KEY_USE_VULKAN_RENDERER = "key_use_vulkan_renderer";
    private static final String KEY_TOUCH_BOOST_ENABLED = "key_touch_boost_enabled";
    private static final String KEY_FORCE_MAX_GPU_FREQ = "key_force_max_gpu_freq";

    // 4. RAM, Swap & Low Memory Killer (LMK)
    private static final String KEY_RAM_PROFILE = "key_ram_profile";
    private static final String KEY_RAM_PLUS_SIZE_GB = "key_ram_plus_size_gb";
    private static final String KEY_AUTO_CLEAR_RAM_ENABLED = "key_auto_clear_ram_enabled";
    private static final String KEY_SWAPPINESS_VALUE = "key_swappiness_value";
    private static final String KEY_LMK_AGGRESSIVENESS = "key_lmk_aggressiveness";

    // 5. Game Target & Quản lý Ứng dụng
    private static final String KEY_TARGET_GAME_PACKAGE = "key_target_game_package";
    private static final String KEY_AUTO_BOOST_GAME_LAUNCH = "key_auto_boost_game_launch";
    private static final String KEY_CUSTOM_GAME_PACKAGES = "key_custom_game_packages";
    private static final String KEY_RAM_CLEANER_WHITELIST = "key_ram_cleaner_whitelist";

    // 6. Trạng thái ứng dụng & Boot Service
    private static final String KEY_IS_FIRST_RUN = "key_is_first_run";
    private static final String KEY_AUTO_START_ON_BOOT = "key_auto_start_on_boot";
    private static final String KEY_PERSISTENT_NOTIFICATION = "key_persistent_notification";
    private static final String KEY_LAST_OPTIMIZATION_TIMESTAMP = "key_last_optimization_timestamp";

    // =========================================================================
    // CONSTRUCTOR & SINGLETON PATTERN
    // =========================================================================

    private AppPreferences(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lấy thực thể duy nhất của AppPreferences (Thread-safe Singleton)
     */
    public static synchronized AppPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
        return instance;
    }

    /**
     * Đăng ký bộ lắng nghe sự thay đổi SharedPreferences
     */
    public void registerChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Hủy đăng ký bộ lắng nghe
     */
    public void unregisterChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    // =========================================================================
    // 1. QUẢN LÝ CHẾ ĐỘ HỆ THỐNG & AI AUTO-PILOT ENGINE
    // =========================================================================

    public void setSystemMode(CommandRegistry.SystemMode mode) {
        sharedPreferences.edit().putString(KEY_SYSTEM_MODE, mode.name()).apply();
    }

    public CommandRegistry.SystemMode getSystemMode() {
        String modeStr = sharedPreferences.getString(KEY_SYSTEM_MODE, CommandRegistry.SystemMode.PRO_MAX_AI_AUTO.name());
        try {
            return CommandRegistry.SystemMode.valueOf(modeStr);
        } catch (Exception e) {
            return CommandRegistry.SystemMode.PRO_MAX_AI_AUTO;
        }
    }

    public void setAiAutoPilotEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AI_AUTOPILOT_ENABLED, enabled).apply();
    }

    public boolean isAiAutoPilotEnabled() {
        return sharedPreferences.getBoolean(KEY_AI_AUTOPILOT_ENABLED, true);
    }

    public void setAiScanIntervalSec(int seconds) {
        sharedPreferences.edit().putInt(KEY_AI_SCAN_INTERVAL_SEC, Math.max(5, seconds)).apply();
    }

    public int getAiScanIntervalSec() {
        return sharedPreferences.getInt(KEY_AI_SCAN_INTERVAL_SEC, 15); // Mặc định 15 giây/lần
    }

    /**
     * Ngưỡng nhiệt độ an toàn (°C) - Nếu vượt quá, AI sẽ tự hạ xung để bảo vệ chip Helio G80
     */
    public void setThermalSafetyThreshold(int tempCelsius) {
        sharedPreferences.edit().putInt(KEY_THERMAL_SAFETY_THRESHOLD, tempCelsius).apply();
    }

    public int getThermalSafetyThreshold() {
        return sharedPreferences.getInt(KEY_THERMAL_SAFETY_THRESHOLD, 43); // Mặc định 43°C cho A32 4G
    }

    // =========================================================================
    // 2. MÀN HÌNH, TẦN SỐ QUÉT (90Hz/60Hz) & ĐỘ PHÂN GIẢI
    // =========================================================================

    public void setRefreshRateMode(DisplayCommands.RefreshRateMode mode) {
        sharedPreferences.edit().putString(KEY_REFRESH_RATE_MODE, mode.name()).apply();
    }

    public DisplayCommands.RefreshRateMode getRefreshRateMode() {
        String modeStr = sharedPreferences.getString(KEY_REFRESH_RATE_MODE, DisplayCommands.RefreshRateMode.HZ_90_LOCKED.name());
        try {
            return DisplayCommands.RefreshRateMode.valueOf(modeStr);
        } catch (Exception e) {
            return DisplayCommands.RefreshRateMode.HZ_90_LOCKED;
        }
    }

    public void setResolutionProfile(DisplayCommands.ResolutionProfile profile) {
        sharedPreferences.edit().putString(KEY_RESOLUTION_PROFILE, profile.name()).apply();
    }

    public DisplayCommands.ResolutionProfile getResolutionProfile() {
        String profileStr = sharedPreferences.getString(KEY_RESOLUTION_PROFILE, DisplayCommands.ResolutionProfile.FHD_1080P_NATIVE.name());
        try {
            return DisplayCommands.ResolutionProfile.valueOf(profileStr);
        } catch (Exception e) {
            return DisplayCommands.ResolutionProfile.FHD_1080P_NATIVE;
        }
    }

    public void setAnimationScale(float scale) {
        sharedPreferences.edit().putFloat(KEY_ANIMATION_SCALE, scale).apply();
    }

    public float getAnimationScale() {
        return sharedPreferences.getFloat(KEY_ANIMATION_SCALE, 0.5f); // 0.5x tối ưu One UI
    }

    public void setDynamicHzEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DYNAMIC_HZ_ENABLED, enabled).apply();
    }

    public boolean isDynamicHzEnabled() {
        return sharedPreferences.getBoolean(KEY_DYNAMIC_HZ_ENABLED, true);
    }

    public void setShowFpsHud(boolean show) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_FPS_HUD, show).apply();
    }

    public boolean isShowFpsHud() {
        return sharedPreferences.getBoolean(KEY_SHOW_FPS_HUD, false);
    }

    // =========================================================================
    // 3. QUẢN LÝ CPU, GPU & SAMSUNG GOS
    // =========================================================================

    public void setCpuProfile(CpuGpuCommands.CpuProfile profile) {
        sharedPreferences.edit().putString(KEY_CPU_PROFILE, profile.name()).apply();
    }

    public CpuGpuCommands.CpuProfile getCpuProfile() {
        String profileStr = sharedPreferences.getString(KEY_CPU_PROFILE, CpuGpuCommands.CpuProfile.EXTREME_GAMING.name());
        try {
            return CpuGpuCommands.CpuProfile.valueOf(profileStr);
        } catch (Exception e) {
            return CpuGpuCommands.CpuProfile.EXTREME_GAMING;
        }
    }

    public void setGosDisabled(boolean disabled) {
        sharedPreferences.edit().putBoolean(KEY_GOS_DISABLED, disabled).apply();
    }

    public boolean isGosDisabled() {
        return sharedPreferences.getBoolean(KEY_GOS_DISABLED, true);
    }

    public void setUseVulkanRenderer(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_USE_VULKAN_RENDERER, enabled).apply();
    }

    public boolean isUseVulkanRenderer() {
        return sharedPreferences.getBoolean(KEY_USE_VULKAN_RENDERER, true); // Khuyên dùng cho Mali-G52
    }

    public void setTouchBoostEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_TOUCH_BOOST_ENABLED, enabled).apply();
    }

    public boolean isTouchBoostEnabled() {
        return sharedPreferences.getBoolean(KEY_TOUCH_BOOST_ENABLED, true);
    }

    public void setForceMaxGpuFreq(boolean force) {
        sharedPreferences.edit().putBoolean(KEY_FORCE_MAX_GPU_FREQ, force).apply();
    }

    public boolean isForceMaxGpuFreq() {
        return sharedPreferences.getBoolean(KEY_FORCE_MAX_GPU_FREQ, true);
    }

    // =========================================================================
    // 4. BỘ NHỚ RAM, ZRAM SWAP & LOW MEMORY KILLER (LMK)
    // =========================================================================

    public void setRamProfile(RamCommands.Profile profile) {
        sharedPreferences.edit().putString(KEY_RAM_PROFILE, profile.name()).apply();
    }

    public RamCommands.Profile getRamProfile() {
        String profileStr = sharedPreferences.getString(KEY_RAM_PROFILE, RamCommands.Profile.GAMING.name());
        try {
            return RamCommands.Profile.valueOf(profileStr);
        } catch (Exception e) {
            return RamCommands.Profile.GAMING;
        }
    }

    public void setRamPlusSizeGb(int sizeGb) {
        sharedPreferences.edit().putInt(KEY_RAM_PLUS_SIZE_GB, sizeGb).apply();
    }

    public int getRamPlusSizeGb() {
        return sharedPreferences.getInt(KEY_RAM_PLUS_SIZE_GB, 2); // 2GB cân bằng cho eMMC 5.1
    }

    public void setAutoClearRamEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_CLEAR_RAM_ENABLED, enabled).apply();
    }

    public boolean isAutoClearRamEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_CLEAR_RAM_ENABLED, true);
    }

    public void setSwappinessValue(int swappiness) {
        sharedPreferences.edit().putInt(KEY_SWAPPINESS_VALUE, Math.min(100, Math.max(0, swappiness))).apply();
    }

    public int getSwappinessValue() {
        return sharedPreferences.getInt(KEY_SWAPPINESS_VALUE, 60); // Standard Linux Swappiness
    }

    /**
     * Mức độ dọn app ngầm LMK: 0 = Standard, 1 = Moderate, 2 = Aggressive (Game Mode)
     */
    public void setLmkAggressiveness(int level) {
        sharedPreferences.edit().putInt(KEY_LMK_AGGRESSIVENESS, level).apply();
    }

    public int getLmkAggressiveness() {
        return sharedPreferences.getInt(KEY_LMK_AGGRESSIVENESS, 2);
    }

    // =========================================================================
    // 5. GAME TARGET, WHITELIST & QUẢN LÝ ỨNG DỤNG
    // =========================================================================

    public void setTargetGamePackage(String packageName) {
        sharedPreferences.edit().putString(KEY_TARGET_GAME_PACKAGE, packageName).apply();
    }

    public String getTargetGamePackage() {
        return sharedPreferences.getString(KEY_TARGET_GAME_PACKAGE, "");
    }

    public void setAutoBoostGameLaunch(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_BOOST_GAME_LAUNCH, enabled).apply();
    }

    public boolean isAutoBoostGameLaunch() {
        return sharedPreferences.getBoolean(KEY_AUTO_BOOST_GAME_LAUNCH, true);
    }

    /**
     * Danh sách game tùy chỉnh do người dùng thêm vào
     */
    public Set<String> getCustomGamePackages() {
        return new HashSet<>(sharedPreferences.getStringSet(KEY_CUSTOM_GAME_PACKAGES, new HashSet<>()));
    }

    public void addCustomGamePackage(String pkgName) {
        Set<String> set = getCustomGamePackages();
        set.add(pkgName);
        sharedPreferences.edit().putStringSet(KEY_CUSTOM_GAME_PACKAGES, set).apply();
    }

    public void removeCustomGamePackage(String pkgName) {
        Set<String> set = getCustomGamePackages();
        set.remove(pkgName);
        sharedPreferences.edit().putStringSet(KEY_CUSTOM_GAME_PACKAGES, set).apply();
    }

    /**
     * Danh sách ứng dụng Whitelist (Không bao giờ tắt ngầm khi dọn RAM/Game Mode)
     */
    public Set<String> getRamCleanerWhitelist() {
        Set<String> defaults = new HashSet<>();
        defaults.add("com.facebook.orca"); // Messenger
        defaults.add("com.zing.zalo");     // Zalo
        defaults.add("com.a32.fixlag");    // App FixLag
        return new HashSet<>(sharedPreferences.getStringSet(KEY_RAM_CLEANER_WHITELIST, defaults));
    }

    public void addPackageToWhitelist(String pkgName) {
        Set<String> set = getRamCleanerWhitelist();
        set.add(pkgName);
        sharedPreferences.edit().putStringSet(KEY_RAM_CLEANER_WHITELIST, set).apply();
    }

    public void removePackageFromWhitelist(String pkgName) {
        Set<String> set = getRamCleanerWhitelist();
        set.remove(pkgName);
        sharedPreferences.edit().putStringSet(KEY_RAM_CLEANER_WHITELIST, set).apply();
    }

    // =========================================================================
    // 6. TRẠNG THÁI HỆ THỐNG, BOOT & TIỀN CẢNH
    // =========================================================================

    public boolean isFirstRun() {
        boolean isFirst = sharedPreferences.getBoolean(KEY_IS_FIRST_RUN, true);
        if (isFirst) {
            sharedPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, false).apply();
        }
        return isFirst;
    }

    public void setAutoStartOnBoot(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_START_ON_BOOT, enabled).apply();
    }

    public boolean isAutoStartOnBoot() {
        return sharedPreferences.getBoolean(KEY_AUTO_START_ON_BOOT, true);
    }

    public void setPersistentNotification(boolean show) {
        sharedPreferences.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, show).apply();
    }

    public boolean isPersistentNotification() {
        return sharedPreferences.getBoolean(KEY_PERSISTENT_NOTIFICATION, true);
    }

    public void setLastOptimizationTimestamp(long timestamp) {
        sharedPreferences.edit().putLong(KEY_LAST_OPTIMIZATION_TIMESTAMP, timestamp).apply();
    }

    public long getLastOptimizationTimestamp() {
        return sharedPreferences.getLong(KEY_LAST_OPTIMIZATION_TIMESTAMP, 0L);
    }

    // =========================================================================
    // 7. HỆ THỐNG SAO LƯU & KHÔI PHỤC CẤU HÌNH (JSON BACKUP / RESTORE)
    // =========================================================================

    /**
     * Xuất toàn bộ cấu hình ứng dụng ra chuỗi JSON để backup
     */
    public String exportConfigToJson() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_SYSTEM_MODE, getSystemMode().name());
            json.put(KEY_AI_AUTOPILOT_ENABLED, isAiAutoPilotEnabled());
            json.put(KEY_REFRESH_RATE_MODE, getRefreshRateMode().name());
            json.put(KEY_RESOLUTION_PROFILE, getResolutionProfile().name());
            json.put(KEY_ANIMATION_SCALE, getAnimationScale());
            json.put(KEY_GOS_DISABLED, isGosDisabled());
            json.put(KEY_USE_VULKAN_RENDERER, isUseVulkanRenderer());
            json.put(KEY_TOUCH_BOOST_ENABLED, isTouchBoostEnabled());
            json.put(KEY_RAM_PLUS_SIZE_GB, getRamPlusSizeGb());
            json.put(KEY_AUTO_CLEAR_RAM_ENABLED, isAutoClearRamEnabled());
            json.put(KEY_THERMAL_SAFETY_THRESHOLD, getThermalSafetyThreshold());

            JSONArray customGames = new JSONArray();
            for (String pkg : getCustomGamePackages()) {
                customGames.put(pkg);
            }
            json.put(KEY_CUSTOM_GAME_PACKAGES, customGames);

            return json.toString(4);
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi khi xuất cấu hình JSON: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * Nhập cấu hình từ chuỗi JSON sao lưu
     */
    public boolean importConfigFromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (json.has(KEY_SYSTEM_MODE)) editor.putString(KEY_SYSTEM_MODE, json.getString(KEY_SYSTEM_MODE));
            if (json.has(KEY_AI_AUTOPILOT_ENABLED)) editor.putBoolean(KEY_AI_AUTOPILOT_ENABLED, json.getBoolean(KEY_AI_AUTOPILOT_ENABLED));
            if (json.has(KEY_REFRESH_RATE_MODE)) editor.putString(KEY_REFRESH_RATE_MODE, json.getString(KEY_REFRESH_RATE_MODE));
            if (json.has(KEY_RESOLUTION_PROFILE)) editor.putString(KEY_RESOLUTION_PROFILE, json.getString(KEY_RESOLUTION_PROFILE));
            if (json.has(KEY_ANIMATION_SCALE)) editor.putFloat(KEY_ANIMATION_SCALE, (float) json.getDouble(KEY_ANIMATION_SCALE));
            if (json.has(KEY_GOS_DISABLED)) editor.putBoolean(KEY_GOS_DISABLED, json.getBoolean(KEY_GOS_DISABLED));
            if (json.has(KEY_USE_VULKAN_RENDERER)) editor.putBoolean(KEY_USE_VULKAN_RENDERER, json.getBoolean(KEY_USE_VULKAN_RENDERER));
            if (json.has(KEY_TOUCH_BOOST_ENABLED)) editor.putBoolean(KEY_TOUCH_BOOST_ENABLED, json.getBoolean(KEY_TOUCH_BOOST_ENABLED));
            if (json.has(KEY_RAM_PLUS_SIZE_GB)) editor.putInt(KEY_RAM_PLUS_SIZE_GB, json.getInt(KEY_RAM_PLUS_SIZE_GB));
            if (json.has(KEY_AUTO_CLEAR_RAM_ENABLED)) editor.putBoolean(KEY_AUTO_CLEAR_RAM_ENABLED, json.getBoolean(KEY_AUTO_CLEAR_RAM_ENABLED));
            if (json.has(KEY_THERMAL_SAFETY_THRESHOLD)) editor.putInt(KEY_THERMAL_SAFETY_THRESHOLD, json.getInt(KEY_THERMAL_SAFETY_THRESHOLD));

            if (json.has(KEY_CUSTOM_GAME_PACKAGES)) {
                JSONArray gamesArray = json.getJSONArray(KEY_CUSTOM_GAME_PACKAGES);
                Set<String> customGames = new HashSet<>();
                for (int i = 0; i < gamesArray.length(); i++) {
                    customGames.add(gamesArray.getString(i));
                }
                editor.putStringSet(KEY_CUSTOM_GAME_PACKAGES, customGames);
            }

            editor.apply();
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi khi nhập cấu hình JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Khôi phục toàn bộ cài đặt về mặc định tối ưu ban đầu
     */
    public void resetAllToDefault() {
        sharedPreferences.edit().clear().apply();
        Log.i(TAG, "Đã khôi phục cài đặt mặc định cho Samsung Galaxy A32 4G.");
    }
}