package io.github.phdbrown.autopicasso.annotations;

import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Picasso Annotation
 * <br>
 * Copyright 2016 <a href="http://www.ovenbits.com">Oven Bits</a>
 *
 * @author Phil Brown
 * @since 11:19 AM Feb 03, 2016
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Picasso {

    /** Id of the ImageView */
    @IdRes int value();

    String url() default "";

    @DrawableRes int drawable() default 0;

    @DrawableRes int placeholder() default 0;

    @DrawableRes int error() default 0;

    String tag() default "";

    boolean fit() default false;

    int[] resize() default {};

    boolean centerCrop() default false;

    boolean centerInside() default false;

    boolean onlyScaleDown() default false;

    float[] rotate() default {};

    Bitmap.Config config() default Bitmap.Config.ARGB_8888;

    String stableKey() default "";

    com.squareup.picasso.Picasso.Priority priority() default com.squareup.picasso.Picasso.Priority.NORMAL;

    MemoryPolicy[] memoryPolicy() default {};

    NetworkPolicy[] networkPolicy() default {};

    boolean noFade() default false;

    boolean indicators() default false;

    boolean log() default false;
}
