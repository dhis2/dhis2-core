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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "system" )
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

    // -------------------------------------------------------------------------
    // Custom script
    // -------------------------------------------------------------------------

    @OpenApi.Response( Serializable.class )
    @GetMapping( "/script" )
    public void getCustomScript( HttpServletResponse response, Writer writer )
        throws IOException
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JAVASCRIPT, CacheStrategy.CACHE_TWO_WEEKS );

        String content = systemSettingManager.getSystemSetting( SettingKey.CUSTOM_JS, StringUtils.EMPTY );

        if ( content != null )
        {
            writer.write( content );
        }
    }

    @PostMapping( value = "/script", consumes = "application/javascript" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    @ResponseBody
    @ResponseStatus( HttpStatus.OK )
    public WebMessage postCustomScript( @RequestBody String content )
    {
        if ( content != null )
        {
            systemSettingManager.saveSystemSetting( SettingKey.CUSTOM_JS, content );
            return ok( "Custom script created" );
        }
        return null;
    }

    @DeleteMapping( "/script" )
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
     * The style/external mapping enables style to be reached from login page /
     * before authentication.
     */
    @OpenApi.Response( Serializable.class )
    @GetMapping( value = { "/style", "/style/external" } )
    public void getCustomStyle( HttpServletResponse response, Writer writer )
        throws IOException
    {
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSS, CacheStrategy.CACHE_TWO_WEEKS );

        String content = systemSettingManager.getSystemSetting( SettingKey.CUSTOM_CSS, StringUtils.EMPTY );

        if ( content != null )
        {
            writer.write( content );
        }
    }

    @PostMapping( value = "/style", consumes = "text/css" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    @ResponseBody
    @ResponseStatus( HttpStatus.OK )
    public WebMessage postCustomStyle( @RequestBody String content )
    {
        if ( content != null )
        {
            systemSettingManager.saveSystemSetting( SettingKey.CUSTOM_CSS, content );
            return ok( "Custom style created" );
        }
        return null;
    }

    @DeleteMapping( "/style" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_INSERT_CUSTOM_JS_CSS')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeCustomStyle( HttpServletResponse response )
    {
        systemSettingManager.deleteSystemSetting( SettingKey.CUSTOM_CSS );
    }
}
