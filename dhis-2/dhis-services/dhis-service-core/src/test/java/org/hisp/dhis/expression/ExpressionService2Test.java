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
package org.hisp.dhis.expression;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.*;
import static org.hisp.dhis.expression.MissingValueStrategy.*;
import static org.hisp.dhis.expression.ParseType.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.commons.math3.util.Precision;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.cache.CacheProvider;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class ExpressionService2Test extends DhisSpringTest
{
    @Mock
    private HibernateGenericStore<Expression> hibernateGenericStore;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ConstantService constantService;

    @Mock
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Mock
    private DimensionService dimensionService;

    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Autowired
    private CacheProvider cacheProvider;

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

    private DataElementOperand opC;

    private DataElementOperand opD;

    private DataElementOperand opE;

    private DataElementOperand opF;

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

    private Constant constantB;

    private OrganisationUnitGroup groupA;

    private OrganisationUnitGroup groupB;

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

    private String expressionO;

    private String expressionP;

    private String expressionR;

    private BeanRandomizer rnd;

    private static final double DELTA = 0.01;

    @Before
    public void setUp()
    {
        target = new DefaultExpressionService( hibernateGenericStore, dataElementService, constantService,
            categoryService, organisationUnitGroupService, dimensionService, idObjectManager, cacheProvider );

        target.init();

        rnd = new BeanRandomizer();

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
        cocA.setUid( CodeGenerator.generateUid() );
        cocB = optionCombos.get( 1 );
        cocB.setUid( CodeGenerator.generateUid() );

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        deE = createDataElement( 'E', categoryCombo );

        coc = rnd.randomObject( CategoryOptionCombo.class );
        coc.setName( DEFAULT_CATEGORY_COMBO_NAME );

        optionCombos.add( coc );

        opA = new DataElementOperand( deA, coc );
        opB = new DataElementOperand( deB, coc );
        opC = new DataElementOperand( deC, coc );
        opD = new DataElementOperand( deD, coc );
        opE = new DataElementOperand( deB, cocA );
        opF = new DataElementOperand( deA, cocA, cocB );

        period = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );

        pteaA = rnd.randomObject( ProgramTrackedEntityAttributeDimensionItem.class );
        pdeA = rnd.randomObject( ProgramDataElementDimensionItem.class );
        piA = rnd.randomObject( ProgramIndicator.class );

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );

        constantA = rnd.randomObject( Constant.class );
        constantA.setName( "ConstantA" );
        constantA.setValue( 2.0 );

        constantB = rnd.randomObject( Constant.class );
        constantB.setName( "ConstantB" );
        constantB.setValue( 5.0 );

        groupA = createOrganisationUnitGroup( 'A' );
        groupA.addOrganisationUnit( unitA );
        groupA.addOrganisationUnit( unitB );
        groupA.addOrganisationUnit( unitC );

        groupB = createOrganisationUnitGroup( 'B' );
        groupB.addOrganisationUnit( unitB );

        DataSet dataSetA = createDataSet( 'A' );
        dataSetA.setUid( "a23dataSetA" );
        dataSetA.addOrganisationUnit( unitA );

        reportingRate = new ReportingRate( dataSetA );

        expressionA = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionB = "#{" + deC.getUid() + SEPARATOR + coc.getUid() + "}-#{" + deD.getUid() + SEPARATOR + coc.getUid()
            + "}";
        expressionC = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+#{" + deE.getUid() + "}-10";
        expressionD = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+" + SYMBOL_DAYS;
        expressionE = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*C{" + constantA.getUid() + "}";
        expressionF = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionG = expressionF + "+#{" + deB.getUid() + "}-#{" + deC.getUid() + "}";
        expressionH = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*OUG{" + groupA.getUid() + "}";
        expressionI = "#{" + opA.getDimensionItem() + "}*" + "#{" + deB.getDimensionItem() + "}+" + "C{"
            + constantA.getUid() +
            "}+5-" +
            "D{" + pdeA.getDimensionItem() + "}+" + "A{" + pteaA.getDimensionItem() + "}-10+" + "I{" +
            piA.getDimensionItem() + "}";
        expressionJ = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionK = "1.5*avg(" + expressionJ + ")";
        expressionL = expressionA + "+avg(" + expressionJ + ")+1.5*stddev(" + expressionJ + ")+" + expressionB;
        expressionM = "#{" + deA.getUid() + SEPARATOR + SYMBOL_WILDCARD + "}-#{" + deB.getUid() + SEPARATOR
            + coc.getUid() + "}";
        expressionN = "#{" + deA.getUid() + SEPARATOR + cocA.getUid() + SEPARATOR + cocB.getUid() + "}-#{"
            + deB.getUid() +
            SEPARATOR + cocA.getUid() + "}";
        expressionO = "#{" + opA.getDimensionItem() + "}+sum(#{" + opB.getDimensionItem() + "})";
        expressionP = "#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionR = "#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}" + " + R{" + reportingRate.getUid() +
            ".REPORTING_RATE}";

    }

    private DimensionalItemId getId( DimensionalItemObject o )
    {
        DimensionItemType type = o.getDimensionItemType();

        switch ( type )
        {
        case DATA_ELEMENT:
            return new DimensionalItemId( type, o.getUid() );

        case DATA_ELEMENT_OPERAND:
            DataElementOperand deo = (DataElementOperand) o;

            return new DimensionalItemId( type,
                deo.getDataElement().getUid(),
                deo.getCategoryOptionCombo() == null ? null : deo.getCategoryOptionCombo().getUid(),
                deo.getAttributeOptionCombo() == null ? null : deo.getAttributeOptionCombo().getUid() );

        case REPORTING_RATE:
            ReportingRate rr = (ReportingRate) o;

            return new DimensionalItemId( type,
                rr.getDataSet().getUid(),
                rr.getMetric().name() );

        case PROGRAM_DATA_ELEMENT:
            ProgramDataElementDimensionItem pde = (ProgramDataElementDimensionItem) o;

            return new DimensionalItemId( type,
                pde.getProgram().getUid(),
                pde.getDataElement().getUid() );

        case PROGRAM_ATTRIBUTE:
            ProgramTrackedEntityAttributeDimensionItem pa = (ProgramTrackedEntityAttributeDimensionItem) o;

            return new DimensionalItemId( type,
                pa.getProgram().getUid(),
                pa.getAttribute().getUid() );

        case PROGRAM_INDICATOR:
            return new DimensionalItemId( type, o.getUid() );

        default:
            return null;
        }
    }

    @Test
    public void testGetExpressionElementAndOptionComboIds()
    {
        Set<String> ids = target.getExpressionElementAndOptionComboIds( expressionC, VALIDATION_RULE_EXPRESSION );

        assertEquals( 2, ids.size() );
        assertTrue( ids.contains( deA.getUid() + SEPARATOR + coc.getUid() ) );
        assertTrue( ids.contains( deE.getUid() ) );
    }

    @Test
    public void testGetExpressionDimensionalItemIdsNullOrEmpty()
    {
        Set<DimensionalItemId> itemIds = target.getExpressionDimensionalItemIds( null, INDICATOR_EXPRESSION );
        assertEquals( 0, itemIds.size() );

        itemIds = target.getExpressionDimensionalItemIds( "", INDICATOR_EXPRESSION );
        assertEquals( 0, itemIds.size() );
    }

    @Test
    public void testGetExpressionDimensionalItemIds()
    {
        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );

        Set<DimensionalItemId> itemIds = target.getExpressionDimensionalItemIds( expressionI, INDICATOR_EXPRESSION );

        assertEquals( 5, itemIds.size() );
        assertTrue( itemIds.contains( getId( opA ) ) );
        assertTrue( itemIds.contains( getId( deB ) ) );
        assertTrue( itemIds.contains( getId( pdeA ) ) );
        assertTrue( itemIds.contains( getId( pteaA ) ) );
        assertTrue( itemIds.contains( getId( piA ) ) );
    }

    @Test
    public void testGetExpressionDimensionalItemObjectsNullOrEmpty()
    {
        Set<DimensionalItemObject> objects = target.getExpressionDimensionalItemObjects( null, INDICATOR_EXPRESSION );
        assertEquals( 0, objects.size() );

        objects = target.getExpressionDimensionalItemObjects( "", INDICATOR_EXPRESSION );
        assertEquals( 0, objects.size() );
    }

    @Test
    public void testGetExpressionDimensionalItemObjects()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( opA ), getId( deB ), getId( pdeA ), getId( pteaA ),
            getId( piA ) );
        Set<DimensionalItemObject> itemObjects = Sets.newHashSet( opA, deB, pdeA, pteaA, piA );
        when( dimensionService.getDataDimensionalItemObjects( itemIds ) ).thenReturn( itemObjects );

        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );

        Set<DimensionalItemObject> objects = target.getExpressionDimensionalItemObjects( expressionI,
            INDICATOR_EXPRESSION );

        assertEquals( 5, objects.size() );
        assertTrue( objects.contains( opA ) );
        assertTrue( objects.contains( deB ) );
        assertTrue( objects.contains( pdeA ) );
        assertTrue( objects.contains( pteaA ) );
        assertTrue( objects.contains( piA ) );
    }

    @Test
    public void testGetDimensionalItemObjectsInIndicators()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( opA ), getId( opB ), getId( deB ), getId( pdeA ),
            getId( pteaA ), getId( piA ) );
        Set<DimensionalItemObject> itemObjects = Sets.newHashSet( opA, opB, deB, pdeA, pteaA, piA );
        when( dimensionService.getDataDimensionalItemObjects( itemIds ) ).thenReturn( itemObjects );

        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );

        Indicator indicator = createIndicator( 'A', null );
        indicator.setNumerator( expressionI );
        indicator.setDenominator( expressionA );

        Set<Indicator> indicators = Sets.newHashSet( indicator );

        Set<DimensionalItemObject> items = target.getIndicatorDimensionalItemObjects( indicators );

        assertEquals( 6, items.size() );
        assertThat( items, hasItems( opA, opB, deB, piA, pdeA, pteaA ) );
    }

    @Test
    public void testGetExpressionDataElements()
    {
        when( dataElementService.getDataElement( opA.getDimensionItem().split( "\\." )[0] ) )
            .thenReturn( opA.getDataElement() );
        when( dataElementService.getDataElement( opB.getDimensionItem().split( "\\." )[0] ) )
            .thenReturn( opB.getDataElement() );
        Set<DataElement> dataElements = target.getExpressionDataElements( expressionA, INDICATOR_EXPRESSION );

        assertThat( dataElements, hasSize( 2 ) );
        assertThat( dataElements, hasItems( opA.getDataElement(), opB.getDataElement() ) );

        // Expression G
        when( dataElementService.getDataElement( deA.getUid() ) ).thenReturn( deA );
        when( dataElementService.getDataElement( deB.getUid() ) ).thenReturn( deB );
        when( dataElementService.getDataElement( deC.getUid() ) ).thenReturn( deC );

        dataElements = target.getExpressionDataElements( expressionG, INDICATOR_EXPRESSION );

        assertThat( dataElements, hasSize( 3 ) );
        assertThat( dataElements, hasItems( deA, deB, deC ) );

        dataElements = target.getExpressionDataElements( expressionM, INDICATOR_EXPRESSION );

        assertThat( dataElements, hasSize( 2 ) );
        assertThat( dataElements, hasItems( deA, deB ) );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void testGetExpressionOperands()
    {
        when( dataElementService.getDataElement( deA.getUid() ) ).thenReturn( deA );
        when( dataElementService.getDataElement( deB.getUid() ) ).thenReturn( deB );
        when( categoryService.getCategoryOptionCombo( coc.getUid() ) ).thenReturn( coc );

        Set<DataElementOperand> operands = target.getExpressionOperands( expressionO, PREDICTOR_EXPRESSION );

        assertEquals( 2, operands.size() );

        assertThat( operands,
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                allOf( hasProperty( "dataElement", is( deA ) ),
                    hasProperty( "categoryOptionCombo", is( coc ) ),
                    hasProperty( "attributeOptionCombo", is( nullValue() ) ) ),
                allOf( hasProperty( "dataElement", is( deB ) ),
                    hasProperty( "categoryOptionCombo", is( coc ) ),
                    hasProperty( "attributeOptionCombo", is( nullValue() ) ) ) ) );
    }

    @Test
    public void testGetExpressionOperandsWhenNull()
    {
        assertThat( target.getExpressionOperands( null, INDICATOR_EXPRESSION ), hasSize( 0 ) );
    }

    @Test
    public void testGetExpressionReportingRates()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( reportingRate ), getId( opB ) );
        Set<DimensionalItemObject> itemObjects = Sets.newHashSet( reportingRate, opB );
        when( dimensionService.getDataDimensionalItemObjects( itemIds ) ).thenReturn( itemObjects );

        Set<DimensionalItemObject> reportingRates = target.getExpressionDimensionalItemObjects( expressionR,
            INDICATOR_EXPRESSION );

        assertEquals( 2, reportingRates.size() );
        assertTrue( reportingRates.contains( reportingRate ) );
    }

    @Test
    public void testGetExpressionOptionComboIds()
    {
        Set<String> comboIds = target.getExpressionOptionComboIds( expressionG, INDICATOR_EXPRESSION );

        assertNotNull( comboIds );
        assertThat( comboIds, hasSize( 1 ) );
        assertThat( comboIds, hasItem( coc.getUid() ) );
    }

    @Test
    public void testExpressionIsValid()
    {
        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );
        when( dimensionService.getDataDimensionalItemObject( getId( deA ) ) ).thenReturn( deA );
        when( dimensionService.getDataDimensionalItemObject( getId( deE ) ) ).thenReturn( deE );
        when( dimensionService.getDataDimensionalItemObject( getId( opA ) ) ).thenReturn( opA );
        when( dimensionService.getDataDimensionalItemObject( getId( opB ) ) ).thenReturn( opB );
        when( dimensionService.getDataDimensionalItemObject( getId( opC ) ) ).thenReturn( opC );
        when( dimensionService.getDataDimensionalItemObject( getId( opD ) ) ).thenReturn( opD );
        when( dimensionService.getDataDimensionalItemObject( getId( opE ) ) ).thenReturn( opE );
        when( dimensionService.getDataDimensionalItemObject( getId( opF ) ) ).thenReturn( opF );
        when( dimensionService.getDataDimensionalItemObject( getId( reportingRate ) ) ).thenReturn( reportingRate );
        when( organisationUnitGroupService.getOrganisationUnitGroup( groupA.getUid() ) ).thenReturn( groupA );

        assertTrue( target.expressionIsValid( expressionA, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionB, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionC, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionD, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionE, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionH, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertFalse( target.expressionIsValid( expressionK, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertFalse( target.expressionIsValid( expressionL, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionM, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionN, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionR, VALIDATION_RULE_EXPRESSION ).isValid() );

        assertTrue( target.expressionIsValid( expressionK, PREDICTOR_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionL, PREDICTOR_EXPRESSION ).isValid() );

        String expression = "#{nonExisting" + SEPARATOR + coc.getUid() + "} + 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "#{" + deA.getUid() + SEPARATOR + "999} + 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, target
            .expressionIsValid( expression, INDICATOR_EXPRESSION ) );

        expression = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "} + ( 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "12 x 4";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "1.5*AVG(" + target;

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "12 + C{nonExisting}";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "12 + OUG{nonExisting}";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );
    }

    @Test
    public void testGetExpressionDescription()
    {
        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );
        when( dimensionService.getDataDimensionalItemObject( getId( opA ) ) ).thenReturn( opA );
        when( dimensionService.getDataDimensionalItemObject( getId( opB ) ) ).thenReturn( opB );

        String description = target.getExpressionDescription( expressionA, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "+" + opB.getDisplayName() ) );

        description = target.getExpressionDescription( expressionD, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "+" + ExpressionService.DAYS_DESCRIPTION ) );

        description = target.getExpressionDescription( expressionE, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "*" + constantA.getDisplayName() ) );

        when( organisationUnitGroupService.getOrganisationUnitGroup( groupA.getUid() ) ).thenReturn( groupA );
        description = target.getExpressionDescription( expressionH, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "*" + groupA.getDisplayName() ) );

        when( dimensionService.getDataDimensionalItemObject( getId( deA ) ) ).thenReturn( deA );
        description = target.getExpressionDescription( expressionM, INDICATOR_EXPRESSION );
        assertThat( description, is( deA.getDisplayName() + "-" + deB.getDisplayName() + " " + coc.getDisplayName() ) );

        when( dimensionService.getDataDimensionalItemObject( getId( reportingRate ) ) ).thenReturn( reportingRate );
        description = target.getExpressionDescription( expressionR, INDICATOR_EXPRESSION );
        assertThat( description,
            is( deB.getDisplayName() + " " + coc.getDisplayName() + " + " + reportingRate.getDisplayName() ) );
    }

    @Test
    public void testGetExpressionValue()
    {
        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 26d );
        valueMap.put( new DataElementOperand( deB, cocA ), 16d );
        valueMap.put( reportingRate, 20d );

        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );

        assertEquals( 46d, target
            .getExpressionValue( expressionA, INDICATOR_EXPRESSION, valueMap, constantMap(), null, null, NEVER_SKIP ),
            DELTA );
        assertEquals( 17d,
            target
                .getExpressionValue( expressionD, INDICATOR_EXPRESSION, valueMap, constantMap(), null, 5, NEVER_SKIP ),
            DELTA );
        assertEquals( 24d, target
            .getExpressionValue( expressionE, INDICATOR_EXPRESSION, valueMap, constantMap(), null, null, NEVER_SKIP ),
            DELTA );
        assertEquals( 36d, target
            .getExpressionValue( expressionH, INDICATOR_EXPRESSION, valueMap, constantMap(), orgUnitCountMap, null,
                NEVER_SKIP ),
            DELTA );
        assertEquals( 10d, target
            .getExpressionValue( expressionN, INDICATOR_EXPRESSION, valueMap, constantMap(), orgUnitCountMap, null,
                NEVER_SKIP ),
            DELTA );
        assertEquals( 54d, target
            .getExpressionValue( expressionR, INDICATOR_EXPRESSION, valueMap, constantMap(), orgUnitCountMap, null,
                NEVER_SKIP ),
            DELTA );
    }

    @Test
    public void testGetIndicatorValueObject()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, Collections.singletonList( period ),
            valueMap, constantMap(), null );

        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 100, value.getMultiplier() );
        assertEquals( 1, value.getDivisor() );
        assertEquals( 100d, value.getFactor(), DELTA );
        assertEquals( 200d, value.getValue(), DELTA );

        // # -------------------------------------------------------------------
        // #

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionN );
        indicatorB.setDenominator( expressionF );
        indicatorB.setAnnualized( true );

        value = target
            .getIndicatorValueObject( indicatorB, Collections.singletonList( period ), valueMap, constantMap(),
                null );

        assertEquals( 36d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 36500, value.getMultiplier() );
        assertEquals( 31, value.getDivisor() );
        assertEquals( 1177.419, value.getFactor(), DELTA );
        assertEquals( 3532.258, value.getValue(), DELTA );
    }

    @Test
    public void testSubstituteIndicatorExpressions()
    {
        String expressionZ = "if(\"A\" < 'B' and true,0,0)";

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionD );
        indicatorA.setDenominator( expressionE );

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionH );
        indicatorB.setDenominator( expressionZ );

        List<Indicator> indicators = Lists.newArrayList( indicatorA, indicatorB );

        List<Constant> constants = ImmutableList.<Constant> builder()
            .add( constantA )
            .add( constantB )
            .build();

        List<OrganisationUnitGroup> orgUnitGroups = ImmutableList.<OrganisationUnitGroup> builder()
            .add( groupA )
            .build();

        when( idObjectManager.getAllNoAcl( Constant.class ) ).thenReturn( constants );
        when( idObjectManager.getAllNoAcl( OrganisationUnitGroup.class ) ).thenReturn( orgUnitGroups );

        target.substituteIndicatorExpressions( indicators );

        assertEquals( "#{deabcdefghA." + coc.getUid() + "}+[days]", indicatorA.getExplodedNumerator() );
        assertEquals( "#{deabcdefghA." + coc.getUid() + "}*2.0", indicatorA.getExplodedDenominator() );
        assertEquals( "#{deabcdefghA." + coc.getUid() + "}*3", indicatorB.getExplodedNumerator() );
        assertEquals( expressionZ, indicatorB.getExplodedDenominator() );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void verifyExpressionIsUpdated()
    {
        Expression expression = rnd.randomObject( Expression.class );
        target.updateExpression( expression );
        verify( hibernateGenericStore ).update( expression );
    }

    @Test
    public void verifyExpressionIsDeleted()
    {
        Expression expression = rnd.randomObject( Expression.class );
        target.deleteExpression( expression );
        verify( hibernateGenericStore ).delete( expression );
    }

    @Test
    public void verifyExpressionIsAdded()
    {
        Expression expression = rnd.randomObject( Expression.class );
        long id = target.addExpression( expression );
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
    public void testGetExpressionOrgUnitGroups()
    {
        when( organisationUnitGroupService.getOrganisationUnitGroup( groupA.getUid() ) ).thenReturn( groupA );
        Set<OrganisationUnitGroup> groups = target.getExpressionOrgUnitGroups( expressionH, INDICATOR_EXPRESSION );
        assertThat( groups, hasSize( 1 ) );
        assertThat( groups, hasItem( groupA ) );

        groups = target.getExpressionOrgUnitGroups( null, INDICATOR_EXPRESSION );

        assertNotNull( groups );
        assertThat( groups, hasSize( 0 ) );
    }

    @Test
    public void testAnnualizedIndicatorValueWhenHavingMultiplePeriods()
    {
        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );

        List<Period> periods = new ArrayList<>( 6 );

        periods.add( createPeriod( "200001" ) );
        periods.add( createPeriod( "200002" ) );
        periods.add( createPeriod( "200003" ) );
        periods.add( createPeriod( "200004" ) );
        periods.add( createPeriod( "200005" ) );
        periods.add( createPeriod( "200006" ) );

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setAnnualized( true );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, periods, valueMap, constantMap(), null );

        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 36500, value.getMultiplier() );
        assertEquals( 182, value.getDivisor() );
        assertEquals( 200.55d, Precision.round( value.getFactor(), 2 ), DELTA );
        assertEquals( 401.1d, Precision.round( value.getValue(), 2 ), DELTA );
    }

    @Test
    public void testAnnualizedIndicatorValueWhenHavingNullPeriods()
    {
        when( constantService.getConstantMap() ).thenReturn(
            ImmutableMap.<String, Constant> builder()
                .put( constantA.getUid(), constantA )
                .put( constantB.getUid(), constantB )
                .build() );

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setAnnualized( true );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, null, valueMap, constantMap(), null );

        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 100, value.getMultiplier() );
        assertEquals( 1, value.getDivisor() );
        assertEquals( 100.0d, Precision.round( value.getFactor(), 2 ), DELTA );
        assertEquals( 200.0d, Precision.round( value.getValue(), 2 ), DELTA );
    }

    @Test
    public void testGetNullWithoutNumeratorDataWithDenominatorData()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionF );
        indicatorA.setDenominator( expressionP );

        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deB, coc ), 12d );
        valueMap.put( new DataElementOperand( deC, coc ), 18d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, null, valueMap, constantMap(), null );

        assertNull( value );
    }

    @Test
    public void testGetNullWithNumeratorDataWithZeroDenominatorData()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionF );
        indicatorA.setDenominator( expressionP );

        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 0d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, null, valueMap, constantMap(), null );

        assertNull( value );
    }

    private Map<String, Constant> constantMap()
    {
        Map<String, Constant> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), new Constant( "two", 2.0 ) );
        return constantMap;
    }
}