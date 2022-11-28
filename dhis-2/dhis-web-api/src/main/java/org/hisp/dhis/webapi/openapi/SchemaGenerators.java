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
import static org.hisp.dhis.webapi.openapi.Property.getProperties;

import java.lang.reflect.Type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.OpenApi;

/**
 * Home for {@link Api.SchemaGenerator} implementations.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class SchemaGenerators
{
    /**
     * A "virtual" UID type that is "context-sensitive" and points to a UID of
     * the current {@link Api.Endpoint}'s
     * {@link org.hisp.dhis.common.OpenApi.EntityType}.
     *
     * In other words by using this type in {@link OpenApi.Param#value()} the
     * annotated parameter becomes a UID string of the controllers' entity type.
     */
    @NoArgsConstructor
    public static final class UID implements Api.SchemaGenerator
    {
        @Override
        public Api.Schema generate( Api.Endpoint endpoint, Type source, Class<?>... args )
        {
            Class<?> uidOf = args.length == 0 ? endpoint.getEntityType() : args[0];
            return Api.Schema.uid( uidOf );
        }
    }

    /**
     * A "virtual" property name enumeration type. It creates an OpenAPI
     * {@code enum} string schema containing all valid property names for the
     * target type. The target type is either the actual type substitute for the
     * {@link org.hisp.dhis.common.OpenApi.EntityType} or the first argument
     * type.
     */
    @NoArgsConstructor
    public static final class PropertyNames implements Api.SchemaGenerator
    {

        @Override
        public Api.Schema generate( Api.Endpoint endpoint, Type source, Class<?>... args )
        {
            Class<?> ofType = args.length == 0 ? endpoint.getEntityType() : args[0];
            return Api.Schema.enumeration( PropertyNames.class, ofType, getProperties( ofType ).stream()
                .map( Property::getName )
                .collect( toList() ) );
        }
    }
}
