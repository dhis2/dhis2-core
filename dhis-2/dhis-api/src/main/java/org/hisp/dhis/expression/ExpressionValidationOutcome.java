package org.hisp.dhis.expression;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
public enum ExpressionValidationOutcome
{
    VALID( "valid" ), 
    EXPRESSION_IS_EMPTY( "expression_is_empty"),
    DIMENSIONAL_ITEM_OBJECT_DOES_NOT_EXIST( "dimensional_item_object_does_not_exist" ),
    CONSTANT_DOES_NOT_EXIST( "constant_does_not_exist"),
    ORG_UNIT_GROUP_DOES_NOT_EXIST( "org_unit_group_does_not_exist"),
    EXPRESSION_IS_NOT_WELL_FORMED( "expression_is_not_well_formed");
    
    private final String key;
    
    ExpressionValidationOutcome( String key )
    {
        this.key = key;
    }
    
    public boolean isValid()
    {
        return this == VALID;
    }
    
    public String getKey()
    {
        return key;
    }
}
