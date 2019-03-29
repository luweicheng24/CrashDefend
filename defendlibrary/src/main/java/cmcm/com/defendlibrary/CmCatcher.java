package cmcm.com.defendlibrary;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import cmcm.com.defendlibrary.exception.CmCrashException;
import cmcm.com.defendlibrary.exception.ReflectException;
import cmcm.com.defendlibrary.handler.CmThrowableHandler;
import cmcm.com.defendlibrary.internal.ActivityCloseManager;
import cmcm.com.defendlibrary.internal.AppLifeCycle;
import cmcm.com.defendlibrary.utils.Reflect;
import me.weishu.reflection.Reflection;

/**
 * Created by luweicheng on 2019/3/26.
 * <p>
 * 全局捕捉类
 */
public final class CmCatcher {
    private static AtomicBoolean hasInstall = new AtomicBoolean(false);
    private static Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;
    private static CmThrowableHandler mHandler;
    static final int LAUNCH_ACTIVITY = 100;
    static final int PAUSE_ACTIVITY = 101;
    static final int PAUSE_ACTIVITY_FINISHING = 102;
    static final int STOP_ACTIVITY_HIDE = 104;
    static final int RESUME_ACTIVITY = 107;
    static final int DESTROY_ACTIVITY = 109;
    static final int NEW_INTENT = 112;
    static final int RELAUNCH_ACTIVITY = 126;
    // 28以后只有一下两个消息类型操作Activity生命周期
    public static final int EXECUTE_TRANSACTION = 159;
    public static final int RELAUNCH_ACTIVITY28 = 160;

    private CmCatcher() {
    }

    public static void registerCatcher(Application ctx, final CmThrowableHandler handler) {
        if (hasInstall.get()) {
            return;  // has register
        }
        if (ctx == null) {
            return;
        }
        try {
            //解除 android P 反射限制
            Reflection.unseal(ctx);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return;
        }
        boolean hasReflect = reflectHandlerActivityLife();
        if (!hasReflect) {
            return;
        }
        ctx.registerActivityLifecycleCallbacks(new AppLifeCycle());
        mHandler = handler;
        hasInstall.compareAndSet(false, true);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                while (true) {  // 防止第二次抛出无法捕捉
                    try {
                        Looper.loop();
                    } catch (Throwable e) {
                        if (e instanceof CmCrashException) {  // unregister 时取消该套机制
                            if (BuildConfig.DEBUG) {
                                Log.i("lwc", "run: 取消crash机制 ");
                            }
                            return;
                        }
                        if (handler != null) {
                            handler.handlerException(e);
                        }
                    }
                }
            }
        });

        mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()

        {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (handler != null) {
                    handler.handlerException(e);
                }
            }
        });
    }

    private static boolean reflectHandlerActivityLife() {
        Object activityThread = Reflect.on("android.app.ActivityThread").method("currentActivityThread").invoke(null);
        final Handler mh = (Handler) Reflect.on(activityThread.getClass()).field("mH").get(activityThread);
        Reflect.on(Handler.class).field("mCallback").set(mh, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case LAUNCH_ACTIVITY:
                    case RESUME_ACTIVITY:
                    case PAUSE_ACTIVITY:
                    case STOP_ACTIVITY_HIDE:
                    case PAUSE_ACTIVITY_FINISHING:
                    case EXECUTE_TRANSACTION:
                    case NEW_INTENT:
                    case RELAUNCH_ACTIVITY28:
                    case RELAUNCH_ACTIVITY: {
                        try {
                            mh.handleMessage(msg);
                        } catch (Throwable e) {
                            handlerActivityLifeCycle(e, msg);
                        }
                        return true;
                    }
                    case DESTROY_ACTIVITY: {
                        try {
                            mh.handleMessage(msg);
                        } catch (Throwable e) {
                            mHandler.handlerException(e);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        return true;
    }

    private static void handlerActivityLifeCycle(Throwable e, Message msg) {
        mHandler.handlerException(e);
        try {
            ActivityCloseManager.getInstance().finish(msg);
        } catch (ReflectException ex) {  // 一般不会出现此情况 实在出现了 finish所有界面
            unRegister();
            ArrayList<Activity> list = AppLifeCycle.queue;
            for (Activity act : list) {
                act.finish();
            }
        }

    }

    public static void unRegister() {
        if (hasInstall.get()) {
            hasInstall.compareAndSet(true, false);
        }
        if (mUncaughtExceptionHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                throw new CmCrashException("cancel exception catcher");  // 取消while循环
            }
        });
    }
}
