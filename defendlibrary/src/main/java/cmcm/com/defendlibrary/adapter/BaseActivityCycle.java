package cmcm.com.defendlibrary.adapter;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import cmcm.com.defendlibrary.utils.Reflect;

/**
 * Created by luweicheng on 2019/3/27.
 */
public abstract class BaseActivityCycle implements IActivityCycle {
    int DONT_FINISH_TASK_WITH_ACTIVITY = 0;

    public void finishActivity(IBinder binder) {
        Object activityManager = Reflect
                .on("android.app.ActivityManagerNative")
                .method("getDefault")
                .invoke(null);
        Reflect.on(activityManager.getClass())
                .method("finishActivity", IBinder.class, int.class, Intent.class, int.class)
                .invoke(activityManager, binder, Activity.RESULT_CANCELED, null, DONT_FINISH_TASK_WITH_ACTIVITY);
    }

    @Override
    public void finish(Message msg) {
        Object activityClientRecord = msg.obj;
        IBinder binder = (IBinder) Reflect.on(activityClientRecord.getClass()).field("token").get(activityClientRecord);
        finishActivity(binder);
    }

}
