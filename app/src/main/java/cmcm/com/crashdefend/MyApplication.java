package cmcm.com.crashdefend;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import cmcm.com.defendlibrary.CmCatcher;
import cmcm.com.defendlibrary.handler.CmThrowableHandler;

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
        CmCatcher.registerCatcher(this, new CmThrowableHandler() {
            @Override
            public void handlerException(Throwable msg) {
                // 异常上报
                Toast.makeText(MyApplication.this, msg.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("CmCatcher", "handlerException: " + msg.getMessage()+"线程name"+Thread.currentThread().getName());
            }
        });
    }
}
