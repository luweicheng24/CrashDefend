package cmcm.com.defendlibrary.adapter;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by luweicheng on 2019/3/27.
 */
public class ActivityCycle28 extends BaseActivityCycle {

    @Override
    public void finishActivity(IBinder binder) {
        try {
            Method getServiceMethod = ActivityManager.class.getDeclaredMethod("getService");
            Object activityManager = getServiceMethod.invoke(null);
            Method finishActivityMethod = activityManager.getClass().getDeclaredMethod("finishActivity", IBinder.class, int.class, Intent.class, int.class);
            finishActivityMethod.setAccessible(true);
            int DONT_FINISH_TASK_WITH_ACTIVITY = 0;
            finishActivityMethod.invoke(activityManager, binder, Activity.RESULT_CANCELED, null, DONT_FINISH_TASK_WITH_ACTIVITY);

        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public void finish(Message msg) {
        try {
            Object clientTransaction = msg.obj;
            Field binder = clientTransaction.getClass().getDeclaredField("mActivityToken");
            binder.setAccessible(true);
            IBinder iBinder = (IBinder) binder.get(clientTransaction);
            finishActivity(iBinder);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
