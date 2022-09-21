/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author Lars Helge Overland
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Controller
@RequestMapping( "/systemSettings" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SystemSettingController
{
    private final SystemSettingManager systemSettingManager;
<<<<<<< HEAD
    private final RenderService renderService;
    private final WebMessageService webMessageService;
    private final CurrentUserService currentUserService;
    private final UserSettingService userSettingService;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

<<<<<<< HEAD
    public SystemSettingController( SystemSettingManager systemSettingManager, RenderService renderService,
        WebMessageService webMessageService, CurrentUserService currentUserService,
        UserSettingService userSettingService )
    {
        checkNotNull( systemSettingManager );
        checkNotNull( renderService );
        checkNotNull( webMessageService );
        checkNotNull( currentUserService );
        checkNotNull( userSettingService );
=======
    private final RenderService renderService;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

<<<<<<< HEAD
        this.systemSettingManager = systemSettingManager;
        this.renderService = renderService;
        this.webMessageService = webMessageService;
        this.currentUserService = currentUserService;
        this.userSettingService = userSettingService;
    }
=======
    private final WebMessageService webMessageService;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

<<<<<<< HEAD
=======
    private final CurrentUserService currentUserService;

    private final UserSettingService userSettingService;

    public SystemSettingController( SystemSettingManager systemSettingManager, RenderService renderService,
        WebMessageService webMessageService, CurrentUserService currentUserService,
        UserSettingService userSettingService )
    {
        checkNotNull( systemSettingManager );
        checkNotNull( renderService );
        checkNotNull( webMessageService );
        checkNotNull( currentUserService );
        checkNotNull( userSettingService );

        this.systemSettingManager = systemSettingManager;
        this.renderService = renderService;
        this.webMessageService = webMessageService;
        this.currentUserService = currentUserService;
        this.userSettingService = userSettingService;
    }

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    @RequestMapping( value = "/{key}", method = RequestMethod.POST, consumes = { ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_TEXT, ContextUtils.CONTENT_TYPE_HTML } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    public void setSystemSettingOrTranslation( @PathVariable( value = "key" ) String key,
        @RequestParam( value = "locale", required = false ) String locale,
        @RequestParam( value = "value", required = false ) String value,
        @RequestBody( required = false ) String valuePayload, HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        validateParameters( key, value, valuePayload );

        Optional<SettingKey> setting = SettingKey.getByName( key );

        if ( !setting.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key is not supported: " + key ) );
        }

        value = ObjectUtils.firstNonNull( value, valuePayload );

        if ( StringUtils.isEmpty( locale ) )
        {
            saveSystemSetting( key, value, setting.get(), response, request );
        }
        else
        {
            saveSystemSettingTranslation( key, locale, value, setting.get(), response, request );
        }
    }

    private void saveSystemSetting( String key, String value, SettingKey setting,
        HttpServletResponse response, HttpServletRequest request )
    {
        Serializable valueObject = SettingKey.getAsRealClass( key, value );

        systemSettingManager.saveSystemSetting( setting, valueObject );

<<<<<<< HEAD
        webMessageService.send( WebMessageUtils.ok( "System setting '" + key + "' set to value '" + valueObject + "'." ), response, request );
    }

    private void saveSystemSettingTranslation( String key, String locale, String value, SettingKey setting,
        HttpServletResponse response, HttpServletRequest request) throws WebMessageException
=======
        webMessageService.send(
            WebMessageUtils.ok( "System setting '" + key + "' set to value '" + valueObject + "'." ), response,
            request );
    }

    private void saveSystemSettingTranslation( String key, String locale, String value, SettingKey setting,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        try
        {
            systemSettingManager.saveSystemSettingTranslation( setting, locale, value );
        }
        catch ( IllegalStateException e )
        {
            throw new WebMessageException( WebMessageUtils.conflict( e.getMessage() ) );
        }

        webMessageService.send( WebMessageUtils.ok( "Translation for system setting '" + key +
            "' and locale: '" + locale + "' set to: '" + value + "'" ), response, request );
    }

    private void validateParameters( String key, String value, String valuePayload )
        throws WebMessageException
    {
        if ( key == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key must be specified" ) );
        }

        if ( value == null && valuePayload == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Value must be specified as query param or as payload" ) );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = { ContextUtils.CONTENT_TYPE_JSON } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    public void setSystemSettingV29( @RequestBody Map<String, Object> settings, HttpServletResponse response,
        HttpServletRequest request )
        throws WebMessageException
    {
        List<String> invalidKeys = settings.keySet().stream()
            .filter( ( key ) -> !SettingKey.getByName( key ).isPresent() ).collect( Collectors.toList() );

        if ( invalidKeys.size() > 0 )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Key(s) is not supported: " + StringUtils.join( invalidKeys, ", " ) ) );
        }

        for ( String key : settings.keySet() )
        {
            Serializable valueObject = SettingKey.getAsRealClass( key, settings.get( key ).toString() );
            systemSettingManager.saveSystemSetting( SettingKey.getByName( key ).get(), valueObject );
        }

        webMessageService.send( WebMessageUtils.ok( "System settings imported" ), response, request );
    }

    @RequestMapping( value = "/{key}", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_TEXT )
    public @ResponseBody String getSystemSettingOrTranslationAsPlainText( @PathVariable( "key" ) String key,
        @RequestParam( value = "locale", required = false ) String locale, HttpServletResponse response )
    {
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );

        return getSystemSettingOrTranslation( key, locale );
<<<<<<< HEAD
    }

    @RequestMapping( value = "/{key}", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_HTML } )
    public @ResponseBody void getSystemSettingOrTranslationAsJson( @PathVariable( "key" ) String key,
        @RequestParam( value = "locale", required = false ) String locale, HttpServletResponse response ) throws IOException
    {
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );

        String systemSettingValue = getSystemSettingOrTranslation( key, locale );

        Map<String, String> systemSettingsForJsonGeneration = new HashMap<>();
        systemSettingsForJsonGeneration.put( key, systemSettingValue );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );
        renderService.toJson( response.getOutputStream(), systemSettingsForJsonGeneration );
    }

    private String getSystemSettingOrTranslation( String key, String locale )
    {
        Optional<SettingKey> settingKey = SettingKey.getByName( key );
        if ( !systemSettingManager.isConfidential( key ) && settingKey.isPresent() )
        {
            Optional<String> localeToFetch = getLocaleToFetch( locale, key );

            if ( localeToFetch.isPresent() )
            {
                Optional<String> translation = systemSettingManager.getSystemSettingTranslation( settingKey.get(), localeToFetch.get() );

                if ( translation.isPresent() )
                {
                    return translation.get();
                }
            }

            Serializable setting = systemSettingManager.getSystemSetting( settingKey.get() );
            return setting != null ? String.valueOf( setting ) : StringUtils.EMPTY;
        }

        return StringUtils.EMPTY;
    }

    private Optional<String> getLocaleToFetch( String locale, String key )
    {
        if ( systemSettingManager.isTranslatable( key ) )
        {
            User currentUser = currentUserService.getCurrentUser();

            if ( StringUtils.isNotEmpty( locale ) )
            {
                return Optional.of( locale );
            }
            else if ( currentUser != null )
            {
                Locale userLocale = (Locale) userSettingService.getUserSetting( UserSettingKey.UI_LOCALE, currentUser );

                if ( userLocale != null )
                {
                    return Optional.of( userLocale.getLanguage() );
                }
            }
        }

        return Optional.empty();
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }

