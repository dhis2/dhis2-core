package org.hisp.dhis.webapi.utils;

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

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.system.util.DateUtils.getSecondsUntilTomorrow;

/**
 * @author Lars Helge Overland
 */
public class ContextUtils
{
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_ZIP = "application/zip";
    public static final String CONTENT_TYPE_GZIP = "application/gzip";
    public static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    public static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    public static final String CONTENT_TYPE_TEXT = "text/plain; charset=UTF-8";
    public static final String CONTENT_TYPE_CSS = "text/css; charset=UTF-8";
    public static final String CONTENT_TYPE_XML = "application/xml; charset=UTF-8";
    public static final String CONTENT_TYPE_XML_ADX = "application/xml+adx; charset=UTF-8";
    public static final String CONTENT_TYPE_CSV = "application/csv; charset=UTF-8";
    public static final String CONTENT_TYPE_PNG = "image/png";
    public static final String CONTENT_TYPE_JPG = "image/jpeg";
    public static final String CONTENT_TYPE_EXCEL = "application/vnd.ms-excel";
    public static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript; charset=UTF-8";
    public static final String CONTENT_TYPE_FORM_ENCODED = "application/x-www-form-urlencoded";

    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_EXPIRES = "Expires";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";

    public static final String QUERY_PARAM_SEP = ";";
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    public static final String HEADER_ETAG = "ETag";
    private static final String QUOTE = "\"";

    @Autowired
    private SystemSettingManager systemSettingManager;

    public void configureResponse( HttpServletResponse response, String contentType, CacheStrategy cacheStrategy )
    {
        configureResponse( response, contentType, cacheStrategy, null, false );
    }

    public void configureAnalyticsResponse( HttpServletResponse response, String contentType, CacheStrategy cacheStrategy, String filename, boolean attachment, Date latestEndDate )
    {
        int cacheThreshold = (int) systemSettingManager.getSystemSetting( SettingKey.CACHE_ANALYTICS_DATA_YEAR_THRESHOLD );
        Calendar threshold = Calendar.getInstance();
        threshold.add( Calendar.YEAR, cacheThreshold * -1 );

        if ( latestEndDate != null && cacheThreshold > 0 && threshold.getTime().before( latestEndDate ) )
        {
            configureResponse( response, contentType, CacheStrategy.NO_CACHE, filename, attachment );
        }
        else
        {
            configureResponse( response, contentType, cacheStrategy, filename, attachment );
        }
    }

    public void configureResponse( HttpServletResponse response, String contentType, CacheStrategy cacheStrategy,
        String filename, boolean attachment )
    {
        CacheControl cacheControl;

        if ( contentType != null )
        {
            response.setContentType( contentType );
        }

        if ( CacheStrategy.RESPECT_SYSTEM_SETTING.equals( cacheStrategy ) )
        {
            String strategy = trimToNull( (String) systemSettingManager.getSystemSetting( SettingKey.CACHE_STRATEGY ) );

            cacheStrategy = strategy != null ? CacheStrategy.valueOf( strategy ) : CacheStrategy.NO_CACHE;
        }

        if ( CacheStrategy.CACHE_15_MINUTES.equals( cacheStrategy ) )
        {
            cacheControl = CacheControl.maxAge( 15, TimeUnit.MINUTES );
        }
        else if ( CacheStrategy.CACHE_30_MINUTES.equals( cacheStrategy ) )
        {
            cacheControl = CacheControl.maxAge( 30, TimeUnit.MINUTES );
        }
        else if ( CacheStrategy.CACHE_1_HOUR.equals( cacheStrategy ) )
        {
            cacheControl = CacheControl.maxAge( 1, TimeUnit.HOURS );
        }
        else if ( CacheStrategy.CACHE_6AM_TOMORROW.equals( cacheStrategy ) )
        {
            cacheControl = CacheControl.maxAge( getSecondsUntilTomorrow( 6 ), TimeUnit.SECONDS );
        }
        else if ( CacheStrategy.CACHE_TWO_WEEKS.equals( cacheStrategy ) )
        {
            cacheControl = CacheControl.maxAge( 14, TimeUnit.DAYS );
        }
        else
        {
            cacheControl = CacheControl.noCache();
        }

        if ( cacheStrategy != null && cacheStrategy != CacheStrategy.NO_CACHE )
        {
            Cacheability cacheability = (Cacheability) systemSettingManager.getSystemSetting( SettingKey.CACHEABILITY );

            if (cacheability.equals( Cacheability.PUBLIC ))
            {
                cacheControl.cachePublic();
            }
            else if ( cacheability.equals( Cacheability.PRIVATE ) )
            {
                cacheControl.cachePrivate();
            }
        }

        response.setHeader( HEADER_CACHE_CONTROL, cacheControl.getHeaderValue() );

        if ( filename != null )
        {
            String type = attachment ? "attachment" : "inline";

            response.setHeader( HEADER_CONTENT_DISPOSITION, type + "; filename=\"" + filename + "\"" );
        }
    }

