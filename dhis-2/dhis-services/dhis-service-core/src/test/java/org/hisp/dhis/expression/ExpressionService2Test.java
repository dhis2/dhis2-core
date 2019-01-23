/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.expression;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.category.*;
import org.hisp.dhis.common.*;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
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
public class ExpressionService2Test {

    @Mock
    private HibernateGenericStore hibernateGenericStore;
    @Mock
    private DataElementService dataElementService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private ConstantService constantService;
    @Mock
    private OrganisationUnitGroupService organisationUnitGroupService;
    @Mock
    private IdentifiableObjectManager idObjectManager;
    @Mock
    private DimensionService dimensionService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultExpressionService target;

    private CategoryOption categoryOptionA;
    private CategoryOption categoryOptionB;
    private CategoryOption categoryOptionC;
    private CategoryOption categoryOptionD;

    private Category categoryA;
    private Category categoryB;

    private CategoryCombo categoryCombo;

    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataElement deE;
    private DataElementOperand opA;
    private DataElementOperand opB;

    private ProgramTrackedEntityAttributeDimensionItem pteaA;
    private ProgramDataElementDimensionItem pdeA;
    private ProgramIndicator piA;

    private Period period;

    private OrganisationUnit unitA;
    private OrganisationUnit unitB;
    private OrganisationUnit unitC;

    private CategoryOptionCombo coc;
    private CategoryOptionCombo cocA;
    private CategoryOptionCombo cocB;

    private Constant constantA;

    private OrganisationUnitGroup groupA;

    private ReportingRate reportingRate;

    private String expressionA;
    private String expressionB;
    private String expressionC;
    private String expressionD;
    private String expressionE;
    private String expressionF;
    private String expressionG;
    private String expressionH;
    private String expressionI;
    private String expressionK;
    private String expressionJ;
    private String expressionL;
    private String expressionM;
    private String expressionN;

    private String expressionR;

    private BeanRandomizer rnd;
    private static final double DELTA = 0.01;

    @Before
    public void setUp() {
        target = new DefaultExpressionService(hibernateGenericStore, dataElementService, constantService,
                categoryService, organisationUnitGroupService, dimensionService, idObjectManager);
        rnd = new BeanRandomizer();

        // SETUP FIXTURES

        categoryOptionA = new CategoryOption( "Under 5" );
        categoryOptionB = new CategoryOption( "Over 5" );
        categoryOptionC = new CategoryOption( "Male" );
        categoryOptionD = new CategoryOption( "Female" );

        categoryA = new Category( "Age", DataDimensionType.DISAGGREGATION );
        categoryB = new Category( "Gender", DataDimensionType.DISAGGREGATION );

        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );

        categoryCombo = new CategoryCombo( "Age and gender", DataDimensionType.DISAGGREGATION );
        categoryCombo.getCategories().add( categoryA );
        categoryCombo.getCategories().add( categoryB );
        categoryCombo.generateOptionCombos();

        List<CategoryOptionCombo> optionCombos = Lists.newArrayList( categoryCombo.getOptionCombos() );

        cocA = optionCombos.get( 0 );
        cocA.setUid(CodeGenerator.generateUid());
        cocB = optionCombos.get( 1 );
        cocB.setUid(CodeGenerator.generateUid());

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        deE = createDataElement( 'E', categoryCombo );

        coc = rnd.randomObject(CategoryOptionCombo.class);
        coc.setName(DEFAULT_CATEGORY_COMBO_NAME);

        optionCombos.add( coc );

        opA = new DataElementOperand( deA, coc );
        opB = new DataElementOperand( deB, coc );

