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
package org.hisp.dhis.fieldfiltering;

import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

/**
 * @author Morten Olav Hansen
 */
public class FieldFilterSimpleBeanPropertyFilter extends SimpleBeanPropertyFilter
{
    private final List<FieldPath> fieldPaths;

    public FieldFilterSimpleBeanPropertyFilter( List<FieldPath> fieldPaths )
    {
        this.fieldPaths = fieldPaths;
    }

    @Override
    protected boolean include( final BeanPropertyWriter writer )
    {
        return true;
    }

    @Override
    protected boolean include( final PropertyWriter writer )
    {
        return true;
    }

    protected boolean include( final PropertyWriter writer, final JsonGenerator jgen )
    {
        for ( FieldPath fieldPath : fieldPaths )
        {
            String path = fieldPath.toFullPath();

            if ( path.startsWith( getPath( writer, jgen ) ) || path.contains( "*" ) )
            {
                return true;
            }
        }

        return false;
    }

    private String getPath( PropertyWriter writer, JsonGenerator jgen )
    {
        StringBuilder nestedPath = new StringBuilder();
        nestedPath.append( writer.getName() );
        JsonStreamContext context = jgen.getOutputContext();

        if ( context != null )
        {
            context = context.getParent();
        }

        for ( ; context != null; context = context.getParent() )
        {
            String name = context.getCurrentName();

            if ( name != null && context.inObject() )
            {
                nestedPath.insert( 0, "." );
                nestedPath.insert( 0, name );
            }
        }

        return nestedPath.toString();
    }

    @Override
    public void serializeAsField( Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer )
        throws Exception
    {
        if ( include( writer, jgen ) )
        {
            writer.serializeAsField( pojo, jgen, provider );
        }
        else if ( !jgen.canOmitFields() )
        { // since 2.3
            writer.serializeAsOmittedField( pojo, jgen, provider );
        }
    }
}
