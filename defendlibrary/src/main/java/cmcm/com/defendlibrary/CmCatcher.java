package cmcm.com.defendlibrary;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public static void registerCatcher(Context ctx, @NonNull final CmThrowableHandler handler) {
        if (hasInstall.get()) {
            return;  // has register
        }
        try {
            //解除 android P 反射限制
            Reflection.unseal(ctx);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        mHandler = handler;
        hasInstall.compareAndSet(false, true);
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null);
            Field mhField = activityThreadClass.getDeclaredField("mH");
            mhField.setAccessible(true);
            final Handler mhHandler = (Handler) mhField.get(activityThread);
            final Field callbackField = Handler.class.getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            callbackField.set(mhHandler, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case LAUNCH_ACTIVITY:
                        case RESUME_ACTIVITY:
                        case PAUSE_ACTIVITY:
                        case STOP_ACTIVITY_HIDE:
                        case DESTROY_ACTIVITY:
                        case PAUSE_ACTIVITY_FINISHING:
                        case EXECUTE_TRANSACTION:
                        case NEW_INTENT:
                        case RELAUNCH_ACTIVITY28:
                        case RELAUNCH_ACTIVITY: {
                            try {
                                mhHandler.handleMessage(msg);
                            } catch (Exception e) {
                                mHandler.handlerException(e);
                                AppLifeCycle.queue.getFirst().finish();
                            } finally {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                while (true) {  // 防止第二次抛出无法捕捉
                    try {
                        Looper.loop();
                    } catch (Throwable e) {
                        if (e instanceof CmCrashException) {  // unregiter 时取消该套机制
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

    public static void unRegister() {
        if (hasInstall.get()) {
            hasInstall.compareAndSet(true, false);
        }
        if (mUncaughtExceptionHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
        }
        if (mHandler != null) {
            throw new CmCrashException("cancel exception catcher");
        }

    }
}
