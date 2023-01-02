/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppType;
import org.hisp.dhis.appmanager.WebModuleManager;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */

@OpenApi.Tags( "webmodules" )
@RestController
@AllArgsConstructor
@RequestMapping( "/webModules" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@Slf4j
public class WebModulesController
{
    private final AppManager appManager;

    private final WebModuleManager webModuleManager;

    private final UserService userService;

    @GetMapping( value = "", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody Map<String, List<WebModule>> getWebModules( @RequestParam String username,
        HttpServletRequest request )
    {
        String contextPath = ContextUtils.getContextPath( request );

        return Map.of( "modules", getAccessibleMenuModulesAndApps( contextPath, username ) );
    }

    public List<WebModule> getAccessibleMenuModulesAndApps( String contextPath, String username )
    {
        List<WebModule> modules = webModuleManager.getWebModules( username );

        User user = userService.getUserByUsername( username );

        List<App> apps = appManager
            .getAccessibleApps( contextPath, user )
            .stream()
            .filter( app -> app.getAppType() == AppType.APP && !app.isBundled() )
            .collect( Collectors.toList() );

        modules.addAll( apps.stream().map( WebModule::getModule ).collect( Collectors.toList() ) );

        return modules;
    }
}
