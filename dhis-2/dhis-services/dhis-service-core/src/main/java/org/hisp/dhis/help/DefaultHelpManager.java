package org.hisp.dhis.help;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import static org.hisp.dhis.commons.util.StreamUtils.ENCODING_UTF8;

/**
 * @author Lars Helge Overland
 */
public class DefaultHelpManager
    implements HelpManager
{
    private static final Log log = LogFactory.getLog( DefaultHelpManager.class );

    // -------------------------------------------------------------------------
    // HelpManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void getHelpContent( OutputStream out, String id, Locale locale )
    {
        try
        {
            ClassPathResource classPathResource = resolveHelpFileResource( locale );

            Source source = new StreamSource( classPathResource.getInputStream(), ENCODING_UTF8 );

            Result result = new StreamResult( out );

            Transformer transformer = getTransformer( "help_stylesheet.xsl" );

            transformer.setParameter( "sectionId", id );

            transformer.transform( source, result );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to get help content", ex );
        }
    }

    @Override
    public void getHelpItems( OutputStream out, Locale locale )
    {
        try
        {
            ClassPathResource classPathResource = resolveHelpFileResource( locale );

            Source source = new StreamSource( classPathResource.getInputStream(), ENCODING_UTF8 );

            Result result = new StreamResult( out );

            getTransformer( "helpitems_stylesheet.xsl" ).transform( source, result );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to get help content", ex );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Transformer getTransformer( String stylesheetName )
        throws IOException, TransformerConfigurationException
    {
        Source stylesheet = new StreamSource( new ClassPathResource( stylesheetName ).getInputStream(), ENCODING_UTF8 );

        return TransformerFactory.newInstance().newTransformer( stylesheet );
    }

    private ClassPathResource resolveHelpFileResource( Locale locale )
    {
        String helpFile = null;
        
        ClassPathResource classPathResource = null;

        if ( locale != null && locale.getDisplayLanguage() != null )
        {
            helpFile = "help_content_" + locale.getLanguage() + "_" + locale.getCountry() + ".xml";

            log.debug( "Help file: " + helpFile );
        }
        else
        {
            helpFile = "help_content.xml";

            log.debug( "Help file: " + helpFile );
        }

        classPathResource = new ClassPathResource( helpFile );

        if ( !classPathResource.exists() )
        {
            log.warn( "Help file: " + helpFile + " not available on classpath, falling back to defaul" );
            
            helpFile = "help_content.xml";

            classPathResource = new ClassPathResource( helpFile );
        }

        return classPathResource;
    }
}
