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
package org.hisp.dhis.webapi.controller.event.mapper;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * Order parameter container to use within services.
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Value
public class OrderParam
{
    private final String field;

    private final SortDirection direction;

    @Getter
    @AllArgsConstructor
    public enum SortDirection
    {
        ASC( "asc", false ),
        DESC( "desc", false ),
        IASC( "iasc", true ),
        IDESC( "idesc", true );

        private static final SortDirection DEFAULT_SORTING_DIRECTION = ASC;

        private final String value;

        private final boolean ignoreCase;

        public static SortDirection of( String value )
        {
            return of( value, DEFAULT_SORTING_DIRECTION );
        }

        public static SortDirection of( String value, SortDirection defaultSortingDirection )
        {
            return Arrays.stream( values() )
                .filter( sortDirection -> sortDirection.getValue().equalsIgnoreCase( value ) )
                .findFirst()
                .orElse( defaultSortingDirection );
        }

        public boolean isAscending()
        {
            return this.equals( ASC ) || this.equals( IASC );
        }
    }
}
