package org.hisp.dhis.system.util;

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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class has the utility methods to invoke rest endpoints for various
 * Verbs. It uses Apache's HttpClient for the same.
 *
 * @author vanyas
 */
public class HttpUtils
{
    private static final Log log = LogFactory.getLog( HttpUtils.class );

    private static final String CONTENT_TYPE_ZIP = "application/gzip";

    /**
     * <pre>
     * <b>Description : </b>
     * Method to make an http GET call to a given URL with/without authentication.
     *
     * @param requestURL
     * @param authorize
     * @param username
     * @param password
     * @param headers
     * @param timeout
     * @return
     * @throws Exception </pre>
     */
    public static DhisHttpResponse httpGET( String requestURL, boolean authorize, String username, String password,
        Map<String, String> headers, int timeout, boolean processResponse ) throws Exception
    {
        DefaultHttpClient httpclient = null;
        DhisHttpResponse dhisHttpResponse = null;
        HttpParams params = new BasicHttpParams();
        
        try
        {
            HttpConnectionParams.setConnectionTimeout( params, timeout );
            HttpConnectionParams.setSoTimeout( params, timeout );
            httpclient = new DefaultHttpClient( params );
            HttpGet httpGet = new HttpGet( requestURL );

            if ( headers instanceof Map )
            {
                for ( Map.Entry<String, String> e : headers.entrySet() )
                {
                    httpGet.addHeader( e.getKey(), e.getValue() );
                }
            }

            if ( authorize )
            {
                httpGet.setHeader( "Authorization", CodecUtils.getBasicAuthString( username, password ) );
            }

            HttpResponse response = httpclient.execute( httpGet );

            if ( processResponse )
            {
                dhisHttpResponse = processResponse( requestURL, username, response );
            }
            else
            {
                dhisHttpResponse = new DhisHttpResponse( response, null, response.getStatusLine().getStatusCode() );
            }
        }
        catch ( Exception e )
        {
            log.error( "Exception occurred in the httpGET call with username " + username, e );
            throw e;

        }
        finally
        {
            if ( httpclient != null )
            {
                httpclient.getConnectionManager().shutdown();
            }
        }
        
        return dhisHttpResponse;
    }

    /**
     * <pre>
     * <b>Description : </b>
     * Method to make an http POST call to a given URL with/without authentication.
     *
     * @param requestURL
     * @param body
     * @param authorize
     * @param username
     * @param password
     * @param contentType
     * @param timeout
     * @return </pre>
     */
    public static DhisHttpResponse httpPOST( String requestURL, Object body, boolean authorize, String username, String password,
        String contentType, int timeout ) throws Exception
    {
        DefaultHttpClient httpclient = null;
        HttpParams params = new BasicHttpParams();
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        DhisHttpResponse dhisHttpResponse = null;

        try
        {
            HttpConnectionParams.setConnectionTimeout( params, timeout );
            HttpConnectionParams.setSoTimeout( params, timeout );
            httpclient = new DefaultHttpClient( params );
            HttpPost httpPost = new HttpPost( requestURL );

            if ( body instanceof Map )
            {
                @SuppressWarnings( "unchecked" )
                Map<String, String> parameters = (Map<String, String>) body;
                for ( Map.Entry<String, String> parameter : parameters.entrySet() )
                {
                    if ( parameter.getValue() != null )
                    {
                        pairs.add( new BasicNameValuePair( parameter.getKey(), parameter.getValue() ) );
                    }
                }
                httpPost.setEntity( new UrlEncodedFormEntity( pairs, "UTF-8" ) );
            }
            else if ( body instanceof String )
            {
                httpPost.setEntity( new StringEntity( (String) body ) );
            }

            if ( !StringUtils.isNotEmpty( contentType ) )
                httpPost.setHeader( "Content-Type", contentType );

            if ( authorize )
            {
                httpPost.setHeader( "Authorization", CodecUtils.getBasicAuthString( username, password ) );
            }

            HttpResponse response = httpclient.execute( httpPost );
            log.info( "Successfully got response from http POST." );
            dhisHttpResponse = processResponse( requestURL, username, response );
        }
        catch ( Exception e )
        {
            log.error( "Exception occurred in httpPOST call with username " + username, e );
            throw e;

        }
        finally
        {
            if ( httpclient != null )
            {
                httpclient.getConnectionManager().shutdown();
            }
        }

        return dhisHttpResponse;
    }

    /**
     * <pre>
     * <b>Description : </b>
     * Method to make an http DELETE call to a given URL with/without authentication.
     *
     * @param requestURL
     * @param authorize
     * @param username
     * @param password
     * @param headers
     * @param timeout
     * @return
     * @throws Exception </pre>
     */
    public static DhisHttpResponse httpDELETE( String requestURL, boolean authorize, String username, String password,
        Map<String, String> headers, int timeout ) throws Exception
    {
        DefaultHttpClient httpclient = null;
        DhisHttpResponse dhisHttpResponse = null;
        try
        {
            HttpParams params = new BasicHttpParams();

            HttpConnectionParams.setConnectionTimeout( params, timeout );
            HttpConnectionParams.setSoTimeout( params, timeout );

            httpclient = new DefaultHttpClient( params );

            HttpDelete httpDelete = new HttpDelete( requestURL );
            
            if ( headers instanceof Map )
            {
                for ( Map.Entry<String, String> e : headers.entrySet() )
                {
                    httpDelete.addHeader( e.getKey(), e.getValue() );
                }
            }

            if ( authorize )
            {
                httpDelete.setHeader( "Authorization", CodecUtils.getBasicAuthString( username, password ) );
            }

            HttpResponse response = httpclient.execute( httpDelete );
            dhisHttpResponse = processResponse( requestURL, username, response );

            return dhisHttpResponse;
        }
        catch ( Exception e )
        {
            log.error( "exception occurred in httpDELETE call with username " + username, e );
            throw e;
        }
        finally
        {
            if ( httpclient != null )
            {
                httpclient.getConnectionManager().shutdown();
            }
        }
    }


    /**
     * <pre>
     * <b>Description : </b>
     * Processes the HttpResponse to create a DHisHttpResponse object
     *
     * @param requestURL
     * @param username
     * @param response
     * @return
     * @throws IOException </pre>
     */
    private static DhisHttpResponse processResponse( String requestURL, String username, HttpResponse response )
        throws Exception
    {
        DhisHttpResponse dhisHttpResponse = null;
        String output = null;
        int statusCode = 0;
        if ( response != null )
        {
            HttpEntity responseEntity = response.getEntity();

            if ( responseEntity != null && responseEntity.getContent() != null )
            {
                Header contentType = response.getEntity().getContentType();

                if ( contentType != null && checkIfGzipContentType( contentType ) )
                {
                    GzipDecompressingEntity gzipDecompressingEntity = new GzipDecompressingEntity( response.getEntity() );
                    InputStream content = gzipDecompressingEntity.getContent();
                    output = IOUtils.toString( content );
                }
                else
                {
                    output = EntityUtils.toString( response.getEntity() );
                }
                statusCode = response.getStatusLine().getStatusCode();
            }
            else
            {
                throw new Exception( "No content found in the response received from http POST call to " + requestURL + " with username " + username );
            }

            dhisHttpResponse = new DhisHttpResponse( response, output, statusCode );
        }
        else
        {
            throw new Exception( "NULL response received from http POST call to " + requestURL + " with username " + username );
        }

        return dhisHttpResponse;
    }

    private static boolean checkIfGzipContentType( Header contentType )
    {
        return contentType.getValue().contains( CONTENT_TYPE_ZIP );
    }
}
