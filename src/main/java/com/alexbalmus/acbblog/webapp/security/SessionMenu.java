package com.alexbalmus.acbblog.webapp.security;

import jakarta.inject.Named;
import org.apache.causeway.applib.annotation.*;
import org.apache.causeway.applib.value.LocalResourcePath;
import org.apache.causeway.applib.value.OpenUrlStrategy;

@Named("acb.security.SessionMenu")
@DomainService
@DomainServiceLayout(menuBar = DomainServiceLayout.MenuBar.TERTIARY)
public class SessionMenu {
    /** Navigation only; Spring's confirmation form submits the CSRF-protected POST. */
    @Action(semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-sign-out-alt")
    public LocalResourcePath logout() {
        return new LocalResourcePath("/logout", OpenUrlStrategy.SAME_WINDOW);
    }
}
