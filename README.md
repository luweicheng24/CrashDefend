
 app的crash大部分是由于代码不健壮或者脏数据造成的，·如何才能最大限度的避免这些crash，提升用户体验，增加留存，下面个人的一些对crash的思考与实践：

> 先来看一下测试视频，一下每个按钮都会触发异常，按照正常android异常处理机制，在生命周期内发生异常会导致界面黑屏等现象，非生命周期内会再直接kill掉application：

![crash.gif](https://upload-images.jianshu.io/upload_images/4082354-5439454b38d1c48c.gif?imageMogr2/auto-orient/strip)

作为一个android开发者基本了解当用户点击launcher上的app图标时，Zygote会fork一个进程，通过classloader加载运行ActivityThread的Main方法，然后bindApplication，由此开启了消息驱动机制来运行这个app。而这个消息驱动的机器便是ActivityThread中Main方法中的Looper：

```

    public static void main(String[] args) {
       ...
        Looper.prepareMainLooper(); // 创建main looper
        ActivityThread thread = new ActivityThread();
        thread.attach(false);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }
  ...

        Looper.loop(); // 开始循环取消息

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```
通过以上代码便开启了消息驱动的大幕，activity、service、broadcast、contentprovider、window、view绘制、事件分发这些都是通过该消息驱动来进行事件分发，而日常最常见的一些crash log 基本都有下面红线里面的部分：
![在这里插入图片描述](http://upload-images.jianshu.io/upload_images/4082354-cb51a7bc502544db?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
了解Throwable运行机制的同学，应该都看得出在进行一系列方法调用过程中，异常消息在收集异常日志时是从调用方法栈中一层一层地将调用的信息作为异常日志保存到异常log中，而既然app是消息驱动，所以我们的大部分crash都是包含上面红线框中的部分，只要在最开始调用的地方也就是方法调用时最先压栈的方法进行`try{} catch{}` 处理就能避免crash的发生，而红线中的方法我们能处理的就是`Looper`了，Thread API中包含UncaughtExceptionHandler这个类，用来专门处理线程在发生异常时的处理，而在Zygote由init进程创建时，系统便实现了该异常处理类，先来看一下Zygote在初始化时的大体逻辑：

```
App_main.main

int main(int argc, char* const argv[])
{
  ...
    //参数解析
    bool zygote = false;
    bool startSystemServer = false;
    bool application = false;
    String8 niceName;
    String8 className;
    ++i;
    while (i < argc) {
        const char* arg = argv[i++];
        if (strcmp(arg, "--zygote") == 0) {
            zygote = true;
            //对于64位系统nice_name为zygote64; 32位系统为zygote
            niceName = ZYGOTE_NICE_NAME;
        } else if (strcmp(arg, "--start-system-server") == 0) {
            startSystemServer = true;
        } else if (strcmp(arg, "--application") == 0) {
            application = true;
        } else if (strncmp(arg, "--nice-name=", 12) == 0) {
            niceName.setTo(arg + 12);
        } else if (strncmp(arg, "--", 2) != 0) {
            className.setTo(arg);
            break;
        } else {
            --i;
            break;
        }
    }
  ...
   //设置进程名
    if (!niceName.isEmpty()) {
        runtime.setArgv0(niceName.string());
        set_process_name(niceName.string());
    }
    if (zygote) {
        // 启动AppRuntime 
        runtime.start("com.android.internal.os.ZygoteInit", args, zygote);
    } else if (className) {
        runtime.start("com.android.internal.os.RuntimeInit", args, zygote);
    } else {
        //没有指定类名或zygote，参数错误
        return 10;
    }
}
```
经过一系列调用到达`RuntimeInit.java`的`main`方法中调用的`commonInit`：

```
 protected static final void commonInit() {
        if (DEBUG) Slog.d(TAG, "Entered RuntimeInit!");

        /*
         * set handlers; these apply to all threads in the VM. Apps can replace
         * the default handler, but not the pre handler.
         */
        LoggingHandler loggingHandler = new LoggingHandler();
        Thread.setUncaughtExceptionPreHandler(loggingHandler);
        Thread.setDefaultUncaughtExceptionHandler(new KillApplicationHandler(loggingHandler)); // 设置系统默认异常处理器
       ...
    }

```
以上代码看出异常处理器为`KillApplicationHandler`,接下来看一下该类的异常处理逻辑：

```
      @Override
        public void uncaughtException(Thread t, Throwable e) {
            try {
                ensureLogging(t, e);  // 处理异常log的输出

                // Don't re-enter -- avoid infinite loops if crash-reporting crashes.
                if (mCrashing) return;
                mCrashing = true;

                // Try to end profiling. If a profiler is running at this point, and we kill the
                // process (below), the in-memory buffer will be lost. So try to stop, which will
                // flush the buffer. (This makes method trace profiling useful to debug crashes.)
                if (ActivityThread.currentActivityThread() != null) {  // 结束androidstudio的进程分析 
                    ActivityThread.currentActivityThread().stopProfiling();  
                }

                // Bring up crash dialog, wait for it to be dismissed
                ActivityManager.getService().handleApplicationCrash(
                        mApplicationObject, new ApplicationErrorReport.ParcelableCrashInfo(e));  // 弹出进程dead的弹框
            } catch (Throwable t2) {
                if (t2 instanceof DeadObjectException) {
                    // System process is dead; ignore
                } else {
                    try {
                        Clog_e(TAG, "Error reporting crash", t2);
                    } catch (Throwable t3) {
                        // Even Clog_e() fails!  Oh well.
                    }
                }
            } finally {
                // Try everything to make sure this process goes away.
                Process.killProcess(Process.myPid());    // 重点 ： 10秒杀死进程
                System.exit(10);   
            }
        }
```
看到这里应该就明白为啥app中的crash机制了 那我们可以自定义异常处理器就可以让app不至于crash导致用户流失了，结合文章开始的分析我们现在通过两点来完成：
1. 异常抛出的底层方法由我们自己调用
2. 自定义异常处理类

首先解决第一点，我们可以自己去往主线程的`Looper`中添加一个死循环的任务，这样就会消息阻塞导致ANR,既然我们自定义的任务由于让`Looper`中的消息无法继续`for(;;)`，那可以在自己的任务中去调用`Looper.loop()`,这样相当于我们该任务是一个阻塞任务替换掉了ActivityThread中`Looper.loop()` 使得我们主线程的消息驱动时方法异常抛出时由我们的方法代理抛出，我们在该处加上`try{}catch{}`就能捕获到在消息驱动app过程中导致应用crash的异常，我们将导致应用crash的该异常处理掉就不会导致应用crash：

```
      new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                while (true) {  // 防止第二次抛出无法捕捉
                    try {
                        Looper.loop();
                    } catch (Throwable e) {
                        if (e instanceof CmCrashException) {  // unregister 时取消该套机制
                            return;
                        }
                        if (handler != null) {  // 交由我们自己处置
                            handler.handlerException(e);
                        }
                    }
                }
            }
        });
```
解决第二点通过自定义异常处理机制：

```
  mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler(); // 设置默认处理类 unregister时设置默认处理
  Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
    // 主线程的异常已经被我们try了，所以该处的异常都是子线程异常
        {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (handler != null) {
                    handler.handlerException(e); //交给我们自己处理
                }
            }
        });
```
通过以上分析很捕获到大部分因为代码的不健壮或者脏数据导致的crash的发生，但是对于Android而言，如果异常发生在Activity的生命周期调用时会导致界面黑屏或者界面白屏等现象，这时候我的解决办法就是去`finish`掉该`activity`，那如何对系统的activity生命周期调用时加try呢？通过反射出`ActivityThread`的`mH(handler)`，给该handler添加回调方法，因为在`ActivityThread`中该handler未实现`callback`,所有我们可以反射添加一个`callback`来我们处理关于`Activity`生命周期调用的方法：

```
      private static boolean reflectHandlerActivityLife() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null);
            Field mhField = activityThreadClass.getDeclaredField("mH");
            mhField.setAccessible(true);
            final Handler mh = (Handler) mhField.get(activityThread);
            final Field callbackField = Handler.class.getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            callbackField.set(mh, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case LAUNCH_ACTIVITY: {  // 由于该事件的msg与其他msg的内容不一致单独处理
                            try {
                                mh.handleMessage(msg);
                            } catch (Throwable e) {
                                mHandler.handlerException(e);
                                ActivityCloseManager.getInstance().finish(msg);
                            } finally {
                                return true;
                            }
                        }
                        case RESUME_ACTIVITY:
                        case PAUSE_ACTIVITY:
                        case STOP_ACTIVITY_HIDE:
                        case PAUSE_ACTIVITY_FINISHING:
                        case EXECUTE_TRANSACTION:
                        case NEW_INTENT:
                        case RELAUNCH_ACTIVITY28:
                        case RELAUNCH_ACTIVITY: {
                            try {
                                mh.handleMessage(msg);
                            } catch (Throwable e) {
                                mHandler.handlerException(e);
                                ActivityCloseManager.getInstance().finish(msg);
                            } finally {
                                return true;
                            }
                        }
                        case DESTROY_ACTIVITY: { // 界面已经销毁 无需再继续finish
                            try {
                                mh.handleMessage(msg);
                            } catch (Throwable e) {
                                mHandler.handlerException(e);
                            } finally {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;// 反射失败
        }
        return true;
    }
```
这样就可以实现在Ativity生命周期调用时异常导致界面黑白屏问题，另外由于android各个版本中activity的启动逻辑的变更，暂时先适配sdk15~28
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
