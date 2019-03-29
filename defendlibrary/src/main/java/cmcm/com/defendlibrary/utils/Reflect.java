package cmcm.com.defendlibrary.utils;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cmcm.com.defendlibrary.exception.ReflectException;

/**
 * Created by luweicheng on 2019/3/29.
 */
public class Reflect {
    private Class<?> mReflectClass;

    private Reflect(String className) {
        try {
            mReflectClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ClassCastException("reflect classname " + className + "not found");
        }
    }

    private Reflect(Class clazz) {
        mReflectClass = clazz;
    }

    public static Reflect on(String className) {
        checkEmpty(className, "class name is empty");
        return new Reflect(className);
    }

    public static Reflect on(Class clazz) {
        checkNull(clazz, "reflect class is null");
        return new Reflect(clazz);
    }

    public final ReflectField field(String fieldName) {
        checkEmpty(fieldName, "reflect field name is empty");
        if (mReflectClass == null) throw new ReflectException("reflect class object is null");
        Field field;
        try {
            field = mReflectClass.getDeclaredField(fieldName);
            return ReflectField.create(field);
        } catch (NoSuchFieldException e) {
            throw new ReflectException("reflect not found field " + e.getMessage());
        }
    }

    public final ReflectMethod method(String methodName, Class<?>... params) {
        checkEmpty(methodName, "method name is empty");
        if (mReflectClass == null) throw new ReflectException("reflect class object is null");
        Method method;
        try {
            if (params.length == 0) {
                method = mReflectClass.getDeclaredMethod(methodName);
            } else {
                method = mReflectClass.getDeclaredMethod(methodName, params);
            }
            method.setAccessible(true);
            return ReflectMethod.create(method);
        } catch (NoSuchMethodException e) {
            throw new ReflectException("method name " + methodName + " not found");
        }
    }

    public static final class ReflectField {
        private Field field;

        private ReflectField(Field field) {
            this.field = field;
            this.field.setAccessible(true);
        }

        public static ReflectField create(Field field) {
            return new ReflectField(field);
        }

        public Object get(Object object) {
            checkNull(object, "field get error object is null");
            try {
                return this.field.get(object);
            } catch (IllegalAccessException e) {
                throw new ReflectException("field reflect get error ");
            }
        }

        public void set(Object object, Object value) {
            checkNull(object, "field set object is null ");
            checkNull(value, "field set value is null ");
            try {
                this.field.set(object, value);
            } catch (IllegalAccessException e) {
                throw new ReflectException("field set error " + e.getMessage());
            }
        }
    }

    public static final class ReflectMethod {
        private Method method;

        private ReflectMethod(Method method) {
            this.method = method;
            this.method.setAccessible(true);
        }

        static ReflectMethod create(Method method) {
            return new ReflectMethod(method);
        }

        public Object invoke(Object invoker, Object... params) {
            Object obj;
            try {
                if (params.length == 0) {
                    obj = this.method.invoke(invoker);
                } else {
                    obj = this.method.invoke(invoker, params);
                }
                return obj;
            } catch (Exception e) {
                throw new ReflectException("reflect method " + method.getName() + "invoke error");
            }
        }
    }

    private static void checkNull(Object obj, String msg) {
        if (obj == null) throw new ReflectException(msg);
    }

    private static void checkEmpty(String str, String msg) {
        if (TextUtils.isEmpty(str)) throw new ReflectException(msg);
    }

}
