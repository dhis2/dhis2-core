package org.hisp.dhis.webapi.controller;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = FileController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class FileController
{
    public static final String RESOURCE_PATH = "/files";

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private WebMessageService webMessageService;

    // -------------------------------------------------------------------------
    // Custom script
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/script", method = RequestMethod.GET )
    public void getCustomScript( HttpServletResponse response, Writer writer )
        throws IOException
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JAVASCRIPT, CacheStrategy.CACHE_TWO_WEEKS );

        String content = (String) systemSettingManager.getSystemSetting( SettingKey.CUSTOM_JS, StringUtils.EMPTY );

        writer.write( content );
    }

    @RequestMapping( value = "/script", method = RequestMethod.POST, consumes = "application/javascript" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    public void postCustomScript( @RequestBody String content, HttpServletResponse response, HttpServletRequest request )
    {
        if ( content != null )
        {
            systemSettingManager.saveSystemSetting( SettingKey.CUSTOM_JS, content );
            webMessageService.send( WebMessageUtils.ok( "Custom script created" ), response, request );
        }
    }

    @RequestMapping( value = "/script", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeCustomScript( HttpServletResponse response )
    {
        systemSettingManager.deleteSystemSetting( SettingKey.CUSTOM_JS );
    }

    // -------------------------------------------------------------------------
    // Custom style
    // -------------------------------------------------------------------------

    /**
     * The style/external mapping enables style to be reached from login page / before authentication.
     */
    @RequestMapping( value = { "/style", "/style/external" }, method = RequestMethod.GET )
    public void getCustomStyle( HttpServletResponse response, Writer writer )
        throws IOException
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSS, CacheStrategy.CACHE_TWO_WEEKS );

        String content = (String) systemSettingManager.getSystemSetting( SettingKey.CUSTOM_CSS, StringUtils.EMPTY );

        writer.write( content );
    }

    @RequestMapping( value = "/style", method = RequestMethod.POST, consumes = "text/css" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    public void postCustomStyle( @RequestBody String content, HttpServletResponse response, HttpServletRequest request )
    {
        if ( content != null )
        {
            systemSettingManager.saveSystemSetting( SettingKey.CUSTOM_CSS, content );
            webMessageService.send( WebMessageUtils.ok( "Custom style created" ), response, request );
        }
    }

    @RequestMapping( value = "/style", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeCustomStyle( HttpServletResponse response )
    {
        systemSettingManager.deleteSystemSetting( SettingKey.CUSTOM_CSS );
    }
}
