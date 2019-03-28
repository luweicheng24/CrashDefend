package cmcm.com.crashdefend;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView tvAge;
    Intent intent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAge = findViewById(R.id.tv_age);
        intent.setComponent(new ComponentName(this, SecActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void create(View view) {
        intent.putExtra("flag", 1);
        startActivity(intent);
    }

    public void start(View view) {
        intent.putExtra("flag", 2);
        startActivity(intent);
    }

    public void resume(View view) {
        intent.putExtra("flag", 3);
        startActivity(intent);
    }

    public void pause(View view) {
        intent.putExtra("flag", 4);
        startActivity(intent);
    }

    public void stop(View view) {
        intent.putExtra("flag", 5);
        startActivity(intent);
    }

    public void click(View view) {
        int a = 5 / 0;
    }

    public void subThread(View view) {
        new Thread() {
            @Override
            public void run() {
                int a = 5 / 0;
            }
        }.start();
    }
}
