package org.hisp.dhis.parsing;

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

import com.google.common.collect.ImmutableList;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.AbstractMap.SimpleEntry;
import static org.hisp.dhis.common.ReportingRateMetric.*;
import static org.junit.Assert.*;

/**
 * @author Jim Grace
 */
public class ParsingServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ParsingService parsingService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ConstantService constantService;

    private OrganisationUnit orgUnitA;
    private OrganisationUnit orgUnitB;
    private OrganisationUnit orgUnitC;
    private OrganisationUnit orgUnitD;
    private OrganisationUnit orgUnitE;
    private OrganisationUnit orgUnitF;
    private OrganisationUnit orgUnitG;
    private OrganisationUnit orgUnitH;
    private OrganisationUnit orgUnitI;
    private OrganisationUnit orgUnitJ;
    private OrganisationUnit orgUnitK;
    private OrganisationUnit orgUnitL;

    private OrganisationUnitGroup orgUnitGroupA;
    private OrganisationUnitGroup orgUnitGroupB;
    private OrganisationUnitGroup orgUnitGroupC;

    private DataSet dataSetA;
    private DataSet dataSetB;

    private Period periodA;
    private Period periodB;
    private Period periodC;
    private Period periodD;
    private Period periodE;
    private Period periodF;
    private Period periodG;
    private Period periodH;
    private Period periodI;
    private Period periodJ;
    private Period periodK;
    private Period periodL;
    private Period periodM;
    private Period periodN;
    private Period periodO;
    private Period periodP;
    private Period periodQ;

    private static DataElement dataElementA;
    private static DataElement dataElementB;
    private static DataElement dataElementC;
    private static DataElement dataElementD;
    private static DataElement dataElementE;

    private static DataElementOperand dataElementOperandA;
    private static DataElementOperand dataElementOperandB;
    private static DataElementOperand dataElementOperandC;
    private static DataElementOperand dataElementOperandD;
    private static DataElementOperand dataElementOperandE;
    private static DataElementOperand dataElementOperandF;

    private static ProgramDataElementDimensionItem programDataElementA;
    private static ProgramDataElementDimensionItem programDataElementB;

    private static Program programA;
    private static Program programB;

    private static ProgramIndicator programIndicatorA;
    private static ProgramIndicator programIndicatorB;

    private static TrackedEntityAttribute trackedEntityAttributeA;
    private static TrackedEntityAttribute trackedEntityAttributeB;

    private static ProgramTrackedEntityAttributeDimensionItem programAttributeA;
    private static ProgramTrackedEntityAttributeDimensionItem programAttributeB;

    private static ReportingRate reportingRateA;
    private static ReportingRate reportingRateB;
    private static ReportingRate reportingRateC;
    private static ReportingRate reportingRateD;
    private static ReportingRate reportingRateE;
    private static ReportingRate reportingRateF;

    private Map<ExpressionItem, Double> valueMap;

    private Map<String, Double> constantMap;

    private static final Map<String, Integer> ORG_UNIT_COUNT_MAP = new HashMap<String, Integer>()
    {{
        put( "orgUnitGrpA", 1000000 );
        put( "orgUnitGrpB", 2000000 );
    }};

    private final static int DAYS = 30;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementE = createDataElement( 'E' );

        dataElementA.setUid( "dataElemenA" );
        dataElementB.setUid( "dataElemenB" );
        dataElementC.setUid( "dataElemenC" );
        dataElementD.setUid( "dataElemenD" );
        dataElementE.setUid( "dataElemenE" );

        dataElementA.setAggregationType( AggregationType.SUM );
        dataElementB.setAggregationType( AggregationType.NONE );
        dataElementC.setAggregationType( AggregationType.SUM );
        dataElementD.setAggregationType( AggregationType.NONE );
        dataElementE.setAggregationType( AggregationType.SUM );

        dataElementC.setDomainType( DataElementDomain.TRACKER );
        dataElementD.setDomainType( DataElementDomain.TRACKER );

        dataElementA.setName( "Data element A name");
        dataElementB.setName( "Data element B name");
        dataElementC.setName( "Data element C name");
        dataElementD.setName( "Data element D name");
        dataElementE.setName( "Data element E name");

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementE );

        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        dataElementOperandA = new DataElementOperand( dataElementA, defaultCategoryOptionCombo );
        dataElementOperandB = new DataElementOperand( dataElementB, defaultCategoryOptionCombo );
        dataElementOperandC = new DataElementOperand( dataElementA, defaultCategoryOptionCombo, defaultCategoryOptionCombo );
        dataElementOperandD = new DataElementOperand( dataElementB, defaultCategoryOptionCombo, defaultCategoryOptionCombo );
        dataElementOperandE = new DataElementOperand( dataElementA, null, defaultCategoryOptionCombo );
        dataElementOperandF = new DataElementOperand( dataElementB, null, defaultCategoryOptionCombo );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        programA.setUid( "programUidA" );
        programB.setUid( "programUidB" );

        programA.setName( "Program A name" );
        programB.setName( "Program B name" );

        manager.save( programA );
        manager.save( programB );

        programDataElementA = new ProgramDataElementDimensionItem( programA, dataElementC );
        programDataElementB = new ProgramDataElementDimensionItem( programB, dataElementD );

        trackedEntityAttributeA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        trackedEntityAttributeB = createTrackedEntityAttribute( 'B', ValueType.NUMBER );

        trackedEntityAttributeA.setUid( "trakEntAttA");
        trackedEntityAttributeB.setUid( "trakEntAttB");

        trackedEntityAttributeA.setAggregationType( AggregationType.SUM );
        trackedEntityAttributeB.setAggregationType( AggregationType.NONE );

        manager.save( trackedEntityAttributeA );
        manager.save( trackedEntityAttributeB );

        programAttributeA = new ProgramTrackedEntityAttributeDimensionItem( programA, trackedEntityAttributeA );
        programAttributeB = new ProgramTrackedEntityAttributeDimensionItem( programB, trackedEntityAttributeB );

        programIndicatorA = createProgramIndicator( 'A', programA, "9.0", "" );
        programIndicatorB = createProgramIndicator( 'B', programA, "19.0", "" );

        programIndicatorA.setUid( "programIndA" );
        programIndicatorB.setUid( "programIndB" );

        programIndicatorA.setName( "Program indicator A name" );
        programIndicatorB.setName( "Program indicator B name" );

        programIndicatorA.setAggregationType( AggregationType.SUM );
        programIndicatorB.setAggregationType( AggregationType.NONE );

        manager.save( programIndicatorA );
        manager.save( programIndicatorB );

        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitB = createOrganisationUnit( 'B', orgUnitA );
        orgUnitC = createOrganisationUnit( 'C', orgUnitA );
        orgUnitD = createOrganisationUnit( 'D', orgUnitA );
        orgUnitE = createOrganisationUnit( 'E', orgUnitB );
        orgUnitF = createOrganisationUnit( 'F', orgUnitC );
        orgUnitG = createOrganisationUnit( 'G', orgUnitC );
        orgUnitH = createOrganisationUnit( 'H', orgUnitC );
        orgUnitI = createOrganisationUnit( 'I', orgUnitD );
        orgUnitJ = createOrganisationUnit( 'J', orgUnitG );
        orgUnitK = createOrganisationUnit( 'K', orgUnitG );
        orgUnitL = createOrganisationUnit( 'L', orgUnitJ );

        orgUnitA.setUid( "OrgUnitUidA" );
        orgUnitB.setUid( "OrgUnitUidB" );
        orgUnitC.setUid( "OrgUnitUidC" );
        orgUnitD.setUid( "OrgUnitUidD" );
        orgUnitE.setUid( "OrgUnitUidE" );
        orgUnitF.setUid( "OrgUnitUidF" );
        orgUnitG.setUid( "OrgUnitUidG" );
        orgUnitH.setUid( "OrgUnitUidH" );
        orgUnitI.setUid( "OrgUnitUidI" );
        orgUnitJ.setUid( "OrgUnitUidJ" );
        orgUnitK.setUid( "OrgUnitUidK" );
        orgUnitL.setUid( "OrgUnitUidL" );

        organisationUnitService.addOrganisationUnit( orgUnitA );
        organisationUnitService.addOrganisationUnit( orgUnitB );
        organisationUnitService.addOrganisationUnit( orgUnitC );
        organisationUnitService.addOrganisationUnit( orgUnitD );
        organisationUnitService.addOrganisationUnit( orgUnitE );
        organisationUnitService.addOrganisationUnit( orgUnitF );
        organisationUnitService.addOrganisationUnit( orgUnitG );
        organisationUnitService.addOrganisationUnit( orgUnitH );
        organisationUnitService.addOrganisationUnit( orgUnitI );
        organisationUnitService.addOrganisationUnit( orgUnitJ );
        organisationUnitService.addOrganisationUnit( orgUnitK );
        organisationUnitService.addOrganisationUnit( orgUnitL );

        orgUnitGroupA = createOrganisationUnitGroup( 'A' );
        orgUnitGroupB = createOrganisationUnitGroup( 'B' );
        orgUnitGroupC = createOrganisationUnitGroup( 'C' );

        orgUnitGroupA.setUid( "orgUnitGrpA" );
        orgUnitGroupB.setUid( "orgUnitGrpB" );
        orgUnitGroupC.setUid( "orgUnitGrpC" );

        orgUnitGroupA.setCode( "orgUnitGroupCodeA" );
        orgUnitGroupB.setCode( "orgUnitGroupCodeB" );
        orgUnitGroupC.setCode( "orgUnitGroupCodeC" );

        orgUnitGroupA.setName( "Org unit group A name" );
        orgUnitGroupB.setName( "Org unit group B name" );
        orgUnitGroupC.setName( "Org unit group C name" );

        orgUnitGroupA.addOrganisationUnit( orgUnitB );
        orgUnitGroupA.addOrganisationUnit( orgUnitC );
        orgUnitGroupA.addOrganisationUnit( orgUnitE );
        orgUnitGroupA.addOrganisationUnit( orgUnitF );
        orgUnitGroupA.addOrganisationUnit( orgUnitG );

        orgUnitGroupB.addOrganisationUnit( orgUnitF );
        orgUnitGroupB.addOrganisationUnit( orgUnitG );
        orgUnitGroupB.addOrganisationUnit( orgUnitH );

        orgUnitGroupC.addOrganisationUnit( orgUnitC );
        orgUnitGroupC.addOrganisationUnit( orgUnitD );
        orgUnitGroupC.addOrganisationUnit( orgUnitG );
        orgUnitGroupC.addOrganisationUnit( orgUnitH );
        orgUnitGroupC.addOrganisationUnit( orgUnitI );

        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupC );

        dataSetA = createDataSet( 'A' );
        dataSetB = createDataSet( 'B' );

        dataSetA.setUid( "dataSetUidA" );
        dataSetB.setUid( "dataSetUidB" );

        dataSetA.setName( "Data set A name" );
        dataSetB.setName( "Data set B name" );

        dataSetA.setCode( "dataSetCodeA" );
        dataSetB.setCode( "dataSetCodeB" );

        dataSetA.addOrganisationUnit( orgUnitE );
        dataSetA.addOrganisationUnit( orgUnitH );
        dataSetA.addOrganisationUnit( orgUnitI );

        dataSetB.addOrganisationUnit( orgUnitF );
        dataSetB.addOrganisationUnit( orgUnitG );
        dataSetB.addOrganisationUnit( orgUnitI );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        reportingRateA = new ReportingRate( dataSetA, REPORTING_RATE );
        reportingRateB = new ReportingRate( dataSetA, REPORTING_RATE_ON_TIME );
        reportingRateC = new ReportingRate( dataSetA, ACTUAL_REPORTS );
        reportingRateD = new ReportingRate( dataSetA, ACTUAL_REPORTS_ON_TIME );
        reportingRateE = new ReportingRate( dataSetA, EXPECTED_REPORTS );
        reportingRateF = new ReportingRate( dataSetB );

        reportingRateA.setUid( "reportRateA" );
        reportingRateB.setUid( "reportRateB" );
        reportingRateC.setUid( "reportRateC" );
        reportingRateD.setUid( "reportRateD" );
        reportingRateE.setUid( "reportRateE" );
        reportingRateF.setUid( "reportRateF" );

        periodA = createPeriod( "200105" );
        periodB = createPeriod( "200106" );
        periodC = createPeriod( "200107" );

        periodD = createPeriod( "200205" );
        periodE = createPeriod( "200206" );
        periodF = createPeriod( "200207" );

        periodG = createPeriod( "200301" );
        periodH = createPeriod( "200302" );
        periodI = createPeriod( "200303" );
        periodJ = createPeriod( "200304" );
        periodK = createPeriod( "200305" );
        periodL = createPeriod( "200306" );
        periodM = createPeriod( "200307" );

        periodN = createPeriod( "200405" );
        periodO = createPeriod( "200406" );
        periodP = createPeriod( "200407" );

        periodQ = createPeriod( "200308" );

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );
        periodService.addPeriod( periodE );
        periodService.addPeriod( periodF );
        periodService.addPeriod( periodG );
        periodService.addPeriod( periodH );
        periodService.addPeriod( periodI );
        periodService.addPeriod( periodJ );
        periodService.addPeriod( periodK );
        periodService.addPeriod( periodL );
        periodService.addPeriod( periodM );
        periodService.addPeriod( periodN );
        periodService.addPeriod( periodO );
        periodService.addPeriod( periodP );
        periodService.addPeriod( periodQ );

        Constant constantA = new Constant( "One half", 0.5 );
        Constant constantB = new Constant( "One quarter", 0.25 );

        constantA.setUid( "xxxxxxxxx05" );
        constantB.setUid( "xxxxxxxx025" );

        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        constantMap = constantService.getConstantMap();

        valueMap = entries(
            entrySum( orgUnitA, periodK, dataElementA, 0.5 ),
            entrySum( orgUnitA, periodL, dataElementA, 0.25 ),
            entrySum( orgUnitA, periodM, dataElementA, 0.125 ),

            entrySum( orgUnitB, periodK, dataElementA, 1.5 ),
            entrySum( orgUnitB, periodL, dataElementA, 2.25 ),
            entrySum( orgUnitB, periodM, dataElementA, 4.125 ),

            entrySum( orgUnitC, periodK, dataElementA, 10.5 ),
            entrySum( orgUnitC, periodL, dataElementA, 20.25 ),
            entrySum( orgUnitC, periodM, dataElementA, 40.125 ),

            entrySum( orgUnitD, periodK, dataElementA, 100.5 ),
            entrySum( orgUnitD, periodL, dataElementA, 200.25 ),
            entrySum( orgUnitD, periodM, dataElementA, 400.125 ),

            entrySum( orgUnitE, periodK, dataElementA, 1.0 ),
            entrySum( orgUnitE, periodL, dataElementA, 2.0 ),
            entrySum( orgUnitE, periodM, dataElementA, 4.0 ),

            entrySum( orgUnitF, periodK, dataElementA, 100.0 ),
            entrySum( orgUnitF, periodL, dataElementA, 200.0 ),
            entrySum( orgUnitF, periodM, dataElementA, 400.0 ),

            entrySum( orgUnitG, periodA, dataElementA, 100000.0 ),
            entrySum( orgUnitG, periodB, dataElementA, 40000.0 ),
            entrySum( orgUnitG, periodC, dataElementA, 20000.0 ),
            entrySum( orgUnitG, periodD, dataElementA, 10000.0 ),
            entrySum( orgUnitG, periodE, dataElementA, 4000.0 ),
            entrySum( orgUnitG, periodF, dataElementA, 2000.0 ),

            entryAll( orgUnitG, periodG, dataElementA, 1000.0 ),
            entryAll( orgUnitG, periodH, dataElementA, 400.0 ),
            entryAll( orgUnitG, periodI, dataElementA, 200.0 ),
            entryAll( orgUnitG, periodJ, dataElementA, 100.0 ),
            entryAll( orgUnitG, periodK, dataElementA, 40.0 ),
            entryAll( orgUnitG, periodL, dataElementA, 20.0 ),
            entryAll( orgUnitG, periodM, dataElementA, 10.0 ),

            entrySum( orgUnitG, periodN, dataElementA, 4.0 ),
            entrySum( orgUnitG, periodO, dataElementA, 2.0 ),
            entrySum( orgUnitG, periodP, dataElementA, 1.0 ),

            entrySum( orgUnitG, periodQ, dataElementA, 20.0 ),

            entrySum( orgUnitH, periodK, dataElementA, 1000.0 ),
            entrySum( orgUnitH, periodL, dataElementA, 2000.0 ),
            entrySum( orgUnitH, periodM, dataElementA, 4000.0 ),

            entrySum( orgUnitI, periodK, dataElementA, 10000.0 ),
            entrySum( orgUnitI, periodL, dataElementA, 20000.0 ),
            entrySum( orgUnitI, periodM, dataElementA, 40000.0 ),

            entrySum( orgUnitJ, periodL, dataElementA, 11.0 ),

            entrySum( orgUnitK, periodL, dataElementA, 22.0 ),

            entrySum( orgUnitL, periodL, dataElementA, 55.0 ),

            entryNone( orgUnitG, periodL, dataElementB, 3.0 ),

            entrySum( orgUnitG, periodL, dataElementOperandA, 5.0 ),
            entryNone( orgUnitG, periodL, dataElementOperandB, 15.0 ),
            entrySum( orgUnitG, periodL, dataElementOperandC, 7.0 ),
            entryNone( orgUnitG, periodL, dataElementOperandD, 17.0 ),
            entrySum( orgUnitG, periodL, dataElementOperandE, 9.0 ),
            entryNone( orgUnitG, periodL, dataElementOperandF, 19.0 ),

            entrySum( orgUnitG, periodL, programDataElementA, 101.0 ),
            entryNone( orgUnitG, periodL, programDataElementB, 102.0 ),

            entrySum( orgUnitG, periodL, programAttributeA, 201.0 ),
            entryNone( orgUnitG, periodL, programAttributeB, 202.0 ),

            entrySum( orgUnitG, periodL, programIndicatorA, 301.0 ),
            entryNone( orgUnitG, periodL, programIndicatorB, 302.0 ),

            entryNull( orgUnitG, periodL, reportingRateA, 401.0 ),
            entryNull( orgUnitG, periodL, reportingRateB, 402.0 ),
            entryNull( orgUnitG, periodL, reportingRateC, 403.0 ),
            entryNull( orgUnitG, periodL, reportingRateD, 404.0 ),
            entryNull( orgUnitG, periodL, reportingRateE, 405.0 ),
            entryNull( orgUnitG, periodL, reportingRateF, 406.0 )
        );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Adds a list of entry lists to the value map.
     *
     * @param entries the lists of entries to add.
     * @return the value map.
     */
    private <K, V> Map<K, V> entries( List<Map.Entry<K, V>>... entries )
    {
        Map<K, V> map = new HashMap<K, V>();

        for ( List<Map.Entry<K, V>> list : entries )
        {
            for ( Map.Entry<K, V> entry : list )
            {
                map.put( entry.getKey(), entry.getValue() );
            }
        }

        return map;
    }

    /**
     * Creates a new Map entry.
     *
     * @param k the key.
     * @param v the value.
     * @return the Map.Entry.
     */
    private <K, V> Map.Entry<K, V> entry( K k, V v )
    {
        return new SimpleEntry<K, V>( k, v );
    }

    /**
     * Creates a list with a single map entry, aggregated by sum.
     *
     * @param orgUnit the organisation unit.
     * @param period the period.
     * @param item the dimensional item object.
     * @param value the entry value.
     * @return the list with a single map entry.
     */
    private List<Map.Entry<ExpressionItem, Double>> entrySum( OrganisationUnit orgUnit,
        Period period, DimensionalItemObject item, Double value )
    {
        return ImmutableList.of(
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.SUM ), value )
        );
    }

    /**
     * Creates a list with a single map entry, with aggregation type none.
     *
     * @param orgUnit the organisation unit.
     * @param period the period.
     * @param item the dimensional item object.
     * @param value the entry value.
     * @return the list with a single map entry.
     */
    private List<Map.Entry<ExpressionItem, Double>> entryNone( OrganisationUnit orgUnit,
        Period period, DimensionalItemObject item, Double value )
    {
        return ImmutableList.of(
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.NONE ), value )
        );
    }

    /**
     * Creates a list with a single map entry, a null aggregation type.
     *
     * @param orgUnit the organisation unit.
     * @param period the period.
     * @param item the dimensional item object.
     * @param value the entry value.
     * @return the list with a single map entry.
     */
    private List<Map.Entry<ExpressionItem, Double>> entryNull( OrganisationUnit orgUnit,
        Period period, DimensionalItemObject item, Double value )
    {
        return ImmutableList.of(
            entry( new ExpressionItem( orgUnit, period, item, null ), value )
        );
    }

    /**
     * Creates a list with a map entry for each aggregation type, with
     * incremental data values per type for testing.
     *
     * @param orgUnit the organisation unit.
     * @param period the period.
     * @param item the dimensional item object.
     * @param value the entry value.
     * @return the list with the map entries.
     */
    private List<Map.Entry<ExpressionItem, Double>> entryAll( OrganisationUnit orgUnit,
        Period period, DimensionalItemObject item, Object value )
    {
        Double val = (Double) value;

        return ImmutableList.of(
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.SUM ), val ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.MAX ), val + 1 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.MIN ), val + 2 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.AVERAGE ), val + 3 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.STDDEV ), val + 4 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.VARIANCE ), val + 5 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.LAST ), val + 6 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.AVERAGE_SUM_ORG_UNIT ), val + 7 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.LAST_AVERAGE_ORG_UNIT ), val + 8 ),
            entry( new ExpressionItem( orgUnit, period, item, AggregationType.NONE ), val + 9 )
        );
    }

    /**
     * Evaluates a test expression, both against getItemsInExpression and
     * getExpressionValue. Returns a string containing first the returned
     * value from getExpressionValue, and then the items returned from
     * getItemsInExpression, if any, separated by spaces. For example,
     * "10.2 F306deB G307deB" means that the value returned from
     * getExpressionValue was 10.0, and getItemsInExpression requested
     * the values from (orgUnitF, period 200306, dataElementB)
     * and (orgUnitG, period 200307, dataElementB).
     * <p/>
     * If the value from getExpressionValue is null, "null" is returned.
     * If it's a Double containing an integer value, just the integer is
     * returned. (This improves readabiliy for test cases not having to add
     * ".0" to each integral value.) If it's a Double non-integral value,
     * the double string is returned. If it's a string, "'value'" is returned.
     * Otherwise (unexpected) the class and string value are returned.
     * <p/>
     * The items returned from getItemsInExpression, if any, are returned
     * in abbreviated format for ease of comparing. Each item has the
     * following format:
     * <li>
     * <le>org unit letter</le>
     * <le>last digit of year and 2 digits of month</le>
     * <le>item type abbreviation and identifying letter</le>
     * <le>aggregation type</le>
     * </li>
     * For example, G306DATA_ELEMENTB-MAX means orgUnitG, period 200306,
     * dataElementB, aggregation type MAX. Since dataElementA is used frequently
     * for testing period and org unit functions, it is omitted from the item
     * string -- so G306-MAX means orgUnitG, period 200306, dataElementA,
     * aggregation type MAX. Since dataElementE is also frequently used, it is
     * abbreviated to "E"
     * <p/>
     * Also, since SUM is used frequently for testing period and org unit
     * functions, it is represented by just a dash, so G306- means orgUnitG,
     * period 200306, dataElementA, aggregation type SUM.
     *
     * @param expr expression to evaluate
     * @return result from getItemsInExpression and getExpressionValue
     */
    private String eval( String expr )
    {
        try {
            parsingService.getExpressionDescription( expr );
        }
        catch ( ParsingException ex )
        {
            return ex.getMessage();
        }

        Expression expression = new Expression( expr, expr );

        Set<ExpressionItem> items = parsingService.getExpressionItems(
            Arrays.asList( expression ), Arrays.asList( orgUnitG ), Arrays.asList( periodL ),
            constantMap, ORG_UNIT_COUNT_MAP );

        Object value = parsingService.getExpressionValue( expression, orgUnitG, periodL,
            valueMap, constantMap, ORG_UNIT_COUNT_MAP, DAYS );

        return result( value, items );
    }

    /**
     * Formats the result from getItemsInExpression and getExpressionValue
     *
     * @param value the value retuned from getExpressionValue
     * @param items the items returned from getExpressionItems
     * @return the result string
     */
    private String result( Object value, Set<ExpressionItem> items )
    {
        String valueString;

        if ( value == null )
        {
            valueString = "null";
        }
        else if ( value instanceof Double )
        {
            Double d = (double)value;

            if ( d == (double) d.intValue() )
            {
                valueString = Integer.toString( d.intValue() );
            }
            else
            {
                valueString = value.toString();
            }
        }
        else if ( value instanceof String )
        {
            valueString = "'" + ( (String) value ) + "'";
        }
        else
        {
            valueString = "Class " + value.getClass().getName() + " " + value.toString();
        }

        List<String> itemAbbreviations = getItemAbbreviations( items );

        String itemsString = String.join( " ", itemAbbreviations );

        if ( itemsString.length() != 0 )
        {
            itemsString = " " + itemsString;
        }

        return valueString + itemsString;
    }

    /**
     * Gets a list of item abbrevivations returned by getExpressionItems
     *
     * @param items the items returned by getItemsInExpression
     * @return list of abbreviations to display in results
     */
    private List<String> getItemAbbreviations( Set<ExpressionItem> items )
    {
        List<String> itemAbbreviations = new ArrayList<>();

        for ( ExpressionItem item : items )
        {
            String ou = item.getOrgUnit().getUid().substring( 10 );
            String pe = item.getPeriod().getIsoDate().substring( 3 );
            String it = item.getDimensionalItemObject().getDimensionItemType().name()
                + item.getDimensionalItemObject().getDimensionItem().substring( 10 );

            String agg = item.getAggregationType() == null ? "" : "-" + item.getAggregationType().name();

            if ( agg.equals( "-SUM" ) )
            {
                agg = "-"; // Shorthand for -SUM.
            }

            if ( it.equals( "DATA_ELEMENTA" ) )
            {
                it = "";
            }

            if ( it.equals( "DATA_ELEMENTE" ) )
            {
                it = "E";
            }

            itemAbbreviations.add( ou + pe + it + agg );
        }

        Collections.sort( itemAbbreviations );

        return itemAbbreviations;
    }

    /**
     * Make sure the expression causes an error
     *
     * @param expr The expression to test
     * @return null if error, otherwise expression description
     */
    private String error( String expr )
    {
        String description;

        try
        {
            description = parsingService.getExpressionDescription( expr );
        }
        catch ( ParsingException ex )
        {
            return null;
        }

        return "Unexpected success getting description: '" + expr + "' - '" + description + "'";
    }

    private String desc( String expr )
    {
        return parsingService.getExpressionDescription( expr );
    }

    // -------------------------------------------------------------------------
    // Test getExpressionValue
    // -------------------------------------------------------------------------

    @Test
    public void testExpressionNumeric()
    {
        // Numeric constants

        assertEquals( "2", eval( "2" ) );
        assertEquals( "2", eval( "2." ) );
        assertEquals( "2", eval( "2.0" ) );
        assertEquals( "2.1", eval( "2.1" ) );
        assertEquals( "0.2", eval( "0.2" ) );
        assertEquals( "0.2", eval( ".2" ) );
        assertEquals( "2", eval( "2E0" ) );
        assertEquals( "2", eval( "2e0" ) );
        assertEquals( "2", eval( "2.E0" ) );
        assertEquals( "2", eval( "2.0E0" ) );
        assertEquals( "2.1", eval( "2.1E0" ) );
        assertEquals( "2.1", eval( "2.1E+0" ) );
        assertEquals( "2.1", eval( "2.1E-0" ) );
        assertEquals( "0.21", eval( "2.1E-1" ) );
        assertEquals( "0.021", eval( "2.1E-2" ) );
        assertEquals( "20", eval( "2E1" ) );
        assertEquals( "20", eval( "2E+1" ) );
        assertEquals( "20", eval( "2E01" ) );
        assertEquals( "200", eval( "2E2" ) );
        assertEquals( "2", eval( "+2" ) );
        assertEquals( "-2", eval( "-2" ) );

        // Numeric operators in precedence order:

        // Exponentiation (right-to-left)

        assertEquals( "512", eval( "2 ^ 3 ^ 2" ) );
        assertEquals( "64", eval( "( 2 ^ 3 ) ^ 2" ) );
        assertEquals( "0.25", eval( "2 ^ -2" ) );

        assertEquals( "null G306- G306E-", eval( "#{dataElemenA} ^ #{dataElemenE}" ) );
        assertEquals( "null G306- G306E-", eval( "#{dataElemenE} ^ #{dataElemenA}" ) );

        // Unary +, -

        assertEquals( "5", eval( "+ (2 + 3)" ) );
        assertEquals( "-5", eval( "- (2 + 3)" ) );

        assertEquals( "null G306E-", eval( "- #{dataElemenE}" ) );

        // Unary +, - after Exponentiation

        assertEquals( "-4", eval( "-(2) ^ 2" ) );
        assertEquals( "4", eval( "(-(2)) ^ 2" ) );
        assertEquals( "4", eval( "+(2) ^ 2" ) );

        // Multiply, divide, modulus (left-to-right)

        assertEquals( "24", eval( "2 * 3 * 4" ) );
        assertEquals( "2", eval( "12 / 3 / 2" ) );
        assertEquals( "8", eval( "12 / ( 3 / 2 )" ) );
        assertEquals( "2", eval( "12 % 5 % 3" ) );
        assertEquals( "0", eval( "12 % ( 5 % 3 )" ) );
        assertEquals( "8", eval( "12 / 3 * 2" ) );
        assertEquals( "2", eval( "12 / ( 3 * 2 )" ) );
        assertEquals( "3", eval( "5 % 2 * 3" ) );
        assertEquals( "1", eval( "3 * 5 % 2" ) );
        assertEquals( "1.5", eval( "7 % 4 / 2" ) );
        assertEquals( "1", eval( "9 / 3 % 2" ) );

        assertEquals( "null G306- G306E-", eval( "#{dataElemenA} * #{dataElemenE}" ) );
        assertEquals( "null G306- G306E-", eval( "#{dataElemenE} / #{dataElemenA}" ) );
        assertEquals( "null G306- G306E-", eval( "#{dataElemenA} % #{dataElemenE}" ) );

        // Multiply, divide, modulus after Unary +, -

        assertEquals( "-6", eval( "-(3) * 2" ) );
        assertEquals( "-6", eval( "-(3 * 2)" ) );
        assertEquals( "-1.5", eval( "-(3) / 2" ) );
        assertEquals( "-1.5", eval( "-(3 / 2)" ) );
        assertEquals( "-1", eval( "-(7) % 3" ) );
        assertEquals( "-1", eval( "-(7 % 3)" ) );

        // Add, subtract (left-to-right)

        assertEquals( "9", eval( "2 + 3 + 4" ) );
        assertEquals( "9", eval( "2 + ( 3 + 4 )" ) );
        assertEquals( "-5", eval( "2 - 3 - 4" ) );
        assertEquals( "3", eval( "2 - ( 3 - 4 )" ) );
        assertEquals( "3", eval( "2 - 3 + 4" ) );
        assertEquals( "-5", eval( "2 - ( 3 + 4 )" ) );

        assertEquals( "null G306- G306E-", eval( "#{dataElemenA} + #{dataElemenE}" ) );
        assertEquals( "null G306- G306E-", eval( "#{dataElemenE} - #{dataElemenA}" ) );

        // Add, subtract after Multiply, divide, modulus

        assertEquals( "10", eval( "4 + 3 * 2" ) );
        assertEquals( "14", eval( "( 4 + 3 ) * 2" ) );
        assertEquals( "5.5", eval( "4 + 3 / 2" ) );
        assertEquals( "3.5", eval( "( 4 + 3 ) / 2" ) );
        assertEquals( "5", eval( "4 + 3 % 2" ) );
        assertEquals( "1", eval( "( 4 + 3 ) % 2" ) );

        assertEquals( "-2", eval( "4 - 3 * 2" ) );
        assertEquals( "2", eval( "( 4 - 3 ) * 2" ) );
        assertEquals( "2.5", eval( "4 - 3 / 2" ) );
        assertEquals( "0.5", eval( "( 4 - 3 ) / 2" ) );
        assertEquals( "3", eval( "4 - 3 % 2" ) );
        assertEquals( "1", eval( "( 4 - 3 ) % 2" ) );

        // Comparisons (left-to-right)

        assertEquals( "1", eval( "if(1 < 2, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 < 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(2 < 1, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 > 2, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 > 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(2 > 1, 1, 0)" ) );

        assertEquals( "1", eval( "if(1 <= 2, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 <= 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(2 <= 1, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 >= 2, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 >= 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(2 >= 1, 1, 0)" ) );

        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenA} > #{dataElemenE}, 1, 0)" ) );
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenE} < #{dataElemenA}, 1, 0)" ) );

        // Comparisons after Add, subtract

        assertEquals( "0", eval( "if(5 < 2 + 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(5 > 2 + 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 <= 2 + 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 >= 2 + 3, 1, 0)" ) );

        assertEquals( "0", eval( "if(5 < 8 - 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(5 > 8 - 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 <= 8 - 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 >= 8 - 3, 1, 0)" ) );

        assertNull( error( "if((5 < 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 > 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 <= 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 >= 2) + 3, 1, 0)" ) );

        assertNull( error( "if((5 < 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 > 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 <= 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 >= 8) - 3, 1, 0)" ) );

        // Equality

        assertEquals( "1", eval( "if(1 == 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 == 2, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 != 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 != 2, 1, 0)" ) );

        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenA} == #{dataElemenE}, 1, 0)" ) );
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenE} != #{dataElemenA}, 1, 0)" ) );

        // Equality after Comparisons

        assertEquals( "1", eval( "if(1 + 2 == 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 + 2 != 3, 1, 0)" ) );

        assertNull( error( "if(1 + (2 == 3), 1, 0)" ) );
        assertNull( error( "if(1 + (2 != 3), 1, 0)" ) );
    }

    @Test
    public void testExpressionString()
    {
        // Concatenation

        assertEquals( "1", eval( "if(\"abc123\" == \"abc\" + \"123\", 1, 0)" ) );

        // Comparisons

        assertEquals( "0", eval( "if( \"a\" < \"a\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"a\" < \"b\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"b\" < \"a\", 1, 0)" ) );

        assertEquals( "0", eval( "if( \"a\" > \"a\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"a\" > \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"b\" > \"a\", 1, 0)" ) );

        assertEquals( "1", eval( "if( \"a\" <= \"a\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"a\" <= \"b\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"b\" <= \"a\", 1, 0)" ) );

        assertEquals( "1", eval( "if( \"a\" >= \"a\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"a\" >= \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"b\" >= \"a\", 1, 0)" ) );

        // Comparisons after Concatenation

        assertEquals( "0", eval( "if( \"ab\" < \"a\" + \"b\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"ab\" > \"a\" + \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"ab\" <= \"a\" + \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"ab\" >= \"a\" + \"b\", 1, 0)" ) );

        assertNull( error( "if( (\"a\" < \"a\") + \"b\", 1, 0)" ) );
        assertNull( error( "if( (\"a\" > \"a\") + \"b\", 1, 0)" ) );
        assertNull( error( "if( (\"a\" <= \"a\") + \"b\", 1, 0)" ) );
        assertNull( error( "if( (\"a\" >= \"a\") + \"b\", 1, 0)" ) );

        // Equality

        assertEquals( "1", eval( "if( \"a\" == \"a\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"a\" == \"b\", 1, 0)" ) );

        assertEquals( "0", eval( "if( \"a\" != \"a\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"a\" != \"b\", 1, 0)" ) );
    }

    @Test
    public void testExpressionBoolean()
    {
        // Boolean constants

        assertEquals( "1", eval( "if( true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false, 1, 0)" ) );

        // Unary not

        assertEquals( "0", eval( "if( ! true, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! false, 1, 0)" ) );

        assertEquals( "null G306- G306E-", eval( "if( ! (#{dataElemenA} == #{dataElemenE}), 1, 0)" ) );

        // Unary not before comparison

        assertNull( error( "if( ! 5 > 3, 1, 0)" ) );
        assertEquals( "0", eval( "if( ! (5 > 3), 1, 0)" ) );

        // Comparison

        assertEquals( "0", eval( "if( true < true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true < false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false < true, 1, 0)" ) );

        assertEquals( "0", eval( "if( true > true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true > false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false > true, 1, 0)" ) );

        assertEquals( "1", eval( "if( true <= true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true <= false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false <= true, 1, 0)" ) );

        assertEquals( "1", eval( "if( true >= true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true >= false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false >= true, 1, 0)" ) );

        // Comparison after Unary not

        assertEquals( "0", eval( "if( ! true < false, 1, 0)" ) );
        assertEquals( "0", eval( "if( ! true > false, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! true <= false, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! true >= false, 1, 0)" ) );

        assertEquals( "0", eval( "if( ! ( true >= false ), 1, 0)" ) );
        assertEquals( "0", eval( "if( ! ( true > false ), 1, 0)" ) );
        assertEquals( "1", eval( "if( ! ( true <= false ), 1, 0)" ) );
        assertEquals( "1", eval( "if( ! ( true < false ), 1, 0)" ) );

        // Equality (associative, left/right parsing direction doesn't matter)

        assertEquals( "1", eval( "if( true == true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true == false, 1, 0)" ) );

        assertEquals( "0", eval( "if( true != true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true != false, 1, 0)" ) );

        assertEquals( "1", eval( "if( true == false == false, 1, 0)" ) );

        // && (and)

        assertEquals( "1", eval( "if( true && true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true && false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false && true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false && false, 1, 0)" ) );

        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenA} == #{dataElemenA} && #{dataElemenE} == #{dataElemenE}, 1, 0)" ) ); // true && null
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenE} == #{dataElemenE} && #{dataElemenA} == #{dataElemenA}, 1, 0)" ) ); // null && true
        assertEquals( "0 G306- G306E-", eval( "if( #{dataElemenA} != #{dataElemenA} && #{dataElemenE} != #{dataElemenE}, 1, 0)" ) ); // false && null
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenE} != #{dataElemenE} && #{dataElemenA} != #{dataElemenA}, 1, 0)" ) ); // null && false

        // && (and) after Equality

        assertEquals( "1", eval( "if( true && 1 == 1, 1, 0)" ) );
        assertNull( error( "if( ( true && 1 ) == 1, 1, 0)" ) );

        // || (or)

        assertEquals( "1", eval( "if( true || true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true || false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false || true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false || false, 1, 0)" ) );

        assertEquals( "1 G306- G306E-", eval( "if( #{dataElemenA} == #{dataElemenA} || #{dataElemenE} == #{dataElemenE}, 1, 0)" ) ); // true && null
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenE} == #{dataElemenE} || #{dataElemenA} == #{dataElemenA}, 1, 0)" ) ); // null && true
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenA} != #{dataElemenA} || #{dataElemenE} != #{dataElemenE}, 1, 0)" ) ); // false && null
        assertEquals( "null G306- G306E-", eval( "if( #{dataElemenE} != #{dataElemenE} || #{dataElemenA} != #{dataElemenA}, 1, 0)" ) ); // null && false

        // || (or) after && (and)

        assertEquals( "1", eval( "if( true || true && false, 1, 0)" ) );
        assertEquals( "0", eval( "if( ( true || true ) && false, 1, 0)" ) );
    }

    @Test
    public void testExpressionItemsAndVariables()
    {
        assertEquals( "HllvX50cXC0", categoryService.getDefaultCategoryOptionCombo().getUid() );

        // Data element

        assertEquals( "20 G306-", eval( "#{dataElemenA}" ) );
        assertEquals( "3 G306DATA_ELEMENTB-NONE", eval( "#{dataElemenB}" ) );

        // Data element operand

        assertEquals( "5 G306DATA_ELEMENT_OPERANDA.HllvX50cXC0-", eval( "#{dataElemenA.HllvX50cXC0}" ) );
        assertEquals( "15 G306DATA_ELEMENT_OPERANDB.HllvX50cXC0-NONE", eval( "#{dataElemenB.HllvX50cXC0}" ) );
        assertEquals( "5 G306DATA_ELEMENT_OPERANDA.HllvX50cXC0-", eval( "#{dataElemenA.HllvX50cXC0.*}" ) );
        assertEquals( "15 G306DATA_ELEMENT_OPERANDB.HllvX50cXC0-NONE", eval( "#{dataElemenB.HllvX50cXC0.*}" ) );
        assertEquals( "7 G306DATA_ELEMENT_OPERANDA.HllvX50cXC0.HllvX50cXC0-", eval( "#{dataElemenA.HllvX50cXC0.HllvX50cXC0}" ) );
        assertEquals( "17 G306DATA_ELEMENT_OPERANDB.HllvX50cXC0.HllvX50cXC0-NONE", eval( "#{dataElemenB.HllvX50cXC0.HllvX50cXC0}" ) );
        assertEquals( "9 G306DATA_ELEMENT_OPERANDA.*.HllvX50cXC0-", eval( "#{dataElemenA.*.HllvX50cXC0}" ) );
        assertEquals( "19 G306DATA_ELEMENT_OPERANDB.*.HllvX50cXC0-NONE", eval( "#{dataElemenB.*.HllvX50cXC0}" ) );

        // Program data element

        assertEquals( "101 G306PROGRAM_DATA_ELEMENTA.dataElemenC-", eval( "D{programUidA.dataElemenC}" ) );
        assertEquals( "102 G306PROGRAM_DATA_ELEMENTB.dataElemenD-NONE", eval( "D{programUidB.dataElemenD}" ) );

        // Program attribute (a.k.a. Program tracked entity attribute)

        assertEquals( "201 G306PROGRAM_ATTRIBUTEA.trakEntAttA-", eval( "A{programUidA.trakEntAttA}" ) );
        assertEquals( "202 G306PROGRAM_ATTRIBUTEB.trakEntAttB-NONE", eval( "A{programUidB.trakEntAttB}" ) );

        // Program indicator

        assertEquals( "301 G306PROGRAM_INDICATORA-", eval( "I{programIndA}" ) );
        assertEquals( "302 G306PROGRAM_INDICATORB-NONE", eval( "I{programIndB}" ) );

        // Data set reporting rate

        assertEquals( "401 G306REPORTING_RATEA.REPORTING_RATE", eval( "R{dataSetUidA.REPORTING_RATE}" ) );
        assertEquals( "402 G306REPORTING_RATEA.REPORTING_RATE_ON_TIME", eval( "R{dataSetUidA.REPORTING_RATE_ON_TIME}" ) );
        assertEquals( "403 G306REPORTING_RATEA.ACTUAL_REPORTS", eval( "R{dataSetUidA.ACTUAL_REPORTS}" ) );
        assertEquals( "404 G306REPORTING_RATEA.ACTUAL_REPORTS_ON_TIME", eval( "R{dataSetUidA.ACTUAL_REPORTS_ON_TIME}" ) );
        assertEquals( "405 G306REPORTING_RATEA.EXPECTED_REPORTS", eval( "R{dataSetUidA.EXPECTED_REPORTS}" ) );
        assertEquals( "406 G306REPORTING_RATEB.REPORTING_RATE", eval( "R{dataSetUidB.REPORTING_RATE}" ) );

        // Constant

        assertEquals( "0.5", eval( "C{xxxxxxxxx05}" ) );
        assertEquals( "0.25", eval( "C{xxxxxxxx025}" ) );

        // Org unit group

        assertEquals( "1000000", eval( "OUG{orgUnitGrpA}" ) );
        assertEquals( "2000000", eval( "OUG{orgUnitGrpB}" ) );

        // Days

        assertEquals( "30", eval( "[days]" ) );
    }

    @Test
    public void testExpressionPeriods()
    {
        // Period letters, and values for dataElementA, orgUnitG, SUM:
        //
        //              Month
        //               Jan    Feb    Mar    Apr    May    Jun    Jul    Aug
        //  Year 2001                                 A      B      C
        //                                        100,000 40,000 20,000
        //       2002                                 D      E      F
        //                                         10,000  4,000  2,000
        //       2003     G      H      I      J      K      L      M      Q
        //              1,000   400    200    100    40     20     10     20
        //       2004                                N      O      P
        //                                           4      2      1

        // period( period )

        assertEquals( "10 G307-", eval( "#{dataElemenA}.period( 1 )" ) );
        assertEquals( "20 G306-", eval( "#{dataElemenA}.period( 0 )" ) );
        assertEquals( "40 G305-", eval( "#{dataElemenA}.period( -1 )" ) );
        assertEquals( "4000 G206-", eval( "#{dataElemenA}.period( -12 )" ) );

        // period( from, to )

        assertNull( error( "#{dataElemenA}.period( 1, 1 )" ) );
        assertEquals( "10 G307-", eval( "#{dataElemenA}.period( 1, 1 ).sum()" ) );
        assertEquals( "30 G306- G307-", eval( "#{dataElemenA}.period( 0, 1 ).sum()" ) );
        assertEquals( "70 G305- G306- G307-", eval( "#{dataElemenA}.period( -1, 1 ).sum()" ) );
        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 1 ).sum()" ) );

        // period( from, to, year )

        assertEquals( "2000 G207-", eval( "#{dataElemenA}.period( 1, 1, -1 ).sum()" ) );
        assertEquals( "6000 G206- G207-", eval( "#{dataElemenA}.period( 0, 1, -1 ).sum()" ) );
        assertEquals( "160000 G105- G106- G107-", eval( "#{dataElemenA}.period( -1, 1, -2 ).sum()" ) );
        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 1, 0 ).sum()" ) );

        // period( from, to, yearFrom, yearTo )

        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 1, 0, 0 ).sum()" ) );
        assertEquals( "16077 G205- G206- G207- G305- G306- G307- G405- G406- G407-", eval( "#{dataElemenA}.period( -1, 1, -1, 1 ).sum()" ) );
        assertEquals( "44022 G106- G206- G306- G406-", eval( "#{dataElemenA}.period( 0, 0, -2, 1 ).sum()" ) );

        // period( from, to, yearFrom, yearTo, period2 )

        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 0, 0, 0,   1 ).sum()" ) );
        assertEquals( "10040 G205- G305-", eval( "#{dataElemenA}.period( -1, -1, -1, -1,   -1 ).sum()" ) );

        // period( from, to, yearFrom, yearTo, from2, to2 )

        assertEquals( "10040 G205- G305-", eval( "#{dataElemenA}.period( -1, -1, -1, -1,   -1, -1 ).sum()" ) );
        assertEquals( "66666 G106- G107- G206- G207- G302- G303- G305- G306- G405- G406-", eval( "#{dataElemenA}.period( -4, -3, 0, 0,   0, 1, -2, -1,   -1, 0, 0, 1).sum()" ) );

        // Periods with no values

        assertEquals( "null G309-", eval( "#{dataElemenA}.period( 3 )" ) );
        assertEquals( "null G309- G310-", eval( "#{dataElemenA}.period( 3, 4 ).sum()" ) );
        assertEquals( "null G201- G202- G203-", eval( "#{dataElemenA}.period( -17, -15 ).sum()" ) );
        assertEquals( "null G201- G202- G203- G204-", eval( "#{dataElemenA}.period( -5, -2, -1 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204-", eval( "#{dataElemenA}.period( -5, -2, -2, -1 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G309-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   3 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G309- G310-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   3, 4 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G408- G409- G410-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   2, 4, 1 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G408- G409- G410- G508- G509- G510-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   2, 4, 1, 2 ).sum()" ) );

        // Chaining period functions (not allowed)

        assertNull( error( "#{dataElemenA}.period(-5,-2).period(2,4).sum()" ) );
        assertNull( error( "#{dataElemenA}.period(-5,-2).ouPeer(0).period(2,4).sum()" ) );
    }

    @Test
    public void testExpressionOrganisationUnits()
    {
        // Org unit letters, and values for (dataElementA, June 2003):
        //
        // Level 1             A
        //                    0.25
        //                  /  |   \
        // Level 2        B    C    D
        //             2.25  20.25  200.25
        //            /      / |  \     \
        // Level 3   E     F   G   H     I
        //           2   200  20  2000  20,000
        //                    /\
        // Level 4           J  K
        //                  11 22
        //                   |
        // Level 5           L
        //                  55

        // ouAncestor

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouAncestor( 0 )" ) );
        assertEquals( "20.25 C306-", eval( "#{dataElemenA}.ouAncestor( 1 )" ) );
        assertEquals( "0.25 A306-", eval( "#{dataElemenA}.ouAncestor( 2 )" ) );

        // ouDescendant

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouDescendant( 0 ).sum()" ) );
        assertEquals( "33 J306- K306-", eval( "#{dataElemenA}.ouDescendant( 1 ).sum()" ) );
        assertEquals( "55 L306-", eval( "#{dataElemenA}.ouDescendant( 2 ).sum()" ) );

        assertEquals( "2220 F306- G306- H306-", eval( "#{dataElemenA}.ouDescendant( 1 ).ouAncestor( 1 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouDescendant( 2 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( "222.75 B306- C306- D306-", eval( "#{dataElemenA}.ouDescendant( 1 ).ouAncestor( 2 ).sum()" ) );

        // ouLevel

        assertEquals( "0.25 A306-", eval( "#{dataElemenA}.ouLevel( 1 ).sum()" ) );
        assertEquals( "222.75 B306- C306- D306-", eval( "#{dataElemenA}.ouLevel( 2 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouLevel( 3 ).sum()" ) );
        assertEquals( "22444.75 B306- C306- D306- E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouLevel( 2, 3 ).sum()" ) );

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouLevel( 3, 4 ).ouDescendant( 0 ).sum()" ) );
        assertEquals( "null", eval( "#{dataElemenA}.ouLevel( 3, 5 ).ouDescendant( 1 ).sum()" ) );

        // ouPeer

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouPeer( 0 ).sum()" ) );
        assertEquals( "2220 F306- G306- H306-", eval( "#{dataElemenA}.ouPeer( 1 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouPeer( 2 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouPeer( 3 ).sum()" ) );

        // ouGroup

        assertEquals( "244.5 B306- C306- E306- F306- G306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpA\" ).sum()" ) );
        assertEquals( "244.5 B306- C306- E306- F306- G306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGroupCodeA\" ).sum()" ) );
        assertEquals( "244.5 B306- C306- E306- F306- G306-", eval( "#{dataElemenA}.ouGroup( \"Org unit group A name\" ).sum()" ) );
        assertEquals( "2220 F306- G306- H306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpB\" ).sum()" ) );
        assertEquals( "22240.5 C306- D306- G306- H306- I306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpC\" ).sum()" ) );

        assertEquals( "2244.5 B306- C306- E306- F306- G306- H306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpA\", \"orgUnitGrpB\" ).sum()" ) );
        assertEquals( "22444.75 B306- C306- D306- E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpA\", \"orgUnitGrpB\", \"orgUnitGrpC\" ).sum()" ) );

        assertEquals( "220 F306- G306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpA\" ).ouGroup( \"orgUnitGrpB\" ).sum()" ) );
        assertEquals( "2040.25 C306- G306- H306-", eval( "#{dataElemenA}.ouGroup( \"orgUnitGrpA\", \"orgUnitGrpB\").ouGroup( \"orgUnitGrpC\" ).sum()" ) );

        // ouDataSet

        assertEquals( "22002 E306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"dataSetUidA\" ).sum()" ) );
        assertEquals( "22002 E306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"dataSetCodeA\" ).sum()" ) );
        assertEquals( "22002 E306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"Data set A name\" ).sum()" ) );
        assertEquals( "20220 F306- G306- I306-", eval( "#{dataElemenA}.ouDataSet( \"dataSetUidB\" ).sum()" ) );

        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"dataSetUidA\", \"dataSetUidB\" ).sum()" ) );
        assertEquals( "20000 I306-", eval( "#{dataElemenA}.ouDataSet( \"dataSetUidA\").ouDataSet( \"dataSetUidB\" ).sum()" ) );
    }

    @Test
    public void testAggregations()
    {
        // Period letters, and values for (dataElementA, orgUnitG):
        //
        // For testing purposes, the values supplied depend on
        // the aggregation type override for the data element.
        //
        //                         Month
        //                         Jan    Feb    Mar    Apr    May    Jun    Jul    Aug
        // Year  2003               G      H      I      J      K      L      M      Q
        // - (SUM)                1,000   400    200    100    40     20     10     20
        // -MAX                   1,001   401    201    101    41     21     11
        // -MIN                   1,002   402    202    102    42     22     12
        // -AVERAGE               1,003   403    203    103    43     23     13
        // -STDDEV                1,004   404    204    104    44     24     14
        // -VARIANCE              1,005   405    205    105    45     25     15
        // -LAST                  1,006   406    206    106    46     26     16
        // -AVERAGE_SUM_ORG_UNIT  1,007   407    207    107    47     27     17
        // -LAST_AVERAGE_ORG_UNIT 1,008   408    208    108    48     28     18
        // -NONE                  1,009   409    209    109    49     29     19

        // sum

        assertEquals( "20 G306-", eval( "#{dataElemenA}.sum()" ) );
        assertEquals( "1770 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).sum()" ) );

        // max

        assertEquals( "21 G306-MAX", eval( "#{dataElemenA}.max()" ) );
        assertEquals( "1001 G301-MAX G302-MAX G303-MAX G304-MAX G305-MAX G306-MAX G307-MAX", eval( "#{dataElemenA}.period( -5, 1 ).max()" ) );

        // min

        assertEquals( "22 G306-MIN", eval( "#{dataElemenA}.min()" ) );
        assertEquals( "12 G301-MIN G302-MIN G303-MIN G304-MIN G305-MIN G306-MIN G307-MIN", eval( "#{dataElemenA}.period( -5, 1 ).min()" ) );

        // average

        assertEquals( "23 G306-AVERAGE", eval( "#{dataElemenA}.average()" ) );
        assertEquals( "45.5 G304-AVERAGE G305-AVERAGE G306-AVERAGE G307-AVERAGE", eval( "#{dataElemenA}.period( -2, 1 ).average()" ) );

        // stddev

        assertEquals( "24 G306-STDDEV", eval( "#{dataElemenA}.stddev()" ) );
        assertEquals( "15.275252316519467 G305-STDDEV G306-STDDEV G307-STDDEV", eval( "#{dataElemenA}.period( -1, 1 ).stddev()" ) );
        assertEquals( "0 G306-STDDEV", eval( "#{dataElemenA}.period( 0, 0 ).stddev()" ) );

        // variance

        assertEquals( "25 G306-VARIANCE", eval( "#{dataElemenA}.variance()" ) );
        assertEquals( "200 G305-VARIANCE G306-VARIANCE", eval( "#{dataElemenA}.period( -1, 0 ).variance()" ) );

        // median

        assertNull( error( "#{dataElemenA}.median()" ) );
        assertEquals( "100 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).median()" ) ); // Odd
        assertEquals( "70 G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -4, 1 ).median()" ) ); // Even

        // first

        assertNull( error( "#{dataElemenA}.first()" ) );
        assertEquals( "1000 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).first()" ) );
        assertEquals( "1000 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).first(1).sum()" ) );
        assertEquals( "1400 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).first(2).sum()" ) );

        // last

        assertEquals( "26 G306-LAST", eval( "#{dataElemenA}.last()" ) );
        assertEquals( "16 G301-LAST G302-LAST G303-LAST G304-LAST G305-LAST G306-LAST G307-LAST", eval( "#{dataElemenA}.period( -5, 1 ).last()" ) );
        assertEquals( "16 G301-LAST G302-LAST G303-LAST G304-LAST G305-LAST G306-LAST G307-LAST", eval( "#{dataElemenA}.period( -5, 1 ).last(1).sum()" ) );
        assertEquals( "42 G301-LAST G302-LAST G303-LAST G304-LAST G305-LAST G306-LAST G307-LAST", eval( "#{dataElemenA}.period( -5, 1 ).last(2).sum()" ) );

        // percentile

        assertNull( error( "#{dataElemenA}.percentile( 25 )" ) );
        assertEquals( "20 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).percentile( 25 )" ) );

        // rankHigh

        assertNull( error( "#{dataElemenA}.rankHigh( 20 )" ) );
        assertEquals( "4 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankHigh( 40 )" ) );
        assertEquals( "3 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankHigh( 20 )" ) );
        assertEquals( "1 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankHigh( 10 )" ) );

        // rankLow

        assertNull( error( "#{dataElemenA}.rankLow( 20 )" ) );
        assertEquals( "1 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankLow( 40 )" ) );
        assertEquals( "2 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankLow( 20 )" ) );
        assertEquals( "4 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankLow( 10 )" ) );

        // rankPercentile

        assertNull( error( "#{dataElemenA}.rankPercentile( 20 )" ) );
        assertEquals( "100 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankPercentile( 40 )" ) );
        assertEquals( "75 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankPercentile( 20 )" ) );
        assertEquals( "25 G305- G306- G307- G308-", eval( "#{dataElemenA}.period( -1, 2 ).rankPercentile( 10 )" ) );

        // averageSumOrgUnit

        assertEquals( "27 G306-AVERAGE_SUM_ORG_UNIT", eval( "#{dataElemenA}.averageSumOrgUnit()" ) );
        assertNull( error( "#{dataElemenA}.period(-5,-2).averageSumOrgUnit()" ) );

        // lastAverageOrgUnit

        assertEquals( "28 G306-LAST_AVERAGE_ORG_UNIT", eval( "#{dataElemenA}.lastAverageOrgUnit()" ) );
        assertNull( error( "#{dataElemenA}.period(-5,-2).lastAverageOrgUnit()" ) );

        // noAggregation

        assertEquals( "29 G306-NONE", eval( "#{dataElemenA}.noAggregation()" ) );
        assertNull( error( "#{dataElemenA}.period(-5,-2).noAggregation()" ) );
    }

    @Test
    public void testExpressionLogical()
    {
        // If function is tested elsewhere

        // IsNull

        assertEquals( "0 G306-", eval( "if( isNull( #{dataElemenA} ), 1, 0)" ) );
        assertEquals( "1 G309-", eval( "if( isNull( #{dataElemenA}.period( 3 ) ), 1, 0)" ) );

        // Coalesce

        assertEquals( "20 G306-", eval( "coalesce( #{dataElemenA} )" ) );
        assertEquals( "20 G306- G309-", eval( "coalesce( #{dataElemenA}.period( 3 ), #{dataElemenA} )" ) );
        assertEquals( "20 G306- G309- G310-", eval( "coalesce( #{dataElemenA}.period( 4 ), #{dataElemenA}.period( 3 ), #{dataElemenA} )" ) );
        assertEquals( "20 G306- G309- G310-", eval( "coalesce( #{dataElemenA}.period( 4 ), #{dataElemenA}, #{dataElemenA}.period( 3 ) )" ) );

        // Except

        assertEquals( "1000 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).first()" ) );
        assertEquals( "400 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.except(#{dataElemenA} == 1000).period( -5, 1 ).first()" ) );
        assertEquals( "100 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.except(#{dataElemenA} > 100).period( -5, 1 ).first()" ) );
    }

    @Test
    public void testGetExpressionDescription()
    {
        assertEquals( "Data element A name + Data element B name", desc("#{dataElemenA} + #{dataElemenB}") );
        assertEquals( "Data element A name.period( -5, -1 ).average() + 2 * Data element A name.period(-5,-1).stddev()", desc("#{dataElemenA}.period( -5, -1 ).average() + 2 * #{dataElemenA}.period(-5,-1).stddev()") );
        assertEquals( "Data element A name - Data element B name / Data element A name", desc("#{dataElemenA.HllvX50cXC0} - #{dataElemenB.HllvX50cXC0.HllvX50cXC0} / #{dataElemenA.*.HllvX50cXC0}") );
        assertEquals( "Program A name Data element C name*Program B name Data element D name", desc("D{programUidA.dataElemenC}*D{programUidB.dataElemenD}") );
        assertEquals( "Program A name AttributeA / Program B name AttributeB", desc("A{programUidA.trakEntAttA} / A{programUidB.trakEntAttB}") );
        assertEquals( "Program indicator A name % Program indicator A name", desc("I{programIndA} % I{programIndA}") );
        assertEquals( "Data set A name Reporting rate ^ Data set A name Actual reports", desc("R{dataSetUidA.REPORTING_RATE} ^ R{dataSetUidA.ACTUAL_REPORTS}") );
        assertEquals( "One half + One quarter", desc("C{xxxxxxxxx05} + C{xxxxxxxx025}") );
        assertEquals( "Org unit group A name - Org unit group B name", desc("OUG{orgUnitGrpA} - OUG{orgUnitGrpB}") );
        assertEquals( "1 + [Number of days]", desc("1 + [days]") );
    }

    @Test
    public void testBadExpressions()
    {
        assertNull( error( "( 1" ) );
        assertNull( error( "( 1 +" ) );
        assertNull( error( "1) + 2" ) );
        assertNull( error( "abc" ) );
        assertNull( error( "\"abc\"" ) );
        assertNull( error( "if(0, 1, 0)" ) );
        assertNull( error( "1 && true" ) );
        assertNull( error( "true && 2" ) );
        assertNull( error( "!5" ) );
    }
}
