package com.a32.fixlag;

import android.app.Application;
import com.a32.fixlag.shizuku.ShizukuManager;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ShizukuManager.init();
    }
}
