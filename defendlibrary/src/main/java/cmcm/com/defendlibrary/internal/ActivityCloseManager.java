package cmcm.com.defendlibrary.internal;

import android.os.Build;
import android.os.Message;

import cmcm.com.defendlibrary.adapter.ActivityCycle15_20;
import cmcm.com.defendlibrary.adapter.ActivityCycle21_23;
import cmcm.com.defendlibrary.adapter.ActivityCycle24_25;
import cmcm.com.defendlibrary.adapter.ActivityCycle26;
import cmcm.com.defendlibrary.adapter.ActivityCycle28;
import cmcm.com.defendlibrary.adapter.IActivityCycle;

/**
 * Created by luweicheng on 2019/3/27.
 */
public class ActivityCloseManager {
    private static ActivityCloseManager instance = new ActivityCloseManager();

    private ActivityCloseManager() {
    }

    public static ActivityCloseManager getInstance() {
        return instance;
    }

    private static IActivityCycle mActivityCycle;

    public void finish(Message msg) {
        if (Build.VERSION.SDK_INT >= 28) {
            mActivityCycle = new ActivityCycle28();
        } else if (Build.VERSION.SDK_INT >= 26) {
            mActivityCycle = new ActivityCycle26();
        } else if (Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 24) {
            mActivityCycle = new ActivityCycle24_25();
        } else if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT <= 23) {
            mActivityCycle = new ActivityCycle21_23();
        } else if (Build.VERSION.SDK_INT >= 15 && Build.VERSION.SDK_INT <= 20) {
            mActivityCycle = new ActivityCycle15_20();
        }
        mActivityCycle.finish(msg);
    }
}
