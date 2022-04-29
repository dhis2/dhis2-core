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
package org.hisp.dhis.analytics.event.data;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.util.AnalyticsSqlUtils;

@Getter
@AllArgsConstructor( access = AccessLevel.PRIVATE )
class ColumnAndAlias
{

    public static final ColumnAndAlias EMPTY = ColumnAndAlias.ofColumn( "" );

    private final String column;

    private final String alias;

    static ColumnAndAlias ofColumn( String column )
    {
        return ofColumnAndAlias( column, null );
    }

    static ColumnAndAlias ofColumnAndAlias( String column, String alias )
    {
        return new ColumnAndAlias( column, alias );
    }

    public String asSql()
    {
        if ( StringUtils.isNotEmpty( alias ) )
        {
            return String.join( " as ", column, getQuotedAlias() );
        }
        else
        {
            return column;
        }
    }

    public String getQuotedAlias()
    {
        return Optional.ofNullable( alias )
            .map( AnalyticsSqlUtils::quote )
            .orElse( null );
    }
}
