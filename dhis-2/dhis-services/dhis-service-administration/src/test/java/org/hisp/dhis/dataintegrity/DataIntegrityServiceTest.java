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
package org.hisp.dhis.dataintegrity;

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataElementGroup;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createIndicatorGroup;
import static org.hisp.dhis.DhisConvenienceTest.createIndicatorType;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnitGroup;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramRule;
import static org.hisp.dhis.DhisConvenienceTest.createProgramRuleAction;
import static org.hisp.dhis.DhisConvenienceTest.createProgramRuleVariableWithDataElement;
import static org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue.issueName;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.validation.ValidationRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith( MockitoExtension.class )
class DataIntegrityServiceTest
{

    private static final String INVALID_EXPRESSION = "INVALID_EXPRESSION";

    @Mock
    private I18nManager i18nManager;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private IndicatorService indicatorService;

    @Mock
    private DataSetService dataSetService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private OrganisationUnitGroupService organisationUnitGroupService;

    private DataElement elementA;

    private DataElement elementB;

    @Mock
    private ValidationRuleService validationRuleService;

    @Mock
    private ExpressionService expressionService;

    @Mock
    private DataEntryFormService dataEntryFormService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PeriodService periodService;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private ProgramRuleService programRuleService;

    @Mock
    private ProgramRuleVariableService programRuleVariableService;

    @Mock
    private ProgramRuleActionService programRuleActionService;

    private DefaultDataIntegrityService subject;

    private DataElementGroup elementGroupA;

    private IndicatorType indicatorTypeA;

    private Indicator indicatorA;

    private Indicator indicatorB;

    private Indicator indicatorC;

    private IndicatorGroup indicatorGroupA;

    private DataSet dataSetA;

    private DataSet dataSetB;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private OrganisationUnit unitC;

    private OrganisationUnit unitD;

    private OrganisationUnit unitE;

    private OrganisationUnit unitF;

    private OrganisationUnitGroup unitGroupA;

    private OrganisationUnitGroup unitGroupB;

    private OrganisationUnitGroup unitGroupC;

    private Program programA;

    private Program programB;

    private ProgramRule programRuleA;

    private ProgramRule programRuleB;

    private ProgramRuleVariable programRuleVariableA;

    private ProgramRuleAction programRuleActionA;

    private final BeanRandomizer rnd = BeanRandomizer.create( DataSet.class, "periodType", "workflow" );