        period = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );

        pteaA = rnd.randomObject(ProgramTrackedEntityAttributeDimensionItem.class);
        pdeA = rnd.randomObject(ProgramDataElementDimensionItem.class);
        piA = rnd.randomObject(ProgramIndicator.class);


        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );

        constantA = rnd.randomObject(Constant.class);
        constantA.setName("ConstantA");
        constantA.setValue(2.0);

        groupA = createOrganisationUnitGroup( 'A' );
        groupA.addOrganisationUnit( unitA );
        groupA.addOrganisationUnit( unitB );
        groupA.addOrganisationUnit( unitC );


        DataSet dataSetA = createDataSet( 'A' );
        dataSetA.setUid( "a23dataSetA" );
        dataSetA.addOrganisationUnit( unitA );

        reportingRate = new ReportingRate( dataSetA );

        expressionA = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionB = "#{" + deC.getUid() + SEPARATOR + coc.getUid() + "}-#{" + deD.getUid() + SEPARATOR
                + coc.getUid() + "}";
        expressionC = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+#{" + deE.getUid() + "}-10";
        expressionD = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+" + SYMBOL_DAYS;
        expressionE = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*C{" + constantA.getUid() + "}";
        expressionF = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionG = expressionF + "+#{" + deB.getUid() + "}-#{" + deC.getUid() + "}";
        expressionH = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*OUG{" + groupA.getUid() + "}";
        expressionI = "#{" + opA.getDimensionItem() + "}*" + "#{" + deB.getDimensionItem() + "}+" + "C{" + constantA.getUid() + "}+5-" +
                "D{" + pdeA.getDimensionItem() + "}+" + "A{" + pteaA.getDimensionItem() + "}-10+" + "I{" + piA.getDimensionItem() + "}";
        expressionJ = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionK = "1.5*AVG(" + expressionJ + ")";
        expressionL = expressionA + "+AVG("+expressionJ+")+1.5*STDDEV("+expressionJ+")+" + expressionB;
        expressionM = "#{" + deA.getUid() + SEPARATOR + SYMBOL_WILDCARD + "}-#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionN = "#{" + deA.getUid() + SEPARATOR + cocA.getUid() + SEPARATOR + cocB.getUid() + "}-#{" + deB.getUid() + SEPARATOR + cocA.getUid() + "}";
        expressionR = "#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}" + " + R{" + reportingRate.getUid() + ".REPORTING_RATE}";

    }

    @Test
    public void testGetElementsAndOptionCombosInExpression() {
        Set<String> ids = target.getElementsAndOptionCombosInExpression( expressionC );

        assertEquals( 2, ids.size() );
        assertTrue( ids.contains( deA.getUid() + SEPARATOR + coc.getUid() ) );
        assertTrue( ids.contains( deE.getUid() ) );
    }


    @Test
    public void testGetDimensionalItemIdsInExpressionNullOrEmpty() {
        SetMap<Class<? extends DimensionalItemObject>, String> res = target.getDimensionalItemIdsInExpression( null );
        assertEquals(0, res.size());
        res = target.getDimensionalItemIdsInExpression( "" );
        assertEquals(0, res.size());
    }


    @Test
    public void testGetDimensionalItemIdsInExpression()
    {
        SetMap<Class<? extends DimensionalItemObject>, String> idMap = target.getDimensionalItemIdsInExpression( expressionI );

        assertEquals( 4, idMap.size() );
        assertTrue( idMap.containsKey( DataElementOperand.class ) );
        assertTrue( idMap.containsKey( ProgramDataElementDimensionItem.class ) );
        assertTrue( idMap.containsKey( ProgramTrackedEntityAttributeDimensionItem.class ) );
        assertTrue( idMap.containsKey( ProgramIndicator.class ) );

        assertEquals( 2, idMap.get( DataElementOperand.class ).size() );
        assertTrue( idMap.get( DataElementOperand.class ).contains( opA.getDimensionItem() ) );
        assertTrue( idMap.get( DataElementOperand.class ).contains( deB.getDimensionItem() ) );

        assertEquals( 1, idMap.get( ProgramDataElementDimensionItem.class ).size() );
        assertTrue( idMap.get( ProgramDataElementDimensionItem.class ).contains( pdeA.getDimensionItem() ) );

        assertEquals( 1, idMap.get( ProgramTrackedEntityAttributeDimensionItem.class ).size() );
        assertTrue( idMap.get( ProgramTrackedEntityAttributeDimensionItem.class ).contains( pteaA.getDimensionItem() ) );

        assertEquals( 1, idMap.get( ProgramIndicator.class ).size() );
        assertTrue( idMap.get( ProgramIndicator.class ).contains( piA.getDimensionItem() ) );
    }

    @Test
    public void testGetDimensionalItemObjectsInIndicators()
    {
        when( dimensionService.getDataDimensionalItemObject( opA.getDimensionItem() ) ).thenReturn( opA );
        when( dimensionService.getDataDimensionalItemObject( opB.getDimensionItem() ) ).thenReturn( opB );
        when( dimensionService.getDataDimensionalItemObject( deB.getDimensionItem() ) ).thenReturn( deB );
        when( dimensionService.getDataDimensionalItemObject( pdeA.getDimensionItem() ) ).thenReturn( pdeA );
        when( dimensionService.getDataDimensionalItemObject( pteaA.getDimensionItem() ) ).thenReturn( pteaA );
        when( dimensionService.getDataDimensionalItemObject( piA.getDimensionItem() ) ).thenReturn( piA );

        Indicator indicator = createIndicator( 'A', null );
        indicator.setNumerator( expressionI );
        indicator.setDenominator( expressionA );

        Set<Indicator> indicators = Sets.newHashSet( indicator );

        Set<DimensionalItemObject> items = target.getDimensionalItemObjectsInIndicators( indicators );

        assertEquals( 6, items.size() );
        assertThat(items, hasItems(opA, opB, deB, piA, pdeA, pteaA));
    }

    @Test
    public void testGetDataElementsInExpression()
    {

        when( dataElementService.getDataElement( opA.getDimensionItem().split( "\\." )[0] ) )
            .thenReturn( opA.getDataElement() );
        when( dataElementService.getDataElement( opB.getDimensionItem().split( "\\." )[0] ) )
            .thenReturn( opB.getDataElement() );
        Set<DataElement> dataElements = target.getDataElementsInExpression( expressionA );

        assertThat( dataElements, hasSize( 2 ) );
        assertThat( dataElements, hasItems( opA.getDataElement(), opB.getDataElement() ) );

        // Expression G
        when( dataElementService.getDataElement( deA.getUid() ) ).thenReturn( deA );
        when( dataElementService.getDataElement( deB.getUid() ) ).thenReturn( deB );
        when( dataElementService.getDataElement( deC.getUid() ) ).thenReturn( deC );

        dataElements = target.getDataElementsInExpression( expressionG );

        assertThat( dataElements, hasSize( 3 ) );
        assertThat( dataElements, hasItems( deA, deB, deC ) );

        dataElements = target.getDataElementsInExpression( expressionM );

        assertThat( dataElements, hasSize( 2 ) );
        assertThat( dataElements, hasItems( deA, deB ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetOperandsInExpression()
    {
        when(dataElementService.getDataElement(opA.getDimensionItem().split("\\.")[0])).thenReturn(deA);
        when(categoryService.getCategoryOptionCombo(opA.getDimensionItem().split("\\.")[1])).thenReturn(coc);
        when(dataElementService.getDataElement(opB.getDimensionItem().split("\\.")[0])).thenReturn(deB);
        when(categoryService.getCategoryOptionCombo(opB.getDimensionItem().split("\\.")[1])).thenReturn(coc);

        Set<DataElementOperand> operands = target.getOperandsInExpression( expressionA );

        assertEquals( 2, operands.size() );

        assertThat( operands,
                IsIterableContainingInAnyOrder.containsInAnyOrder(
                        allOf( hasProperty( "dataElement", is( deA) ),
                               hasProperty( "categoryOptionCombo", is( coc) ),
                               hasProperty( "attributeOptionCombo", is(nullValue())) ),
                        allOf( hasProperty( "dataElement", is( deB) ),
                               hasProperty( "categoryOptionCombo", is( coc ) ),
                               hasProperty( "attributeOptionCombo", is(nullValue()) ) ) ) );

    }

    @Test
    public void testGetOperandsInExpressionWhenNull()
    {
        assertThat( target.getOperandsInExpression( null ), hasSize( 0 ) );
    }

    @Test
    public void testGetReportingRatesInExpression()
    {
        when( dimensionService.getDataDimensionalItemObject( deB.getUid() + SEPARATOR + coc.getUid() ) ).thenReturn( new BaseDimensionalItemObject("1"));
        when( dimensionService.getDataDimensionalItemObject( reportingRate.getUid() + ".REPORTING_RATE" ) ).thenReturn( reportingRate );
        Set<DimensionalItemObject> reportingRates = target.getDimensionalItemObjectsInExpression( expressionR );

        assertEquals( 2, reportingRates.size() );
        assertTrue( reportingRates.contains( reportingRate ) );
    }

    @Test
    public void testGetAggregatesAndNonAggregatesInExpression()
    {
        Set<String> aggregates = new HashSet<>();
        Set<String> nonAggregates = new HashSet<>();
        target.getAggregatesAndNonAggregatesInExpression( expressionK, aggregates, nonAggregates );

        assertEquals( 1, aggregates.size() );
        assertTrue( aggregates.contains( expressionJ ) );

        assertEquals( 1, nonAggregates.size() );
        assertTrue( nonAggregates.contains( "1.5*" ) );

        aggregates = new HashSet<>();
        nonAggregates = new HashSet<>();
        target.getAggregatesAndNonAggregatesInExpression( expressionL, aggregates, nonAggregates );

        assertEquals( 1, aggregates.size() );
        assertTrue( aggregates.contains( expressionJ ) );

        assertEquals( 3, nonAggregates.size() );
        assertTrue( nonAggregates.contains( expressionA + "+" ) );
        assertTrue( nonAggregates.contains( "+1.5*" ) );
        assertTrue( nonAggregates.contains( "+" + expressionB ) );
    }

    @Test
    public void testCalculateExpressionWithCustomFunctions()
    {
        assertEquals( 5.0, calculateExpression( "COUNT([1,2,3,4,5])" ) );
        assertEquals( 15.0, calculateExpression( "SUM([1,2,3,4,5])" ) );
        assertEquals( 1.0, calculateExpression( "MIN([1,2,3,4,5])" ) );
        assertEquals( 5.0, calculateExpression( "MAX([1,2,3,4,5])" ) );
        assertEquals( 3.0, calculateExpression( "AVG([1,2,3,4,5])" ) );
        assertEquals( Math.sqrt( 2 ), calculateExpression( "STDDEV([1,2,3,4,5])" ) );
    }

    private Object calculateExpression( String expressionString )
    {
        Expression expression = new Expression( expressionString, "Test " + expressionString );

        return target.getExpressionValue( expression, new HashMap<>(),
                new HashMap<>(), new HashMap<>(), 0 );
    }

    @Test
    public void testGetOptionCombosInExpression()
    {
        when(categoryService.getCategoryOptionCombo(coc.getUid() )).thenReturn(coc);
        Set<CategoryOptionCombo> optionCombos = target.getOptionCombosInExpression( expressionG );

        assertNotNull( optionCombos );
        assertThat(optionCombos, hasSize(1));
        assertThat(optionCombos, hasItem(coc));
    }

    @Test
    public void testExpressionIsValid()
    {
        when( dimensionService.getDataDimensionalItemObject( opA.getUid() ) ).thenReturn( opA );
        when( dimensionService.getDataDimensionalItemObject( opB.getUid() ) ).thenReturn( opB );
        when( dimensionService.getDataDimensionalItemObject( deA.getUid() + SEPARATOR + coc.getUid() ) )
            .thenReturn( deA );
        when( dimensionService.getDataDimensionalItemObject( deA.getUid() + SEPARATOR + SYMBOL_WILDCARD ) )
                .thenReturn( deA );
        when( dimensionService.getDataDimensionalItemObject( deB.getUid() + SEPARATOR + coc.getUid() ) )
            .thenReturn( deB );
        when( dimensionService.getDataDimensionalItemObject( deC.getUid() + SEPARATOR + coc.getUid() ) )
            .thenReturn( deC );
        when( dimensionService.getDataDimensionalItemObject( deD.getUid() + SEPARATOR + coc.getUid() ) )
            .thenReturn( deC );
        when( dimensionService.getDataDimensionalItemObject( deE.getUid() ) ).thenReturn( deE );
        when( idObjectManager.getNoAcl( Constant.class, constantA.getUid() ) ).thenReturn( constantA );
        when( idObjectManager.getNoAcl( OrganisationUnitGroup.class, groupA.getUid() ) ).thenReturn( groupA );

        when( dimensionService.getDataDimensionalItemObject( reportingRate.getUid() + ".REPORTING_RATE" ) )
                .thenReturn( reportingRate );
        when( dimensionService.getDataDimensionalItemObject( deA.getUid() + SEPARATOR + cocA.getUid() + SEPARATOR + cocB.getUid() ) ).thenReturn( deA );
        when( dimensionService.getDataDimensionalItemObject( deB.getUid() + SEPARATOR + cocA.getUid() ) ).thenReturn( deB );

        assertTrue( target.expressionIsValid( expressionA ).isValid() );
        assertTrue( target.expressionIsValid( expressionB ).isValid() );
        assertTrue( target.expressionIsValid( expressionC ).isValid() );
        assertTrue( target.expressionIsValid( expressionD ).isValid() );
        assertTrue( target.expressionIsValid( expressionE ).isValid() );
        assertTrue( target.expressionIsValid( expressionH ).isValid() );
        assertTrue( target.expressionIsValid( expressionK ).isValid() );
        assertTrue( target.expressionIsValid( expressionL ).isValid() );
        assertTrue( target.expressionIsValid( expressionM ).isValid() );
        assertTrue( target.expressionIsValid( expressionN ).isValid() );
        assertTrue( target.expressionIsValid( expressionR ).isValid() );

        String expression = "#{nonExisting" + SEPARATOR + coc.getUid() + "} + 12";

        assertEquals( ExpressionValidationOutcome.DIMENSIONAL_ITEM_OBJECT_DOES_NOT_EXIST, target.expressionIsValid( expression ) );

        expression = "#{" + deA.getUid() + SEPARATOR + "999} + 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, target
                .expressionIsValid( expression ) );

        expression = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "} + ( 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, target.expressionIsValid( expression ) );

        expression = "12 x 4";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, target.expressionIsValid( expression ) );

        expression = "1.5*AVG(" + target;

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, target.expressionIsValid( expression ) );

        expression = "12 + C{nonExisting}";

        assertEquals( ExpressionValidationOutcome.CONSTANT_DOES_NOT_EXIST, target.expressionIsValid( expression ) );

        expression = "12 + OUG{nonExisting}";

        assertEquals( ExpressionValidationOutcome.ORG_UNIT_GROUP_DOES_NOT_EXIST, target.expressionIsValid( expression ) );
    }

    @Test
    public void testGetExpressionDescription()
    {
        when(dimensionService.getDataDimensionalItemObject(opA.getDimensionItem())).thenReturn(opA);
        when(dimensionService.getDataDimensionalItemObject(opB.getDimensionItem())).thenReturn(opB);

        String description = target.getExpressionDescription( expressionA );
        assertThat( description, is( opA.getDisplayName() + "+" + opB.getDisplayName() ) );

        description = target.getExpressionDescription( expressionD );
        assertThat( description, is( opA.getDisplayName() + "+" + ExpressionService.DAYS_DESCRIPTION ) );

        when(constantService.getConstant(constantA.getUid())).thenReturn(constantA);
        description = target.getExpressionDescription( expressionE );
        assertThat( description, is( opA.getDisplayName() + "*" + constantA.getDisplayName() ) );

        when(organisationUnitGroupService.getOrganisationUnitGroup(groupA.getUid())).thenReturn(groupA);
        description = target.getExpressionDescription( expressionH );
        assertThat( description, is( opA.getDisplayName() + "*" + groupA.getDisplayName() ) );

        when( dimensionService.getDataDimensionalItemObject( deA.getUid() + SEPARATOR + SYMBOL_WILDCARD ) )
                .thenReturn( deA );
        when( dimensionService.getDataDimensionalItemObject( deB.getUid() + SEPARATOR + coc.getUid() ) )
            .thenReturn( deB );
        description = target.getExpressionDescription( expressionM );
        assertThat( description, is( deA.getDisplayName() + "-" + deB.getDisplayName() ) );

        when( dimensionService.getDataDimensionalItemObject( reportingRate.getUid() + ".REPORTING_RATE" ) )
                .thenReturn( reportingRate );
        description = target.getExpressionDescription( expressionR );
        assertThat( description, is( deB.getDisplayName() + " + " + reportingRate.getDisplayName() ) );
    }

    @Test
    public void testGenerateExpressionWithMap()
    {
        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 26d );
        valueMap.put( new DataElementOperand( deB, cocA ), 16d );
        valueMap.put( reportingRate, 20d );

        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );

        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );


        assertEquals( "12.0+34.0", target.generateExpression( expressionA, valueMap, constantMap, null, null, null ) );
        assertEquals( "12.0+5", target.generateExpression( expressionD, valueMap, constantMap, null, 5, null ) );
        assertEquals( "12.0*2.0", target.generateExpression( expressionE, valueMap, constantMap, null, null, null ) );
        assertEquals( "12.0*3", target.generateExpression( expressionH, valueMap, constantMap, orgUnitCountMap, null, null ) );
        assertEquals( "26.0-16.0", target.generateExpression( expressionN, valueMap, constantMap, orgUnitCountMap, null, null ) );
        assertEquals( "34.0 + 20.0", target.generateExpression( expressionR, valueMap, constantMap, orgUnitCountMap, null, null ) );
    }

    @Test
    public void testGenerateExpressionWithMapNullIfNoValues()
    {
        Map<DataElementOperand, Double> valueMap = new HashMap<>();

        Map<String, Double> constantMap = new HashMap<>();

        assertNull( target.generateExpression( expressionA, valueMap, constantMap, null, null, MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING ) );
        assertNull( target.generateExpression( expressionD, valueMap, constantMap, null, 5, MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING ) );
        assertNotNull( target.generateExpression( expressionE, valueMap, constantMap, null, null, MissingValueStrategy.NEVER_SKIP ) );
    }

    @Test
    public void testGetExpressionValue()
    {
        Expression expA = new Expression( expressionA, null );
        Expression expD = new Expression( expressionD, null );
        Expression expE = new Expression( expressionE, null );
        Expression expH = new Expression( expressionH, null );
        Expression expN = new Expression( expressionN, null );
        Expression expR = new Expression( expressionR, null );

        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 26d );
        valueMap.put( new DataElementOperand( deB, cocA ), 16d );
        valueMap.put( reportingRate, 20d );

        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );

        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );

        assertEquals( 46d, target.getExpressionValue( expA, valueMap, constantMap, null, null ), DELTA );
        assertEquals( 17d, target.getExpressionValue( expD, valueMap, constantMap, null, 5 ), DELTA );
        assertEquals( 24d, target.getExpressionValue( expE, valueMap, constantMap, null, null ), DELTA );
        assertEquals( 36d, target.getExpressionValue( expH, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
        assertEquals( 10d, target.getExpressionValue( expN, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
        assertEquals( 54d, target.getExpressionValue( expR, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
    }

    @Test
    public void testGetIndicatorValue()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionN );
        indicatorB.setDenominator( expressionF );

        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );

        assertEquals( 200d, target.getIndicatorValue( indicatorA, period, valueMap, constantMap, null ), DELTA );
        assertEquals( 300d, target.getIndicatorValue( indicatorB, period, valueMap, constantMap, null ), DELTA );
    }

    @Test
    public void testGetIndicatorValueObject()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DataElementOperand, Double> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, period, valueMap, constantMap, null );

        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 100, value.getMultiplier() );
        assertEquals( 1, value.getDivisor() );
        assertEquals( 100d, value.getFactor(), DELTA );
        assertEquals( 200d, value.getValue(), DELTA );

        // # ------------------------------------------------------------------- #

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionN );
        indicatorB.setDenominator( expressionF );
        indicatorB.setAnnualized( true );

        value = target.getIndicatorValueObject( indicatorB, period, valueMap, constantMap, null );

        assertEquals( 36d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 36500, value.getMultiplier() );
        assertEquals( 31, value.getDivisor() );
        assertEquals( 1177.419, value.getFactor(), DELTA );
        assertEquals( 3532.258, value.getValue(), DELTA );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void verifyExpressionIsUpdated()
    {
        Expression expression = rnd.randomObject( Expression.class );
        target.updateExpression(expression);
        verify( hibernateGenericStore ).update( expression );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyExpressionIsDeleted()
    {
        Expression expression = rnd.randomObject( Expression.class );
        target.deleteExpression(expression);
        verify( hibernateGenericStore ).delete( expression );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifyExpressionIsAdded()
    {

        Expression expression = rnd.randomObject( Expression.class );
        int id = target.addExpression( expression );
        assertThat( id, is( expression.getId() ) );
        verify( hibernateGenericStore ).save( expression );

    }

    @Test
    public void verifyAllExpressionsCanBeFetched()
    {
        when( hibernateGenericStore.getAll() ).thenReturn( Lists.newArrayList( rnd.randomObject( Expression.class ) ) );
        List<Expression> expressions = target.getAllExpressions();
        assertThat( expressions, hasSize( 1 ) );
        verify( hibernateGenericStore ).getAll();

    }

    @Test
    public void testGetOrganisationUnitGroupsInExpression()
    {
        when(organisationUnitGroupService.getOrganisationUnitGroup(groupA.getUid())).thenReturn(groupA);
        Set<OrganisationUnitGroup> groups = target.getOrganisationUnitGroupsInExpression( expressionH );
        assertThat(groups, hasSize(1));
        assertThat(groups, hasItem(groupA));

        groups = target.getOrganisationUnitGroupsInExpression( null );

        assertNotNull( groups );
        assertThat(groups, hasSize(0));
    }


}
