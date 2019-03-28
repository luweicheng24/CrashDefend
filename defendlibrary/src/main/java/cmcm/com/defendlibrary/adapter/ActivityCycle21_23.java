package cmcm.com.defendlibrary.adapter;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;

import java.lang.reflect.Method;

/**
 * Created by luweicheng on 2019/3/27.
 */
public class ActivityCycle21_23 extends BaseActivityCycle {

    @Override
    public void finishActivity(IBinder binder) {
        try {
            Class activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = activityManagerNativeClass.getDeclaredMethod("getDefault");
            Object activityManager = getDefaultMethod.invoke(null);
            Method finishActivityMethod = activityManager.getClass().getDeclaredMethod("finishActivity", IBinder.class, int.class, Intent.class, boolean.class);
            finishActivityMethod.invoke(activityManager, binder, Activity.RESULT_CANCELED, null, false);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