    @BeforeEach
    public void setUp()
    {
        subject = new DefaultDataIntegrityService( i18nManager, programRuleService, programRuleActionService,
            programRuleVariableService, dataElementService, indicatorService, dataSetService,
            organisationUnitService, organisationUnitGroupService, validationRuleService, expressionService,
            dataEntryFormService, categoryService, periodService, programIndicatorService,
            mock( CacheProvider.class ), mock( DataIntegrityStore.class ), mock( SchemaService.class ) );
        setUpFixtures();
    }

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private void setUpFixtures()
    {
        // ---------------------------------------------------------------------
        // Objects
        // ---------------------------------------------------------------------

        elementA = createDataElement( 'A' );
        elementB = createDataElement( 'B' );

        indicatorTypeA = createIndicatorType( 'A' );

        indicatorA = createIndicator( 'A', indicatorTypeA );
        indicatorB = createIndicator( 'B', indicatorTypeA );
        indicatorC = createIndicator( 'C', indicatorTypeA );

        indicatorA.setNumerator( " " );
        indicatorB.setNumerator( "Numerator" );
        indicatorB.setDenominator( "Denominator" );
        indicatorC.setNumerator( "Numerator" );
        indicatorC.setDenominator( "Denominator" );

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B', unitA );
        unitC = createOrganisationUnit( 'C', unitB );
        unitD = createOrganisationUnit( 'D', unitC );
        unitE = createOrganisationUnit( 'E', unitD );
        unitF = createOrganisationUnit( 'F' );
        unitA.setParent( unitC );

        dataSetA = createDataSet( 'A', new MonthlyPeriodType() );
        dataSetB = createDataSet( 'B', new QuarterlyPeriodType() );

        dataSetA.addDataSetElement( elementA );
        dataSetA.addDataSetElement( elementB );

        dataSetA.getSources().add( unitA );
        unitA.getDataSets().add( dataSetA );

        dataSetB.addDataSetElement( elementA );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        dataSetB.addDataSetElement( elementA );

        // ---------------------------------------------------------------------
        // Groups
        // ---------------------------------------------------------------------

        elementGroupA = createDataElementGroup( 'A' );

        elementGroupA.getMembers().add( elementA );
        elementA.getGroups().add( elementGroupA );

        indicatorGroupA = createIndicatorGroup( 'A' );

        indicatorGroupA.getMembers().add( indicatorA );
        indicatorA.getGroups().add( indicatorGroupA );

        unitGroupA = createOrganisationUnitGroup( 'A' );
        unitGroupB = createOrganisationUnitGroup( 'B' );
        unitGroupC = createOrganisationUnitGroup( 'C' );

        unitGroupA.getMembers().add( unitA );
        unitGroupA.getMembers().add( unitB );
        unitGroupA.getMembers().add( unitC );
        unitA.getGroups().add( unitGroupA );
        unitB.getGroups().add( unitGroupA );
        unitC.getGroups().add( unitGroupA );

        unitGroupB.getMembers().add( unitA );
        unitGroupB.getMembers().add( unitB );
        unitGroupB.getMembers().add( unitF );
        unitA.getGroups().add( unitGroupB );
        unitB.getGroups().add( unitGroupB );
        unitF.getGroups().add( unitGroupB );

        unitGroupC.getMembers().add( unitA );
        unitA.getGroups().add( unitGroupC );

        programRuleA = createProgramRule( 'A', programA );
        programRuleB = createProgramRule( 'B', programB );

        programRuleVariableA = createProgramRuleVariableWithDataElement( 'A', programA, elementA );

        programRuleActionA = createProgramRuleAction( 'A' );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------

    @Test
    void testGetDataElementsWithoutDataSet()
    {
        subject.getDataElementsWithoutDataSet();
        verify( dataElementService ).getDataElementsWithoutDataSets();
        verifyNoMoreInteractions( dataElementService );
    }

    @Test
    void testGetDataElementsWithoutGroups()
    {
        subject.getDataElementsWithoutGroups();
        verify( dataElementService ).getDataElementsWithoutGroups();
        verifyNoMoreInteractions( dataElementService );
    }

    @Test
    void testGetDataElementsAssignedToDataSetsWithDifferentPeriodType()
    {
        String seed = "abcde";
        Map<String, DataElement> dataElements = createRandomDataElements( 6, seed );

        DataSet dataSet1 = rnd.nextObject( DataSet.class );
        dataSet1.setPeriodType( PeriodType.getPeriodTypeFromIsoString( "2011" ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 1 ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 2 ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 3 ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 4 ) );

        DataSet dataSet2 = rnd.nextObject( DataSet.class );
        dataSet2.setPeriodType( PeriodType.getByIndex( 5 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 4 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 5 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 6 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 1 ) );

        when( dataElementService.getAllDataElements() ).thenReturn( List.copyOf( dataElements.values() ) );
        when( dataSetService.getAllDataSets() ).thenReturn( List.of( dataSet1, dataSet2 ) );

        List<DataIntegrityIssue> result = subject
            .getDataElementsAssignedToDataSetsWithDifferentPeriodTypes();

        assertEquals( 2, result.size() );
        DataIntegrityIssue issue0 = result.get( 0 );
        assertEquals( seed + 1, issue0.getId() );
        assertContainsOnly( issue0.getRefs(), issueName( dataSet1 ), issueName( dataSet2 ) );
        DataIntegrityIssue issue1 = result.get( 1 );
        assertEquals( seed + 4, issue1.getId() );
        assertContainsOnly( issue1.getRefs(), issueName( dataSet1 ), issueName( dataSet2 ) );
    }

    @Test
    void testGetDataElementsAssignedToDataSetsWithDifferentPeriodTypeNoResult()
    {

        String seed = "abcde";
        Map<String, DataElement> dataElements = createRandomDataElements( 6, seed );

        DataSet dataSet1 = rnd.nextObject( DataSet.class );
        dataSet1.setPeriodType( PeriodType.getPeriodTypeFromIsoString( "2011" ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 1 ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 2 ) );
        dataSet1.addDataSetElement( dataElements.get( seed + 3 ) );

        DataSet dataSet2 = rnd.nextObject( DataSet.class );
        dataSet2.setPeriodType( PeriodType.getByIndex( 5 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 4 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 5 ) );
        dataSet2.addDataSetElement( dataElements.get( seed + 6 ) );

        when( dataElementService.getAllDataElements() ).thenReturn( new ArrayList<>( dataElements.values() ) );
        when( dataSetService.getAllDataSets() ).thenReturn( newArrayList( dataSet1, dataSet2 ) );

        assertTrue( subject.getDataElementsAssignedToDataSetsWithDifferentPeriodTypes().isEmpty() );
    }

    @Test
    void testGetDataSetsNotAssignedToOrganisationUnits()
    {
        clearInvocations( dataSetService );
        subject.getDataSetsNotAssignedToOrganisationUnits();
        verify( dataSetService ).getDataSetsNotAssignedToOrganisationUnits();
        verifyNoMoreInteractions( dataSetService );
    }

    @Test
    void testGetIndicatorsWithIdenticalFormulas()
    {
        when( indicatorService.getAllIndicators() ).thenReturn( List.of( indicatorA, indicatorB, indicatorC ) );
        List<DataIntegrityIssue> issues = subject.getIndicatorsWithIdenticalFormulas();

        assertEquals( 1, issues.size() );
        assertContainsOnly( issues.get( 0 ).getRefs(),
            issueName( indicatorB ), issueName( indicatorC ) );
    }

    @Test
    void testGetIndicatorsWithoutGroups()
    {
        subject.getIndicatorsWithoutGroups();
        verify( indicatorService ).getIndicatorsWithoutGroups();
        verifyNoMoreInteractions( dataElementService );
    }

    @Test
    void testGetOrganisationUnitsWithCyclicReferences()
    {
        subject.getOrganisationUnitsWithCyclicReferences();
        verify( organisationUnitService ).getOrganisationUnitsWithCyclicReferences();
        verifyNoMoreInteractions( organisationUnitService );
    }

    @Test
    void testGetOrphanedOrganisationUnits()
    {
        subject.getOrphanedOrganisationUnits();
        verify( organisationUnitService ).getOrphanedOrganisationUnits();
        verifyNoMoreInteractions( organisationUnitService );
    }

    @Test
    void testGetOrganisationUnitsWithoutGroups()
    {
        subject.getOrganisationUnitsWithoutGroups();
        verify( organisationUnitService ).getOrganisationUnitsWithoutGroups();
        verifyNoMoreInteractions( organisationUnitService );
    }

    @Test
    void testGetProgramRulesWithNoExpression()
    {
        programRuleB.setCondition( null );
        when( programRuleService.getProgramRulesWithNoCondition() ).thenReturn( List.of( programRuleB ) );

        List<DataIntegrityIssue> issues = subject.getProgramRulesWithNoCondition();

        verify( programRuleService ).getProgramRulesWithNoCondition();
        verify( programRuleService, times( 1 ) ).getProgramRulesWithNoCondition();

        assertEquals( 1, issues.size() );
        DataIntegrityIssue issue = issues.get( 0 );
        assertEquals( issueName( programB ), issue.getName() );
        assertContainsOnly( issue.getRefs(), issueName( programRuleB ) );
    }

    @Test
    void testGetProgramRulesVariableWithNoDataElement()
    {
        programRuleVariableA.setProgram( programA );

        when( programRuleVariableService.getVariablesWithNoDataElement() )
            .thenReturn( List.of( programRuleVariableA ) );

        List<DataIntegrityIssue> issues = subject.getProgramRuleVariablesWithNoDataElement();

        verify( programRuleVariableService ).getVariablesWithNoDataElement();
        verify( programRuleVariableService, times( 1 ) ).getVariablesWithNoDataElement();

        assertEquals( 1, issues.size() );
        DataIntegrityIssue issue = issues.get( 0 );
        assertEquals( issueName( programA ), issue.getName() );
        assertContainsOnly( issue.getRefs(), issueName( programRuleVariableA ) );
    }

    @Test
    void testGetProgramRuleActionsWithNoDataObject()
    {
        programRuleActionA.setProgramRule( programRuleA );

        when( programRuleActionService.getProgramActionsWithNoLinkToDataObject() )
            .thenReturn( List.of( programRuleActionA ) );

        List<DataIntegrityIssue> issues = subject.getProgramRuleActionsWithNoDataObject();

        verify( programRuleActionService ).getProgramActionsWithNoLinkToDataObject();
        verify( programRuleActionService, times( 1 ) ).getProgramActionsWithNoLinkToDataObject();

        assertEquals( 1, issues.size() );
        DataIntegrityIssue issue = issues.get( 0 );
        assertEquals( issueName( programRuleA ), issue.getName() );
        assertContainsOnly( issue.getRefs(), issueName( programRuleActionA ) );
    }

    @Test
    void testInvalidProgramIndicatorExpression()
    {
        ProgramIndicator programIndicator = new ProgramIndicator();
        programIndicator.setName( "Test-PI" );
        programIndicator.setExpression( "A{someuid} + 1" );

        when( programIndicatorService.expressionIsValid( anyString() ) ).thenReturn( false );
        when( programIndicatorService.getAllProgramIndicators() ).thenReturn( List.of( programIndicator ) );

        when( expressionService.getExpressionDescription( anyString(), any() ) )
            .thenThrow( new ParserException( INVALID_EXPRESSION ) );

        List<DataIntegrityIssue> issues = subject.getInvalidProgramIndicatorExpressions();

        assertNotNull( issues );
        assertEquals( 1, issues.size() );
        DataIntegrityIssue issue = issues.get( 0 );
        assertEquals( issueName( programIndicator ), issue.getName() );
        assertEquals( INVALID_EXPRESSION, issue.getComment() );
    }

    @Test
    void programIndicatorGetAggregationTypeFallbackReturnsRelatedAggregationType()
    {
        // arrange
        ProgramIndicator programIndicator = new ProgramIndicator();
        programIndicator.setName( "Test-PI" );
        programIndicator.setExpression( "#{someuid} + 1" );

        // act
        // assert
        programIndicator.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        assertEquals( AggregationType.SUM, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.LAST_IN_PERIOD );
        assertEquals( AggregationType.SUM, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.LAST );
        assertEquals( AggregationType.SUM, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.FIRST );
        assertEquals( AggregationType.SUM, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.LAST_IN_PERIOD_AVERAGE_ORG_UNIT );
        assertEquals( AggregationType.AVERAGE, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.FIRST_AVERAGE_ORG_UNIT );
        assertEquals( AggregationType.AVERAGE, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.LAST_AVERAGE_ORG_UNIT );
        assertEquals( AggregationType.AVERAGE, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( null );
        assertEquals( AggregationType.AVERAGE, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.CUSTOM );
        assertEquals( AggregationType.CUSTOM, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.AVERAGE );
        assertEquals( AggregationType.AVERAGE, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.SUM );
        assertEquals( AggregationType.SUM, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.STDDEV );
        assertEquals( AggregationType.STDDEV, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.COUNT );
        assertEquals( AggregationType.COUNT, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.MAX );
        assertEquals( AggregationType.MAX, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.MIN );
        assertEquals( AggregationType.MIN, programIndicator.getAggregationTypeFallback() );

        programIndicator.setAggregationType( AggregationType.DEFAULT );
        assertEquals( AggregationType.AVERAGE, programIndicator.getAggregationTypeFallback() );
    }

    @Test
    void testInvalidProgramIndicatorFilter()
    {
        ProgramIndicator programIndicator = new ProgramIndicator();
        programIndicator.setName( "Test-PI" );
        programIndicator.setFilter( "A{someuid} + 1" );

        when( programIndicatorService.filterIsValid( anyString() ) ).thenReturn( false );
        when( programIndicatorService.getAllProgramIndicators() ).thenReturn( List.of( programIndicator ) );

        when( expressionService.getExpressionDescription( anyString(), any() ) )
            .thenThrow( new ParserException( INVALID_EXPRESSION ) );

        List<DataIntegrityIssue> issues = subject.getInvalidProgramIndicatorFilters();

        assertEquals( 1, issues.size(), 1 );
        DataIntegrityIssue issue = issues.get( 0 );
        assertEquals( issueName( programIndicator ), issue.getName() );
        assertEquals( INVALID_EXPRESSION, issue.getComment() );
    }

    @Test
    void testValidProgramIndicatorFilter()
    {
        ProgramIndicator programIndicator = new ProgramIndicator();
        programIndicator.setName( "Test-PI" );
        programIndicator.setFilter( "1 < 2" );

        when( programIndicatorService.filterIsValid( anyString() ) ).thenReturn( true );
        when( programIndicatorService.getAllProgramIndicators() ).thenReturn( List.of( programIndicator ) );

        List<DataIntegrityIssue> issues = subject.getInvalidProgramIndicatorFilters();

        verify( expressionService, times( 0 ) ).getExpressionDescription( anyString(), any() );
        assertTrue( issues.isEmpty() );
    }

    private Map<String, DataElement> createRandomDataElements( int quantity, String uidSeed )
    {

        return IntStream.range( 1, quantity + 1 ).mapToObj( i -> {
            DataElement d = rnd.nextObject( DataElement.class );
            d.setUid( uidSeed + i );
            return d;
        } ).collect( Collectors.toMap( DataElement::getUid, Function.identity() ) );
    }
}
