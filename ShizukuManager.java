package com.a32.fixlag.shizuku;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import rikka.shizuku.Shizuku;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp Quản Lý Kết Nối & Cấp Quyền Shizuku Đa Năng (ShizukuManager Pro Max)
 * Tối ưu hóa phản hồi theo thời gian thực (Real-time Listener) cho Samsung Galaxy A32 4G.
 *
 * Package: com.a32.fixlag.shizuku
 */
public class ShizukuManager {

    private static final String TAG = "ShizukuManager_A32";
    public static final int SHIZUKU_REQ_CODE = 1001;

    // Danh sách lắng nghe thay đổi trạng thái kết nối Shizuku
    private static final List<ShizukuStateListener> stateListeners = new CopyOnWriteArrayList<>();
    private static boolean isListenerRegistered = false;

    public interface ShizukuStateListener {
        void onShizukuConnected();
        void onShizukuDisconnected();
        void onPermissionGranted();
        void onPermissionDenied();
    }

    // Binder Received Listener
    private static final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = () -> {
        Log.i(TAG, "Shizuku Binder đã kết nối thành công!");
        notifyConnected();
    };

    // Binder Dead Listener
    private static final Shizuku.OnBinderDeadListener BINDER_DEAD_LISTENER = () -> {
        Log.w(TAG, "Shizuku Binder đã ngắt kết nối!");
        notifyDisconnected();
    };

    // Request Permission Result Listener
    private static final Shizuku.OnRequestPermissionResultListener PERMISSION_RESULT_LISTENER = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_REQ_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Quyền Shizuku đã được người dùng phê duyệt!");
                notifyPermissionGranted();
            } else {
                Log.w(TAG, "Quyền Shizuku bị từ chối.");
                notifyPermissionDenied();
            }
        }
    };

    /**
     * Khởi tạo bộ lắng nghe trạng thái Shizuku (Nên gọi trong Application hoặc MainActivity)
     */
    public static synchronized void init() {
        if (isListenerRegistered) return;
        try {
            Shizuku.addBinderReceivedListener(BINDER_RECEIVED_LISTENER);
            Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER);
            Shizuku.addRequestPermissionResultListener(PERMISSION_RESULT_LISTENER);
            isListenerRegistered = true;
            Log.i(TAG, "Đã khởi tạo bộ lắng nghe Shizuku thành công.");
        } catch (Throwable t) {
            Log.e(TAG, "Lỗi khi khởi tạo Shizuku Listeners: " + t.getMessage());
        }
    }

    /**
     * Kiểm tra xem dịch vụ Shizuku ADB có đang hoạt động hay không
     */
    public static boolean isShizukuAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Kiểm tra xem ứng dụng đã có quyền Shizuku ADB hay chưa
     */
    public static boolean hasPermission() {
        try {
            if (!isShizukuAvailable()) {
                return false;
            }
            if (Shizuku.isPreV11()) {
                return false;
            }
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            Log.e(TAG, "Lỗi kiểm tra quyền Shizuku: " + t.getMessage());
            return false;
        }
    }

    /**
     * Gửi yêu cầu xin quyền Shizuku đơn giản
     */
    public static void requestPermission(int requestCode) {
        try {
            if (isShizukuAvailable() && !hasPermission()) {
                Shizuku.requestPermission(requestCode);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Lỗi khi xin quyền Shizuku: " + t.getMessage());
        }
    }

    /**
     * Xin quyền Shizuku có kiểm tra Activity
     */
    public static void requestPermission(Activity activity) {
        if (!isShizukuAvailable()) {
            Log.w(TAG, "Shizuku chưa chạy trên thiết bị!");
            return;
        }
        if (!hasPermission()) {
            Shizuku.requestPermission(SHIZUKU_REQ_CODE);
        }
    }

    /**
     * Đăng ký nhận sự kiện trạng thái
     */
    public static void addListener(ShizukuStateListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký nhận sự kiện
     */
    public static void removeListener(ShizukuStateListener listener) {
        if (listener != null) {
            stateListeners.remove(listener);
        }
    }

    // --- CÁC HÀM THÔNG BÁO NỘI BỘ ---
    private static void notifyConnected() {
        for (ShizukuStateListener l : stateListeners) l.onShizukuConnected();
    }

    private static void notifyDisconnected() {
        for (ShizukuStateListener l : stateListeners) l.onShizukuDisconnected();
    }

    private static void notifyPermissionGranted() {
        for (ShizukuStateListener l : stateListeners) l.onPermissionGranted();
    }

    private static void notifyPermissionDenied() {
        for (ShizukuStateListener l : stateListeners) l.onPermissionDenied();
    }

    /**
     * Tương thích ngược với code cũ
     */
    public static void registerBinderListeners(Shizuku.OnBinderReceivedListener receivedListener,
                                               Shizuku.OnBinderDeadListener deadListener) {
        try {
            if (receivedListener != null) Shizuku.addBinderReceivedListener(receivedListener);
            if (deadListener != null) Shizuku.addBinderDeadListener(deadListener);
        } catch (Throwable ignored) {}
    }

    /**
     * Lấy tóm tắt trạng thái Shizuku dưới dạng văn bản
     */
    public static String getStatusSummary(Context context) {
        if (!isShizukuAvailable()) {
            return "Trạng thái: CHƯA KÍCH HOẠT SHIZUKU (Hãy bật Gỡ lỗi Wi-Fi)";
        }
        if (hasPermission()) {
            return "Trạng thái: ĐÃ KẾT NỐI & ĐÃ CẤP QUYỀN SHIZUKU ADB (Sẵn sàng tối ưu)";
        }
        return "Trạng thái: ĐÃ BẬT SHIZUKU - CHỜ CẤP QUYỀN CHOP APP";
    }
}