    public static void setCacheControl( HttpServletResponse response, CacheControl value )
    {
        response.setHeader( HEADER_CACHE_CONTROL, value.getHeaderValue() );
    }

    public static void okResponse( HttpServletResponse response, String message ) //TODO remove message
    {
        setResponse( response, HttpServletResponse.SC_OK, message );
    }

    public static void badRequestResponse( HttpServletResponse response, String message )
    {
        setResponse( response, HttpServletResponse.SC_BAD_REQUEST, message );
    }

    private static void setResponse( HttpServletResponse response, int statusCode, String message )
    {
        response.setStatus( statusCode );
        response.setContentType( CONTENT_TYPE_TEXT );
        response.setHeader( HEADER_CACHE_CONTROL, CacheControl.noStore().getHeaderValue() );

        PrintWriter writer = null;

        try
        {
            writer = response.getWriter();
            writer.println( message );
            writer.flush();
        }
        catch ( IOException ex )
        {
            // Ignore
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }
    }

    public static HttpServletRequest getRequest()
    {
        return ( (ServletRequestAttributes) RequestContextHolder.getRequestAttributes() ).getRequest();
    }

    public static String getContextPath( HttpServletRequest request )
    {
        StringBuilder builder = new StringBuilder();
        String xForwardedProto = request.getHeader( "X-Forwarded-Proto" );
        String xForwardedPort = request.getHeader( "X-Forwarded-Port" );

        if ( xForwardedProto != null && ( xForwardedProto.equalsIgnoreCase( "http" ) || xForwardedProto.equalsIgnoreCase( "https" ) ) )
        {
            builder.append( xForwardedProto );
        }
        else
        {
            builder.append( request.getScheme() );
        }

        builder.append( "://" ).append( request.getServerName() );

        int port;

        try
        {
            port = Integer.parseInt( xForwardedPort );
        }
        catch ( NumberFormatException e )
        {
            port = request.getServerPort();
        }

        if ( port != 80 && port != 443 )
        {
            builder.append( ":" ).append( port );
        }

        builder.append( request.getContextPath() );

        return builder.toString();
    }

    public static String getRootPath( HttpServletRequest request )
    {
        return getContextPath( request ) + request.getServletPath();
    }

    /**
     * Indicates whether the media type (content type) of the
     * given HTTP request is compatible with the given media type.
     * 
     * @param request the HTTP response.
     * @param mediaType the media type.
     */
    public static boolean isCompatibleWith( HttpServletResponse response, MediaType mediaType )
    {                
        try
        {
            String contentType = response.getContentType();
            
            return contentType != null && MediaType.parseMediaType( contentType ).isCompatibleWith( mediaType );
        }
        catch ( InvalidMediaTypeException ex )
        {
            return false;
        }
    }
        
