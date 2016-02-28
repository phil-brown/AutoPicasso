package io.github.phdbrown.autopicasso.compiler;

import com.squareup.javapoet.TypeName;

import io.github.phdbrown.autopicasso.annotations.Picasso;

/**
 * Used to insert Picasso Request Creators into the Generated PicassoLoader class.
 * <br>
 * Copyright 2016 <a href="http://www.ovenbits.com">Oven Bits</a>
 *
 * @author Phil Brown
 * @since 11:00 AM Feb 04, 2016
 */
final class PicassoBinding {
    private final Picasso mPicasso;

    private final String name;
    private final TypeName type;
    private final boolean required;

    PicassoBinding(String name, TypeName type, boolean required, Picasso picasso) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.mPicasso = picasso;
    }

    public String getName() {
        return name;
    }

    public TypeName getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public Picasso getPicasso() {
        return mPicasso;
    }
}
