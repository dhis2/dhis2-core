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
package org.hisp.dhis.analytics.resolver;

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataElementGroup;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupStore;
import org.hisp.dhis.expression.ExpressionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;

/**
 * @author Dusan Bernat
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class DataElementGroupResolverTest
{

    @Mock
    private DataElementGroupStore dataElementGroupStore;

    @Mock
    private ExpressionService expressionService;

    private ExpressionResolver resolver;

    private String uid1;

    private String uid2;

    private String uid3;

    private DataElement de1;

    private DataElement de2;

    private DataElement de3;

    DimensionalItemId dimensionalItemId;

    private static final String DATA_ELEMENT_GROUP_PREFIX = "deGroup:";

    @BeforeEach
    public void setUp()
    {
        uid1 = CodeGenerator.generateUid();

        uid2 = CodeGenerator.generateUid();

        uid3 = CodeGenerator.generateUid();

        de1 = createDataElement( 'X' );

        de2 = createDataElement( 'Y' );

        de3 = createDataElement( 'Z' );

        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );

        dataElementGroup.addDataElement( de1 );

        dataElementGroup.addDataElement( de2 );

        dataElementGroup.addDataElement( de3 );

        resolver = new DataElementGroupResolver( expressionService, dataElementGroupStore );

        when( dataElementGroupStore.getByUid( anyString() ) ).thenReturn( dataElementGroup );
    }

    @Test
    void verifyExpressionIsResolvedProperly()
    {
        // arrange

        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND,
            DATA_ELEMENT_GROUP_PREFIX + uid1,
            uid2, uid3, createIndicatorExpression() );

        String expression = createIndicatorExpression();

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertResolvedExpressionEquals( resolvedExpression, de1.getUid(), de2.getUid(), de3.getUid() );
    }

    @Test
    void verifyExpressionIsNotResolvedWhenDimensionalItemIdHasNoItem()
    {
        // arrange

        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND,
            DATA_ELEMENT_GROUP_PREFIX + uid1, uid2, uid3 );

        String expression = createIndicatorExpression();

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertEquals( expression, resolvedExpression );
    }

    @Test
    void verifyExpressionIsNotResolvedWhenDeGroupPrefixNotInUid0()
    {
        // arrange

        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, uid1,
            uid2, uid3, createIndicatorExpression() );

        String expression = createIndicatorExpression();

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertEquals( expression, resolvedExpression );
    }

    @Test
    void verifyExpressionIsNotResolvedWhenExpressionIsNotValid()
    {
        // arrange

        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, uid1,
            uid2, uid3, createIndicatorExpression() );

        String expression = "lsdjflakjdflkajdslfhaglakujdhfg";

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertEquals( expression, resolvedExpression );
    }

    private String createIndicatorExpression()
    {
        return String.format( "#{deGroup:%s.%s.%s}", uid1, uid2, uid3 );
    }

    private void assertResolvedExpressionEquals( String result, String cocUid1, String cocUid2, String cocUid3 )
    {
        assertTrue( result.equals( formatExpression( cocUid1, cocUid2, cocUid3 ) )
            || result.equals( formatExpression( cocUid1, cocUid3, cocUid2 ) )
            || result.equals( formatExpression( cocUid2, cocUid3, cocUid1 ) )
            || result.equals( formatExpression( cocUid2, cocUid1, cocUid3 ) )
            || result.equals( formatExpression( cocUid3, cocUid1, cocUid2 ) )
            || result.equals( formatExpression( cocUid3, cocUid2, cocUid1 ) ) );
    }

    private String formatExpression( String de1_uid, String de2_uid, String de3_uid )
    {
        return String.format( "(#{%s.%s.%s}+#{%s.%s.%s}+#{%s.%s.%s})",
            de1_uid, uid2, uid3,
            de2_uid, uid2, uid3,
            de3_uid, uid2, uid3 );
    }
}
