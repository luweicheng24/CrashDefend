#### CrashDefend使用步骤

 1. 添加jetpack仓库
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
2. 引入到项目

```
dependencies {
	        implementation 'com.github.luweicheng24:CrashDefend:1.0.3'
	}
```
3. 自定义Application中初始化：

```
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
                // 异常上报 该处用户自定义处理异常
                Toast.makeText(MyApplication.this, msg.getMessage(), Toast.LENGTH_LONG);
                Log.e("lwc", "handlerException: " + msg.getMessage());
            }
        });
    }
}
```
