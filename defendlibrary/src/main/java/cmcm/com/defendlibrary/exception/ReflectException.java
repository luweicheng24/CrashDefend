package cmcm.com.defendlibrary.exception;

/**
 * Created by luweicheng on 2019/3/29.
 */
public class ReflectException extends RuntimeException {

    public ReflectException(String message) {
        super(message);
    }

    public ReflectException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
