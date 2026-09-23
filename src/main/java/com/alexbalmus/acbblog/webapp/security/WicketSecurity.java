package com.alexbalmus.acbblog.webapp.security;

import org.apache.causeway.viewer.wicket.model.causeway.WicketApplicationInitializer;
import org.apache.wicket.protocol.http.ResourceIsolationRequestCycleListener;
import org.apache.wicket.protocol.http.WebApplication;
import org.springframework.stereotype.Component;

@Component
public class WicketSecurity implements WicketApplicationInitializer {
    @Override public void init(WebApplication application) {
        application.getRequestCycleListeners().add(new ResourceIsolationRequestCycleListener());
    }
}
