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
package org.hisp.dhis.commons.jsonfiltering.context.provider;

import org.hisp.dhis.commons.jsonfiltering.context.JsonFilteringContext;
import org.hisp.dhis.commons.jsonfiltering.filter.JsonFilteringPropertyFilter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * Used for supplying a @{@link JsonFilteringPropertyFilter} with a way to
 * retrieve a context.
 */
public interface JsonFilteringContextProvider
{

    /**
     * Get the context.
     *
     * @param beanClass the class of the top-level bean being filtered
     * @return context
     */
    JsonFilteringContext getContext( Class<?> beanClass );

    /**
     * Hook method to enable/disable filtering.
     *
     * @return ture if enabled, false if not
     */
    boolean isFilteringEnabled();

    // Hook method for custom included serialization
    void serializeAsIncludedField( Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer )
        throws Exception;

    // Hook method for custom excluded serialization
    void serializeAsExcludedField( Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer )
        throws Exception;
}
