package cmcm.com.crashdefend;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by luweicheng on 2019/3/26.
 */
public class SecActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sec);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int a = 54 / 0;
    }
}
