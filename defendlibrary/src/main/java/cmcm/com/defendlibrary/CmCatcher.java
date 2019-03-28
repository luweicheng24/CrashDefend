package cmcm.com.defendlibrary;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import cmcm.com.defendlibrary.exception.CmCrashException;
import cmcm.com.defendlibrary.handler.CmThrowableHandler;
import cmcm.com.defendlibrary.internal.AppLifeCycle;
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
                            return;
                        }
                        if (handler != null) {  // 交由我们自己处置
                            handler.handlerException(e);
                        }
                    }
                }
            }
        });

        mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler(); // 设置默认处理类 unregister时设置默认处理

        // 下面线程异常处理机制基本就是子线程的处理 主线程我们已经自己处理了
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()

        {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (handler != null) {
                    handler.handlerException(e); //交给我们自己处理
                }
            }
        });
    }

    private static boolean reflectHandlerActivityLife() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null);
            Field mhField = activityThreadClass.getDeclaredField("mH");
            mhField.setAccessible(true);
            final Handler mh = (Handler) mhField.get(activityThread);
            final Field callbackField = Handler.class.getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            callbackField.set(mh, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case LAUNCH_ACTIVITY: {
                            try {
                                mh.handleMessage(msg);
                            } catch (Throwable e) {
                                mHandler.handlerException(e);
                                ActivityCloseManager.getInstance().finish(msg);
                            }
                            return true;
                        }
                        case RESUME_ACTIVITY:
                        case PAUSE_ACTIVITY:
                        case STOP_ACTIVITY_HIDE:
                        case PAUSE_ACTIVITY_FINISHING: // home键界面消失
                        case EXECUTE_TRANSACTION: // android8.0新的Activity生命周期调用message
                        case NEW_INTENT:
                        case RELAUNCH_ACTIVITY28:
                        case RELAUNCH_ACTIVITY: {
                            try {
                                mh.handleMessage(msg);
                            } catch (Throwable e) {
                                mHandler.handlerException(e);
                                ActivityCloseManager.getInstance().finish(msg);
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;// 反射失败
        }
        return true;
    }

    public static void unRegister() {
        if (hasInstall.get()) {
            hasInstall.compareAndSet(true, false);
        }
        if (mUncaughtExceptionHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
        }
        if (mHandler != null) {
            throw new CmCrashException("cancel exception catcher");  // 取消while循环
        }
    }
}
