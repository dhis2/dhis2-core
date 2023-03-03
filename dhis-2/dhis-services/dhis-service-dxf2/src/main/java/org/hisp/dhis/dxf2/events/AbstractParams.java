/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.events;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;

/**
 * @author Luca Cambi <luca@dhis2.org>
 *         <p>
 *         Abstract Params class to store a {@link Param} set which maps to the
 *         set of fields in a request. It includes a builder to initialize an
 *         empty or all (*) params list and to create a new Params
 *         implementation instance in case the params list has mutated
 */
@AllArgsConstructor
public abstract class AbstractParams
{
    protected final Set<Param> params;

    public boolean hasIncluded( Param param )
    {
        return params.contains( param );
    }

    public static class ParamsBuilder<T extends AbstractParams>
    {
        protected Set<Param> params;

        public ParamsBuilder<T> empty()
        {
            this.params = EnumSet.noneOf( Param.class );
            return this;
        }

        public ParamsBuilder<T> all()
        {
            return this;
        }

        public ParamsBuilder<T> with( Param param, boolean isIncluded )
        {
            this.params = isIncluded ? include( EnumSet.of( param ) ) : exclude( EnumSet.of( param ) );
            return this;
        }

        public ParamsBuilder<T> with( Map<Param, Boolean> paramsToInclusion )
        {
            paramsToInclusion.forEach(
                ( key, value ) -> this.params = Boolean.TRUE.equals( value ) ? include( EnumSet.of( key ) )
                    : exclude( EnumSet.of( key ) ) );
            return this;
        }

        EnumSet<Param> include( Set<Param> params )
        {
            return Stream.concat( this.params.stream(), params.stream() )
                .collect( Collectors.toCollection( () -> EnumSet.noneOf( Param.class ) ) );
        }

        EnumSet<Param> exclude( Set<Param> params )
        {
            return this.params.stream().filter( p -> !params.contains( p ) )
                .collect( Collectors.toCollection( () -> EnumSet.noneOf( Param.class ) ) );
        }

        public T build()
        {
            return null;
        }
    }
}
