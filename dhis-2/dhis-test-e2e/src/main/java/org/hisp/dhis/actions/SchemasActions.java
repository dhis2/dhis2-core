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
package org.hisp.dhis.actions;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.schemas.Schema;
import org.hisp.dhis.dto.schemas.SchemaProperty;
import org.hisp.dhis.helpers.QueryParamsBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SchemasActions
    extends RestApiActions
{
    public SchemasActions()
    {
        super( "/schemas" );
    }

    public List<SchemaProperty> getRequiredProperties( String resource )
    {
        List<SchemaProperty> list = get( resource ).extractList( "properties", SchemaProperty.class );

        return list.stream()
            .filter( (schemaProperty -> schemaProperty.isRequired()) )
            .collect( Collectors.toList() );
    }

    public Schema getSchema( String resource )
    {
        return get( resource ).extractObject( "", Schema.class );
    }

    public ApiResponse validateObjectAgainstSchema( String resource, Object obj )
    {
        return post( resource, obj );
    }

    public String findSchemaPropertyByKlassName( String klass, String property )
    {
        return findSchemaPropertyByKnownProperty( property, "klass", klass );
    }

    public String findSchemaPropertyByKnownProperty( String propertyToFind, String knownPropertyName,
        String knownPropertyValue )
    {
        return get( "",
            new QueryParamsBuilder().add( String.format( "fields=%s,%s", propertyToFind, knownPropertyName ) ) )
                .extractString(
                    String.format( "schemas.find{it.%s == '%s'}.%s", knownPropertyName, knownPropertyValue,
                        propertyToFind ) );
    }
}
