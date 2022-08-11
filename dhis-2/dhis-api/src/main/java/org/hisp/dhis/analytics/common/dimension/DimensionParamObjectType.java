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
package org.hisp.dhis.analytics.common.dimension;

import java.util.Arrays;

import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;

/**
 * map DimensionType and DimensionItemType into a single enum
 */
public enum DimensionParamObjectType
{

    // Types that are common
    PROGRAM_DATA_ELEMENT( DimensionType.PROGRAM_DATA_ELEMENT, DimensionItemType.PROGRAM_DATA_ELEMENT ),
    PROGRAM_ATTRIBUTE( DimensionType.PROGRAM_ATTRIBUTE, DimensionItemType.PROGRAM_ATTRIBUTE ),
    PROGRAM_INDICATOR( DimensionType.PROGRAM_INDICATOR, DimensionItemType.PROGRAM_INDICATOR ),
    PERIOD( DimensionType.PERIOD, DimensionItemType.PERIOD ),
    ORGANISATION_UNIT( DimensionType.ORGANISATION_UNIT, DimensionItemType.ORGANISATION_UNIT ),
    ORGANISATION_UNIT_GROUP( DimensionType.ORGANISATION_UNIT_GROUP, DimensionItemType.ORGANISATION_UNIT_GROUP ),

    // Types that are proper of a DimensionalObject
    DATA_X( DimensionType.DATA_X ),
    DATA_COLLAPSED( DimensionType.DATA_COLLAPSED ),
    CATEGORY_OPTION_COMBO( DimensionType.CATEGORY_OPTION_COMBO ),
    ATTRIBUTE_OPTION_COMBO( DimensionType.ATTRIBUTE_OPTION_COMBO ),
    CATEGORY_OPTION_GROUP_SET( DimensionType.CATEGORY_OPTION_GROUP_SET ),
    DATA_ELEMENT_GROUP_SET( DimensionType.DATA_ELEMENT_GROUP_SET ),
    ORGANISATION_UNIT_GROUP_SET( DimensionType.ORGANISATION_UNIT_GROUP_SET ),
    CATEGORY( DimensionType.CATEGORY ),
    OPTION_GROUP_SET( DimensionType.OPTION_GROUP_SET ),
    VALIDATION_RULE( DimensionType.VALIDATION_RULE ),
    STATIC( DimensionType.STATIC ),
    ORGANISATION_UNIT_LEVEL( DimensionType.ORGANISATION_UNIT_LEVEL ),

    // Types that are proper of a QueryItem
    DATA_ELEMENT( DimensionItemType.DATA_ELEMENT ),
    DATA_ELEMENT_OPERAND( DimensionItemType.DATA_ELEMENT_OPERAND ),
    INDICATOR( DimensionItemType.INDICATOR ),
    REPORTING_RATE( DimensionItemType.REPORTING_RATE ),
    CATEGORY_OPTION( DimensionItemType.CATEGORY_OPTION ),
    OPTION_GROUP( DimensionItemType.OPTION_GROUP ),
    DATA_ELEMENT_GROUP( DimensionItemType.DATA_ELEMENT_GROUP ),
    CATEGORY_OPTION_GROUP( DimensionItemType.CATEGORY_OPTION_GROUP ),

    STATIC_DIMENSION( null, null );

    private final DimensionType dimensionType;

    private final DimensionItemType dimensionItemType;

    DimensionParamObjectType( DimensionType dimensionType, DimensionItemType dimensionItemType )
    {
        this.dimensionType = dimensionType;
        this.dimensionItemType = dimensionItemType;
    }

    DimensionParamObjectType( DimensionType dimensionType )
    {
        this.dimensionType = dimensionType;
        this.dimensionItemType = null;
    }

    DimensionParamObjectType( DimensionItemType dimensionItemType )
    {
        this.dimensionItemType = dimensionItemType;
        this.dimensionType = null;
    }

    public static DimensionParamObjectType byForeignType( DimensionType dimensionType )
    {
        return Arrays.stream( values() )
            .filter( dimensionParamObjectType -> dimensionType == dimensionParamObjectType.dimensionType )
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException( "Unable to detect DimensionParamType from " + dimensionType ) );
    }

    public static DimensionParamObjectType byForeignType( DimensionItemType dimensionItemType )
    {
        return Arrays.stream( values() )
            .filter( dimensionParamObjectType -> dimensionItemType == dimensionParamObjectType.dimensionItemType )
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException( "Unable to detect DimensionParamType from " + dimensionItemType ) );
    }
}
