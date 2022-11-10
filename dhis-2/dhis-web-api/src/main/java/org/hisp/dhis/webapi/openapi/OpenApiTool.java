/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import org.hisp.dhis.webapi.controller.AbstractCrudController;

/**
 * A classic command line application to generate DHIS2 OpenAPI documents.
 *
 * The application is also provided as {@link ToolProvider} to be more
 * accessible in CI build chains.
 *
 * @author Jan Bernitt
 */
public class OpenApiTool implements ToolProvider
{
    @SuppressWarnings( "java:S106" )
    public static void main( String[] args )
    {
        int errorCode = new OpenApiTool().run( System.out, System.err, args );
        if ( errorCode != 0 )
            System.exit( errorCode );
    }

    @Override
    public String name()
    {
        return "openapi";
    }

    @Override
    public int run( PrintWriter out, PrintWriter err, String... args )
    {
        if ( args.length == 0 )
        {
            out.println( "Usage: [<path or tag>...] <output-file>" );
            return -1;
        }
        String root = AbstractCrudController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        List<Class<?>> classes;
        try ( Stream<Path> files = Files.walk( Path.of( root ) ) )
        {
            classes = files
                .filter( f -> f.getFileName().toString().endsWith( "Controller.class" ) )
                .map( OpenApiTool::toClassName )
                .map( OpenApiTool::toClass )
                .filter( c -> !Modifier.isAbstract( c.getModifiers() ) )
                .collect( toList() );
        }
        catch ( IOException ex )
        {
            ex.printStackTrace( err );
            return -1;
        }
        if ( classes.isEmpty() )
        {
            err.println( "Controller classes need to be compiled first" );
            return -1;
        }
        Set<String> paths = new HashSet<>();
        Set<String> tags = new HashSet<>();
        for ( int i = 0; i < args.length - 1; i++ )
        {
            String arg = args[i];
            if ( arg.startsWith( "/" ) )
            {
                paths.add( arg );
            }
            else
            {
                tags.add( arg );
            }
        }
        Api api = ApiAnalyser.describeApi( classes, paths, tags );
        String doc = OpenApiGenerator.generate( api );
        try
        {
            Path output = Files.writeString( Path.of( args[args.length - 1] ), doc );
            out.printf( "Generated OpenAPI document %s with %d controllers, %d schemas %n",
                output, api.getControllers().size(), api.getSchemas().size() );
        }
        catch ( IOException ex )
        {
            ex.printStackTrace( err );
            return -1;
        }
        return 0;
    }

    private static String toClassName( Path f )
    {
        return f.toString().substring( f.toString().indexOf( "org/hisp/" ) )
            .replace( ".class", "" )
            .replace( '/', '.' );
    }

    private static Class<?> toClass( String className )
    {
        try
        {
            return Class.forName( className );
        }
        catch ( Exception ex )
        {
            throw new IllegalArgumentException( "failed loading: " + className, ex );
        }
    }
}
