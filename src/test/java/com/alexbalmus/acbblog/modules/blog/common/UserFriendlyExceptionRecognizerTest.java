package com.alexbalmus.acbblog.modules.blog.common;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.apache.causeway.applib.services.exceprecog.Recognition;

import static org.assertj.core.api.Assertions.assertThat;

class UserFriendlyExceptionRecognizerTest
{
    private final UserFriendlyExceptionRecognizer recognizer = new UserFriendlyExceptionRecognizer();

    @Test
    void showsMessageOfIllegalArgumentException()
    {
        Optional<Recognition> recognition =
            recognizer.recognize(new IllegalArgumentException("Handle 'x' is already associated with another user"));

        assertThat(recognition).isPresent();
        assertThat(recognition.orElseThrow().reason())
            .contains("Handle 'x' is already associated with another user");
    }

    @Test
    void showsMessageOfNestedIllegalArgumentException()
    {
        var nested = new RuntimeException(new IllegalArgumentException("business rule veto"));

        Optional<Recognition> recognition = recognizer.recognize(nested);

        assertThat(recognition).isPresent();
        assertThat(recognition.orElseThrow().reason()).contains("business rule veto");
    }

    @Test
    void unexpectedExceptionsMapToGenericMessage()
    {
        Optional<Recognition> recognition =
            recognizer.recognize(new IllegalStateException("SQLState 23505, table BLOG.POST, column ..."));

        assertThat(recognition).isPresent();
        assertThat(recognition.orElseThrow().reason())
            .contains("The requested operation could not be completed.")
            .doesNotContain("SQLState");
    }

    @Test
    void nullPointerExceptionWithoutMessageMapsToGenericMessage()
    {
        Optional<Recognition> recognition = recognizer.recognize(new NullPointerException());

        assertThat(recognition).isPresent();
        assertThat(recognition.orElseThrow().reason())
            .contains("The requested operation could not be completed.");
    }

    @Test
    void messageWhitespaceIsNormalized()
    {
        Optional<Recognition> recognition =
            recognizer.recognize(new IllegalArgumentException("too   many\n\nspaces\there"));

        assertThat(recognition).isPresent();
        assertThat(recognition.orElseThrow().reason()).contains("too many spaces here");
    }
}
