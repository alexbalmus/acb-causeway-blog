package com.alexbalmus.acbblog.modules.blog.common;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.inject.Named;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.services.exceprecog.Category;
import org.apache.causeway.applib.services.exceprecog.ExceptionRecognizer;
import org.apache.causeway.applib.services.exceprecog.Recognition;

@Service
@Named("blog.UserFriendlyExceptionRecognizer")
@Priority(PriorityPrecedence.FIRST)
public class UserFriendlyExceptionRecognizer implements ExceptionRecognizer
{
    private static final String FALLBACK_MESSAGE = "The requested operation could not be completed.";

    @Override
    public Optional<Recognition> recognize(final Throwable ex)
    {
        return Recognition.of(Category.RECOVERABLE, userMessage(ex));
    }

    private String userMessage(final Throwable ex)
    {
        String message = deepestNonBlankMessage(ex);
        return Strings.isBlank(message) ? FALLBACK_MESSAGE : sanitize(message);
    }

    private String deepestNonBlankMessage(final Throwable ex)
    {
        String message = null;
        Throwable current = ex;
        while (current != null)
        {
            if (!Strings.isBlank(current.getMessage()))
            {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return message;
    }

    private String sanitize(final String message)
    {
        return message
            .replaceAll("[\\p{C}&&[^\\r\\n\\t]]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
