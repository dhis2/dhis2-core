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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.DhisConvenienceTest.createCategory;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryCombo;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOption;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataElementGroup;
import static org.hisp.dhis.DhisConvenienceTest.createDataElementGroupSet;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createIndicatorType;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith( MockitoExtension.class )
class QueryValidatorTest
{
    private DefaultQueryValidator queryValidator;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private CategoryOption coA;

    private CategoryOption coB;

    private CategoryOption coC;

    private CategoryOption coD;

    private Category caA;

    private Category caB;

    private CategoryCombo ccA;

    private CategoryCombo ccB;

    private CategoryOptionCombo cocC;

    private CategoryOptionCombo cocD;

    private IndicatorType itA;

    private Indicator inA;

    private Indicator inB;

    private Program prA;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private ProgramDataElementDimensionItem pdeA;

    private ProgramDataElementDimensionItem pdeB;

    private DataElementOperand doC;

    private DataElementOperand doD;

    private ReportingRate rrA;

    private Period peA;

    private Period peB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private DataElementGroup degA;

    private DataElementGroupSet dgsA;

    @BeforeEach
    public void setUp()
    {
        queryValidator = new DefaultQueryValidator( mock( SystemSettingManager.class ) );
        PeriodType pt = new MonthlyPeriodType();

        coA = createCategoryOption( 'A' );
        coB = createCategoryOption( 'B' );
        coC = createCategoryOption( 'C' );
        coD = createCategoryOption( 'D' );

        caA = createCategory( 'A', coA, coB );
        caB = createCategory( 'B', coC, coD );

        ccA = createCategoryCombo( 'A', caA );
        ccB = createCategoryCombo( 'B', caB );
        ccB.setSkipTotal( true );

        cocC = createCategoryOptionCombo( ccB, coC );
        cocD = createCategoryOptionCombo( ccB, coD );

        itA = createIndicatorType( 'A' );

        inA = createIndicator( 'A', itA );
        inB = createIndicator( 'B', itA );

        prA = createProgram( 'A' );

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.SUM );

        deA.setCategoryCombo( ccA );
        deB.setCategoryCombo( ccA );
        deC.setCategoryCombo( ccB );

        pdeA = new ProgramDataElementDimensionItem( prA, deA );
        pdeB = new ProgramDataElementDimensionItem( prA, deB );

        doC = new DataElementOperand( deC, cocC );
        doD = new DataElementOperand( deC, cocD );

        DataSet dsA = createDataSet( 'A', pt );

