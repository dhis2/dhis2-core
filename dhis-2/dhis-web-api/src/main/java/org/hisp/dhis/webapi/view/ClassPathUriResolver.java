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
package org.hisp.dhis.webapi.view;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ClassPathUriResolver implements URIResolver
{
    private String templatePath = "/templates/";

    public ClassPathUriResolver()
    {
    }

    public ClassPathUriResolver( String templatePath )
    {
        this.templatePath = templatePath;
    }

    public String getTemplatePath()
    {
        return templatePath;
    }

    public void setTemplatePath( String templatePath )
    {
        this.templatePath = templatePath;
    }

    @Override
    public Source resolve( String href, String base )
        throws TransformerException
    {
        String url = getTemplatePath() + href;
        ClassPathResource classPathResource = new ClassPathResource( url );

        if ( !classPathResource.exists() )
        {
            throw new TransformerException( "Resource does not exist in classpath: " + url );
        }

        Source source = null;

        try
        {
            source = new StreamSource( classPathResource.getInputStream() );
        }
        catch ( IOException e )
        {
            throw new TransformerException( "IOException while reading URL: " + url );
        }

        return source;
    }
}
