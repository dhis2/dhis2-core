package org.hisp.dhis.system.velocity;

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

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;

public class VelocityManager
{
    public static final String CONTEXT_KEY = "object";

    private static final String RESOURCE_LOADER_NAME = "class";
    private static final String VM_SUFFIX = ".vm";
    
    private VelocityEngine velocity;

    public VelocityManager()
    {
        velocity = new VelocityEngine();
        velocity.setProperty( RuntimeConstants.RESOURCE_LOADER, RESOURCE_LOADER_NAME );
        velocity.setProperty( RESOURCE_LOADER_NAME + ".resource.loader.class", ClasspathResourceLoader.class.getName() );
        velocity.setProperty( "runtime.log.logsystem.log4j.logger", "console" );
        velocity.setProperty( "runtime.log", "" );
                
        velocity.init();
    }

    public VelocityEngine getEngine()
    {
        return velocity;
    }

    public String render( String template )
    {
        return render( null, template );
    }
    
    public String render( Object object, String template )
    {
        try
        {
            StringWriter writer = new StringWriter();

            VelocityContext context = new VelocityContext();

            if ( object != null )
            {
                context.put( CONTEXT_KEY, object );
            }

            velocity.getTemplate( template + VM_SUFFIX ).merge( context, writer );

            return writer.toString();

            // TODO include encoder in context
        } 
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to merge velocity template", ex );
        }
    }
}
