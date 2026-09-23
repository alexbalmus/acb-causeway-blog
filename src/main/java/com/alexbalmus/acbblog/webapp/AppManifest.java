package com.alexbalmus.acbblog.webapp;

import com.alexbalmus.acbblog.modules.blog.BlogModule;

import org.apache.causeway.applib.CausewayModuleApplibChangeAndExecutionLoggers;
import org.apache.causeway.applib.CausewayModuleApplibMixins;
import org.apache.causeway.core.config.presets.CausewayPresets;
import org.apache.causeway.core.runtimeservices.CausewayModuleCoreRuntimeServices;
import org.apache.causeway.persistence.jpa.eclipselink.CausewayModulePersistenceJpaEclipselink;
import org.apache.causeway.viewer.graphql.viewer.CausewayModuleViewerGraphqlViewer;
import org.apache.causeway.viewer.restfulobjects.viewer.CausewayModuleViewerRestfulObjectsViewer;
import org.apache.causeway.viewer.wicket.applib.CausewayModuleViewerWicketApplibMixins;
import org.apache.causeway.viewer.wicket.viewer.CausewayModuleViewerWicketViewer;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


@Configuration
@Import({
    CausewayModuleApplibMixins.class,
    CausewayModuleApplibChangeAndExecutionLoggers.class,

    CausewayModuleCoreRuntimeServices.class,
    com.alexbalmus.acbblog.webapp.security.SecurityConfiguration.class,
    CausewayModulePersistenceJpaEclipselink.class,
    CausewayModuleViewerRestfulObjectsViewer.class,
    CausewayModuleViewerGraphqlViewer.class,
    CausewayModuleViewerWicketApplibMixins.class,
    CausewayModuleViewerWicketViewer.class,

    BlogModule.class
})
@PropertySources({
    @PropertySource(CausewayPresets.NoTranslations),
})
public class AppManifest
{
}
