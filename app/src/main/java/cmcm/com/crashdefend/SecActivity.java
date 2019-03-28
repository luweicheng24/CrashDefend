package cmcm.com.crashdefend;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by luweicheng on 2019/3/26.
 */
public class SecActivity extends Activity {
    private int flag;
    private static final String TAG = "SecActivity";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: ");
        setContentView(R.layout.activity_sec);
        flag = getIntent().getIntExtra("flag", -1);
        System.out.println(10/(flag-1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        System.out.println(10/(flag-3));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
        System.out.println(10/(flag-2));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        System.out.println(10/(flag-4));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
        System.out.println(10/(flag-5));
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }
}
