package org.hisp.dhis.expression;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.DAYS_SYMBOL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class ExpressionServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    private DataElementCategoryOption categoryOptionA;
    private DataElementCategoryOption categoryOptionB;
    private DataElementCategoryOption categoryOptionC;
    private DataElementCategoryOption categoryOptionD;

    private DataElementCategory categoryA;
    private DataElementCategory categoryB;

    private DataElementCategoryCombo categoryCombo;

    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private DataElement dataElementD;
    private DataElement dataElementE;

    private Period period;

    private OrganisationUnit unitA;
    private OrganisationUnit unitB;
    private OrganisationUnit unitC;

    private DataElementCategoryOptionCombo categoryOptionCombo;
    
    private Constant constantA;
    
    private OrganisationUnitGroup groupA;
    
    private String expressionA;
    private String expressionB;
    private String expressionC;
    private String expressionD;    
    private String expressionE;
    private String expressionF;
    private String expressionG;
    private String expressionH;

    private String descriptionA;
    private String descriptionB;
    
    private Set<DataElement> dataElements = new HashSet<>();

    private Set<DataElementCategoryOptionCombo> optionCombos = new HashSet<>();

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        categoryOptionA = new DataElementCategoryOption( "Under 5" );
        categoryOptionB = new DataElementCategoryOption( "Over 5" );
        categoryOptionC = new DataElementCategoryOption( "Male" );
        categoryOptionD = new DataElementCategoryOption( "Female" );

        categoryService.addDataElementCategoryOption( categoryOptionA );
        categoryService.addDataElementCategoryOption( categoryOptionB );
        categoryService.addDataElementCategoryOption( categoryOptionC );
        categoryService.addDataElementCategoryOption( categoryOptionD );

        categoryA = new DataElementCategory( "Age" );
        categoryB = new DataElementCategory( "Gender" );

        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );

        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );

        categoryCombo = new DataElementCategoryCombo( "Age and gender" );
        categoryCombo.getCategories().add( categoryA );
        categoryCombo.getCategories().add( categoryB );

        categoryService.addDataElementCategoryCombo( categoryCombo );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementE = createDataElement( 'E', categoryCombo );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementE );

        categoryOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        categoryOptionCombo.getId();
        optionCombos.add( categoryOptionCombo );

        period = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 2, 1 ) );

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );

        constantA = new Constant( "ConstantA", 2.0 );
                
        constantService.saveConstant( constantA );

        groupA = createOrganisationUnitGroup( 'A' );
        groupA.addOrganisationUnit( unitA );
        groupA.addOrganisationUnit( unitB );
        groupA.addOrganisationUnit( unitC );
        
        organisationUnitGroupService.addOrganisationUnitGroup( groupA );
        
        expressionA = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}+#{" + dataElementB.getUid() + SEPARATOR
            + categoryOptionCombo.getUid() + "}";
        expressionB = "#{" + dataElementC.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}-#{" + dataElementD.getUid() + SEPARATOR
            + categoryOptionCombo.getUid() + "}";
        expressionC = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}+#{" + dataElementE.getUid() + "}-10";
        expressionD = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}+" + DAYS_SYMBOL;
        expressionE = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}*C{" + constantA.getUid() + "}";
        expressionF = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}";
        expressionG = expressionF + "+#{" + dataElementB.getUid() + "}-#{" + dataElementC.getUid() + "}";
        expressionH = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}*OUG{" + groupA.getUid() + "}";

        descriptionA = "Expression A";
        descriptionB = "Expression B";

        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );
        dataElements.add( dataElementE );

        dataValueService.addDataValue( createDataValue( dataElementA, period, unitA, "10", categoryOptionCombo, categoryOptionCombo ) );
        dataValueService.addDataValue( createDataValue( dataElementB, period, unitA, "5", categoryOptionCombo, categoryOptionCombo ) );
    }

    // -------------------------------------------------------------------------
    // Business logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testExplodeExpressionA()
    {
        categoryService.generateOptionCombos( categoryCombo );

        String actual = expressionService.explodeExpression( expressionC );

        Set<DataElementCategoryOptionCombo> categoryOptionCombos = categoryCombo.getOptionCombos();

        assertTrue( actual.contains( "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}" ) );

        for ( DataElementCategoryOptionCombo categoryOptionCombo : categoryOptionCombos )
        {
            assertTrue( actual.contains( "#{" + dataElementE.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "}" ) );
        }
    }

    @Test
    public void testExplodeExpressionB()
    {
        assertEquals( "1", expressionService.explodeExpression( "1" ) );
        assertEquals( "2+6/4", expressionService.explodeExpression( "2+6/4" ) );
    }

    @Test
    public void testGetDataElementsInExpression()
    {
        Set<DataElement> dataElements = expressionService.getDataElementsInExpression( expressionA );

        assertTrue( dataElements.size() == 2 );
        assertTrue( dataElements.contains( dataElementA ) );
        assertTrue( dataElements.contains( dataElementB ) );
        
        dataElements = expressionService.getDataElementsInExpression( expressionG );

        assertEquals( 3, dataElements.size() );
        assertTrue( dataElements.contains( dataElementA ) );
        assertTrue( dataElements.contains( dataElementB ) );
        assertTrue( dataElements.contains( dataElementC ) );
    }

    @Test
    public void testGetDataElementsInIndicators()
    {
        Indicator inA = createIndicator( 'A', null );
        inA.setNumerator( expressionA );
        
        Set<DataElement> dataElements = expressionService.getDataElementsInIndicators( Lists.newArrayList( inA ) );

        assertTrue( dataElements.size() == 2 );
        assertTrue( dataElements.contains( dataElementA ) );
        assertTrue( dataElements.contains( dataElementB ) );

        Indicator inG = createIndicator( 'G', null );
        inG.setNumerator( expressionG );
        
        dataElements = expressionService.getDataElementsInIndicators( Lists.newArrayList( inG ) );

        assertEquals( 3, dataElements.size() );
        assertTrue( dataElements.contains( dataElementA ) );
        assertTrue( dataElements.contains( dataElementB ) );
        assertTrue( dataElements.contains( dataElementC ) );
    }

    @Test
    public void testGetDataElementTotalsInIndicators()
    {
        Indicator inG = createIndicator( 'G', null );
        inG.setNumerator( expressionG );
        
        Set<DataElement> dataElements = expressionService.getDataElementTotalsInIndicators( Lists.newArrayList( inG ) );

        assertEquals( 2, dataElements.size() );
        assertTrue( dataElements.contains( dataElementB ) );
        assertTrue( dataElements.contains( dataElementC ) );
    }

    @Test
    public void testGetDataElementWithOptionCombosInIndicators()
    {
        Indicator inG = createIndicator( 'G', null );
        inG.setNumerator( expressionG );
        
        Set<DataElement> dataElements = expressionService.getDataElementWithOptionCombosInIndicators( Lists.newArrayList( inG ) );

        assertEquals( 1, dataElements.size() );
        assertTrue( dataElements.contains( dataElementA ) );
    }

    @Test
    public void testGetDataElementTotalUids()
    {
        Set<String> uids = new HashSet<>();
        Set<String> empty = new HashSet<>();
        
        uids.add( dataElementB.getUid() );
        uids.add( dataElementC.getUid() );
        
        assertEquals( uids, expressionService.getDataElementTotalUids( expressionG ) );
        assertEquals( empty, expressionService.getDataElementTotalUids( expressionA ) );
    }
    
    @Test
    public void testGetOperandsInExpression()
    {
        Set<DataElementOperand> operands = expressionService.getOperandsInExpression( expressionA );

        assertNotNull( operands );
        assertEquals( 2, operands.size() );

        DataElementOperand operandA = new DataElementOperand( dataElementA.getUid(), categoryOptionCombo.getUid() );
        DataElementOperand operandB = new DataElementOperand( dataElementB.getUid(), categoryOptionCombo.getUid() );

        assertTrue( operands.contains( operandA ) );
        assertTrue( operands.contains( operandB ) );
        
        operands = expressionService.getOperandsInExpression( expressionG );

        assertNotNull( operands );
        assertEquals( 1, operands.size() );

        assertTrue( operands.contains( operandA ) );
    }

    @Test
    public void testGetOptionCombosInExpression()
    {
        Set<DataElementCategoryOptionCombo> optionCombos = expressionService.getOptionCombosInExpression( expressionG );

        assertNotNull( optionCombos );
        assertEquals( 1, optionCombos.size() );

        assertTrue( optionCombos.contains( categoryOptionCombo ) );
    }

    @Test
    public void testExpressionIsValid()
    {
        assertEquals( ExpressionService.VALID, expressionService.expressionIsValid( expressionA ) );
        assertEquals( ExpressionService.VALID, expressionService.expressionIsValid( expressionB ) );
        assertEquals( ExpressionService.VALID, expressionService.expressionIsValid( expressionC ) );
        assertEquals( ExpressionService.VALID, expressionService.expressionIsValid( expressionD ) );
        assertEquals( ExpressionService.VALID, expressionService.expressionIsValid( expressionE ) );
        assertEquals( ExpressionService.VALID, expressionService.expressionIsValid( expressionH ) );

        expressionA = "#{NonExistingUid" + SEPARATOR + categoryOptionCombo.getUid() + "} + 12";

        assertEquals( ExpressionService.DATAELEMENT_DOES_NOT_EXIST, expressionService.expressionIsValid( expressionA ) );

        expressionA = "#{" + dataElementA.getUid() + SEPARATOR + 999 + "} + 12";

        assertEquals( ExpressionService.CATEGORYOPTIONCOMBO_DOES_NOT_EXIST, expressionService
            .expressionIsValid( expressionA ) );

        expressionA = "#{" + dataElementA.getUid() + SEPARATOR + categoryOptionCombo.getUid() + "} + ( 12";

        assertEquals( ExpressionService.EXPRESSION_NOT_WELL_FORMED, expressionService.expressionIsValid( expressionA ) );

        expressionA = "12 x 4";

        assertEquals( ExpressionService.EXPRESSION_NOT_WELL_FORMED, expressionService.expressionIsValid( expressionA ) );
        
        expressionA = "12 + C{999999}";

        assertEquals( ExpressionService.CONSTANT_DOES_NOT_EXIST, expressionService.expressionIsValid( expressionA ) );
        
        expressionA = "12 + OUG{999999}";
        
        assertEquals( ExpressionService.OU_GROUP_DOES_NOT_EXIST, expressionService.expressionIsValid( expressionA ) );
    }

    @Test
    public void testGetExpressionDescription()
    {
        String description = expressionService.getExpressionDescription( expressionA );

        assertEquals( "DataElementA+DataElementB", description );
        
        description = expressionService.getExpressionDescription( expressionD );
        
        assertEquals( "DataElementA+" + ExpressionService.DAYS_DESCRIPTION, description );

        description = expressionService.getExpressionDescription( expressionE );
        
        assertEquals( "DataElementA*ConstantA", description );
        
        description = expressionService.getExpressionDescription( expressionH );
        
        assertEquals( "DataElementA*OrganisationUnitGroupA", description );
    }
    
    @Test
    public void testGenerateExpressionMap()
    {
        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( dataElementA.getUid(), categoryOptionCombo.getUid() ), 12d );
        valueMap.put( new DataElementOperand( dataElementB.getUid(), categoryOptionCombo.getUid() ), 34d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );

        assertEquals( "12.0+34.0", expressionService.generateExpression( expressionA, valueMap, constantMap, null, null, null ) );
        assertEquals( "12.0+5", expressionService.generateExpression( expressionD, valueMap, constantMap, null, 5, null ) );
        assertEquals( "12.0*2.0", expressionService.generateExpression( expressionE, valueMap, constantMap, null, null, null ) );
        assertEquals( "12.0*3", expressionService.generateExpression( expressionH, valueMap, constantMap, orgUnitCountMap, null, null ) );
    }

    @Test
    public void testGenerateExpressionMapNullIfNoValues()
    {
        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        
        Map<String, Double> constantMap = new HashMap<>();

        assertNull( expressionService.generateExpression( expressionA, valueMap, constantMap, null, null, MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING ) );
        assertNull( expressionService.generateExpression( expressionD, valueMap, constantMap, null, 5, MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING ) );
        assertNotNull( expressionService.generateExpression( expressionE, valueMap, constantMap, null, null, MissingValueStrategy.NEVER_SKIP ) );
    }
    
    @Test
    public void testGetExpressionValue()
    {
        Expression expA = createExpression( 'A', expressionA, null, null );
        Expression expD = createExpression( 'D', expressionD, null, null );
        Expression expE = createExpression( 'E', expressionE, null, null );
        Expression expH = createExpression( 'H', expressionH, null, null );
        
        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( dataElementA.getUid(), categoryOptionCombo.getUid() ), 12d );
        valueMap.put( new DataElementOperand( dataElementB.getUid(), categoryOptionCombo.getUid() ), 34d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );
        
        assertEquals( 46d, expressionService.getExpressionValue( expA, valueMap, constantMap, null, null ), DELTA );
        assertEquals( 17d, expressionService.getExpressionValue( expD, valueMap, constantMap, null, 5 ), DELTA );
        assertEquals( 24d, expressionService.getExpressionValue( expE, valueMap, constantMap, null, null ), DELTA );
        assertEquals( 36d, expressionService.getExpressionValue( expH, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
    }
    
    @Test
    public void testGetIndicatorValue()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );
        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( dataElementA.getUid(), categoryOptionCombo.getUid() ), 12d );
        valueMap.put( new DataElementOperand( dataElementB.getUid(), categoryOptionCombo.getUid() ), 34d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        assertEquals( 200d, expressionService.getIndicatorValue( indicatorA, period, valueMap, constantMap, null ), DELTA );        
    }
    
    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetExpression()
    {
        Expression expression = new Expression( expressionA, descriptionA, dataElements );

        int id = expressionService.addExpression( expression );

        expression = expressionService.getExpression( id );

        assertEquals( expressionA, expression.getExpression() );
        assertEquals( descriptionA, expression.getDescription() );
        assertEquals( dataElements, expression.getDataElementsInExpression() );
    }

    @Test
    public void testUpdateExpression()
    {
        Expression expression = new Expression( expressionA, descriptionA, dataElements );

        int id = expressionService.addExpression( expression );

        expression = expressionService.getExpression( id );

        assertEquals( expressionA, expression.getExpression() );
        assertEquals( descriptionA, expression.getDescription() );

        expression.setExpression( expressionB );
        expression.setDescription( descriptionB );

        expressionService.updateExpression( expression );

        expression = expressionService.getExpression( id );

        assertEquals( expressionB, expression.getExpression() );
        assertEquals( descriptionB, expression.getDescription() );
    }

    @Test
    public void testDeleteExpression()
    {
        Expression exprA = new Expression( expressionA, descriptionA, dataElements );
        Expression exprB = new Expression( expressionB, descriptionB, dataElements );

        int idA = expressionService.addExpression( exprA );
        int idB = expressionService.addExpression( exprB );

        assertNotNull( expressionService.getExpression( idA ) );
        assertNotNull( expressionService.getExpression( idB ) );

        expressionService.deleteExpression( exprA );

        assertNull( expressionService.getExpression( idA ) );
        assertNotNull( expressionService.getExpression( idB ) );

        expressionService.deleteExpression( exprB );

        assertNull( expressionService.getExpression( idA ) );
        assertNull( expressionService.getExpression( idB ) );
    }

    @Test
    public void testGetAllExpressions()
    {
        Expression exprA = new Expression( expressionA, descriptionA, dataElements );
        Expression exprB = new Expression( expressionB, descriptionB, dataElements );

        expressionService.addExpression( exprA );
        expressionService.addExpression( exprB );

        List<Expression> expressions = expressionService.getAllExpressions();

        assertTrue( expressions.size() == 2 );
        assertTrue( expressions.contains( exprA ) );
        assertTrue( expressions.contains( exprB ) );
    }
}
