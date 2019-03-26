package cmcm.com.crashdefend;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import cmcm.com.defendlibrary.AppLifeCycle;
import cmcm.com.defendlibrary.CmCatcher;
import cmcm.com.defendlibrary.CmThrowableHandler;

/**
 * Created by luweicheng on 2019/3/26.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        registerCmCatcher();
    }

    private void registerCmCatcher() {
        registerActivityLifecycleCallbacks(new AppLifeCycle());
        CmCatcher.registerCatcher(this, new CmThrowableHandler() {
            @Override
            public void handlerException(Throwable msg) {
                // 异常上报
                Toast.makeText(MyApplication.this, msg.getMessage(), Toast.LENGTH_LONG);
                Log.e("lwc", "handlerException: " + msg.getMessage());
            }
        });
    }
}
