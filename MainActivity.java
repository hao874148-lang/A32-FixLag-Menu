package com.a32.fixlag.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.a32.fixlag.R;
import com.a32.fixlag.shizuku.ShizukuManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity - Màn hình chính điều phối ứng dụng FixLag A32 VIP Pro Max
 * Package: com.a32.fixlag.ui
 */
public class MainActivity extends AppCompatActivity implements ShizukuManager.ShizukuStateListener {

    private View viewShizukuDot;
    private TextView tvShizukuStatus;
    private Button btnGrantShizuku;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation();

        // Đăng ký nhận sự kiện kết nối Shizuku
        ShizukuManager.addListener(this);
        updateShizukuUi();

        // Load Fragment mặc định (Dashboard Telemetry)
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void initViews() {
        viewShizukuDot = findViewById(R.id.viewShizukuDot);
        tvShizukuStatus = findViewById(R.id.tvShizukuStatus);
        btnGrantShizuku = findViewById(R.id.btnGrantShizuku);
        bottomNav = findViewById(R.id.bottomNav);

        btnGrantShizuku.setOnClickListener(v -> ShizukuManager.requestPermission(this));
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_menu) {
                loadFragment(new MenuFixLagFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateShizukuUi() {
        boolean available = ShizukuManager.isShizukuAvailable();
        boolean hasPermission = ShizukuManager.hasPermission();

        if (available && hasPermission) {
            viewShizukuDot.setBackgroundColor(getColor(R.color.status_good));
            tvShizukuStatus.setText("Shizuku ADB: Đã kết nối & cấp quyền VIP");
            btnGrantShizuku.setVisibility(View.GONE);
        } else if (available) {
            viewShizukuDot.setBackgroundColor(getColor(R.color.status_warning));
            tvShizukuStatus.setText("Shizuku ADB: Đang chờ cấp quyền");
            btnGrantShizuku.setVisibility(View.VISIBLE);
        } else {
            viewShizukuDot.setBackgroundColor(getColor(R.color.status_danger));
            tvShizukuStatus.setText("Shizuku ADB: Chưa chạy (Bật Wi-Fi Debugging)");
            btnGrantShizuku.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShizukuManager.removeListener(this);
    }

    @Override
    public void onShizukuConnected() {
        runOnUiThread(this::updateShizukuUi);
    }

    @Override
    public void onShizukuDisconnected() {
        runOnUiThread(this::updateShizukuUi);
    }

    @Override
    public void onPermissionGranted() {
        runOnUiThread(this::updateShizukuUi);
    }

    @Override
    public void onPermissionDenied() {
        runOnUiThread(this::updateShizukuUi);
    }
}