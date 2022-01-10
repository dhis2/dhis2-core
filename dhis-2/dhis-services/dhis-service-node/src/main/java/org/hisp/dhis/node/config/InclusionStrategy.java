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
package org.hisp.dhis.node.config;

import java.util.Collection;

import org.apache.commons.lang3.ObjectUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface InclusionStrategy
{
    <T> boolean include( T object );

    enum Include
        implements
        InclusionStrategy
    {
        /**
         * Inclusion strategy that includes all objects.
         */
        ALWAYS,

        /**
         * Inclusion strategy that only includes non null objects.
         */
        NON_NULL
        {
            @Override
            public <T> boolean include( T object )
            {
                return object != null;
            }
        },

        /**
         * Inclusion strategy that only includes non empty objects: -
         */
        NON_EMPTY
        {
            @Override
            public <T> boolean include( T object )
            {
                if ( object == null )
                {
                    return false;
                }

                if ( Collection.class.isAssignableFrom( object.getClass() ) )
                {
                    return !((Collection<?>) object).isEmpty();
                }
                else if ( String.class.isAssignableFrom( object.getClass() ) )
                {
                    return !ObjectUtils.isEmpty( object );
                }

                return true;
            }
        };

        @Override
        public <T> boolean include( T object )
        {
            return true;
        }
    }
}