    /**
     * Returns a mapping of dimension identifiers and dimension option identifiers
     * based on the given set of dimension strings. Splits the strings using : as
     * separator. Returns null of dimensions are null or empty.
     * <p/>
     * TODO remove
     *
     * @param dimensions the set of strings on format dimension:dimension-option.
     * @return a map of dimensions and dimension options.
     */
    public static Map<String, String> getDimensionsAndOptions( Set<String> dimensions )
    {
        if ( dimensions == null || dimensions.isEmpty() )
        {
            return null;
        }

        Map<String, String> map = new HashMap<>();

        for ( String dim : dimensions )
        {
            String[] dims = dim.split( DimensionalObject.DIMENSION_NAME_SEP );

            if ( dims.length == 2 && dims[0] != null && dims[1] != null )
            {
                map.put( dims[0], dims[1] );
            }
        }

        return map;
    }

    /**
     * Returns the base URL for the given request.
     *
     * @param request the HTTP servlet request.
     * @return the base URL.
     */
    public static String getBaseUrl( HttpServletRequest request )
    {
        String server = request.getServerName();

        String scheme = request.getScheme();
        int port = request.getServerPort();
        String baseUrl = scheme + "://" + server + ":" + port + "/";

        return baseUrl;
    }

    /**
     * Adds basic authentication by adding an Authorization header to the
     * given HttpHeaders object.
     *
     * @param headers  the HttpHeaders object.
     * @param username the user name.
     * @param password the password.
     */
    public static void setBasicAuth( HttpHeaders headers, String username, String password )
    {
        headers.add( "Authorization", CodecUtils.getBasicAuthString( username, password ) );
    }

    /**
     * Clears the given collection if it is not modified according to the HTTP
     * cache validation. This method looks up the ETag sent in the request from
     * the "If-None-Match" header value, generates an ETag based on the given
     * collection of IdentifiableObjects and compares them for equality. If this
     * evaluates to true, it will set status code 304 Not Modified on the response
     * and remove all elements from the given list. It will set the ETag header
     * on the response in any case.
     *
     * @param request  the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @return true if the eTag values are equals, false otherwise.
     */
    public static boolean clearIfNotModified( HttpServletRequest request, HttpServletResponse response, Collection<? extends IdentifiableObject> objects )
    {
        String tag = QUOTE + IdentifiableObjectUtils.getLastUpdatedTag( objects ) + QUOTE;

        response.setHeader( HEADER_ETAG, tag );

        String inputTag = request.getHeader( HEADER_IF_NONE_MATCH );

        if ( objects != null && inputTag != null && inputTag.equals( tag ) )
        {
            response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );

            objects.clear();

            return true;
        }

        return false;
    }

    /**
     * This method looks up the ETag sent in the request from the "If-None-Match"
     * header value and compares it to the given tag. If they match, it will set
     * status code 304 Not Modified on the response. It will set the ETag header
     * on the response in any case. It will wrap the given tag in quotes.
     *
     * @param request  the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param tag      the tag to compare.
     * @return true if the given tag match the request tag and the response is
     * considered not modified, false if not.
     */
    public static boolean isNotModified( HttpServletRequest request, HttpServletResponse response, String tag )
    {
        tag = tag != null ? (QUOTE + tag + QUOTE) : null;

        String inputTag = request.getHeader( HEADER_IF_NONE_MATCH );

        response.setHeader( HEADER_ETAG, tag );

        if ( inputTag != null && inputTag.equals( tag ) )
        {
            response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );

            return true;
        }

        return false;
    }

    /**
     * Indicates whether the given requests indicates that it accepts a compressed
     * response.
     *
     * @param request the HttpServletRequest.
     * @return whether the given requests indicates that it accepts a compressed
     * response.
     */
    public static boolean isAcceptCsvGzip( HttpServletRequest request )
    {
        return request != null && ((request.getPathInfo() != null && request.getPathInfo().endsWith( ".gz" ))
            || (request.getHeader( "Accept" ) != null && request.getHeader( "Accept" ).contains( "application/csv+gzip" )));
    }
}