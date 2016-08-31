package org.hisp.dhis.webapi.view;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.OutputStream;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.web.servlet.view.AbstractView;

import com.fasterxml.jackson.databind.util.JSONPObject;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class JacksonJsonView
    extends AbstractView
{
    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_APPLICATION_JSON_GZIP = "application/json+gzip";
    private static final String CONTENT_TYPE_APPLICATION_JAVASCRIPT = "application/javascript";
    private static final String CONTENT_TYPE_APPLICATION_JAVASCRIPT_GZIP = "application/javascript+gzip";

    private boolean withPadding = false;

    private boolean withCompression = false;

    private String callbackParameter = "callback";

    private String paddingFunction = "callback";

    public JacksonJsonView()
    {
        setContentType( CONTENT_TYPE_APPLICATION_JSON );
    }

    public JacksonJsonView( String contentType )
    {
        setContentType( contentType );
    }

    public JacksonJsonView( boolean withPadding, boolean withCompression )
    {
        this.withPadding = withPadding;
        this.withCompression = withCompression;

        if ( !withPadding )
        {
            if ( !withCompression )
            {
                setContentType( CONTENT_TYPE_APPLICATION_JSON );
            }
            else
            {
                setContentType( CONTENT_TYPE_APPLICATION_JSON_GZIP );
            }
        }
        else
        {
            if ( !withCompression )
            {
                setContentType( CONTENT_TYPE_APPLICATION_JAVASCRIPT );
            }
            else
            {
                setContentType( CONTENT_TYPE_APPLICATION_JAVASCRIPT_GZIP );
            }
        }
    }

    public void setCallbackParameter( String callbackParameter )
    {
        this.callbackParameter = callbackParameter;
    }

    @Override
    protected void renderMergedOutputModel( Map<String, Object> model, HttpServletRequest request,
        HttpServletResponse response ) throws Exception
    {
        Object object = model.get( "model" );
        Class<?> viewClass = JacksonUtils.getViewClass( model.get( "viewClass" ) );
        response.setContentType( getContentType() + "; charset=UTF-8" );

        OutputStream outputStream;

        if ( !withCompression )
        {
            outputStream = response.getOutputStream();
        }
        else
        {
            if ( !withPadding )
            {
                response.setContentType( CONTENT_TYPE_APPLICATION_JSON_GZIP );
                response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
                outputStream = new GZIPOutputStream( response.getOutputStream() );
            }
            else
            {
                response.setContentType( CONTENT_TYPE_APPLICATION_JAVASCRIPT_GZIP );
                response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
                outputStream = new GZIPOutputStream( response.getOutputStream() );
            }
        }

        if ( withPadding )
        {
            String callback = request.getParameter( callbackParameter );

            if ( callback == null || callback.length() == 0 )
            {
                callback = paddingFunction;
            }

            object = new JSONPObject( callback, object );
        }

        if ( viewClass != null )
        {
            JacksonUtils.toJsonWithView( outputStream, object, viewClass );
        }
        else
        {
            JacksonUtils.toJson( outputStream, object );
        }
    }
}
