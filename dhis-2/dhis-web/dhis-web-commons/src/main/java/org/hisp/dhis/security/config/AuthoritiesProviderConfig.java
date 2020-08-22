package org.hisp.dhis.security.config;

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.authority.AppsSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.CachingSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.CompositeSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.DefaultRequiredAuthoritiesProvider;
import org.hisp.dhis.security.authority.DetectingSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.ModuleSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.RequiredAuthoritiesProvider;
import org.hisp.dhis.security.authority.SchemaAuthoritiesProvider;
import org.hisp.dhis.security.authority.SimpleSystemAuthoritiesProvider;
import org.hisp.dhis.security.authority.SystemAuthoritiesProvider;
import org.hisp.dhis.startup.DefaultAdminUserPopulator;
import org.hisp.dhis.webportal.module.ModuleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
public class AuthoritiesProviderConfig
{
    @Autowired
    private ModuleManager moduleManager;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private AppManager appManager;

    @Bean( "org.hisp.dhis.security.authority.SystemAuthoritiesProvider" )
    public SystemAuthoritiesProvider systemAuthoritiesProvider()
    {
        SchemaAuthoritiesProvider schemaAuthoritiesProvider = new SchemaAuthoritiesProvider( schemaService );
        AppsSystemAuthoritiesProvider appsSystemAuthoritiesProvider = new AppsSystemAuthoritiesProvider( appManager );

        DetectingSystemAuthoritiesProvider detectingSystemAuthoritiesProvider = new DetectingSystemAuthoritiesProvider();
        detectingSystemAuthoritiesProvider.setRequiredAuthoritiesProvider( requiredAuthoritiesProvider() );

        CompositeSystemAuthoritiesProvider provider = new CompositeSystemAuthoritiesProvider();
        provider.setSources( ImmutableSet.of(
            new CachingSystemAuthoritiesProvider( detectingSystemAuthoritiesProvider ),
            new CachingSystemAuthoritiesProvider( moduleSystemAuthoritiesProvider() ),
            new CachingSystemAuthoritiesProvider( simpleSystemAuthoritiesProvider() ),
            schemaAuthoritiesProvider,
            appsSystemAuthoritiesProvider
        ) );
        return provider;
    }

    private SystemAuthoritiesProvider simpleSystemAuthoritiesProvider()
    {
        SimpleSystemAuthoritiesProvider provider = new SimpleSystemAuthoritiesProvider();
        provider.setAuthorities( DefaultAdminUserPopulator.ALL_AUTHORITIES );
        return provider;
    }

    private RequiredAuthoritiesProvider requiredAuthoritiesProvider()
    {
        DefaultRequiredAuthoritiesProvider provider = new DefaultRequiredAuthoritiesProvider();
        provider.setRequiredAuthoritiesKey( "requiredAuthorities" );
        provider.setAnyAuthoritiesKey( "anyAuthorities" );
        provider.setGlobalAttributes( ImmutableSet.of( "M_MODULE_ACCESS_VOTER_ENABLED" ) );
        return provider;
    }

    private ModuleSystemAuthoritiesProvider moduleSystemAuthoritiesProvider()
    {
        ModuleSystemAuthoritiesProvider provider = new ModuleSystemAuthoritiesProvider();
        provider.setAuthorityPrefix( "M_" );
        provider.setModuleManager( moduleManager );
        provider.setExcludes( ImmutableSet.of(
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
            "dhis-web-portal"
        ) );
        return provider;
    }
}
