package com.limitless.hook_plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Nick on 2018/9/30
 *
 * @author Nick
 */
public class HookUtils {

    private Context context;
    public HookUtils(Context context) {
        this.context = context;
    }

    public void hookStartActivity() {
        //        还原 gDefault 成员变量  反射  调用一次
        try {
//            Class<?> ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
//            Field gDefault = ActivityManagerNative.getDeclaredField("gDefault");
//            gDefault.setAccessible(true);
//            //            因为是静态变量  所以获取的到的是系统值  hook   伪hook
//            Object defaultValue = gDefault.get(null);
//            //mInstance对象
//            Class<?> SingletonClass = Class.forName("android.util.Singleton");
//
//            Field mInstance = SingletonClass.getDeclaredField("mInstance");
//            //        还原 IActivityManager对象  系统对象
//            mInstance.setAccessible(true);
//            Object iActivityManagerObject = mInstance.get(defaultValue);
//            Class<?> IActivityManagerIntercept = Class.forName("android.app.IActivityManager");


            Class<?> ActivityManagerNative = Class.forName("android.app.ActivityManager");
            Field gDefault = ActivityManagerNative.getDeclaredField("IActivityManagerSingleton");
            gDefault.setAccessible(true);
            //            因为是静态变量  所以获取的到的是系统值  hook   伪hook
            Object defaultValue = gDefault.get(null);
            //mInstance对象
            Class<?> SingletonClass = Class.forName("android.util.Singleton");

            Field mInstance = SingletonClass.getDeclaredField("mInstance");
            //        还原 IActivityManager对象  系统对象
            mInstance.setAccessible(true);
            Object iActivityManagerObject = mInstance.get(defaultValue);

            StartActivity startActivityMethod = new StartActivity(iActivityManagerObject);
            Class<?> IActivityManagerIntercept = Class.forName("android.app.IActivityManager");

            // 第二参数  是即将返回的对象 需要实现那些接口
            Object oldIActivityManager = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader()
                    , new Class[]{IActivityManagerIntercept, View.OnClickListener.class}, startActivityMethod);

            // 将系统的iActivityManager  替换成    自己通过动态代理实现的对象   oldIActivityManager对象  实现了 IActivityManager这个接口的所有方法
            mInstance.set(defaultValue, oldIActivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class StartActivity implements InvocationHandler {
        private Object iActivityManagerObject;

        public StartActivity( Object iActivityManagerObject) {
            this.iActivityManagerObject = iActivityManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if ("startActivity".equals(method.getName())) {
                //瞒天过海  寻找传进来的intent
                Intent intent = null;
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    // 过滤调用startActivity时传入的intent
                    Object arg = args[i];
                    if (arg instanceof Intent) {
                        intent = (Intent) args[i];
                        index = i;
                    }
                }
                //使用伪装的Intent，并把真实的Intent的附加上去
                Intent newIntent = new Intent();
                ComponentName componentName = new ComponentName(context, ProxyActivity.class);
                newIntent.setComponent(componentName);
                //真实的意图 被我隐藏到了  键值对
                newIntent.putExtra("oldIntent", intent);
                args[index] = newIntent;
            }
            return method.invoke(iActivityManagerObject, args);
        }
    }

    public void replaceRealIntent() {
        try {
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = forName.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            // 还原系统的ActivityTread   mH
            Object activityThreadObj = currentActivityThreadField.get(null);

            Field handlerField = forName.getDeclaredField("mH");
            handlerField.setAccessible(true);
            //hook点找到了
            Handler mH = (Handler) handlerField.get(activityThreadObj);

            Field callbackField = Handler.class.getDeclaredField("mCallback");
            callbackField.setAccessible(true);

            callbackField.set(mH, new ActivityMH(mH));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ActivityMH implements Handler.Callback {
        private Handler mH;
        public ActivityMH(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {
            //LAUNCH_ACTIVITY ==100 即将要加载一个activity了
            if (msg.what == 100) {
                handleLauncherActivity(msg);
            }
            //做了真正的跳转
            mH.handleMessage(msg);
            return true;
        }

        private void handleLauncherActivity(Message msg) {
            // 还原
            Object obj = msg.obj;
            try {
                Field intentField = obj.getClass().getDeclaredField("intent");
                intentField.setAccessible(true);
                //  ProxyActivity
                Intent realIntent = (Intent) intentField.get(obj);

                Intent oldIntent = realIntent.getParcelableExtra("oldIntent");
                if (oldIntent != null) {
                    // 集中式登录
                    if (App.isLogin) {
                        // 登录  还原  把原有的意图    放到realIntent
                        realIntent.setComponent(oldIntent.getComponent());
                    } else {
                        ComponentName componentName = new ComponentName(context, LoginActivity.class);
                        realIntent.putExtra("extraIntent", oldIntent.getComponent().getClassName());
                        realIntent.setComponent(componentName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
