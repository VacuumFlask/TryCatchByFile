package com.example.administrator.trycatchbyfile;

import android.app.Application;

/**
 * Created by Administrator on 2016/9/2 0002.
 */
public class CrashApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
//        CrashHandler crashHandler = CrashHandler.getInstance();
//        crashHandler.init(this);
        CrashHandlerTwo instance = CrashHandlerTwo.getInstance();
        instance.init(this);
    }
}
