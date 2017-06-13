package org.hisp.dhis.webportal.module;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.dispatcher.Dispatcher;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppType;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.ActionAccessResolver;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.entities.PackageConfig;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: DefaultModuleManager.java 4883 2008-04-12 13:12:54Z larshelg $
 */
public class DefaultModuleManager
    implements ModuleManager
{
    private static final Log log = LogFactory.getLog( DefaultModuleManager.class );

    private boolean modulesDetected = false;

    private Map<String, Module> modulesByName = new HashMap<>();

    private Map<String, Module> modulesByNamespace = new HashMap<>();

    private List<Module> menuModules = new ArrayList<>();

    private ThreadLocal<Module> currentModule = new ThreadLocal<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private AppManager appManager;

    @Autowired
    private I18nManager i18nManager;

    private ActionAccessResolver actionAccessResolver;

    public void setActionAccessResolver( ActionAccessResolver actionAccessResolver )
    {
        this.actionAccessResolver = actionAccessResolver;
    }

    private Comparator<Module> moduleComparator;

    public void setModuleComparator( Comparator<Module> moduleComparator )
    {
        this.moduleComparator = moduleComparator;
    }

    private String defaultActionName;

    public void setDefaultActionName( String defaultActionName )
    {
        this.defaultActionName = defaultActionName;
    }

    private Set<String> menuModuleExclusions = new HashSet<>();

    public void setMenuModuleExclusions( Set<String> menuModuleExclusions )
    {
        this.menuModuleExclusions = menuModuleExclusions;
    }

    // -------------------------------------------------------------------------
    // ModuleManager
    // -------------------------------------------------------------------------

    @Override
    public Module getModuleByName( String name )
    {
        detectModules();

        return modulesByName.get( name );
    }

    @Override
    public Module getModuleByNamespace( String namespace )
    {
        detectModules();

        return modulesByNamespace.get( namespace );
    }

    @Override
    public boolean moduleExists( String name )
    {
        return getModuleByName( name ) != null;
    }

    @Override
    public List<Module> getMenuModules()
    {
        detectModules();

        return new ArrayList<>( menuModules );
    }

    @Override
    public List<Module> getAccessibleMenuModules()
    {
        detectModules();

        return getAccessibleModules( menuModules );
    }

    @Override
    public List<Module> getAccessibleMenuModulesAndApps( String contextPath )
    {
        List<Module> modules = getAccessibleMenuModules();
        List<App> apps = appManager
            .getAccessibleApps( contextPath )
            .stream()
            .filter( app -> app.getAppType() == AppType.APP )
            .collect( Collectors.toList() );

        modules.addAll( apps.stream().map( Module::getModule ).collect( Collectors.toList() ) );

        return modules;
    }

    @Override
    public Collection<Module> getAllModules()
    {
        detectModules();

        return new ArrayList<>( modulesByName.values() );
    }

    @Override
    public Module getCurrentModule()
    {
        return currentModule.get();
    }

    @Override
    public void setCurrentModule( Module module )
    {
        currentModule.set( module );
    }

    // -------------------------------------------------------------------------
    // Module detection
    // -------------------------------------------------------------------------

    private synchronized void detectModules()
    {
        if ( modulesDetected )
        {
            return;
        }

        I18n i18n = i18nManager.getI18n();

        for ( PackageConfig packageConfig : getPackageConfigs() )
        {
            String name = packageConfig.getName();
            String namespace = packageConfig.getNamespace();
            String displayName = i18n.getString( name );

            log.debug( "Package config: " + name + ", " + namespace );

            if ( packageConfig.getAllActionConfigs().size() == 0 )
            {
                log.debug( "Ignoring action package with no actions: " + name );

                continue;
            }

            if ( namespace == null || namespace.length() == 0 )
            {
                throw new RuntimeException( "Missing namespace in action package: " + name );
            }

            if ( modulesByName.containsKey( name ) )
            {
                throw new RuntimeException( "Two action packages have the same name: " + name );
            }

            if ( modulesByNamespace.containsKey( namespace ) )
            {
                Module module = modulesByNamespace.get( namespace );

                throw new RuntimeException( "These action packages have the same namespace: " +
                    name + " and " + module.getName() );
            }

            Module module = new Module( name, namespace );
            module.setDisplayName( displayName );
            modulesByName.put( name, module );
            modulesByNamespace.put( namespace, module );

            boolean include = !menuModuleExclusions.contains( name );

            if ( packageConfig.getActionConfigs().containsKey( defaultActionName ) && include )
            {
                module.setDefaultAction( ".." + namespace + "/" + defaultActionName + ".action" );

                menuModules.add( module );

                log.debug( "Has default action: " + name );
            }
            else
            {
                log.debug( "Doesn't have default action: " + name );
            }
        }

        Collections.sort( menuModules, moduleComparator );

        log.debug( "Menu modules detected: " + menuModules );

        modulesDetected = true;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Collection<PackageConfig> getPackageConfigs()
    {
        Configuration configuration = Dispatcher.getInstance().getConfigurationManager().getConfiguration();

        Map<String, PackageConfig> packageConfigs = configuration.getPackageConfigs();

        return packageConfigs.values();
    }

    private List<Module> getAccessibleModules( List<Module> modules )
    {
        List<Module> allowed = modules.stream()
            .filter( module -> module != null && actionAccessResolver.hasAccess( module.getName(), defaultActionName ) )
            .collect( Collectors.toList() );

        if ( modules.size() > allowed.size() )
        {
            List<Module> denied = new ArrayList<>( modules );
            denied.removeAll( allowed );

            log.debug( "User denied access to modules: " + denied );
        }

        return allowed;
    }
}
