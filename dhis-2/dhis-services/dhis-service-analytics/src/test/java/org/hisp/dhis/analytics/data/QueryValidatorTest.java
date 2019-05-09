package org.hisp.dhis.analytics.data;

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

import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class QueryValidatorTest
{
    @Mock
    private SystemSettingManager systemSettingManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultQueryValidator queryValidator;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private IndicatorType itA;
    private Indicator inA;
    private Indicator inB;

    private Program prA;

    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataElement deE;
    private DataElement deF;
    private DataElement deG;
    private DataElement deH;

    private ProgramDataElementDimensionItem pdeA;
    private ProgramDataElementDimensionItem pdeB;

    private ReportingRate rrA;

    private Period peA;
    private Period peB;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private OrganisationUnit ouD;
    private OrganisationUnit ouE;

    private DataElementGroup degA;

    private DataElementGroupSet dgsA;

    @Before
    public void setUp()
    {
        PeriodType pt = new MonthlyPeriodType();

        itA = createIndicatorType( 'A' );

        inA = createIndicator( 'A', itA );
        inB = createIndicator( 'B', itA );

        prA = createProgram( 'A' );

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        deD = createDataElement( 'D', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        deE = createDataElement( 'E', ValueType.TEXT, AggregationType.NONE );
        deF = createDataElement( 'F', ValueType.TEXT, AggregationType.NONE );
        deG = createDataElement( 'G', ValueType.INTEGER, AggregationType.SUM );
        deH = createDataElement( 'H', ValueType.INTEGER, AggregationType.SUM );


        pdeA = new ProgramDataElementDimensionItem( prA, deA );
        pdeB = new ProgramDataElementDimensionItem( prA, deB );

        DataSet dsA = createDataSet( 'A', pt );

        rrA = new ReportingRate( dsA );
        peA = PeriodType.getPeriodFromIsoString( "201501" );
        peB = PeriodType.getPeriodFromIsoString( "201502" );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        ouD = createOrganisationUnit( 'D' );
        ouE = createOrganisationUnit( 'E' );

        degA = createDataElementGroup( 'A' );
        degA.addDataElement( deA );
        degA.addDataElement( deB );

        dgsA = createDataElementGroupSet( 'A' );
        dgsA.getMembers().add( degA );

    }

    @Test
    public void validateSuccesA()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) ).build();

        queryValidator.validate( params );
    }

    @Test
    public void validateSuccesB()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB, pdeA, pdeB ) ) )
            .addFilter( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) ).build();

        queryValidator.validate( params );
    }

    @Test
    public void validateSuccesSingleIndicatorFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( inA ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureSingleIndicatorAsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, inA ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureMultipleIndicatorsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( inA, inB ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureReportingRatesAndDataElementGroupSetAsDimensions()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( rrA, inA ) ) )
            .addDimension( new BaseDimensionalObject( dgsA.getDimension(), DimensionType.DATA_ELEMENT_GROUP_SET, getList( deA ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureReportingRatesAndStartEndDates()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( rrA, inA ) ) )
            .withStartDate( getDate( 2018, 3, 1 ) )
            .withEndDate( getDate( 2018, 6, 30 ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureValueType()
    {
        deB.setValueType( ValueType.FILE_RESOURCE );

        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureAggregationType()
    {
        deB.setAggregationType( AggregationType.CUSTOM );

        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateFailureOptionCombosWithIndicators()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, inA ) ) )
            .addDimension( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID, DimensionType.DATA_X, getList() ) )
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) ).build();

        queryValidator.validate( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void validateMissingOrgUnitDimensionOutputFormatDataValueSet()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .withOutputFormat( OutputFormat.DATA_VALUE_SET ).build();

        queryValidator.validate( params );
    }

    @Test
    public void validateSuccesWithSkipDataDimensionCheck()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
                .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
                .withSkipDataDimensionValidation( true )
                .withSkipPartitioning( true )
                .build();

        queryValidator.validate( params );
    }
}
