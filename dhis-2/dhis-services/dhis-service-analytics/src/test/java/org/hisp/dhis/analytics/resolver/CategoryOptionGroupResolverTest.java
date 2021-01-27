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

import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionGroup;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupStore;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.random.BeanRandomizer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class CategoryOptionGroupResolverTest
{
    @Mock
    private CategoryOptionGroupStore categoryOptionGroupStore;

    @Mock
    private ExpressionService expressionService;

    @Mock
    private CategoryOptionComboStore categoryOptionComboStore;

    private ExpressionResolver resolver;

    private BeanRandomizer beanRandomizer;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private String elem1;

    private String elem2;

    private String elem3;

    private int COCS_IN_COG = 2;

    @Before
    public void setUp()
    {
        elem1 = CodeGenerator.generateUid();
        elem2 = CodeGenerator.generateUid();
        elem3 = CodeGenerator.generateUid();
        resolver = new CategoryOptionGroupResolver( categoryOptionGroupStore, categoryOptionComboStore,
            expressionService );
        beanRandomizer = new BeanRandomizer();
    }

    /**
     * case: #{DEUID.COGUID.AOCUID} resolves to: #{DEUID.COCUID1.AOCUID} +
     * #{DEUID.COCUID2.AOCUID} + #{DEUID.COCUID3.AOCUID}
     */
    @Test
    public void verifySecondElementIsCogThirdElementIsAoc()
    {
        DimensionalItemId dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, elem1,
            elem2, elem3 );

        CategoryOptionGroup categoryOptionGroup = createCategoryOptionGroup( 'A' );

        // #{DEUID.COGUID.AOCUID}
        String exp = createIndicatorExp();

        when( expressionService.getExpressionDimensionalItemIds( exp, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        when( categoryOptionGroupStore.getByUid( elem2 ) ).thenReturn( categoryOptionGroup );

        List<CategoryOptionCombo> cocs = beanRandomizer.randomObjects( CategoryOptionCombo.class, COCS_IN_COG );

        when( categoryOptionComboStore.getCategoryOptionCombosByGroupUid( categoryOptionGroup.getUid() ) )
            .thenReturn( cocs );
        when( categoryOptionComboStore.getByUid( elem3 ) )
            .thenReturn( beanRandomizer.randomObject( CategoryOptionCombo.class ) );

        String expression = resolver.resolve( exp );
        // split resolved expression into a List of Strings
        List<String> expressionList = Arrays.asList( expression.split( "\\+" ) );
        assertEquals( COCS_IN_COG, expressionList.size() );

        collectionsHaveIdenticalValuesIgnoreOrder( expressionList, buildExpectedExpression( elem1, cocs, elem3 ) );
    }

    /**
     * case: #{DEUID.COGUID.COG2UID} resolves to:
     *
     */
    @Test
    public void verifySecondElementIsCogThirdElementIsCog()
    {
        DimensionalItemId dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, elem1,
            elem2, elem3 );

        CategoryOptionGroup categoryOptionGroup1 = createCategoryOptionGroup( 'A' );
        CategoryOptionGroup categoryOptionGroup2 = createCategoryOptionGroup( 'B' );

        // #{DEUID.COGUID.COGUID}
        String exp = createIndicatorExp();

        when( expressionService.getExpressionDimensionalItemIds( exp, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        when( categoryOptionGroupStore.getByUid( elem2 ) ).thenReturn( categoryOptionGroup1 );
        when( categoryOptionGroupStore.getByUid( elem3 ) ).thenReturn( categoryOptionGroup2 );

        List<CategoryOptionCombo> cocs1 = beanRandomizer.randomObjects( CategoryOptionCombo.class, COCS_IN_COG );
        List<CategoryOptionCombo> cocs2 = beanRandomizer.randomObjects( CategoryOptionCombo.class, COCS_IN_COG );

        when( categoryOptionComboStore.getCategoryOptionCombosByGroupUid( categoryOptionGroup1.getUid() ) )
            .thenReturn( cocs1 );
        when( categoryOptionComboStore.getCategoryOptionCombosByGroupUid( categoryOptionGroup2.getUid() ) )
            .thenReturn( cocs2 );

        String expression = resolver.resolve( exp );
        // split resolved expression into a List of Strings
        List<String> expressionList = Arrays.asList( expression.split( "\\+" ) );
        assertEquals( COCS_IN_COG * 2, expressionList.size() );

        collectionsHaveIdenticalValuesIgnoreOrder( expressionList, buildExpectedExpression( elem1, cocs1, cocs2 ) );
    }

    /**
     * case: #{DEUID.COGUID.COG2UID} resolves to:
     *
     */
    @Test
    public void verifySecondElementIsCocThirdElementIsCog()
    {
        DimensionalItemId dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, elem1,
            elem2, elem3 );

        CategoryOptionGroup categoryOptionGroup1 = createCategoryOptionGroup( 'A' );

        // #{DEUID.COCUID.COGUID}
        String exp = createIndicatorExp();

        when( expressionService.getExpressionDimensionalItemIds( exp, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        when( categoryOptionGroupStore.getByUid( elem3 ) ).thenReturn( categoryOptionGroup1 );

        List<CategoryOptionCombo> cocs1 = beanRandomizer.randomObjects( CategoryOptionCombo.class, COCS_IN_COG );

        when( categoryOptionComboStore.getCategoryOptionCombosByGroupUid( categoryOptionGroup1.getUid() ) )
            .thenReturn( cocs1 );

        String expression = resolver.resolve( exp );

        // split resolved expression into a List of Strings
        List<String> expressionList = Arrays.asList( expression.split( "\\+" ) );
        assertEquals( COCS_IN_COG, expressionList.size() );

        collectionsHaveIdenticalValuesIgnoreOrder( expressionList, buildExpectedExpression( elem1, elem2, cocs1 ) );
    }

    @Test
    public void verifySecondElementIsCocThirdElementIsAoc()
    {
        DimensionalItemId dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND, elem1,
            elem2, elem3 );

        // #{DEUID.COCUID.AOCUID}
        String exp = createIndicatorExp();

        when( expressionService.getExpressionDimensionalItemIds( exp, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        String expression = resolver.resolve( exp );

        // split resolved expression into a List of Strings
        List<String> expressionList = Arrays.asList( expression.split( "\\+" ) );
        assertEquals( 1, expressionList.size() );

        // original expression is returned
        collectionsHaveIdenticalValuesIgnoreOrder( expressionList, Lists.newArrayList( exp ) );
    }

    @Test
    public void verifyExpressionIsNotDataElementOperand()
    {
        DimensionalItemId dimensionalItemId = new DimensionalItemId( DimensionItemType.DATA_ELEMENT, elem1 );

        // #{DEUID}
        String exp = "#{" + elem1 + "}";

        when( expressionService.getExpressionDimensionalItemIds( exp, INDICATOR_EXPRESSION ) )
            .thenReturn( Sets.newHashSet( dimensionalItemId ) );

        String expression = resolver.resolve( exp );

        // split resolved expression into a List of Strings
        List<String> expressionList = Arrays.asList( expression.split( "\\+" ) );
        assertEquals( 1, expressionList.size() );

        // original expression is returned
        collectionsHaveIdenticalValuesIgnoreOrder( expressionList, Lists.newArrayList( exp ) );
    }

    private void collectionsHaveIdenticalValuesIgnoreOrder( List<String> listA, List<String> listB )
    {

        for ( String s : listA )
        {
            assertTrue( listB.contains( s ) );
        }
    }

    private String createIndicatorExp()
    {
        return String.format( "#{%s.%s.%s}", elem1, elem2, elem3 );
    }

    private String buildOperand( String s1, String s2, String s3 )
    {

        return "#{" + s1 + "." + s2 + "." + s3 + "}";
    }

    private String buildOperand( String s1, String s2 )
    {

        return "#{" + s1 + "." + s2 + "}";
    }

    private List<String> buildExpectedExpression( String dataElementUid, List<CategoryOptionCombo> cocs, String aocUid )
    {
        List<String> expressionAsList = new ArrayList<>( cocs.size() );
        for ( CategoryOptionCombo coc : cocs )
        {
            expressionAsList.add( buildOperand( dataElementUid, coc.getUid(), aocUid ) );
        }
        return expressionAsList;
    }

    private List<String> buildExpectedExpression( String dataElementUid, List<CategoryOptionCombo> cocs1,
        List<CategoryOptionCombo> cocs2 )
    {
        return Stream.concat( cocs1.stream(), cocs2.stream() ).map( c -> buildOperand( dataElementUid, c.getUid() ) )
            .collect( Collectors.toList() );

    }

    private List<String> buildExpectedExpression( String dataElementUid, String cocUid, List<CategoryOptionCombo> cocs )
    {
        List<String> expressionAsList = new ArrayList<>( cocs.size() );
        for ( CategoryOptionCombo coc : cocs )
        {
            expressionAsList.add( buildOperand( dataElementUid, coc.getUid() ) );
        }
        expressionAsList.add( buildOperand( dataElementUid, cocUid ) );
        return expressionAsList;
    }
}