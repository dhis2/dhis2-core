/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.service.WebCache;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author Lars Helge Overland
 */
@Component
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

    public static final String CONTENT_TYPE_XML_ADX = "application/adx+xml; charset=UTF-8";

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

    public static final String HEADER_VALUE_NO_STORE = "no-cache, no-store, max-age=0, must-revalidate";

    public static final String QUERY_PARAM_SEP = ";";

    public static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    public static final String HEADER_ETAG = "ETag";

    private static final String QUOTE = "\"";

    private static final String QUERY_STRING_SEP = "?";

    /**
     * Regular expression that extracts the attachment file name from a content
     * disposition header value.
     */
    private static final Pattern CONTENT_DISPOSITION_ATTACHMENT_FILENAME_PATTERN = Pattern
        .compile( "attachment;\\s*filename=\"?([^;\"]+)\"?" );

    private final WebCache webCache;

    public ContextUtils( final WebCache webCache )
    {
        checkNotNull( webCache );

        this.webCache = webCache;
    }

    public void configureResponse( HttpServletResponse response, String contentType, CacheStrategy cacheStrategy )
    {
        configureResponse( response, contentType, cacheStrategy, null, false );
    }

    public void configureAnalyticsResponse( HttpServletResponse response, String contentType,
        CacheStrategy cacheStrategy, String filename, boolean attachment, Date latestEndDate )
    {
        // Progressive cache will always take priority
        if ( RESPECT_SYSTEM_SETTING == cacheStrategy && webCache.isProgressiveCachingEnabled()
            && latestEndDate != null )
        {
            // Uses the progressive TTL
            final CacheControl cacheControl = webCache.getCacheControlFor( latestEndDate );

            configureResponse( response, contentType, filename, attachment, cacheControl );
        }
        else
        {
            // Respects the fixed (predefined) settings
            configureResponse( response, contentType, cacheStrategy, filename, attachment );
        }
    }

    public void configureResponse( HttpServletResponse response, String contentType,
        CacheStrategy cacheStrategy, String filename, boolean attachment )
    {
        CacheControl cacheControl = webCache.getCacheControlFor( cacheStrategy );

        configureResponse( response, contentType, filename, attachment, cacheControl );
    }

    private void configureResponse( HttpServletResponse response, String contentType, String filename,
        boolean attachment, CacheControl cacheControl )
    {
        if ( contentType != null )
        {
            response.setContentType( contentType );
        }

        response.setHeader( HEADER_CACHE_CONTROL, cacheControl.getHeaderValue() );

        if ( filename != null )
        {
            final String type = attachment ? "attachment" : "inline";

            response.setHeader( HEADER_CONTENT_DISPOSITION, type + "; filename=\"" + filename + "\"" );
        }
    }

    public static HttpServletResponse setCacheControl( HttpServletResponse response, CacheControl value )
    {
        response.setHeader( HEADER_CACHE_CONTROL, value.getHeaderValue() );
        return response;
    }

    public static HttpServletResponse setNoStore( HttpServletResponse response )
    {
        response.setHeader( HEADER_CACHE_CONTROL, HEADER_VALUE_NO_STORE );
        return response;
    }

    public static void okResponse( HttpServletResponse response, String message ) // TODO
                                                                                  // remove
                                                                                  // message
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

        try ( PrintWriter writer = response.getWriter() )
        {
            writer.println( message );
            writer.flush();
        }
        catch ( IOException ex )
        {
            // Ignore
        }
    }

    public static HttpServletRequest getRequest()
    {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static String getContextPath( HttpServletRequest request )
    {
        StringBuilder builder = new StringBuilder();
        String xForwardedProto = request.getHeader( "X-Forwarded-Proto" );
        String xForwardedPort = request.getHeader( "X-Forwarded-Port" );

        if ( xForwardedProto != null
            && (xForwardedProto.equalsIgnoreCase( "http" ) || xForwardedProto.equalsIgnoreCase( "https" )) )
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
     * Indicates whether the media type (content type) of the given HTTP request
     * is compatible with the given media type.
     *
     * @param response the HTTP response.
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
     * Returns a mapping of dimension identifiers and dimension option
     * identifiers based on the given set of dimension strings. Splits the
     * strings using : as separator. Returns null of dimensions are null or
     * empty.
     * <p/>
     * TODO remove
     *
     * @param dimensions the set of strings on format
     *        dimension:dimension-option.
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
     * Adds basic authentication by adding an Authorization header to the given
     * HttpHeaders object.
     *
     * @param headers the HttpHeaders object.
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
     * evaluates to true, it will set status code 304 Not Modified on the
     * response and remove all elements from the given list. It will set the
     * ETag header on the response in any case.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @return true if the eTag values are equals, false otherwise.
     */
    public static boolean clearIfNotModified( HttpServletRequest request, HttpServletResponse response,
        Collection<? extends IdentifiableObject> objects )
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
     * This method looks up the ETag sent in the request from the
     * "If-None-Match" header value and compares it to the given tag. If they
     * match, it will set status code 304 Not Modified on the response. It will
     * set the ETag header on the response in any case. It will wrap the given
     * tag in quotes.
     *
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param tag the tag to compare.
     * @return true if the given tag match the request tag and the response is
     *         considered not modified, false if not.
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
     * Indicates whether the given requests indicates that it accepts a
     * compressed response.
     *
     * @param request the HttpServletRequest.
     * @return whether the given requests indicates that it accepts a compressed
     *         response.
     */
    public static boolean isAcceptCsvGzip( HttpServletRequest request )
    {
        return request != null && ((request.getPathInfo() != null && request.getPathInfo().endsWith( ".gz" ))
            || (request.getHeader( "Accept" ) != null
                && request.getHeader( "Accept" ).contains( "application/csv+gzip" )));
    }

    /**
     * Extracts and returns the file name from a content disposition header
     * value.
     *
     * @param contentDispositionHeaderValue the content disposition header value
     *        from which the file name should be extracted.
     * @return the extracted file name or <code>null</code> content disposition
     *         has no filename.
     */
    @Nullable
    public static String getAttachmentFileName( @Nullable String contentDispositionHeaderValue )
    {
        if ( contentDispositionHeaderValue == null )
        {
            return null;
        }

        Matcher matcher = CONTENT_DISPOSITION_ATTACHMENT_FILENAME_PATTERN.matcher( contentDispositionHeaderValue );
        return matcher.matches() ? matcher.group( 1 ) : null;
    }

    /**
     * Returns the value associated with a double wildcard ({@code **}) in the
     * request mapping.
     * <p>
     * As an example, for a request mapping {@code /apps/**} and a request
     * {@code /apps/data-visualizer/index.html?id=123}, this method will return
     * {@code data-visualizer/index.html?id=123}.
     *
     * @param request the {@link HttpServletRequest}.
     * @return the wildcard value.
     */
    public static String getWildcardPathValue( HttpServletRequest request )
    {
        String path = (String) request.getAttribute( HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE );
        String bestMatchPattern = (String) request.getAttribute( HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE );
        String wildcardPath = new AntPathMatcher().extractPathWithinPattern( bestMatchPattern, path );

        String queryString = !StringUtils.isBlank( request.getQueryString() )
            ? QUERY_STRING_SEP.concat( request.getQueryString() )
            : StringUtils.EMPTY;

        return String.format( "%s%s", wildcardPath, queryString );
    }
}
