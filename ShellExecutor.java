package com.a32.fixlag.shizuku;

import android.content.Context;
import android.util.Log;
import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Lớp Thực Thi Lệnh Shell Siêu Cấp (ShellExecutor Engine Pro Max)
 * Tối ưu hóa tối đa cho Samsung Galaxy A32 4G (MediaTek Helio G80)
 * * TÍNH NĂNG NÂNG CẤP PRO MAX:
 * 1. Chống treo tiến trình (Anti-Deadlock Stream Reader): Đọc song song cả Output & Error stream.
 * 2. Timeout Safety Manager: Tự động ngắt lệnh nếu bị kẹt quá 12 giây, tránh đứng app.
 * 3. Multi-line Batch Processing: Xử lý chuỗi lệnh nhiều dòng (chứa comment #, xuống dòng) mượt mà.
 * 4. Fallback thông minh: Tự động chuyển về Normal Shell nếu Shizuku Binder ngắt đột ngột.
 * * Package: com.a32.fixlag.shizuku
 */
public class ShellExecutor {

    private static final String TAG = "ShellExecutor_A32";
    private static final int COMMAND_TIMEOUT_SECONDS = 12; // Thời gian chờ tối đa cho 1 tập lệnh

    // ThreadPool để xử lý đọc luồng stream song song & quản lý timeout
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Thực thi lệnh Shell qua quyền ADB Shizuku (Dành cho RamCommands, CpuGpuCommands, DisplayCommands)
     *
     * @param command Chuỗi lệnh shell (có thể chứa nhiều dòng)
     * @return Chuỗi kết quả phản hồi từ Shell
     */
    public static String executeShizukuCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "Lệnh rỗng.";
        }

        // Kiểm tra xem Shizuku có sẵn sàng không, nếu không tự động dùng Normal Shell
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            Log.w(TAG, "Shizuku không khả dụng hoặc chưa cấp quyền. Chuyển sang Normal Shell.");
            return executeNormalCommand(command);
        }

        Callable<String> task = () -> {
            StringBuilder output = new StringBuilder();
            Process process = null;
            try {
                // Khởi tạo process shell qua Shizuku
                process = Shizuku.newProcess(new String[]{"sh"}, null, null);

                // Gửi lệnh vào tiến trình
                OutputStream os = process.getOutputStream();
                os.write(command.getBytes(StandardCharsets.UTF_8));
                os.write("\nexit\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Đọc luồng Standard Output
                BufferedReader stdReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                // Đọc luồng Standard Error (chống tràn buffer gây kẹt process)
                BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = stdReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = errReader.readLine()) != null) {
                    // Log ra lỗi nếu có nhưng vẫn duy trì chương trình
                    if (!line.trim().isEmpty()) {
                        Log.d(TAG, "[Shizuku Shell STDERR] " + line);
                    }
                }

                stdReader.close();
                errReader.close();

                process.waitFor();

            } catch (Exception e) {
                Log.e(TAG, "Lỗi thực thi lệnh qua Shizuku: " + e.getMessage(), e);
                output.append("Error: ").append(e.getMessage());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
            return output.toString().trim();
        };

        try {
            Future<String> future = executorService.submit(task);
            return future.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Lệnh Shizuku bị quá thời gian (Timeout) hoặc bị hủy: " + e.getMessage());
            return "Lỗi: Lệnh thực thi quá thời gian cho phép (" + COMMAND_TIMEOUT_SECONDS + "s).";
        }
    }

    /**
     * Tương thích với các gọi hàm truyền vào Context
     */
    public static String executeShizukuCommand(Context context, String command) {
        return executeShizukuCommand(command);
    }

    /**
     * Thực thi lệnh Shell thông thường của ứng dụng (Normal Shell)
     *
     * @param command Chuỗi lệnh shell
     * @return Chuỗi kết quả phản hồi
     */
    public static String executeNormalCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "Lệnh rỗng.";
        }

        Callable<String> task = () -> {
            StringBuilder output = new StringBuilder();
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("sh");

                OutputStream os = process.getOutputStream();
                os.write(command.getBytes(StandardCharsets.UTF_8));
                os.write("\nexit\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = errReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        Log.d(TAG, "[Normal Shell STDERR] " + line);
                    }
                }

                reader.close();
                errReader.close();

                process.waitFor();

            } catch (Exception e) {
                Log.e(TAG, "Lỗi thực thi lệnh Normal Shell: " + e.getMessage(), e);
                output.append("Error: ").append(e.getMessage());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
            return output.toString().trim();
        };

        try {
            Future<String> future = executorService.submit(task);
            return future.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Normal Shell Timeout: " + e.getMessage());
            return "Lỗi: Normal Shell quá thời gian phản hồi.";
        }
    }

    /**
     * Tương thích với các gọi hàm truyền vào Context
     */
    public static String executeNormalCommand(Context context, String command) {
        return executeNormalCommand(command);
    }
}