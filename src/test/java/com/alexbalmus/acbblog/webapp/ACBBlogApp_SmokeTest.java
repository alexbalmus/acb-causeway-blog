package com.alexbalmus.acbblog.webapp;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the complete application context. Since the app runs with
 * {@code causeway.core.meta-model.introspector.mode: full} and
 * {@code lock-after-full-introspection: true}, a successful start also
 * implies that the Causeway metamodel is valid.
 */
@SpringBootTest(classes = ACBBlogApp.class)
class ACBBlogApp_SmokeTest
{
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextBootsAndMetamodelIsValid()
    {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBean(AppManifest.class)).isNotNull();
    }
}
