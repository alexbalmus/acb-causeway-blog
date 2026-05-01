package com.alexbalmus.acbblog.modules.blog.common;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextSanitizer
{
    public String sanitize(String text)
    {
        if (Strings.isBlank(text))
        {
            return "";
        }

        String regex = "[\\p{C}]"; // control characters

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text.trim());

        return matcher.replaceAll("");
    }
}
