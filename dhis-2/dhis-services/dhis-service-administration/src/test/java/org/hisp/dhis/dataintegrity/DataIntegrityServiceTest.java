package org.hisp.dhis.dataintegrity;

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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryFormService;
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
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.validation.ValidationRuleService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataIntegrityServiceTest
{

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

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultDataIntegrityService subject;

    private BeanRandomizer rnd;

    private DataElement elementA;
    private DataElement elementB;

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
    private List<OrganisationUnit> allOrgUnits;

    private OrganisationUnitGroup unitGroupA;
    private OrganisationUnitGroup unitGroupB;
    private OrganisationUnitGroup unitGroupC;
    private OrganisationUnitGroup unitGroupD;

    @Before
    public void setUp()
    {
        subject = new DefaultDataIntegrityService( i18nManager, dataElementService, indicatorService, dataSetService,
                organisationUnitService, organisationUnitGroupService, validationRuleService, expressionService,
                dataEntryFormService, categoryService, periodService, programIndicatorService );
        rnd = new BeanRandomizer();
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
        allOrgUnits = Lists.newArrayList(unitA, unitB, unitC, unitD, unitE, unitF);

        dataSetA = createDataSet( 'A', new MonthlyPeriodType() );
        dataSetB = createDataSet( 'B', new QuarterlyPeriodType() );

        dataSetA.addDataSetElement( elementA );
        dataSetA.addDataSetElement( elementB );

        dataSetA.getSources().add( unitA );
        unitA.getDataSets().add( dataSetA );

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
        unitGroupD = createOrganisationUnitGroup( 'D' );

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

    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetDataElementsWithoutDataSet()
    {
        subject.getDataElementsWithoutDataSet();
        verify(dataElementService).getDataElementsWithoutDataSets();
        verifyNoMoreInteractions(dataElementService);
    }

    @Test
    public void testGetDataElementsWithoutGroups()
    {
        subject.getDataElementsWithoutGroups();
        verify(dataElementService).getDataElementsWithoutGroups();
        verifyNoMoreInteractions(dataElementService);
    }

    @Test
    public void testGetDataElementsAssignedToDataSetsWithDifferentPeriodType()
    {
        String seed = "abcde";
        Map<String, DataElement> dataElements = createRandomDataElements(6, seed);

        DataSet dataSet1 = rnd.randomObject( DataSet.class, "periodType", "workflow" );
        dataSet1.setPeriodType( PeriodType.getPeriodTypeFromIsoString( "2011" ) );
        dataSet1.addDataSetElement( dataElements.get(seed + 1) );
        dataSet1.addDataSetElement( dataElements.get(seed + 2) );
        dataSet1.addDataSetElement( dataElements.get(seed + 3) );
        dataSet1.addDataSetElement( dataElements.get(seed + 4) );

        DataSet dataSet2 = rnd.randomObject( DataSet.class, "periodType", "workflow" );
        dataSet2.setPeriodType( PeriodType.getByIndex( 5 ) );
        dataSet2.addDataSetElement( dataElements.get(seed + 4) );
        dataSet2.addDataSetElement( dataElements.get(seed + 5) );
        dataSet2.addDataSetElement( dataElements.get(seed + 6) );
        dataSet2.addDataSetElement( dataElements.get(seed + 1) );

        when( dataElementService.getAllDataElements() ).thenReturn(new ArrayList<>(dataElements.values()));
        when( dataSetService.getAllDataSets() ).thenReturn( newArrayList( dataSet1, dataSet2 ) );

        SortedMap<DataElement, Collection<DataSet>> result = subject
                .getDataElementsAssignedToDataSetsWithDifferentPeriodTypes();

        assertThat( result.get( dataElements.get(seed + 4) ), hasSize( 2 ) );
        assertThat( result.get( dataElements.get(seed + 1) ), hasSize( 2 ) );
        assertThat( result.get( dataElements.get(seed + 4) ), containsInAnyOrder( dataSet1, dataSet2 ) );
        assertThat( result.get( dataElements.get(seed + 1) ), containsInAnyOrder( dataSet1, dataSet2 ) );
    }

    @Test
    public void testGetDataElementsAssignedToDataSetsWithDifferentPeriodTypeNoResult()
    {

        String seed = "abcde";
        Map<String, DataElement> dataElements = createRandomDataElements(6, seed);

        DataSet dataSet1 = rnd.randomObject( DataSet.class, "periodType", "workflow" );
        dataSet1.setPeriodType( PeriodType.getPeriodTypeFromIsoString( "2011" ) );
        dataSet1.addDataSetElement( dataElements.get(seed + 1) );
        dataSet1.addDataSetElement( dataElements.get(seed + 2) );
        dataSet1.addDataSetElement( dataElements.get(seed + 3) );

        DataSet dataSet2 = rnd.randomObject( DataSet.class, "periodType", "workflow" );
        dataSet2.setPeriodType( PeriodType.getByIndex( 5 ) );
        dataSet2.addDataSetElement( dataElements.get(seed + 4) );
        dataSet2.addDataSetElement( dataElements.get(seed + 5) );
        dataSet2.addDataSetElement( dataElements.get(seed + 6) );

        when( dataElementService.getAllDataElements() ).thenReturn(new ArrayList<>(dataElements.values()));
        when( dataSetService.getAllDataSets() ).thenReturn( newArrayList( dataSet1, dataSet2 ) );

        SortedMap<DataElement, Collection<DataSet>> result = subject
                .getDataElementsAssignedToDataSetsWithDifferentPeriodTypes();
        assertThat(result.keySet(), hasSize(0));
    }


    @Test
    public void testGetDataSetsNotAssignedToOrganisationUnits()
    {
        when(dataSetService.getAllDataSets()).thenReturn(Lists.newArrayList(dataSetA, dataSetB));
        Collection<DataSet> expected = subject.getDataSetsNotAssignedToOrganisationUnits();
        assertThat(expected, hasSize(1));
        assertThat(expected, hasItem(dataSetB));
    }

    @Test
    public void testGetIndicatorsWithIdenticalFormulas()
    {
        when(indicatorService.getAllIndicators()).thenReturn(Lists.newArrayList(indicatorA, indicatorB, indicatorC));
        Set<Set<Indicator>> expected = subject.getIndicatorsWithIdenticalFormulas();

        Collection<Indicator> violation = expected.iterator().next();
        assertThat(expected, hasSize(1));
        assertThat(violation, hasSize(2));
        assertThat(violation, hasItem(indicatorB));
        assertThat(violation, hasItem(indicatorC));
    }

    @Test
    public void testGetIndicatorsWithoutGroups()
    {
        subject.getIndicatorsWithoutGroups();
        verify(indicatorService).getIndicatorsWithoutGroups();
        verifyNoMoreInteractions(dataElementService);
    }

    @Test
    public void testGetOrganisationUnitsWithCyclicReferences()
    {
        when(organisationUnitService.getAllOrganisationUnits()).thenReturn(allOrgUnits);

        Collection<OrganisationUnit> expected = subject.getOrganisationUnitsWithCyclicReferences();
        assertThat(expected, hasSize(3));
        assertThat(expected, hasItems(unitA, unitB, unitC));
    }

    @Test
    public void testGetOrphanedOrganisationUnits()
    {
        when(organisationUnitService.getAllOrganisationUnits()).thenReturn(allOrgUnits);

        Collection<OrganisationUnit> expected = subject.getOrphanedOrganisationUnits();
        assertThat(expected, hasSize(1));
        assertThat(expected, hasItem(unitF));
    }

    @Test
    public void testGetOrganisationUnitsWithoutGroups()
    {
        subject.getOrganisationUnitsWithoutGroups();
        verify(organisationUnitService).getOrganisationUnitsWithoutGroups();
        verifyNoMoreInteractions(organisationUnitService);
    }

    private Map<String, DataElement> createRandomDataElements(int quantity, String uidSeed) {

        return IntStream.range( 1, quantity + 1 ).mapToObj(i -> {
            DataElement d = rnd.randomObject( DataElement.class );
            d.setUid( uidSeed + i );
            return d;
        } ).collect( Collectors.toMap(DataElement::getUid, Function.identity()) );
    }
}
