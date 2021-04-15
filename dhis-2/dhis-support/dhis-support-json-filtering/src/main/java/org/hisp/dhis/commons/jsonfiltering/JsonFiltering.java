/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.hisp.dhis.commons.jsonfiltering.context.provider.JsonFilteringContextProvider;
import org.hisp.dhis.commons.jsonfiltering.filter.JsonFilteringPropertyFilter;
import org.hisp.dhis.commons.jsonfiltering.filter.JsonFilteringPropertyFilterMixin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Provides various way of registering a {@link JsonFilteringPropertyFilter}
 * with a Jackson ObjectMapper.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class JsonFiltering
{

    /**
     * Initialize a @{@link JsonFilteringPropertyFilter} with a specific context
     * provider.
     *
     * @param mapper the Jackson Object Mapper
     * @param contextProvider the context provider to use
     * @return object mapper, mainly for convenience
     * @throws IllegalStateException if the filter was unable to be registered
     */
    public static ObjectMapper init( ObjectMapper mapper, JsonFilteringContextProvider contextProvider )
        throws IllegalStateException
    {
        return init( mapper, new JsonFilteringPropertyFilter( contextProvider ) );
    }

    /**
     * Initialize a @{@link JsonFilteringPropertyFilter} with a specific
     * property filter.
     *
     * @param mapper the Jackson Object Mapper
     * @param filter the property filter
     * @return object mapper, mainly for convenience
     * @throws IllegalStateException if the filter was unable to be registered
     */
    @SuppressWarnings( "deprecation" )
    public static ObjectMapper init( ObjectMapper mapper, JsonFilteringPropertyFilter filter )
        throws IllegalStateException
    {
        FilterProvider filterProvider = mapper.getSerializationConfig().getFilterProvider();
        SimpleFilterProvider simpleFilterProvider;

        if ( filterProvider instanceof SimpleFilterProvider )
        {
            simpleFilterProvider = (SimpleFilterProvider) filterProvider;
        }
        else if ( filterProvider == null )
        {
            simpleFilterProvider = new SimpleFilterProvider();
            mapper.setFilters( simpleFilterProvider );
        }
        else
        {
            throw new IllegalStateException( "Unable to register json-filtering filter with FilterProvider of type "
                + filterProvider.getClass().getName() + ".  You'll have to register the filter manually" );

        }

        simpleFilterProvider.addFilter( JsonFilteringPropertyFilter.FILTER_ID, filter );
        mapper.addMixIn( Object.class, JsonFilteringPropertyFilterMixin.class );

        return mapper;
    }

}
