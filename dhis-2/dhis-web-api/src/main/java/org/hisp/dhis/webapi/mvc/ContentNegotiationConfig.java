package org.hisp.dhis.webapi.mvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configure content negotiation so that both path extensions {@code .json} and
 * {@code .xml} can be used as well as the {@code Accept} header.
 *
 * Default response is JSON.
 *
 * @author Jan Bernitt
 */
@Configuration
public class ContentNegotiationConfig implements WebMvcConfigurer
{
    @Override
    public void configureContentNegotiation( ContentNegotiationConfigurer config )
    {
        config
            .favorPathExtension( true )
            .favorParameter( false )
            .ignoreAcceptHeader( false )
            .defaultContentType( MediaType.APPLICATION_JSON )
            .mediaType( "json", MediaType.APPLICATION_JSON )
            .mediaType( "xml", MediaType.APPLICATION_XML );
    }

    @Override
    public void configurePathMatch( PathMatchConfigurer config )
    {
        config.setUseSuffixPatternMatch( true );
    }
}
