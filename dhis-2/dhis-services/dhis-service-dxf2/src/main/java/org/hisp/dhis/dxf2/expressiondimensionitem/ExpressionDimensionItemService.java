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
package org.hisp.dhis.dxf2.expressiondimensionitem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.springframework.stereotype.Service;

/**
 * Parsing the expression of ExpressionDimensionItem, provides collection of
 * BaseDimensionalItemObjects.
 */
@Service
@RequiredArgsConstructor
public class ExpressionDimensionItemService
{
    //valid expressions fbfJHSPpUQD, fbfJHSPpUQD.pq2XI5kz2BY, fbfJHSPpUQD.pq2XI5kz2BY.pq2XI5kz2BZ
    public static final Pattern pattern = Pattern
        .compile( "[a-zA-Z0-9]{11}[.]?[a-zA-Z0-9]{0,11}[.]?[a-zA-Z0-9]{0,11}" );

    private final IdentifiableObjectManager manager;

    /**
     * Provides collection of selected item types inside the expression
     *
     * @param dataDimensionItem {@link IdentifiableObjectManager} expression
     *        dimension item
     * @return collection of selected item types
     */
    public List<BaseDimensionalItemObject> getExpressionItems( DataDimensionItem dataDimensionItem )
    {
        if ( dataDimensionItem.getExpressionDimensionItem() == null )
        {
            return new ArrayList<>();
        }

        String expression = dataDimensionItem.getExpressionDimensionItem().getExpression();

        List<String> expressionTokens = getExpressionTokens( pattern, expression );

        List<BaseDimensionalItemObject> baseDimensionalItemObjects = new ArrayList<>();

        expressionTokens.forEach( et -> {
            String[] uids = et.split( Pattern.quote( "." ) );
            if ( uids.length > 1 )
            {
                DataElementOperand deo = new DataElementOperand( manager.get( DataElement.class, uids[0] ),
                    manager.get( CategoryOptionCombo.class, uids[1] ) );
                baseDimensionalItemObjects.add( deo );
            }
            else if ( uids.length > 0 )
            {
                baseDimensionalItemObjects.add( manager.get( DataElement.class, uids[0] ) );
            }
        } );

        return baseDimensionalItemObjects;
    }

    /**
     * Provides expression validation
     *
     * @param expression or indicator of expression dimension item
     * @return true when expression is valid
     */
    public boolean isValidExpressionItems( String expression )
    {
        if( expression.chars().allMatch( Character::isDigit ) )
        {
            return true;
        }

        List<String> expressionTokens = getExpressionTokens( pattern, expression );

        return expressionTokens.stream().allMatch( et -> {
            String[] uids = et.split( Pattern.quote( "." ) );
            if ( uids.length > 2 )
            {
                return false;
            }
            else if ( uids.length > 1 )
            {
                IdentifiableObject de = manager.get( DataElement.class, uids[0] );

                IdentifiableObject coc = manager.get( CategoryOptionCombo.class, uids[1] );

                return de != null && coc != null;

            }
            else if ( uids.length > 0 )
            {
                IdentifiableObject de = manager.get( DataElement.class, uids[0] );

                return de != null;
            }

            return false;
        } );
    }

    /**
     * Expression parser for expression tokens of indicator (
     * data_element.category_option_combo or data_element only )
     *
     * @param pattern compiled Patter object of regular expression
     * @param expression expression of indicator
     * @return collection of tokens
     */
    public List<String> getExpressionTokens( Pattern pattern, String expression )
    {
        List<String> expressionTokens = new ArrayList<>();

        pattern.matcher( expression )
            .results()
            .map( mr -> mr.group( 0 ) )
            .forEach( expressionTokens::add );

        return expressionTokens;
    }
}
