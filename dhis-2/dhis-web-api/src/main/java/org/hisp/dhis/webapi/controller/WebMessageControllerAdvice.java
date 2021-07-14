package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * When returning {@link WebMessage} or even subclasses of {@link WebMessage}s
 * the message's {@link WebMessage#getHttpStatusCode()} is used to set the HTTP
 * response status code.
 *
 * In case the response is a 4xx or 5xx caching is turned off.
 *
 * @author Jan Bernitt
 */
@ControllerAdvice
public class WebMessageControllerAdvice implements ResponseBodyAdvice<WebMessage>
{
    @Override
    public boolean supports( MethodParameter returnType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType )
    {
        return WebMessage.class.isAssignableFrom( returnType.getParameterType() );
    }

    @Override
    public WebMessage beforeBodyWrite( WebMessage body, MethodParameter returnType, MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
        ServerHttpResponse response )
    {
        HttpStatus httpStatus = HttpStatus.resolve( body.getHttpStatusCode() );
        if ( httpStatus != null )
        {
            response.setStatusCode( httpStatus );
            if ( httpStatus.is4xxClientError() || httpStatus.is5xxServerError() )
            {
                response.getHeaders().addIfAbsent( "Cache-Control",
                    CacheControl.noCache().cachePrivate().getHeaderValue() );
            }
        }
        return body;
    }
}
