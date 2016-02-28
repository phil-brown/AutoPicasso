package io.github.phdbrown.autopicasso.compiler;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;
import com.squareup.picasso.RequestCreator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.github.phdbrown.autopicasso.annotations.Picasso;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Annotation Processor for Picasso annotation
 */
@AutoService(Processor.class)
public class AutoPicassoProcessor extends AbstractProcessor {

    static final String IMAGE_VIEW_TYPE = "android.widget.ImageView";
    private static final String NULLABLE_ANNOTATION_NAME = "Nullable";
    private static final String BINDING_CLASS_SUFFIX = "$$AutoPicasso";

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        filer = env.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Picasso.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        Map<TypeElement, PicassoClass> targetClassMap = new LinkedHashMap<>();
        Set<String> erasedTargetNames = new LinkedHashSet<>();

        for (Element element : env.getElementsAnnotatedWith(Picasso.class)) {
            if (!SuperficialValidation.validateElement(element)) continue;
            try {
                parsePicasso(element, targetClassMap, erasedTargetNames);
            } catch (Exception e) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));
                error(element, "Unable to parse @Picasso binding.\n\n%s", stackTrace);
            }
        }

        for (Map.Entry<TypeElement, PicassoClass> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            PicassoClass picassoClass = entry.getValue();

            try {
                picassoClass.brewJava().writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Unable to write Auto Picasso for type %s: %s", typeElement, e.getMessage());
            }
        }

        return true;
    }

    private void parsePicasso(Element element, Map<TypeElement, PicassoClass> targetClassMap, Set<String> erasedTargetNames) {
        // Verify common generated code restrictions.
        if (isInaccessibleViaGeneratedCode(Picasso.class, "fields", element)
                || isBindingInWrongPackage(Picasso.class, element)) {
            return;
        }
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        //Verify that the target type extends from ImageView
        TypeMirror elementType = element.asType();
        if (elementType.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = (TypeVariable) elementType;
            elementType = typeVariable.getUpperBound();
        }
        if (!isSubtypeOfType(elementType, IMAGE_VIEW_TYPE) && !isInterface(elementType)) {
            error(element, "@Picasso fields must extend from ImageView or be an interface. (%s.%s)",
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            hasError = true;
        }

        // Assemble information on the field.
        Picasso picasso = element.getAnnotation(Picasso.class);
        int id = picasso.value();
        if (id == 0) {
            error(element, "@Picasso for a view must specify a valid ID. Found: 0. (%s.%s)",
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            hasError = true;
        }

        if (hasError) {
            return;
        }

        PicassoClass picassoClass = targetClassMap.get(enclosingElement);
        if (picassoClass != null) {
            if (picassoClass.containsId(id)) {
                error(element, "Attempt to use @Picasso for an already bound ID %d. (%s.%s)",
                        id, enclosingElement.getQualifiedName(), element.getSimpleName());
            }
        }
        else {
            picassoClass = getOrCreateTargetClass(targetClassMap, enclosingElement);
        }

        String name = element.getSimpleName().toString();
        TypeName type = TypeName.get(elementType);
        boolean required = isFieldRequired(element);

        picassoClass.addPicassoElement(id, new PicassoBinding(name, type, required, picasso));

        // Add the type-erased version to the valid binding targets set.
        erasedTargetNames.add(enclosingElement.toString());

    }

    private PicassoClass getOrCreateTargetClass(Map<TypeElement, PicassoClass> targetClassMap,
                                                TypeElement enclosingElement) {
        PicassoClass picassoClass = targetClassMap.get(enclosingElement);
        if (picassoClass == null) {
            //String targetType = enclosingElement.getQualifiedName().toString();
            String classPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, classPackage) + BINDING_CLASS_SUFFIX;

            picassoClass = new PicassoClass(classPackage, className);
            targetClassMap.put(enclosingElement, picassoClass);
        }
        return picassoClass;
    }

    //Below methods copied from Jake Wharton's ButterKnife
    //https://github.com/JakeWharton/butterknife

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }

    private boolean isInterface(TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType
                && ((DeclaredType) typeMirror).asElement().getKind() == INTERFACE;
    }

    private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotationWithName(Element element, String simpleName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if (simpleName.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFieldRequired(Element element) {
        return !hasAnnotationWithName(element, NULLABLE_ANNOTATION_NAME);
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }
}
