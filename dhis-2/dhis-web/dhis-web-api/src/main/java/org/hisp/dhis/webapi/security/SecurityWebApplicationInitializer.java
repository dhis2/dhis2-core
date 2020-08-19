package org.hisp.dhis.webapi.security;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer
{
    // This is needed for triggering the setup of the SpringSecurityFilterChain and ContextLoaderListener
}
