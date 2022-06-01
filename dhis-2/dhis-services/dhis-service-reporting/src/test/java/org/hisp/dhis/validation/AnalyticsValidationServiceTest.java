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
package org.hisp.dhis.validation;

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.not_equal_to;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.category.CategoryManager;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.mock.MockAnalyticsService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
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
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 */
@Slf4j
class AnalyticsValidationServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private TrackedEntityAttributeService entityAttributeService;

    @Autowired
    private TrackedEntityAttributeValueService entityAttributeValueService;

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
    private AnalyticsService analyticsService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private CategoryManager categoryManager;

    @Autowired
    private DataValidationRunner runner;

    @Autowired
    private UserService _userService;

    private CategoryOptionCombo defaultCombo;

    private OrganisationUnit orgUnitA;

    private int testYear;

    private Period periodMar;

    private Period periodApr;

    private int dayInPeriod;

    private ValidationRule ruleA;

    private ValidationRule ruleD;

    private ValidationRule ruleI;

    private ValidationRule ruleASlide;

    private ValidationRule ruleDSlide;

    private ValidationRule ruleISlide;

    private ValidationRule ruleX;

    @Override
    public void setUpTest()
    {

        this.userService = _userService;

        final String DATA_ELEMENT_A_UID = "DataElement";
        final String TRACKED_ENTITY_ATTRIBUTE_UID = "TEAttribute";
        final String PROGRAM_UID = "ProgramABCD";
        final String PROGRAM_INDICATOR_UID = "ProgramIndA";
        // A - ProgramTrackedEntityAttribute
        final String EXPRESSION_A = "A{" + PROGRAM_UID + SEPARATOR + TRACKED_ENTITY_ATTRIBUTE_UID + "}";
        // D - ProgramDataElement
        final String EXPRESSION_D = "D{" + PROGRAM_UID + SEPARATOR + DATA_ELEMENT_A_UID + "}";
        // I - ProgramIndicator
        final String EXPRESSION_I = "I{" + PROGRAM_INDICATOR_UID + "}";
        // Program Indicator expression
        final String EX_INDICATOR = "#{" + PROGRAM_UID + SEPARATOR + DATA_ELEMENT_A_UID + "} + 4";
        final String EXPRESSION_AI = EXPRESSION_A + " + " + EXPRESSION_I;
        final String EXPRESSION_DI = EXPRESSION_D + " + " + EXPRESSION_I;
        defaultCombo = categoryService.getDefaultCategoryOptionCombo();
        orgUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( orgUnitA );
        PeriodType periodTypeMonthly = new MonthlyPeriodType();
        testYear = Calendar.getInstance().get( Calendar.YEAR ) - 1;
        periodMar = createPeriod( periodTypeMonthly, getDate( testYear, 3, 1 ), getDate( testYear, 3, 31 ) );
        periodApr = createPeriod( periodTypeMonthly, getDate( testYear, 4, 1 ), getDate( testYear, 4, 30 ) );
        periodService.addPeriod( periodMar );
        periodService.addPeriod( periodApr );
        dayInPeriod = 15;
        Date dateMar20 = getDate( testYear, 3, 20 );
        Date dateApr10 = getDate( testYear, 4, 10 );
        DataElement dataElementA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM,
            DataElementDomain.TRACKER );
        dataElementA.setUid( DATA_ELEMENT_A_UID );
        dataElementService.addDataElement( dataElementA );
        TrackedEntityAttribute entityAttribute = createTrackedEntityAttribute( 'A' );
        entityAttribute.setAggregationType( AggregationType.COUNT );
        entityAttribute.setUid( TRACKED_ENTITY_ATTRIBUTE_UID );
        entityAttributeService.addTrackedEntityAttribute( entityAttribute );
        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', orgUnitA, entityAttribute );
        entityInstanceService.addTrackedEntityInstance( entityInstance );
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue( entityAttribute,
            entityInstance );
        trackedEntityAttributeValue.setValue( "123" );
        entityAttributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValue );
        entityInstance.setTrackedEntityAttributeValues( Sets.newHashSet( trackedEntityAttributeValue ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstance );
        Program program = createProgram( 'A', null, Sets.newHashSet( entityAttribute ),
            Sets.newHashSet( orgUnitA, orgUnitA ), null );
        program.setUid( PROGRAM_UID );
        programService.addProgram( program );
        ProgramStage stageA = createProgramStage( 'A', 0 );
        stageA.setProgram( program );
        stageA.addDataElement( dataElementA, 1 );
        programStageService.saveProgramStage( stageA );
        ProgramIndicator programIndicator = createProgramIndicator( 'A', program, EX_INDICATOR, null );
        programIndicator.setAggregationType( AggregationType.SUM );
        programIndicator.setUid( PROGRAM_INDICATOR_UID );
        programIndicatorService.addProgramIndicator( programIndicator );
        program.setProgramStages( Sets.newHashSet( stageA ) );
        program.getProgramIndicators().add( programIndicator );
        programService.updateProgram( program );
        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program,
            dateMar20, dateMar20, orgUnitA );
        programInstanceService.addProgramInstance( programInstance );
        ProgramStageInstance stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance,
            stageA, dateMar20, dateMar20, orgUnitA );
        ProgramStageInstance stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance,
            stageA, dateApr10, dateApr10, orgUnitA );
        stageInstanceA.setExecutionDate( dateMar20 );
        stageInstanceB.setExecutionDate( dateApr10 );
        stageInstanceA.setAttributeOptionCombo( defaultCombo );
        stageInstanceB.setAttributeOptionCombo( defaultCombo );
        programStageInstanceService.addProgramStageInstance( stageInstanceA );
        programStageInstanceService.addProgramStageInstance( stageInstanceB );
        categoryManager.addAndPruneAllOptionCombos();
        Expression expressionA = new Expression( EXPRESSION_A, "ProgramTrackedEntityAttribute" );
        Expression expressionD = new Expression( EXPRESSION_D, "ProgramDataElement" );
        Expression expressionI = new Expression( EXPRESSION_I, "ProgramIndicator" );
        Expression expressionAI = new Expression( EXPRESSION_AI, "ProgramTrackedEntityAttribute + ProgramIndicator" );
        Expression expressionASlide = new Expression( EXPRESSION_A, "ProgramTrackedEntityAttribute Slide" );
        Expression expressionDSlide = new Expression( EXPRESSION_D, "ProgramDataElement Slide" );
        Expression expressionISlide = new Expression( EXPRESSION_I, "ProgramIndicator Slide" );
        Expression expressionDISlide = new Expression( EXPRESSION_DI, "ProgramDataElement + ProgramIndicator Slide" );
        expressionASlide.setSlidingWindow( true );
        expressionDSlide.setSlidingWindow( true );
        expressionISlide.setSlidingWindow( true );
        ruleA = createValidationRule( "A", not_equal_to, expressionA, expressionA, periodTypeMonthly );
        ruleD = createValidationRule( "D", not_equal_to, expressionD, expressionD, periodTypeMonthly );
        ruleI = createValidationRule( "I", not_equal_to, expressionI, expressionI, periodTypeMonthly );
        ruleASlide = createValidationRule( "T", not_equal_to, expressionASlide, expressionASlide, periodTypeMonthly );
        ruleDSlide = createValidationRule( "U", not_equal_to, expressionDSlide, expressionDSlide, periodTypeMonthly );
        ruleISlide = createValidationRule( "V", not_equal_to, expressionISlide, expressionISlide, periodTypeMonthly );
        ruleX = createValidationRule( "X", equal_to, expressionAI, expressionDISlide, periodTypeMonthly );
        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleD );
        validationRuleService.saveValidationRule( ruleI );
        validationRuleService.saveValidationRule( ruleASlide );
        validationRuleService.saveValidationRule( ruleDSlide );
        validationRuleService.saveValidationRule( ruleISlide );
        validationRuleService.saveValidationRule( ruleX );
        Map<Date, Grid> dateGridMap = new HashMap<>();
        dateGridMap.put( periodMar.getStartDate(), newGrid( 4, 1, 8, 3 ) );
        dateGridMap.put( periodApr.getStartDate(), newGrid( 5, 1, 9, 2 ) );
        MockAnalyticsService mockAnalyticsSerivce = new MockAnalyticsService();
        mockAnalyticsSerivce.setDateGridMap( dateGridMap );
        runner.setAnalyticsService( mockAnalyticsSerivce );

        User user = createAndAddUser( Sets.newHashSet( orgUnitA ), null );
        injectSecurityContext( user );
    }

    @Override
    public void tearDownTest()
    {
        runner.setAnalyticsService( analyticsService );
    }

    // -------------------------------------------------------------------------
    // Local convenience methods
    // -------------------------------------------------------------------------
    /**
     * Make a data grid for MockAnalyticsService to return.
     *
     * @param dataElementVal Program data element value
     * @param teAttributeVal Tracked entity attribute value
     * @param piVal Program Indicator value
     * @param indicatorVal Indicator value
     * @return the Grid, as would be returned by analytics
     */
    private Grid newGrid( double dataElementVal, double teAttributeVal, double piVal, double indicatorVal )
    {
        Grid grid = new ListGrid();
        grid.addHeader( new GridHeader( DimensionalObject.DATA_X_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.ORGUNIT_DIM_ID ) );
        grid.addHeader( new GridHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) );
        grid.addHeader( new GridHeader( VALUE_ID ) );
        grid.addRow();
        grid.addValue( "ProgramABCD.DataElement" );
        grid.addValue( orgUnitA.getUid() );
        grid.addValue( "HllvX50cXC0" );
        grid.addValue( Double.valueOf( dataElementVal ) );
        grid.addRow();
        grid.addValue( "ProgramABCD.TEAttribute" );
        grid.addValue( orgUnitA.getUid() );
        grid.addValue( "HllvX50cXC0" );
        grid.addValue( Double.valueOf( teAttributeVal ) );
        grid.addRow();
        grid.addValue( "ProgramIndA" );
        grid.addValue( orgUnitA.getUid() );
        grid.addValue( "HllvX50cXC0" );
        grid.addValue( Double.valueOf( piVal ) );
        return grid;
    }

    /**
     * Returns a naturally ordered list of ValidationResults.
     * <p>
     * When comparing two collections, this assures that all the items are in
     * the same order for comparison. It also means that when there are
     * different values for the same period/rule/source, etc., the results are
     * more likely to be in the same order to make it easier to see the
     * difference.
     * <p>
     * By making this a List instead of, say a TreeSet, duplicate values (if any
     * should exist by mistake!) are preserved.
     *
     * @param results collection of ValidationResult to order.
     * @return ValidationResults in their natural order.
     */
    private List<ValidationResult> orderedList( Collection<ValidationResult> results )
    {
        List<ValidationResult> resultList = new ArrayList<>( results );
        Collections.sort( resultList );
        return resultList;
    }

    /**
     * Asserts that a collection of ValidationResult matches a reference
     * collection. If it doesn't, log some extra diagnostic information.
     * <p>
     * This method was written in response to intermittent test failures. The
     * extra diagnostic information is an attempt to further investigate the
     * nature of the failures.
     * <p>
     * A partial stack trace is logged (just within this file), so when the test
     * is working, the check inequality can be commented out and the tester can
     * generate a reference of expected vales for each call.
     * <p>
     * Also tests to be sure that each result expression was evaluated
     * correctly.
     *
     * @param reference the reference collection of ValidationResult.
     * @param results collection of ValidationResult to test.
     */
    private void assertResultsEquals( Collection<ValidationResult> reference, Collection<ValidationResult> results )
    {
        List<ValidationResult> referenceList = orderedList( reference );
        List<ValidationResult> resultsList = orderedList( results );
        boolean success = referenceList.equals( resultsList );
        if ( !success )
        {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] e = Thread.currentThread().getStackTrace();
            for ( int i = 1; i < e.length && e[i].getFileName().equals( e[1].getFileName() ); i++ )
            {
                sb.append( "  at " ).append( e[i].getMethodName() ).append( "(" ).append( e[i].getFileName() )
                    .append( ":" ).append( e[i].getLineNumber() ).append( ")\n" );
            }
            sb.append( formatResultsList( "Expected", referenceList ) )
                .append( formatResultsList( "But was", resultsList ) );
            log.error( sb.toString() );
        }
        assertTrue( success );
        for ( ValidationResult result : results )
        {
            String test = result.getLeftsideValue() + result.getValidationRule().getOperator().getMathematicalOperator()
                + result.getRightsideValue();
            assertFalse( (Boolean) expressionService.getExpressionValue( ExpressionParams.builder()
                .expression( test ).parseType( SIMPLE_TEST ).build() ) );
        }
    }

    private String formatResultsList( String label, List<ValidationResult> results )
    {
        StringBuilder sb = new StringBuilder( label + " (" + results.size() + "):\n" );
        results.forEach( r -> sb.append( "  " ).append( r.toString() ).append( "\n" ) );
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Business logic tests
    // -------------------------------------------------------------------------
    @Test
    void testAnalyticsValidate()
    {
        // Just one test, so we don't have to rebuild analytics multiple times.
        // ---------------
        // Test validation
        // ---------------
        Collection<ValidationResult> reference = new HashSet<>();
        reference.add( new ValidationResult( ruleA, periodMar, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleA, periodApr, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleD, periodMar, orgUnitA, defaultCombo, 4.0, 4.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleD, periodApr, orgUnitA, defaultCombo, 5.0, 5.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleI, periodMar, orgUnitA, defaultCombo, 8.0, 8.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleI, periodApr, orgUnitA, defaultCombo, 9.0, 9.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleASlide, periodMar, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleASlide, periodApr, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleDSlide, periodMar, orgUnitA, defaultCombo, 4.0, 4.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleDSlide, periodApr, orgUnitA, defaultCombo, 5.0, 5.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleISlide, periodMar, orgUnitA, defaultCombo, 8.0, 8.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleISlide, periodApr, orgUnitA, defaultCombo, 9.0, 9.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleX, periodMar, orgUnitA, defaultCombo, 9.0, 12.0, dayInPeriod ) );
        reference.add( new ValidationResult( ruleX, periodApr, orgUnitA, defaultCombo, 10.0, 14.0, dayInPeriod ) );
        Date startDate = getDate( testYear, 3, 1 );
        Date endDate = getDate( testYear, 4, 30 );
        ValidationAnalysisParams params1 = validationService.newParamsBuilder( null, orgUnitA, startDate, endDate )
            .build();
        Collection<ValidationResult> results = validationService.validationAnalysis( params1,
            NoopJobProgress.INSTANCE );
        assertResultsEquals( reference, results );
        // ---------------------------------------
        // Test validation rule expression details
        // ---------------------------------------
        ValidationAnalysisParams params2 = validationService
            .newParamsBuilder( Lists.newArrayList( ruleX ), orgUnitA, Lists.newArrayList( periodMar ) )
            .withAttributeOptionCombo( defaultCombo ).build();
        List<Map<String, String>> leftSideExpected = Lists.newArrayList(
            ImmutableMap.of( "name", "IndicatorA", "value", "8.0" ),
            ImmutableMap.of( "name", "ProgramA AttributeA", "value", "1.0" ) );
        List<Map<String, String>> rightSideExpected = Lists.newArrayList(
            ImmutableMap.of( "name", "IndicatorA", "value", "8.0" ),
            ImmutableMap.of( "name", "ProgramA DataElementA", "value", "4.0" ) );
        ValidationRuleExpressionDetails details = validationService.getValidationRuleExpressionDetails( params2 );
        assertEquals( leftSideExpected, details.getLeftSide() );
        assertEquals( rightSideExpected, details.getRightSide() );
    }
}
