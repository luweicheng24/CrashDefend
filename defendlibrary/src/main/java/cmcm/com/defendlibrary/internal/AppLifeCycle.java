package cmcm.com.defendlibrary.internal;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by luweicheng on 2019/3/26.
 */
public class AppLifeCycle implements Application.ActivityLifecycleCallbacks {
    public static ArrayList<Activity> queue = new ArrayList<>(4);
    private static final String TAG = "AppLifeCycle";

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated: ");
        queue.add(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.i(TAG, "onActivityResumed: ");
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.i(TAG, "onActivityDestroyed: ");
        queue.remove(activity);
    }
}
