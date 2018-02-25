package org.hisp.dhis.predictor;

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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.mock.MockAnalyticsService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.junit.Assert.assertEquals;

/**
 * Test the ability of predictions to access event data through analytics.
 *
 * @author Jim Grace
 */
@Category( IntegrationTest.class )
public class EventPredictionServiceTest
    extends DhisTest
{
    @Autowired
    private PredictorService predictorService;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private TrackedEntityAttributeService entityAttributeService;

    @Autowired
    TrackedEntityAttributeValueService entityAttributeValueService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private CurrentUserService currentUserService;

    private DataElementCategoryOptionCombo defaultCombo;

    private OrganisationUnit orgUnitA;

    private int testYear;

    private Period periodMar;
    private Period periodApr;
    private Period periodMay;

    private DataElement predictorOutputA;
    private DataElement predictorOutputD;
    private DataElement predictorOutputI;

    private Predictor predictorA;
    private Predictor predictorD;
    private Predictor predictorI;

    @Override
    public void setUpTest()
    {
        final String DATA_ELEMENT_A_UID = "DataElemenA";
        final String DATA_ELEMENT_D_UID = "DataElemenD";
        final String DATA_ELEMENT_I_UID = "DataElemenI";
        final String DATA_ELEMENT_X_UID = "DataElemenX";
        final String TRACKED_ENTITY_ATTRIBUTE_UID = "TEAttribute";
        final String PROGRAM_UID = "ProgramUidA";
        final String PROGRAM_INDICATOR_A_UID = "ProgramIndA";
        final String PROGRAM_INDICATOR_B_UID = "ProgramIndB";
        final String PROGRAM_TRACKED_ENTITY_ATTRIBUTE_DIMENSION_ITEM = PROGRAM_UID + SEPARATOR + TRACKED_ENTITY_ATTRIBUTE_UID;
        final String PROGRAM_DATA_ELEMENT_DIMENSION_ITEM = PROGRAM_UID + SEPARATOR + DATA_ELEMENT_X_UID;

        final String EXPRESSION_A = "SUM( A{" + PROGRAM_TRACKED_ENTITY_ATTRIBUTE_DIMENSION_ITEM + "} )"; // A - ProgramTrackedEntityAttribute
        final String EXPRESSION_D = "SUM( D{" + PROGRAM_DATA_ELEMENT_DIMENSION_ITEM + "} )"; // D - ProgramDataElement
        final String EXPRESSION_I = "SUM( I{" + PROGRAM_INDICATOR_A_UID + "} + I{" + PROGRAM_INDICATOR_B_UID + "} )"; // I - ProgramIndicators

        final String EX_INDICATOR_A = "#{" + PROGRAM_DATA_ELEMENT_DIMENSION_ITEM + "} + 4"; // Program Indicator A expression
        final String EX_INDICATOR_B = "V{enrollment_count}"; // Program Indicator B expression

        defaultCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        orgUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( orgUnitA );
        Set<OrganisationUnit> orgUnitASet = Sets.newHashSet( orgUnitA );

        PeriodType periodTypeMonthly = new MonthlyPeriodType();

        testYear = Calendar.getInstance().get( Calendar.YEAR ) - 1;

        periodMar = createPeriod( periodTypeMonthly, getDate( testYear, 3, 1 ), getDate( testYear, 3, 31 ) );
        periodApr = createPeriod( periodTypeMonthly, getDate( testYear, 4, 1 ), getDate( testYear, 4, 30 ) );
        periodMay = createPeriod( periodTypeMonthly, getDate( testYear, 5, 1 ), getDate( testYear, 5, 31 ) );

        periodService.addPeriod( periodMar );
        periodService.addPeriod( periodApr );
        periodService.addPeriod( periodMay );

        Date dateMar20 = getDate( testYear, 3, 20 );
        Date dateApr10 = getDate( testYear, 4, 10 );

        predictorOutputA = createDataElement( 'A' );
        predictorOutputD = createDataElement( 'D' );
        predictorOutputI = createDataElement( 'I' );
        DataElement dataElementX = createDataElement( 'P', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER );

        predictorOutputA.setUid( DATA_ELEMENT_A_UID );
        predictorOutputD.setUid( DATA_ELEMENT_D_UID );
        predictorOutputI.setUid( DATA_ELEMENT_I_UID );
        dataElementX.setUid( DATA_ELEMENT_X_UID );

        dataElementService.addDataElement( predictorOutputA );
        dataElementService.addDataElement( predictorOutputD );
        dataElementService.addDataElement( predictorOutputI );
        dataElementService.addDataElement( dataElementX );

        TrackedEntityAttribute entityAttribute = createTrackedEntityAttribute( 'A' );
        entityAttribute.setAggregationType( AggregationType.COUNT );
        entityAttribute.setUid( TRACKED_ENTITY_ATTRIBUTE_UID );
        entityAttributeService.addTrackedEntityAttribute( entityAttribute );

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', orgUnitA, entityAttribute );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue( entityAttribute, entityInstance );
        trackedEntityAttributeValue.setValue( "123" );
        entityAttributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );

        entityInstance.setTrackedEntityAttributeValues( Sets.newHashSet( trackedEntityAttributeValue ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstance );

        Program program = createProgram( 'A', null,
            Sets.newHashSet( entityAttribute ), orgUnitASet, null );
        program.setUid( PROGRAM_UID );
        programService.addProgram( program );

        ProgramStage stageA = createProgramStage( 'A', 0 );
        stageA.setProgram( program );
        stageA.addDataElement( dataElementX, 1 );
        programStageService.saveProgramStage( stageA );

        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', program, EX_INDICATOR_A, null );
        programIndicatorA.setAggregationType( AggregationType.SUM );
        programIndicatorA.setUid( PROGRAM_INDICATOR_A_UID );
        programIndicatorService.addProgramIndicator( programIndicatorA );

        ProgramIndicator programIndicatorB = createProgramIndicator( 'B', program, EX_INDICATOR_B, null );
        programIndicatorB.setAnalyticsType( AnalyticsType.ENROLLMENT );
        programIndicatorB.setAggregationType( AggregationType.COUNT );
        programIndicatorB.setUid( PROGRAM_INDICATOR_B_UID );
        programIndicatorService.addProgramIndicator( programIndicatorB );

        program.setProgramStages( Sets.newHashSet( stageA ) );
        program.getProgramIndicators().add( programIndicatorA );
        program.getProgramIndicators().add( programIndicatorB );
        programService.updateProgram( program );

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program, dateMar20, dateMar20, orgUnitA );
        programInstanceService.addProgramInstance( programInstance );

        ProgramStageInstance stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance, stageA, dateMar20, dateMar20, orgUnitA );
        ProgramStageInstance stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance, stageA, dateApr10, dateApr10, orgUnitA );

        stageInstanceA.setExecutionDate( dateMar20 );
        stageInstanceB.setExecutionDate( dateApr10 );

        stageInstanceA.setAttributeOptionCombo( defaultCombo );
        stageInstanceB.setAttributeOptionCombo( defaultCombo );

        programStageInstanceService.addProgramStageInstance( stageInstanceA );
        programStageInstanceService.addProgramStageInstance( stageInstanceB );

        categoryService.addAndPruneAllOptionCombos();

        Expression expressionA = new Expression( EXPRESSION_A, "ProgramTrackedEntityAttribute" );
        Expression expressionD = new Expression( EXPRESSION_D, "ProgramDataElement" );
        Expression expressionI = new Expression( EXPRESSION_I, "ProgramIndicators" );

        expressionService.addExpression( expressionA );
        expressionService.addExpression( expressionD );
        expressionService.addExpression( expressionI );

        OrganisationUnitLevel orgUnitLevel1 = new OrganisationUnitLevel( 1, "Level1" );
        organisationUnitService.addOrganisationUnitLevel( orgUnitLevel1 );

        predictorA = createPredictor( predictorOutputA, defaultCombo, "A", expressionA, null,
            periodTypeMonthly, orgUnitLevel1, 2, 0, 0 );

        predictorD = createPredictor( predictorOutputD, defaultCombo, "D", expressionD, null,
            periodTypeMonthly, orgUnitLevel1, 2, 0, 0 );

        predictorI = createPredictor( predictorOutputI, defaultCombo, "I", expressionI, null,
            periodTypeMonthly, orgUnitLevel1, 2, 0, 0 );

        predictorService.addPredictor( predictorA );
        predictorService.addPredictor( predictorD );
        predictorService.addPredictor( predictorI );

        TrackedEntityDataValue dataValueA = new TrackedEntityDataValue( stageInstanceA, dataElementX, "4" );
        TrackedEntityDataValue dataValueB = new TrackedEntityDataValue( stageInstanceB, dataElementX, "5" );

        trackedEntityDataValueService.saveTrackedEntityDataValue( dataValueA );
        trackedEntityDataValueService.saveTrackedEntityDataValue( dataValueB );

        Map<String, Grid> itemGridMap = new HashMap<>();
        itemGridMap.put( PROGRAM_TRACKED_ENTITY_ATTRIBUTE_DIMENSION_ITEM, newGrid( PROGRAM_TRACKED_ENTITY_ATTRIBUTE_DIMENSION_ITEM, 1.0, 1.0 ) );
        itemGridMap.put( PROGRAM_DATA_ELEMENT_DIMENSION_ITEM, newGrid( PROGRAM_DATA_ELEMENT_DIMENSION_ITEM, 4.0, 5.0 ) );
        itemGridMap.put( PROGRAM_INDICATOR_A_UID, newGrid( PROGRAM_INDICATOR_A_UID, 8.0, 9.0 ) );
        itemGridMap.put( PROGRAM_INDICATOR_B_UID, newGrid( PROGRAM_INDICATOR_B_UID ) );

        MockAnalyticsService mockAnalyticsSerivce = new MockAnalyticsService();
        mockAnalyticsSerivce.setItemGridMap( itemGridMap );

        setDependency( predictionService, "analyticsService", mockAnalyticsSerivce, AnalyticsService.class );

        CurrentUserService mockCurrentUserService = new MockCurrentUserService( true, orgUnitASet, orgUnitASet );
        setDependency( predictionService, "currentUserService", mockCurrentUserService, CurrentUserService.class );
    }

    @Override
    public void tearDownTest()
    {
        setDependency( predictionService, "analyticsService", analyticsService, AnalyticsService.class );
        setDependency( predictionService, "currentUserService", currentUserService, CurrentUserService.class );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Local convenience methods
    // -------------------------------------------------------------------------

    /**
     * Make a data grid for MockAnalyticsService to return.
     *
     * @param dimensionItem Dimension item to be queried for
     * @param values (if any), starting with March 2017
     * @return the Grid, as would be returned by analytics
     */
    private Grid newGrid( String dimensionItem, double ...values )
    {
        Grid grid = new ListGrid();
        grid.addHeader( new GridHeader( DimensionalObject.DATA_X_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.PERIOD_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.ORGUNIT_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) );
        grid.addHeader( new GridHeader( VALUE_ID ) );

        int month = 201703;

        for ( double value : values )
        {
            grid.addRow();
            grid.addValue( dimensionItem );
            grid.addValue( Integer.toString( month++ ) );
            grid.addValue( orgUnitA.getUid() );
            grid.addValue( "HllvX50cXC0" );
            grid.addValue( new Double( value ) );
        }

        return grid;
    }

    /**
     * Gets a data value from the database.
     *
     * @param dataElement element of value to get
     * @param period period of value to get
     * @return the value
     */
    private String getDataValue( DataElement dataElement, Period period )
    {
        DataValue dv = dataValueService.getDataValue( dataElement, period, orgUnitA, defaultCombo, defaultCombo );

        if ( dv != null )
        {
            return dv.getValue();
        }

        return null;
    }

    /**
     * The tests for all event predictors are combined into one test, because
     * the setup time for a test is so long.
     */
    @Test
    public void testPredictEvents()
    {
        predictionService.predict( predictorA, getDate( testYear, 4, 1 ), getDate( testYear, 5, 31 ) );

        assertEquals("1", getDataValue( predictorOutputA, periodApr ) );
        assertEquals("2", getDataValue( predictorOutputA, periodMay ) );

        predictionService.predict( predictorD, getDate( testYear, 4, 1 ), getDate( testYear, 5, 31 ) );

        assertEquals("4", getDataValue( predictorOutputD, periodApr ) );
        assertEquals("9", getDataValue( predictorOutputD, periodMay ) );

        predictionService.predict( predictorI, getDate( testYear, 4, 1 ), getDate( testYear, 5, 31 ) );

        assertEquals("8", getDataValue( predictorOutputI, periodApr ) );
        assertEquals("17", getDataValue( predictorOutputI, periodMay ) );
    }
}
