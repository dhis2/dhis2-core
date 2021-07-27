/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.DhisConvenienceTest.createCategoryOption;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

/**
 * @author Dusan Bernat
 */
public class CategoryOptionResolverTest
{
    @Mock
    private CategoryOptionStore categoryOptionStore;

    @Mock
    private ExpressionService expressionService;

    private ExpressionResolver resolver;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private String uid1;

    private String uid2;

    private String uid3;

    private CategoryOptionCombo coc1;

    private CategoryOptionCombo coc2;

    private CategoryOptionCombo coc3;

    DimensionalItemId dimensionalItemId;

    private static final String CATEGORY_OPTION_PREFIX = "co:";

    @Before
    public void setUp()
    {
        uid1 = CodeGenerator.generateUid();

        uid2 = CodeGenerator.generateUid();

        uid3 = CodeGenerator.generateUid();

        CategoryOption categoryOption = createCategoryOption( 'A' );

        coc1 = createCategoryOptionCombo( 'X' );

        categoryOption.addCategoryOptionCombo( coc1 );

        coc2 = createCategoryOptionCombo( 'Y' );

        categoryOption.addCategoryOptionCombo( coc2 );

        coc3 = createCategoryOptionCombo( 'Z' );

        categoryOption.addCategoryOptionCombo( coc3 );

        resolver = new CategoryOptionResolver( expressionService, categoryOptionStore );

        when( categoryOptionStore.getByUid( anyString() ) ).thenReturn( categoryOption );
    }

    @Test
    public void verifyExpressionIsResolvedProperly()
    {
        // arrange
        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, uid1,
            CATEGORY_OPTION_PREFIX + uid2, uid3, 0, createIndicatorExpression() );

        String expression = createIndicatorExpression();

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertEquals( expectedResolvedIndicatorExpression( coc1.getUid(), coc2.getUid(), coc3.getUid() ),
            resolvedExpression );
    }

    @Test
    public void verifyExpressionIsNotResolvedWhenDimensionalItemIdHasNoItem()
    {
        // arrange
        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, uid1,
            CATEGORY_OPTION_PREFIX + uid2, uid3, 0, createIndicatorExpression() );

        String expression = createIndicatorExpression();

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertEquals( expectedResolvedIndicatorExpression( coc1.getUid(), coc2.getUid(), coc3.getUid() ),
            resolvedExpression );
    }

    @Test
    public void verifyExpressionIsNotResolvedWhenCoPrefixNotInUid1()
    {
        // arrange
        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, uid1,
            uid2, uid3, 0, createIndicatorExpression() );

        String expression = createIndicatorExpression();

        when( expressionService.getExpressionDimensionalItemIds( expression, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        // act

        String resolvedExpression = resolver.resolve( expression );

        // assert

        assertEquals( expression, resolvedExpression );
    }

    @Test
    public void verifyExpressionIsNotResolvedWhenExpressionIsNotValid()
    {
        // arrange
        dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, uid1,
            uid2, uid3, 0, createIndicatorExpression() );

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
        return String.format( "#{%s.co:%s.%s}", uid1, uid2, uid3 );
    }

    private String expectedResolvedIndicatorExpression( String coc1_uid, String coc2_uid, String coc3_uid )
    {
        return String.format( "(#{%s.%s.%s}+#{%s.%s.%s}+#{%s.%s.%s})",
            uid1, coc1_uid, uid3,
            uid1, coc2_uid, uid3,
            uid1, coc3_uid, uid3 );
    }
}
