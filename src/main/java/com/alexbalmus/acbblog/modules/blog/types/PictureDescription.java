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

@Property(editing = Editing.ENABLED, maxLength = PictureDescription.MAX_LEN, optionality = Optionality.OPTIONAL)
@PropertyLayout(hidden = Where.ALL_TABLES, multiLine = 3, named = "Description")
@Parameter(maxLength = PictureDescription.MAX_LEN, optionality = Optionality.OPTIONAL)
@ParameterLayout(multiLine = 3, named = "Picture description")
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PictureDescription
{
    int MAX_LEN = 500;
}
