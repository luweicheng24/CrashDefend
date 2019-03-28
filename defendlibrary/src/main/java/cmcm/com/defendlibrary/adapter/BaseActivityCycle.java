package cmcm.com.defendlibrary.adapter;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by luweicheng on 2019/3/27.
 */
public abstract class BaseActivityCycle implements IActivityCycle {
    public  void finishActivity(IBinder binder) {
        Class activityManagerNativeClass = null;
        try {
            activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = activityManagerNativeClass.getDeclaredMethod("getDefault");
            Object activityManager = getDefaultMethod.invoke(null);
            Method finishActivityMethod = activityManager.getClass().getDeclaredMethod("finishActivity", IBinder.class, int.class, Intent.class, int.class);
            int DONT_FINISH_TASK_WITH_ACTIVITY = 0;
            finishActivityMethod.invoke(activityManager, binder, Activity.RESULT_CANCELED, null, DONT_FINISH_TASK_WITH_ACTIVITY);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish(Message msg) {
        try {
            Object activityClientRecord = msg.obj;
            Field tokenField = activityClientRecord.getClass().getDeclaredField("token");
            tokenField.setAccessible(true);
            IBinder binder = (IBinder) tokenField.get(activityClientRecord);
            finishActivity(binder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
