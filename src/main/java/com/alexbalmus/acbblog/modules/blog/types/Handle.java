package com.alexbalmus.acbblog.modules.blog.types;

import org.apache.causeway.applib.annotation.Parameter;
import org.apache.causeway.applib.annotation.ParameterLayout;
import org.apache.causeway.applib.annotation.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Property(maxLength = Handle.MAX_LEN)
@Parameter(maxLength = Handle.MAX_LEN)
@ParameterLayout(named = "Handle")
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Handle
{
    int MAX_LEN = 40;
}
