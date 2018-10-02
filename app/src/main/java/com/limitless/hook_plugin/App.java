package com.limitless.hook_plugin;

import android.app.Application;

/**
 * Created by Nick on 2018/9/30
 *
 * @author Nick
 */
public class App extends Application {

    public static boolean isLogin=false;
    @Override
    public void onCreate() {
        super.onCreate();
        HookUtils hookUtils = new HookUtils(this);
        hookUtils.hookStartActivity();
        hookUtils.replaceRealIntent();

    }
}