        rrA = new ReportingRate( dsA );
        peA = PeriodType.getPeriodFromIsoString( "201501" );
        peB = PeriodType.getPeriodFromIsoString( "201502" );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );

        degA = createDataElementGroup( 'A' );
        degA.addDataElement( deA );
        degA.addDataElement( deB );

        dgsA = createDataElementGroupSet( 'A' );
        dgsA.getMembers().add( degA );
    }

    @Test
    void validateSuccessA()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .build();

        queryValidator.validate( params );
    }

    @Test
    void validateSuccessB()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB, pdeA, pdeB ) ) )
            .addFilter(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        queryValidator.validate( params );
    }

    @Test
    void validateSuccessSingleIndicatorFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( inA ) ) ).build();

        queryValidator.validate( params );
    }

    @Test
    void validateSuccessSingleProgramIndicatorFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.PROGRAM_INDICATOR, getList( inA ) ) )
            .build();

        queryValidator.validate( params );
    }

    @Test
    void validateFailureNoPeriods()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList() ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, inA ) ) ).build();

        assertValidatonError( ErrorCode.E7104, params );
    }

    @Test
    void validateFailureNoDataItems()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList() ) )
            .build();

        assertValidatonError( ErrorCode.E7102, params );
    }

    @Test
    void validateFailureSingleIndicatorAsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, inA ) ) ).build();

        assertValidatonError( ErrorCode.E7108, params );
    }

    @Test
    void validateFailureSingleProgramIndicatorAsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter(
                new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.PROGRAM_INDICATOR, getList( deA, inA ) ) )
            .build();

        assertValidatonError( ErrorCode.E7108, params );
    }

    @Test
    void validateErrorSingleIndicatorAsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, inA ) ) ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7108, error.getErrorCode() );
    }

    @Test
    void validateFailureMultipleIndicatorsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( inA, inB ) ) ).build();

        assertValidatonError( ErrorCode.E7108, params );
    }

    @Test
    void validateErrorMultipleIndicatorsFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .addFilter( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( inA, inB ) ) ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7108, error.getErrorCode() );
    }

    @Test
    void validateFailureReportingRatesAndDataElementGroupSetAsDimensions()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( rrA, inA ) ) )
            .addDimension(
                new BaseDimensionalObject( dgsA.getDimension(), DimensionType.DATA_ELEMENT_GROUP_SET, getList( deA ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        assertValidatonError( ErrorCode.E7112, params );
    }

    @Test
    void validateErrorReportingRatesAndDataElementGroupSetAsDimensions()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( rrA, inA ) ) )
            .addDimension(
                new BaseDimensionalObject( dgsA.getDimension(), DimensionType.DATA_ELEMENT_GROUP_SET, getList( deA ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7112, error.getErrorCode() );
    }

    @Test
    void validateFailureReportingRatesAndStartEndDates()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( rrA, inA ) ) )
            .withStartDate( getDate( 2018, 3, 1 ) )
            .withEndDate( getDate( 2018, 6, 30 ) ).build();

        assertValidatonError( ErrorCode.E7107, params );
    }

    @Test
    void validateFailureValueType()
    {
        deB.setValueType( ValueType.FILE_RESOURCE );

        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        assertValidatonError( ErrorCode.E7115, params );
    }

    @Test
    void validateFailureAggregationType()
    {
        deB.setAggregationType( AggregationType.CUSTOM );

        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        assertValidatonError( ErrorCode.E7115, params );
    }

    @Test
    void validateFailureOptionCombosWithIndicators()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, inA ) ) )
            .addDimension( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID, DimensionType.DATA_X, getList() ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        assertValidatonError( ErrorCode.E7114, params );
    }

    @Test
    void validateMissingOrgUnitDimensionOutputFormatDataValueSet()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .withOutputFormat( OutputFormat.DATA_VALUE_SET ).build();

        assertValidatonError( ErrorCode.E7119, params );
    }

    /**
     * Asserts that the total value cannot be retrieved for data elements with
     * category combinations with skip total enabled.
     */
    @Test
    void validateFailureSkipTotalDataElements()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deC ) ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA ) ) )
            .build();

        assertValidatonError( ErrorCode.E7134, params );
    }

    /**
     * Asserts that the total value can be retrieved for data elements with
     * category combinations with skip total enabled if the query specifies all
     * categories of the category combination with items as dimensions.
     */
    @Test
    void validateSuccessSkipTotalDataElementsWithCategoryDimension()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deC ) ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA ) ) )
            .addDimension( new BaseDimensionalObject( caB.getDimension(), DimensionType.CATEGORY, getList( coC ) ) )
            .build();

        queryValidator.validate( params );
    }

    /**
     * Asserts that the total value can be retrieved for data elements with
     * category combinations with skip total enabled if the query specifies all
     * categories of the category combination with items as filters.
     */
    @Test
    void validateSuccessSkipTotalDataElementsWithCategoryFilter()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA, deC ) ) )
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA ) ) )
            .addFilter( new BaseDimensionalObject( caB.getDimension(), DimensionType.CATEGORY, getList( coD ) ) )
            .build();

        queryValidator.validate( params );
    }

    /**
     * Asserts that the total value can be retrieved for data element operands
     * with data elements category combinations with skip total enabled.
     */
    @Test
    void validateSuccessSkipTotalWithOperands()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( doC, doD ) ) )
            .addFilter(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA, peB ) ) )
            .build();

        queryValidator.validate( params );
    }

    @Test
    void validateSuccessWithSkipDataDimensionCheck()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA, ouB ) ) )
            .withSkipDataDimensionValidation( true )
            .withSkipPartitioning( true )
            .build();

        queryValidator.validate( params );
    }

    /**
     * Asserts whether the given error code is thrown by the query validator for
     * the given query.
     *
     * @param errorCode the {@link ErrorCode}.
     * @param params the {@link DataQueryParams}.
     */
    private void assertValidatonError( final ErrorCode errorCode, final DataQueryParams params )
    {
        IllegalQueryException ex = assertThrows( IllegalQueryException.class, () -> queryValidator.validate( params ) );
        assertEquals( errorCode, ex.getErrorCode() );
    }
}
