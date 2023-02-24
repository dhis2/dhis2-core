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

import org.apache.commons.lang3.StringUtils;

/**
 * The class responsibility is to generate sql statement with nullif function
 * like nullif(ax.\"w75KJ2mc4zz\",'') as \"w75KJ2mc4zz\"
 */
final class ColumnWithNullIfAndAlias extends ColumnAndAlias
{
    /**
     * private constructor
     *
     * @param column db table column name.
     * @param alias db table column alias name.
     */
    private ColumnWithNullIfAndAlias( String column, String alias )
    {
        super( column, alias );
    }

    /**
     * Builder method to create an instance of this class.
     *
     * @param column db table column name.
     * @param alias db table column alias name.
     * @return ColumnWithNullIfAndAlias instance.
     */
    static ColumnWithNullIfAndAlias ofColumnWithNullIfAndAlias( String column, String alias )
    {
        return new ColumnWithNullIfAndAlias( column, alias );
    }

    /**
     * Generate sql snippet with nullif function.
     *
     * @return sql snippet with nullif.
     */
    @Override
    public String asSql()
    {
        if ( StringUtils.isNotEmpty( alias ) )
        {
            return String.join( " as ", "nullif(" + column + ",'')", getQuotedAlias() );
        }
        else
        {
            return column;
        }
    }
}
