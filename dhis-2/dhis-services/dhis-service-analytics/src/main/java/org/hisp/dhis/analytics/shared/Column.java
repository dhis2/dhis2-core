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
package org.hisp.dhis.analytics.shared;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.wrap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.common.ValueType;

/**
 * Simple representation of a column that used by different analytics components
 * and flows. Its main purpose is to encapsulate common and relevant attributes
 * of a column.
 *
 * @author maikel arabori
 */
@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
public class Column implements Comparable<Column>
{

    /**
     * The column value.
     */
    private final String value;

    /**
     * The column data type.
     */
    private final ColumnDataType type;

    /**
     * The column alias.
     */
    private final String alias;

    /**
     * Flag to show/hide column.
     */
    private final boolean hidden;

    /**
     * Flag to indicate if this column is a metadata column or not.
     */
    private final boolean meta;

    /**
     * This method will find and return the value type associated with the
     * current data type set.
     *
     * @return the respective value type or null if the column has no data type
     *         set
     */
    public ValueType valueType()
    {
        if ( type != null )
        {
            // TODO: Implement it correctly for each type
            switch ( type )
            {
            case DOUBLE:
                return ValueType.NUMBER;
            default:
                return ValueType.TEXT;
            }
        }

        return null;
    }

    @Override
    public int compareTo( final Column column )
    {
        return new CompareToBuilder().append( this.alias, column.alias ).toComparison();
    }

    @Override
    public String toString()
    {
        return value + (isBlank( alias ) ? EMPTY : " AS " + wrap( alias, "\"" ));
    }
}
