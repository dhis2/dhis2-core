package org.hisp.dhis.validation;

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
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.mock.MockAnalyticsService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.*;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.Operator.not_equal_to;

/**
 * @author Jim Grace
 */
@Category( IntegrationTest.class )
public class EventValidationServiceTest
    extends DhisTest
{
    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;

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

    private DataElementCategoryOptionCombo defaultCombo;

    private OrganisationUnit orgUnitA;

    private int testYear;

    private Period periodMar;
    private Period periodApr;

    private int dayInPeriod;

    private ValidationRule validationRuleA;
    private ValidationRule validationRuleD;
    private ValidationRule validationRuleI;
    private ValidationRule validationRuleASliding;
    private ValidationRule validationRuleDSliding;
    private ValidationRule validationRuleISliding;

    @Override
    public void setUpTest()
    {
        final String DATA_ELEMENT_A_UID = "DataElement";
        final String TRACKED_ENTITY_ATTRIBUTE_UID = "TEAttribute";
        final String PROGRAM_UID = "ProgramABCD";
        final String PROGRAM_INDICATOR_UID = "ProgramIndA";

        final String EXPRESSION_A = "A{" + PROGRAM_UID + SEPARATOR + TRACKED_ENTITY_ATTRIBUTE_UID + "}"; // A - ProgramTrackedEntityAttribute
        final String EXPRESSION_D = "D{" + PROGRAM_UID + SEPARATOR + DATA_ELEMENT_A_UID + "}"; // D - ProgramDataElement
        final String EXPRESSION_I = "I{" + PROGRAM_INDICATOR_UID + "}"; // I - ProgramIndicator

        final String EX_INDICATOR = "#{" + PROGRAM_UID + SEPARATOR + DATA_ELEMENT_A_UID + "} + 4"; // Program Indicator expression

        defaultCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        orgUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( orgUnitA );

        PeriodType periodTypeMonthly = new MonthlyPeriodType();

        testYear = Calendar.getInstance().get(Calendar.YEAR) - 1;

        periodMar = createPeriod( periodTypeMonthly, getDate( testYear, 3, 1 ), getDate( testYear, 3, 31 ) );
        periodApr = createPeriod( periodTypeMonthly, getDate( testYear, 4, 1 ), getDate( testYear, 4, 30 ) );

        periodService.addPeriod( periodMar );
        periodService.addPeriod( periodApr );

        dayInPeriod = 15;

        Date dateMar20 = getDate( testYear, 3, 20 );
        Date dateApr10 = getDate( testYear, 4, 10 );

        DataElement dataElementA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER );
        dataElementA.setUid( DATA_ELEMENT_A_UID );
        dataElementService.addDataElement( dataElementA );

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
            Sets.newHashSet( entityAttribute ), Sets.newHashSet( orgUnitA, orgUnitA ), null);
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
        Expression expressionI = new Expression( EXPRESSION_I, "ProgramIndicator" );

        Expression expressionASliding = new Expression( EXPRESSION_A, "ProgramTrackedEntityAttribute Sliding" );
        Expression expressionDSliding = new Expression( EXPRESSION_D, "ProgramDataElement Sliding" );
        Expression expressionISliding = new Expression( EXPRESSION_I, "ProgramIndicator Sliding" );

        expressionASliding.setSlidingWindow( true );
        expressionDSliding.setSlidingWindow( true );
        expressionISliding.setSlidingWindow( true );

        expressionService.addExpression( expressionA );
        expressionService.addExpression( expressionD );
        expressionService.addExpression( expressionI );

        validationRuleA = createValidationRule( "A", not_equal_to, expressionA, expressionA, periodTypeMonthly ); // A - ProgramTrackedEntityAttribute
        validationRuleD = createValidationRule( "D", not_equal_to, expressionD, expressionD, periodTypeMonthly ); // D - ProgramDataElement
        validationRuleI = createValidationRule( "I", not_equal_to, expressionI, expressionI, periodTypeMonthly ); // I - ProgramIndicator
        validationRuleASliding = createValidationRule( "T", not_equal_to, expressionASliding, expressionASliding, periodTypeMonthly ); // A - ProgramTrackedEntityAttribute (Sliding)
        validationRuleDSliding = createValidationRule( "U", not_equal_to, expressionDSliding, expressionDSliding, periodTypeMonthly ); // D - ProgramDataElement (Sliding)
        validationRuleISliding = createValidationRule( "V", not_equal_to, expressionISliding, expressionISliding, periodTypeMonthly ); // I - ProgramIndicator (Sliding)

        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleD );
        validationRuleService.saveValidationRule( validationRuleI );
        validationRuleService.saveValidationRule( validationRuleASliding );
        validationRuleService.saveValidationRule( validationRuleDSliding );
        validationRuleService.saveValidationRule( validationRuleISliding );

        TrackedEntityDataValue dataValueA = new TrackedEntityDataValue( stageInstanceA, dataElementA, "4" );
        TrackedEntityDataValue dataValueB = new TrackedEntityDataValue( stageInstanceB, dataElementA, "5" );

        trackedEntityDataValueService.saveTrackedEntityDataValue( dataValueA );
        trackedEntityDataValueService.saveTrackedEntityDataValue( dataValueB );

        Map<Date, Grid> dateGridMap = new HashMap<>();
        dateGridMap.put( periodMar.getStartDate(), newGrid( 4, 1, 8 ) );
        dateGridMap.put( periodApr.getStartDate(), newGrid( 5, 1, 9 ) );

        MockAnalyticsService mockAnalyticsSerivce = new MockAnalyticsService();
        mockAnalyticsSerivce.setDateGridMap( dateGridMap );

        setDependency( validationService, "analyticsService", mockAnalyticsSerivce, AnalyticsService.class );
    }

    @Override
    public void tearDownTest()
    {
        setDependency( validationService, "analyticsService", analyticsService, AnalyticsService.class );
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
     * @param dataElementVal Program data element value
     * @param teAttributeVal Tracked entity attribute value
     * @param piVal Program Indicator value
     * @return the Grid, as would be returned by analytics
     */
    private Grid newGrid( double dataElementVal, double teAttributeVal, double piVal )
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
        grid.addValue( new Double( dataElementVal ) );

        grid.addRow();
        grid.addValue( "ProgramABCD.TEAttribute" );
        grid.addValue( orgUnitA.getUid() );
        grid.addValue( "HllvX50cXC0" );
        grid.addValue( new Double( teAttributeVal ) );

        grid.addRow();
        grid.addValue( "ProgramIndA" );
        grid.addValue( orgUnitA.getUid() );
        grid.addValue( "HllvX50cXC0" );
        grid.addValue( new Double( piVal ) );

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
     *
     * Asserts that a collection of ValidationResult matches a reference
     * collection. If it doesn't, log some extra diagnostic information.
     * <p>
     * This method was written in response to intermittent test failures.
     * The extra diagnostic information is an attempt to further investigate
     * the nature of the failures.
     * <p>
     * A partial stack trace is logged (just within this file), so when the
     * test is working, the check inequality can be commented out and the
     * tester can generate a reference of expected vales for each call.
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

        if ( !referenceList.equals( resultsList ) )
        {
            StringBuilder sb = new StringBuilder();

            StackTraceElement[] e = Thread.currentThread().getStackTrace();

            for ( int i = 1; i < e.length && e[ i ].getFileName().equals( e[ 1 ].getFileName() ); i++ )
            {
                sb.append("  at " ).append( e[ i ].getMethodName() )
                    .append( "(" ).append( e[ i ].getFileName() )
                    .append( ":" ).append( e[ i ].getLineNumber() ).append( ")\n" );
            }

            sb.append( formatResultsList( "Expected", referenceList ) )
                .append( formatResultsList ( "But was", resultsList ) );

            log.error( sb.toString() );
        }

        assertTrue( referenceList.equals( resultsList ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
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
    public void testEventValidate()
    {
        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleA, periodMar, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleA, periodApr, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleD, periodMar, orgUnitA, defaultCombo, 4.0, 4.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleD, periodApr, orgUnitA, defaultCombo, 5.0, 5.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleI, periodMar, orgUnitA, defaultCombo, 8.0, 8.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleI, periodApr, orgUnitA, defaultCombo, 9.0, 9.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleASliding, periodMar, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleASliding, periodApr, orgUnitA, defaultCombo, 1.0, 1.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleDSliding, periodMar, orgUnitA, defaultCombo, 4.0, 4.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleDSliding, periodApr, orgUnitA, defaultCombo, 5.0, 5.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleISliding, periodMar, orgUnitA, defaultCombo, 8.0, 8.0, dayInPeriod ) );
        reference.add( new ValidationResult( validationRuleISliding, periodApr, orgUnitA, defaultCombo, 9.0, 9.0, dayInPeriod ) );

        Date startDate = getDate( testYear, 3, 1 );
        Date endDate = getDate( testYear, 4, 30 );

        ValidationAnalysisParams params = validationService.newParamsBuilder( null, orgUnitA, startDate, endDate ).build();

        Collection<ValidationResult> results = validationService.validationAnalysis( params );

        assertResultsEquals( reference, results );
    }
}
