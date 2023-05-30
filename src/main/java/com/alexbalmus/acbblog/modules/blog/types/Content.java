package com.alexbalmus.acbblog.modules.blog.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.causeway.applib.annotation.*;

@Property(editing = Editing.ENABLED, maxLength = Content.MAX_LEN, optionality = Optionality.OPTIONAL)
@PropertyLayout(multiLine = 10, hidden = Where.ALL_TABLES)
@Parameter(maxLength = Content.MAX_LEN, optionality = Optionality.OPTIONAL)
@ParameterLayout(multiLine = 10)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Content
{
    int MAX_LEN = 4000;
}
