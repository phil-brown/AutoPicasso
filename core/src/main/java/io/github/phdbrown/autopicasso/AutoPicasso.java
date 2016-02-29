package io.github.phdbrown.autopicasso;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;

import com.squareup.picasso.Transformation;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.phdbrown.autopicasso.internal.PicassoLoader;

/**
 * Loader. This is used to run the generated code from the Activity.
 * <br>
 *
 * @author Phil Brown
 * @since 8:10 AM Feb 04, 2016
 */
public final class AutoPicasso {

    static final Map<Class<?>, PicassoLoader> LOADERS = new LinkedHashMap<>();
    static final PicassoLoader NOP_LOADER = new PicassoLoader() {
        @Override
        public void load(Activity activity, Transformation[] transformations) {
        }
    };

    private static final String TAG = "AutoPicasso";
    private static boolean debug = false;

    /** Control whether debug logging is enabled. */
    public static void setDebug(boolean debug) {
        AutoPicasso.debug = debug;
    }

    public static void load(Activity activity, Transformation... transformations) {
        Class<?> targetClass = activity.getClass();
        try {
            if (debug) Log.d(TAG, "Looking up view binder for " + targetClass.getName());
            PicassoLoader loader = findViewBinderForClass(targetClass);
            loader.load(activity, transformations);
        } catch (Exception e) {
            throw new RuntimeException("Unable to bind views for " + targetClass.getName(), e);
        }
    }

    @NonNull
    private static PicassoLoader findViewBinderForClass(Class<?> cls)
            throws IllegalAccessException, InstantiationException {
        PicassoLoader picassoLoader = LOADERS.get(cls);
        if (picassoLoader != null) {
            if (debug) Log.d(TAG, "HIT: Cached in view binder map.");
            return picassoLoader;
        }
        String clsName = cls.getName();
        if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
            if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return NOP_LOADER;
        }
        try {
            Class<?> viewBindingClass = Class.forName(clsName + "$$AutoPicasso");
            //noinspection unchecked
            picassoLoader = (PicassoLoader) viewBindingClass.newInstance();
            if (debug) Log.d(TAG, "HIT: Loaded view binder class.");
        } catch (ClassNotFoundException e) {
            if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            picassoLoader = findViewBinderForClass(cls.getSuperclass());
        }
        LOADERS.put(cls, picassoLoader);
        return picassoLoader;
    }

}
