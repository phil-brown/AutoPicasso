package io.github.phdbrown.autopicasso.compiler;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import javax.tools.JavaFileObject;

import io.github.phdbrown.autopicasso.compiler.AutoPicassoProcessor;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

/**
 * TODO Description
 * <br>
 *
 * @author Phil Brown
 * @since 3:22 PM Feb 04, 2016
 */
public class PicassoTest {

    @Test public void compilerTest() {

        JavaFileObject source = JavaFileObjects.forSourceString("test.Test",
                Joiner.on('\n').join(
                        "package test;",
                        "import android.app.Activity;",
                        "import android.widget.ImageView;",
                        "import io.github.phdbrown.autopicasso.annotations.Picasso;",
                        "public class Test extends Activity {",
                        "    @Picasso(value = 1, url = \"http://square.github.io/picasso/static/sample.png\") ImageView thing;",
                        "}"
                ));

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/Test$$AutoPicasso",
                Joiner.on('\n').join(
                        "// Generated code from AutoPicasso. Do not modify!",
                        "package test;",
                        "",
                        "import android.app.Activity;",
                        "import android.widget.ImageView;",
                        "import com.squareup.picasso.Picasso",
                        "import com.squareup.picasso.RequestCreator",
                        "import com.squareup.picasso.Transformation",
                        "import io.github.phdbrown.autopicasso.internal.PicassoLoader",
                        "import java.lang.Override;",
                        "import java.lang.SuppressWarnings;",
                        "",
                        "public class Test$$AutoPicasso implements PicassoLoader {",
                        "  @Override",
                        "  @SuppressWarnings(\"ResourceType\")\"",
                        "  public void load(final Activity activity, final Transformation[] transformations) {",
                        "    ImageView view;",
                        "    Picasso picasso;",
                        "    view = (ImageView) activity.findViewById(1);",
                        "    picasso = Picasso.with(activity);",
                        "    RequestCreator creator = picasso.load(\"http://square.github.io/picasso/static/sample.png\");",
                        "    creator.config(android.graphics.Bitmap.Config.ARGB_8888);",
                        "    creator.priority(com.squareup.picasso.Picasso.Priority.NORMAL);",
                        "    for (Transformation t : transformations) { creator.transform(t); };",
                        "    creator.into(view);",
                        "  }",
                        "}"
                ));

        assertAbout(javaSource()).that(source).processedWith(new AutoPicassoProcessor()).compilesWithoutError();//FIXME  .and().generatesSources(expectedSource);
    }

}
