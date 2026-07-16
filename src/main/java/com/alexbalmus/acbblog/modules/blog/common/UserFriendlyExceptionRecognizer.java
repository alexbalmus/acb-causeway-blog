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

/**
 * Last-resort recognizer that prevents raw stacktraces from reaching the UI.
 * <p>
 * Runs at {@link PriorityPrecedence#LAST} so that the framework's own recognizers
 * (constraint violations, optimistic locking, ...) take precedence. Only messages of
 * deliberately thrown {@link IllegalArgumentException}s (business rule vetoes) are
 * shown to the user; any other exception maps to a generic message so that internal
 * details (SQL, class names, ...) are never leaked.
 */
@Service
@Named("blog.UserFriendlyExceptionRecognizer")
@Priority(PriorityPrecedence.LAST)
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
        String message = deepestBusinessRuleMessage(ex);
        return Strings.isBlank(message) ? FALLBACK_MESSAGE : sanitize(message);
    }

    private String deepestBusinessRuleMessage(final Throwable ex)
    {
        String message = null;
        Throwable current = ex;
        while (current != null)
        {
            if (current instanceof IllegalArgumentException && !Strings.isBlank(current.getMessage()))
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
