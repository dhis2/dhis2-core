package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Arrays;

import static org.apache.commons.lang3.EnumUtils.isValidEnum;

/**
 * @author Jim Grace
 */
public class DimensionalItemId
{
    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    private DimensionItemType dimensionItemType;

    private String[] ids;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DimensionalItemId( DimensionItemType dimensionItemType, String[] ids )
    {
        this.dimensionItemType = dimensionItemType;
        this.ids = ids;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasValidIds()
    {
        switch ( dimensionItemType )
        {
            case DATA_ELEMENT:
            case PROGRAM_INDICATOR:
                return ids.length == 1 && ids[0] != null;

            case DATA_ELEMENT_OPERAND:
                return ids.length == 3 && ids[0] != null;

            case REPORTING_RATE:
                return ids.length == 2 && ids[0] != null && ids[1] != null
                    && isValidEnum( ReportingRateMetric.class, ids[1] );

            case PROGRAM_DATA_ELEMENT:
            case PROGRAM_ATTRIBUTE:
                return ids.length == 2 && ids[0] != null && ids[1] != null;

            default:
                return false;
        }
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        DimensionalItemId that = (DimensionalItemId) o;
        return dimensionItemType == that.dimensionItemType &&
            Arrays.equals( ids, that.ids );
    }

    @Override
    public int hashCode()
    {
        int result = dimensionItemType.hashCode();
        result = 31 * result + Arrays.hashCode( ids );

        return result;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"dimensionItemType\":\"" + dimensionItemType.toString() + "\", " +
            "\"ids\":\"" + Arrays.toString( ids ) +
            '}';
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public DimensionItemType getDimensionItemType()
    {
        return dimensionItemType;
    }

    public String[] getIds()
    {
        return ids;
    }
}
