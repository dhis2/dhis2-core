/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security.config;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.oust.manager.DefaultSelectionTreeManager;
import org.hisp.dhis.ouwt.manager.DefaultOrganisationUnitSelectionManager;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.SpringSecurityActionAccessResolver;
import org.hisp.dhis.security.SystemAuthoritiesProvider;
import org.hisp.dhis.security.action.RestrictOrganisationUnitsAction;
import org.hisp.dhis.security.authority.AppsSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.CachingSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.CompositeSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.DefaultRequiredAuthoritiesProvider;
import org.hisp.dhis.security.authority.DetectingSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.ModuleSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.RequiredAuthoritiesProvider;
import org.hisp.dhis.security.authority.SchemaAuthoritiesProvider;
import org.hisp.dhis.security.authority.SimpleSystemAuthoritiesProvider;
import org.hisp.dhis.security.intercept.LoginInterceptor;
import org.hisp.dhis.security.intercept.XWorkSecurityInterceptor;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.startup.DefaultAdminUserPopulator;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webportal.module.ModuleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDecisionManager;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 3200 )
public class AuthoritiesProviderConfig
{

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ModuleManager moduleManager;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private AppManager appManager;

    @Autowired
    @Qualifier( "org.hisp.dhis.user.CurrentUserService" )
    public CurrentUserService currentUserService;

    @Autowired
    @Qualifier( "accessDecisionManager" )
    public AccessDecisionManager accessDecisionManager;

    @Autowired
    public TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

    @Autowired
    @Qualifier( "org.hisp.dhis.organisationunit.OrganisationUnitService" )
    public OrganisationUnitService organisationUnitService;

    @Primary
    @Bean( "org.hisp.dhis.security.SystemAuthoritiesProvider" )
    public SystemAuthoritiesProvider systemAuthoritiesProvider()
    {
        SchemaAuthoritiesProvider schemaAuthoritiesProvider = new SchemaAuthoritiesProvider( schemaService );
        AppsSystemAuthoritiesProvider appsSystemAuthoritiesProvider = new AppsSystemAuthoritiesProvider( appManager );

        DetectingSystemAuthoritiesProvider detectingSystemAuthoritiesProvider = new DetectingSystemAuthoritiesProvider();
        detectingSystemAuthoritiesProvider.setRequiredAuthoritiesProvider( requiredAuthoritiesProvider() );

        CompositeSystemAuthoritiesProvider provider = new CompositeSystemAuthoritiesProvider();
        provider.setSources( Set.of(
            new CachingSystemAuthoritiesProvider( detectingSystemAuthoritiesProvider ),
            new CachingSystemAuthoritiesProvider( moduleSystemAuthoritiesProvider() ),
            new CachingSystemAuthoritiesProvider( simpleSystemAuthoritiesProvider() ),
            schemaAuthoritiesProvider,
            appsSystemAuthoritiesProvider ) );
        return provider;
    }

    private SystemAuthoritiesProvider simpleSystemAuthoritiesProvider()
    {
        SimpleSystemAuthoritiesProvider provider = new SimpleSystemAuthoritiesProvider();
        provider.setAuthorities( DefaultAdminUserPopulator.ALL_AUTHORITIES );
        return provider;
    }

    @Bean
    public RequiredAuthoritiesProvider requiredAuthoritiesProvider()
    {
        DefaultRequiredAuthoritiesProvider provider = new DefaultRequiredAuthoritiesProvider();
        provider.setRequiredAuthoritiesKey( "requiredAuthorities" );
        provider.setAnyAuthoritiesKey( "anyAuthorities" );
        provider.setGlobalAttributes( Set.of( "M_MODULE_ACCESS_VOTER_ENABLED" ) );
        return provider;
    }

    private ModuleSystemAuthoritiesProvider moduleSystemAuthoritiesProvider()
    {
        ModuleSystemAuthoritiesProvider provider = new ModuleSystemAuthoritiesProvider();
        provider.setAuthorityPrefix( "M_" );
        provider.setModuleManager( moduleManager );
        provider.setExcludes( Set.of(
            "dhis-web-commons-menu",
            "dhis-web-commons-menu-management",
            "dhis-web-commons-oust",
            "dhis-web-commons-ouwt",
            "dhis-web-commons-security",
            "dhis-web-commons-i18n",
            "dhis-web-commons-ajax",
            "dhis-web-commons-ajax-json",
            "dhis-web-commons-ajax-html",
            "dhis-web-commons-stream",
            "dhis-web-commons-help",
            "dhis-web-commons-about",
            "dhis-web-apps",
            "dhis-web-api-mobile",
            "dhis-web-portal" ) );
        return provider;
    }

    @Bean( "org.hisp.dhis.security.intercept.XWorkSecurityInterceptor" )
    public XWorkSecurityInterceptor xWorkSecurityInterceptor()
        throws Exception
    {
        DefaultRequiredAuthoritiesProvider provider = new DefaultRequiredAuthoritiesProvider();
        provider.setRequiredAuthoritiesKey( "requiredAuthorities" );
        provider.setAnyAuthoritiesKey( "anyAuthorities" );
        provider.setGlobalAttributes( Set.of( "M_MODULE_ACCESS_VOTER_ENABLED" ) );

        SpringSecurityActionAccessResolver resolver = new SpringSecurityActionAccessResolver();
        resolver.setRequiredAuthoritiesProvider( provider );
        resolver.setAccessDecisionManager( accessDecisionManager );

        XWorkSecurityInterceptor interceptor = new XWorkSecurityInterceptor();
        interceptor.setAccessDecisionManager( accessDecisionManager );
        interceptor.setValidateConfigAttributes( false );
        interceptor.setRequiredAuthoritiesProvider( provider );
        interceptor.setActionAccessResolver( resolver );
        interceptor.setSecurityService( securityService );

        return interceptor;
    }

    @Bean( "org.hisp.dhis.security.intercept.LoginInterceptor" )
    public LoginInterceptor loginInterceptor()
    {
        RestrictOrganisationUnitsAction unitsAction = new RestrictOrganisationUnitsAction();
        unitsAction.setCurrentUserService( currentUserService );
        DefaultOrganisationUnitSelectionManager selectionManager = new DefaultOrganisationUnitSelectionManager();
        selectionManager.setOrganisationUnitService( organisationUnitService );
        unitsAction.setSelectionManager( selectionManager );
        DefaultSelectionTreeManager selectionTreeManager = new DefaultSelectionTreeManager();
        selectionTreeManager.setOrganisationUnitService( organisationUnitService );
        unitsAction.setSelectionTreeManager( selectionTreeManager );

        LoginInterceptor interceptor = new LoginInterceptor();
        interceptor.setActions( List.of( unitsAction ) );

        return interceptor;
    }
}
