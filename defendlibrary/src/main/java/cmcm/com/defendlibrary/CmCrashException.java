package cmcm.com.defendlibrary;

/**
 * Created by luweicheng on 2019/3/26.
 * custom exception handler
 */
public class CmCrashException extends RuntimeException {
    public CmCrashException(String msg) {
        super(msg);
    }

}
