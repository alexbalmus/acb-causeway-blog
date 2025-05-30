package com.alexbalmus.acbblog.webapp;

import org.apache.causeway.core.config.presets.CausewayPresets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
    AppManifest.class,
})
public class ACBBlogApp extends SpringBootServletInitializer
{
    public static void main(String[] args)
    {
        CausewayPresets.prototyping(); // or run with use -DPROTOTYPING=true
        SpringApplication.run(new Class[] { ACBBlogApp.class }, args);
    }
}
