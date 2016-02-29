package io.github.phdbrown.autopicasso.compiler;

import android.app.Activity;
import android.graphics.Bitmap;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Transformation;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.phdbrown.autopicasso.annotations.Picasso;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Auto-Generation Class
 * <br>
 *
 * @author Phil Brown
 * @since 1:10 PM Feb 03, 2016
 */
final class PicassoClass {

    private static final ClassName LOADER = ClassName.get("io.github.phdbrown.autopicasso.internal", "PicassoLoader");
    private static final ClassName IMAGE_VIEW = ClassName.get("android.widget", "ImageView");
    private static final ClassName PICASSO = ClassName.get("com.squareup.picasso", "Picasso");
    private static final ClassName TRANSFORMATION = ClassName.get("com.squareup.picasso", "Transformation");
    private static final ClassName REQUEST_CREATOR = ClassName.get("com.squareup.picasso", "RequestCreator");

    private static final String BITMAP_CONFIG = Bitmap.Config.class.getCanonicalName();
    private static final String MEMORY_POLICY = MemoryPolicy.class.getCanonicalName();
    private static final String PRIORITY = com.squareup.picasso.Picasso.Priority.class.getCanonicalName();
    private static final String NETWORK_POLICY = NetworkPolicy.class.getCanonicalName();

    private final Map<Integer, PicassoBinding> viewIdMap = new LinkedHashMap<>();

    private final String classPackage;
    private final String className;

    PicassoClass(String classPackage, String className) {
        this.classPackage = classPackage;
        this.className = className;
    }

    JavaFile brewJava() {
        TypeSpec.Builder result = TypeSpec.classBuilder(className)
                .addModifiers(PUBLIC)
                .addSuperinterface(LOADER);

        result.addMethod(createLoadMethod());

        return JavaFile.builder(classPackage, result.build())
                .addFileComment("Generated code from AutoPicasso. Do not modify!")
                .build();
    }