<<<<<<< HEAD
    @RequestMapping( method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_HTML } )
    public void getSystemSettingsJson( @RequestParam( value = "key", required = false ) Set<String> keys, HttpServletResponse response )
=======
    @RequestMapping( value = "/{key}", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_HTML } )
    public @ResponseBody void getSystemSettingOrTranslationAsJson( @PathVariable( "key" ) String key,
        @RequestParam( value = "locale", required = false ) String locale, HttpServletResponse response )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        throws IOException
    {
<<<<<<< HEAD
        Set<SettingKey> settingKeys = getSettingKeysToFetch( keys );
=======
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );

        String systemSettingValue = getSystemSettingOrTranslation( key, locale );

        Map<String, String> systemSettingsForJsonGeneration = new HashMap<>();
        systemSettingsForJsonGeneration.put( key, systemSettingValue );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );
        renderService.toJson( response.getOutputStream(), systemSettingsForJsonGeneration );
    }

    private String getSystemSettingOrTranslation( String key, String locale )
    {
        Optional<SettingKey> settingKey = SettingKey.getByName( key );
        if ( !systemSettingManager.isConfidential( key ) && settingKey.isPresent() )
        {
            Optional<String> localeToFetch = getLocaleToFetch( locale, key );

            if ( localeToFetch.isPresent() )
            {
                Optional<String> translation = systemSettingManager.getSystemSettingTranslation( settingKey.get(),
                    localeToFetch.get() );

                if ( translation.isPresent() )
                {
                    return translation.get();
                }
            }

            Serializable setting = systemSettingManager.getSystemSetting( settingKey.get() );
            return setting != null ? String.valueOf( setting ) : StringUtils.EMPTY;
        }
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        return StringUtils.EMPTY;
    }

    private Optional<String> getLocaleToFetch( String locale, String key )
    {
        if ( systemSettingManager.isTranslatable( key ) )
        {
            User currentUser = currentUserService.getCurrentUser();

            if ( StringUtils.isNotEmpty( locale ) )
            {
                return Optional.of( locale );
            }
            else if ( currentUser != null )
            {
                Locale userLocale = (Locale) userSettingService.getUserSetting( UserSettingKey.UI_LOCALE, currentUser );

                if ( userLocale != null )
                {
                    return Optional.of( userLocale.getLanguage() );
                }
            }
        }

        return Optional.empty();
    }

    @RequestMapping( method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_HTML } )
    public void getSystemSettingsJson( @RequestParam( value = "key", required = false ) Set<String> keys,
        HttpServletResponse response )
        throws IOException
    {
        Set<SettingKey> settingKeys = getSettingKeysToFetch( keys );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );
        renderService.toJson( response.getOutputStream(), getSystemSettings( settingKeys ) );
    }

    @RequestMapping( method = RequestMethod.GET, produces = "application/javascript" )
    public void getSystemSettingsJsonP( @RequestParam( value = "key", required = false ) Set<String> keys,
        @RequestParam( defaultValue = "callback" ) String callback, HttpServletResponse response )
        throws IOException
    {
        Set<SettingKey> settingKeys = getSettingKeysToFetch( keys );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );
        renderService.toJsonP( response.getOutputStream(), getSystemSettings( settingKeys ), callback );
    }

    private Set<SettingKey> getSettingKeysToFetch( Set<String> keys )
    {
        Set<SettingKey> settingKeys;

        if ( keys != null )
        {
            keys.removeIf( systemSettingManager::isConfidential );
            settingKeys = keys.stream()
                .map( SettingKey::getByName )
                .filter( Optional::isPresent )
                .map( Optional::get )
                .collect( Collectors.toSet() );
        }
        else
        {
            settingKeys = new HashSet<>( Arrays.asList( SettingKey.values() ) );
            settingKeys.removeIf( key -> systemSettingManager.isConfidential( key.getName() ) );
        }

        return settingKeys;
    }

    private Map<String, Serializable> getSystemSettings( Set<SettingKey> keys )
    {
        return systemSettingManager.getSystemSettings( keys );
    }

    @RequestMapping( value = "/{key}", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void removeSystemSetting( @PathVariable( "key" ) String key,
        @RequestParam( value = "locale", required = false ) String locale )
        throws WebMessageException
    {
        Optional<SettingKey> setting = SettingKey.getByName( key );

        if ( !setting.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key is not supported: " + key ) );
        }

        if ( StringUtils.isNotEmpty( locale ) )
        {
            systemSettingManager.saveSystemSettingTranslation( setting.get(), locale, StringUtils.EMPTY );
        }
        else
        {
            systemSettingManager.deleteSystemSetting( setting.get() );
        }
    }
}
