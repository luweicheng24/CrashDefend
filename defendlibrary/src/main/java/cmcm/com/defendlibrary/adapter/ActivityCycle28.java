package cmcm.com.defendlibrary.adapter;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import cmcm.com.defendlibrary.utils.Reflect;

/**
 * Created by luweicheng on 2019/3/27.
 */
public class ActivityCycle28 extends BaseActivityCycle {

    @Override
    public void finishActivity(IBinder binder) {
        Object activityManager = Reflect
                .on(ActivityManager.class)
                .method("getService")
                .invoke(null);
        Reflect.on(activityManager.getClass())
                .method("finishActivity", IBinder.class, int.class, Intent.class, int.class)
                .invoke(activityManager, binder, Activity.RESULT_CANCELED, null, DONT_FINISH_TASK_WITH_ACTIVITY);
    }

    @Override
    public void finish(Message msg) {
        Object activityClientRecord = msg.obj;
        IBinder binder = (IBinder) Reflect.on(activityClientRecord.getClass()).field("mActivityToken").get(activityClientRecord);
        finishActivity(binder);
    }
}