    private MethodSpec createLoadMethod() {
        MethodSpec.Builder result = MethodSpec.methodBuilder("load")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(Activity.class, "activity", FINAL)
                .addParameter(Transformation[].class, "transformations", FINAL);

        result.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "ResourceType")
                .build());

        if (!viewIdMap.isEmpty()) {
            // Local variables
            result.addStatement("$T view", IMAGE_VIEW);
            result.addStatement("$T picasso", PICASSO);

            for (Map.Entry<Integer, PicassoBinding> entry : viewIdMap.entrySet()) {
                addPicassoBinding(result, entry.getKey(), entry.getValue());
            }
        }

        return result.build();
    }

    public void addPicassoElement(int resId, PicassoBinding binding) {
        viewIdMap.put(resId, binding);
    }

    private void addPicassoBinding(MethodSpec.Builder result, int id, PicassoBinding binding) {
        result.addStatement("view = ($T) activity.findViewById($L)", IMAGE_VIEW, id);
        //bind to the Activity's view
        //result.addStatement("activity.$L = view;//$T.class.cast(view)", binding.getName(), binding.getType().toString());//TODO does this work?
        //Now add the Picasso Code
        result.addStatement("picasso = $T.with(activity)", PICASSO);
        String configuration = getConfigurationStatement(binding.getPicasso());
        if (configuration.length() > 0) {
            result.addStatement(configuration);
        }
        addLoader(result, binding.getPicasso());
        result.addStatement("for ($T t : transformations) { creator.transform(t); }", TRANSFORMATION);
        result.addStatement("creator.into(view)");
    }

    private String getConfigurationStatement(Picasso picasso) {
        StringBuilder builder = new StringBuilder();
        boolean hasEntry = false;
        if (picasso.log()) {
            appendMethod(builder, "setLoggingEnabled");
            hasEntry = true;
        }
        if (picasso.indicators()) {
            appendMethod(builder, "setIndicatorsEnabled");
            hasEntry = true;
        }
        if (hasEntry) {
            builder.insert(0, "picasso");
        }
        return builder.toString();
    }

    private String getLoadString(Picasso picasso) {
        StringBuilder builder = new StringBuilder("picasso");
        String url = picasso.url();
        int drawable = picasso.drawable();
        if (url != null && url.length() != 0) {
            appendMethod(builder, "load", "\"" + url + "\"");
        }
        else if (drawable != 0) {
            appendMethod(builder, "load", drawable);
        }
        return builder.toString();
    }

    private void addLoader(MethodSpec.Builder result, Picasso picasso) {
        result.addStatement("$T creator = $L", REQUEST_CREATOR, getLoadString(picasso));

        int placeholder = picasso.placeholder();
        if (placeholder != 0) {
            result.addStatement("creator.placeholder($L)", placeholder);
        }
        int error = picasso.error();
        if (error != 0) {
            result.addStatement("creator.error($L)", error);
        }
        String tag = picasso.tag();
        if (tag != null && tag.length() != 0) {
            result.addStatement("creator.tag($L)", tag);
        }
        boolean fit = picasso.fit();
        if (fit) {
            result.addStatement("creator.fit()");
        }
        int[] resize = picasso.resize();
        if (resize.length == 2) {
            result.addStatement("creator.resize($L,$L)", resize[0], resize[1]);
        }
        boolean centerCrop = picasso.centerCrop();
        if (centerCrop) {
            result.addStatement("creator.centerCrop()");
        }
        boolean centerInside = picasso.centerInside();
        if (centerInside) {
            result.addStatement("creator.centerInside()");
        }
        boolean onlyScaleDown = picasso.onlyScaleDown();
        if (onlyScaleDown) {
            result.addStatement("creator.onlyScaleDown()");
        }
        float[] rotate = picasso.rotate();
        if (rotate.length == 1) {
            result.addStatement("creator.rotate($L)", rotate[0]);
        }
        else if (rotate.length == 3) {
            result.addStatement("creator.rotate($L,$L,$L)", rotate[0], rotate[1], rotate[2]);
        }
        result.addStatement("creator.config($L.$L)", BITMAP_CONFIG, picasso.config());
        String stableKey = picasso.stableKey();
        if (stableKey != null && stableKey.length() != 0) {
            result.addStatement("creator.stableKey($L)", stableKey);
        }
        result.addStatement("creator.priority($L.$L)", PRIORITY, picasso.priority());

        MemoryPolicy[] memoryPolicy = picasso.memoryPolicy();
        if (memoryPolicy.length > 0) {
            result.addStatement("creator.memoryPolicy($L)", arrayToString(MEMORY_POLICY + ".", memoryPolicy));
        }
        NetworkPolicy[] networkPolicy = picasso.networkPolicy();
        if (networkPolicy.length > 0) {
            result.addStatement("creator.networkPolicy($L)", arrayToString(NETWORK_POLICY + ".", networkPolicy));
        }
        boolean noFade = picasso.noFade();
        if (noFade) {
            result.addStatement("creator.noFade()");
        }
    }

    private void appendMethod(StringBuilder builder, String name, Object... args) {
        builder.append(".").append(name).append("(");
        if (args.length > 0) {
            int i;
            for (i = 0; i < args.length - 1; i++) {
                builder.append(args[i]);
                builder.append(",");
            }
            builder.append(args[i]);
        }

        builder.append(")");
    }

    private String arrayToString(String prefix, Object[] array) {
        StringBuilder builder = new StringBuilder();
        if (array.length > 0) {
            int i;
            for (i = 0; i < array.length - 1; i++) {
                builder.append(prefix).append(array[i]);
                builder.append(",");
            }
            builder.append(prefix).append(array[i]);
        }
        return builder.toString();
    }

    public boolean containsId(int id) {
        return viewIdMap.containsKey(id);
    }
}
