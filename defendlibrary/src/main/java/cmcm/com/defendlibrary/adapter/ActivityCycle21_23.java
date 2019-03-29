package cmcm.com.defendlibrary.adapter;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;

import cmcm.com.defendlibrary.utils.Reflect;

/**
 * Created by luweicheng on 2019/3/27.
 */
public class ActivityCycle21_23 extends BaseActivityCycle {

    @Override
    public void finishActivity(IBinder binder) {
        Object activityManager = Reflect
                .on("android.app.ActivityManagerNative")
                .method("getDefault")
                .invoke(null);
        Reflect.on(activityManager.getClass())
                .method("finishActivity", IBinder.class, int.class, Intent.class, boolean.class)
                .invoke(activityManager, binder, Activity.RESULT_CANCELED, null, false);
    }
}
