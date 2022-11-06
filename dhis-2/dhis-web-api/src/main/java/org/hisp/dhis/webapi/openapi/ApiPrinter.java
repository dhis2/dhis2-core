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

import static java.lang.String.format;

/**
 * Converts the {@link Api} model to a readable document mostly for debugging
 * purposes.
 *
 * @author Jan Bernitt
 */
public class ApiPrinter
{
    public static String printApi( Api api )
    {
        StringBuilder str = new StringBuilder();
        for ( Api.Controller c : api.getControllers() )
        {
            str.append( format( "%s %s%n", c.getName(), c.getPaths() ) );
            for ( Api.Endpoint e : c.getEndpoints() )
            {
                Api.Response response = e.getResponses().get( 0 );
                str.append( format( "\t%s %s => %s %s: %s%n", e.getMethods(), e.getPaths(), response.getStatus(),
                    e.getName(), printName( response.getBody() ) ) );
                for ( Api.Parameter p : e.getParameters() )
                {
                    str.append( format( "\t\t%s %s %s: %s%n", p.isRequired() ? "!" : "?", p.getLocation(), p.getName(),
                        printName( p.getType() ) ) );
                }
            }
        }
        for ( Api.Schema s : api.getSchemas().values() )
        {
            str.append( s.getName() ).append( '\n' );
            for ( Api.Field f : s.getFields() )
            {
                str.append( '\t' );
                if ( f.getRequired() != null )
                {
                    str.append( f.getRequired() ? "! " : "? " );
                }
                else
                    str.append( "  " );
                str.append( printName( f.getType() ) );
                str.append( " " ).append( f.getName() ).append( '\n' );
            }
        }
        return str.toString();
    }

    private static String printName( Api.Schema schema )
    {
        if ( schema == null )
            return "";
        String name = schema.getName().isEmpty() ? schema.getSource().getSimpleName() : schema.getName();
        return schema.getHint() == null ? name : name + " (" + schema.getHint() + ")";
    }
}
