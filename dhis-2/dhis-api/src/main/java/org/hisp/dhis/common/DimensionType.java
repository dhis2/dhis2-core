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

/**
 * @author Lars Helge Overland
 */
public enum DimensionType
{
    DATA_X( 0 ),
    PROGRAM_DATA_ELEMENT,
    PROGRAM_ATTRIBUTE,
    PROGRAM_INDICATOR,
    DATA_COLLAPSED,
    CATEGORY_OPTION_COMBO( 1 ),
    ATTRIBUTE_OPTION_COMBO( 2 ),
    PERIOD,
    ORGANISATION_UNIT,
    CATEGORY_OPTION_GROUP_SET,
    DATA_ELEMENT_GROUP_SET,
    ORGANISATION_UNIT_GROUP_SET,
    ORGANISATION_UNIT_GROUP,
    CATEGORY,
    OPTION_GROUP_SET,
    VALIDATION_RULE,
    STATIC,
    ORGANISATION_UNIT_LEVEL;
    
    private static final int LAST_ORDER = 999;
    
    private int order = LAST_ORDER;
    
    DimensionType()
    {
    }
    
    DimensionType( int order )
    {
        this.order = order;
    }
    
    public int getOrder()
    {
        return order;
    }
}
