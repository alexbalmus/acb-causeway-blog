package com.alexbalmus.acbblog.modules.blog.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.Parameter;
import org.apache.causeway.applib.annotation.ParameterLayout;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.annotation.PropertyLayout;
import org.apache.causeway.applib.annotation.Where;

@Property(editing = Editing.ENABLED, fileAccept = Picture.FILE_ACCEPT, optionality = Optionality.OPTIONAL)
@PropertyLayout(hidden = Where.ALL_TABLES, named = "Picture")
@Parameter(fileAccept = Picture.FILE_ACCEPT, optionality = Optionality.OPTIONAL)
@ParameterLayout(named = "Picture")
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Picture
{
    String FILE_ACCEPT = "image/*";
}
