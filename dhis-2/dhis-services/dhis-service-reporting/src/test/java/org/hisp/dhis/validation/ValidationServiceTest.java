package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.*;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.Operator.*;

/**
 * @author Jim Grace
 */
public class ValidationServiceTest
    extends DhisTest
{
    private static final Log log = LogFactory.getLog( ValidationServiceTest.class );

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataValueStore dataValueStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private DataElement dataElementD;
    private DataElement dataElementE;

    private Set<DataElementCategoryOptionCombo> optionCombos;

    private DataElementCategoryOptionCombo optionCombo;

    private Expression expressionA;
    private Expression expressionB;
    private Expression expressionC;
    private Expression expressionD;
    private Expression expressionE;
    private Expression expressionF;
    private Expression expressionG;
    private Expression expressionH;
    private Expression expressionI;
    private Expression expressionJ;
    private Expression expressionK;
    private Expression expressionL;
    private Expression expressionP;
    private Expression expressionQ;

    private DataSet dataSetWeekly;

    private DataSet dataSetMonthly;

    private DataSet dataSetYearly;

    private Period periodA;
    private Period periodB;
    private Period periodY;

    private int dayInPeriodA;
    private int dayInPeriodB;
    private int dayInPeriodY;

    private OrganisationUnit sourceA;
    private OrganisationUnit sourceB;
    private OrganisationUnit sourceC;
    private OrganisationUnit sourceD;
    private OrganisationUnit sourceE;
    private OrganisationUnit sourceF;
    private OrganisationUnit sourceG;

    private List<OrganisationUnit> sourcesA = new ArrayList<>();

    private Set<OrganisationUnit> allSources = new HashSet<>();

    private ValidationRule validationRuleA;
    private ValidationRule validationRuleB;
    private ValidationRule validationRuleC;
    private ValidationRule validationRuleD;
    private ValidationRule validationRuleE;
    private ValidationRule validationRuleF;
    private ValidationRule validationRuleG;
    private ValidationRule validationRuleP;
    private ValidationRule validationRuleQ;
    private ValidationRule validationRuleX;

    private ValidationRuleGroup group;

    private PeriodType periodTypeWeekly;
    private PeriodType periodTypeMonthly;
    private PeriodType periodTypeYearly;

    private DataElementCategoryOptionCombo defaultCombo;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        CurrentUserService currentUserService = new MockCurrentUserService( allSources, null );
        setDependency( validationService, "currentUserService", currentUserService, CurrentUserService.class );

        periodTypeWeekly = new WeeklyPeriodType();
        periodTypeMonthly = new MonthlyPeriodType();
        periodTypeYearly = new YearlyPeriodType();

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementE = createDataElement( 'E' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementE );

        optionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        String suffix = SEPARATOR + optionCombo.getUid();

        optionCombos = new HashSet<>();
        optionCombos.add( optionCombo );

        expressionA = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA" );
        expressionB = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB" );
        expressionC = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC" );
        expressionD = new Expression( "#{" + dataElementB.getUid() + suffix + "}", "expressionD" );
        expressionE = new Expression( "AVG(#{" + dataElementB.getUid() + suffix + "} * 1.5)", "expressionE" );
        expressionF = new Expression( "#{" + dataElementB.getUid() + suffix + "} / #{" + dataElementE.getUid() + suffix + "}", "expressionF" );
        expressionG = new Expression( "AVG(#{" + dataElementB.getUid() + suffix + "} * 1.5 / #{" + dataElementE.getUid() + suffix + "})", "expressionG" );
        expressionH = new Expression( "AVG(#{" + dataElementB.getUid() + suffix + "}) + 1.5*STDDEV(#{" + dataElementB.getUid() + suffix + "})", "expressionH" );
        expressionI = new Expression( "#{" + dataElementA.getUid() + suffix + "}", "expressionI" );
        expressionJ = new Expression( "#{" + dataElementB.getUid() + suffix + "}", "expressionJ" );
        expressionK = new Expression( "#{" + dataElementC.getUid() + "}", "expressionK", NEVER_SKIP );
        expressionL = new Expression( "#{" + dataElementD.getUid() + "}", "expressionL", NEVER_SKIP );
        expressionP = new Expression( SYMBOL_DAYS, "expressionP", NEVER_SKIP );
        expressionQ = new Expression( "#{" + dataElementE.getUid() + "}", "expressionQ", NEVER_SKIP );

        expressionService.addExpression( expressionA );
        expressionService.addExpression( expressionB );
        expressionService.addExpression( expressionC );
        expressionService.addExpression( expressionD );
        expressionService.addExpression( expressionE );
        expressionService.addExpression( expressionF );
        expressionService.addExpression( expressionG );
        expressionService.addExpression( expressionH );
        expressionService.addExpression( expressionI );
        expressionService.addExpression( expressionJ );
        expressionService.addExpression( expressionK );
        expressionService.addExpression( expressionL );
        expressionService.addExpression( expressionP );
        expressionService.addExpression( expressionQ );

        periodA = createPeriod( periodTypeMonthly, getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) );
        periodB = createPeriod( periodTypeMonthly, getDate( 2000, 4, 1 ), getDate( 2000, 4, 30 ) );
        periodY = createPeriod( periodTypeYearly, getDate( 2000, 1, 1 ), getDate( 2000, 12, 31 ) );

        dayInPeriodA = periodService.getDayInPeriod( periodA, new Date() );
        dayInPeriodB = periodService.getDayInPeriod( periodB, new Date() );
        dayInPeriodY = periodService.getDayInPeriod( periodY, new Date() );

        dataSetWeekly = createDataSet( 'W', periodTypeWeekly );
        dataSetMonthly = createDataSet( 'M', periodTypeMonthly );
        dataSetYearly = createDataSet( 'Y', periodTypeYearly );

        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B' );
        sourceC = createOrganisationUnit( 'C', sourceB );
        sourceD = createOrganisationUnit( 'D', sourceB );
        sourceE = createOrganisationUnit( 'E', sourceD );
        sourceF = createOrganisationUnit( 'F', sourceD );
        sourceG = createOrganisationUnit( 'G' );

        sourcesA.add( sourceA );
        sourcesA.add( sourceB );

        allSources.add( sourceA );
        allSources.add( sourceB );
        allSources.add( sourceC );
        allSources.add( sourceD );
        allSources.add( sourceE );
        allSources.add( sourceF );
        allSources.add( sourceG );

        dataSetMonthly.addOrganisationUnit( sourceA );
        dataSetMonthly.addOrganisationUnit( sourceB );
        dataSetMonthly.addOrganisationUnit( sourceC );
        dataSetMonthly.addOrganisationUnit( sourceD );
        dataSetMonthly.addOrganisationUnit( sourceE );
        dataSetMonthly.addOrganisationUnit( sourceF );

        dataSetWeekly.addOrganisationUnit( sourceB );
        dataSetWeekly.addOrganisationUnit( sourceC );
        dataSetWeekly.addOrganisationUnit( sourceD );
        dataSetWeekly.addOrganisationUnit( sourceE );
        dataSetWeekly.addOrganisationUnit( sourceF );
        dataSetWeekly.addOrganisationUnit( sourceG );

        dataSetYearly.addOrganisationUnit( sourceB );
        dataSetYearly.addOrganisationUnit( sourceC );
        dataSetYearly.addOrganisationUnit( sourceD );
        dataSetYearly.addOrganisationUnit( sourceE );
        dataSetYearly.addOrganisationUnit( sourceF );

        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );
        organisationUnitService.addOrganisationUnit( sourceD );
        organisationUnitService.addOrganisationUnit( sourceE );
        organisationUnitService.addOrganisationUnit( sourceF );
        organisationUnitService.addOrganisationUnit( sourceG );

        dataSetMonthly.addDataSetElement( dataElementA );
        dataSetMonthly.addDataSetElement( dataElementB );
        dataSetMonthly.addDataSetElement( dataElementC );
        dataSetMonthly.addDataSetElement( dataElementD );

        dataSetWeekly.addDataSetElement( dataElementE );

        dataSetYearly.addDataSetElement( dataElementE );

        dataSetService.addDataSet( dataSetWeekly );
        dataSetService.addDataSet( dataSetMonthly );
        dataSetService.addDataSet( dataSetYearly );

        dataElementService.updateDataElement( dataElementA );
        dataElementService.updateDataElement( dataElementB );
        dataElementService.updateDataElement( dataElementC );
        dataElementService.updateDataElement( dataElementD );
        dataElementService.updateDataElement( dataElementE );

        validationRuleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodTypeMonthly ); // deA + deB = deC - deD
        validationRuleB = createValidationRule( "B", greater_than, expressionB, expressionC, periodTypeMonthly ); // deC - deD > deB * 2
        validationRuleC = createValidationRule( "C", less_than_or_equal_to, expressionB, expressionA, periodTypeMonthly ); // deC - deD <= deA + deB
        validationRuleD = createValidationRule( "D", less_than, expressionA, expressionC, periodTypeMonthly ); // deA + deB < deB * 2
        validationRuleE = createValidationRule( "E", compulsory_pair, expressionI, expressionJ, periodTypeMonthly ); // deA [Compulsory pair] deB
        validationRuleF = createValidationRule( "F", exclusive_pair, expressionI, expressionJ, periodTypeMonthly ); // deA [Exclusive pair] deB
        validationRuleG = createValidationRule( "G", equal_to, expressionK, expressionL, periodTypeMonthly ); // deC = deD
        validationRuleP = createValidationRule( "P", equal_to, expressionI, expressionP, periodTypeMonthly ); // deA = [days]
        validationRuleQ = createValidationRule( "Q", equal_to, expressionQ, expressionP, periodTypeYearly ); // deE = [days]
        validationRuleX = createValidationRule( "X", equal_to, expressionA, expressionC, periodTypeMonthly ); // deA + deB = deB * 2
        group = createValidationRuleGroup( 'A' );

        defaultCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    // -------------------------------------------------------------------------
    // Local convenience methods
    // -------------------------------------------------------------------------

    private ValidationResult createValidationResult( ValidationRule validationRule, Period period, OrganisationUnit orgUnit,
        DataElementCategoryOptionCombo catCombo, double ls, double rs, int dayInPeriod )
    {
        ValidationResult vr = new ValidationResult( validationRule, period, orgUnit, catCombo, ls, rs, dayInPeriod );

        return vr;
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
     * Asserts that a collection of ValidationResult is empty.
     *
     * @param results collection of ValidationResult to test.
     */
    private void assertResultsEmpty( Collection<ValidationResult> results )
    {
        assertResultsEquals( new HashSet<ValidationResult>(), results );
    }

    /**
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
                .append( formatResultsList ( "But was", resultsList ) )
                .append( getAllDataValues() )
                .append( getAllValidationRules() );

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

    private String getAllDataValues()
    {
        List<DataValue> allDataValues = dataValueStore.getAllDataValues();

        StringBuilder sb = new StringBuilder( "All data values (" + allDataValues.size() + "):\n" );

        allDataValues.forEach( d -> sb.append( "  " ).append( d.toString() ).append( "\n" ) );

        return sb.toString();
    }

    private String getAllValidationRules()
    {
        List<ValidationRule> allValidationRules = validationRuleService.getAllValidationRules();

        StringBuilder sb = new StringBuilder( "All validation rules (" + allValidationRules.size() + "):\n" );

        allValidationRules.forEach( v -> sb
            .append( "  " ).append( v.getName() )
            .append(": ").append( v.getLeftSide().getExpression() )
            .append(" [").append( v.getOperator() ).append("] ")
            .append( v.getRightSide().getExpression() )
            .append( "\n" ) );

        return sb.toString();
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, String value )
    {
        dataValueService.addDataValue( createDataValue( e, p, s, value, optionCombo, optionCombo ) );
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, String value,
        DataElementCategoryOptionCombo oc1, DataElementCategoryOptionCombo oc2 )
    {
        dataValueService.addDataValue( createDataValue( e, p, s, value, oc1, oc2 ) );
    }

    // -------------------------------------------------------------------------
    // Business logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testValidateDateDateSources()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );
        useDataValue( dataElementC, periodA, sourceA, "3" );
        useDataValue( dataElementD, periodA, sourceA, "4" );

        useDataValue( dataElementA, periodB, sourceA, "1" );
        useDataValue( dataElementB, periodB, sourceA, "2" );
        useDataValue( dataElementC, periodB, sourceA, "3" );
        useDataValue( dataElementD, periodB, sourceA, "4" );

        useDataValue( dataElementA, periodA, sourceB, "1" );
        useDataValue( dataElementB, periodA, sourceB, "2" );
        useDataValue( dataElementC, periodA, sourceB, "3" );
        useDataValue( dataElementD, periodA, sourceB, "4" );

        useDataValue( dataElementA, periodB, sourceB, "1" );
        useDataValue( dataElementB, periodB, sourceB, "2" );
        useDataValue( dataElementC, periodB, sourceB, "3" );
        useDataValue( dataElementD, periodB, sourceB, "4" );

        validationRuleService.saveValidationRule( validationRuleA ); // Invalid
        validationRuleService.saveValidationRule( validationRuleB ); // Invalid
        validationRuleService.saveValidationRule( validationRuleC ); // Valid
        validationRuleService.saveValidationRule( validationRuleD ); // Valid

        // Note: in this and subsequent tests we insert the validation results
        // collection into a new HashSet. This
        // insures that if they are the same as the reference results, they will
        // appear in the same order.

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( getDate( 2000, 2, 1 ),
            getDate( 2000, 6, 1 ), sourcesA, null, null, false, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( createValidationResult( validationRuleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleA, periodB, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodB ) );
        reference.add( createValidationResult( validationRuleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB ) );

        reference.add( createValidationResult( validationRuleB, periodA, sourceA, defaultCombo, -1.0, 4.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleB, periodB, sourceA, defaultCombo, -1.0, 4.0, dayInPeriodB ) );
        reference.add( createValidationResult( validationRuleB, periodA, sourceB, defaultCombo, -1.0, 4.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleB, periodB, sourceB, defaultCombo, -1.0, 4.0, dayInPeriodB ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateDateDateSourcesGroup()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );
        useDataValue( dataElementC, periodA, sourceA, "3" );
        useDataValue( dataElementD, periodA, sourceA, "4" );

        useDataValue( dataElementA, periodB, sourceA, "1" );
        useDataValue( dataElementB, periodB, sourceA, "2" );
        useDataValue( dataElementC, periodB, sourceA, "3" );
        useDataValue( dataElementD, periodB, sourceA, "4" );

        useDataValue( dataElementA, periodA, sourceB, "1" );
        useDataValue( dataElementB, periodA, sourceB, "2" );
        useDataValue( dataElementC, periodA, sourceB, "3" );
        useDataValue( dataElementD, periodA, sourceB, "4" );

        useDataValue( dataElementA, periodB, sourceB, "1" );
        useDataValue( dataElementB, periodB, sourceB, "2" );
        useDataValue( dataElementC, periodB, sourceB, "3" );
        useDataValue( dataElementD, periodB, sourceB, "4" );

        validationRuleService.saveValidationRule( validationRuleA ); // Invalid
        validationRuleService.saveValidationRule( validationRuleB ); // Invalid
        validationRuleService.saveValidationRule( validationRuleC ); // Valid
        validationRuleService.saveValidationRule( validationRuleD ); // Valid

        group.getMembers().add( validationRuleA );
        group.getMembers().add( validationRuleC );

        validationRuleService.addValidationRuleGroup( group );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( getDate( 2000, 2, 1 ),
            getDate( 2000, 6, 1 ), sourcesA, null, group, false, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodB ) );
        reference.add( new ValidationResult( validationRuleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateDataSetPeriodSource()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );
        useDataValue( dataElementC, periodA, sourceA, "3" );
        useDataValue( dataElementD, periodA, sourceA, "4" );

        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );
        validationRuleService.saveValidationRule( validationRuleC );
        validationRuleService.saveValidationRule( validationRuleD );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleB, periodA, sourceA, defaultCombo, -1.0, 4.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateDays()
    {
        useDataValue( dataElementA, periodA, sourceA, "1111" );
        useDataValue( dataElementE, periodY, sourceB, "2222" );

        validationRuleService.saveValidationRule( validationRuleP );
        validationRuleService.saveValidationRule( validationRuleQ );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleP, periodA, sourceA, defaultCombo, 1111.0, 31.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );

        results = validationService.startInteractiveValidationAnalysis( dataSetYearly, periodY, sourceB, null );

        reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleQ, periodY, sourceB, defaultCombo, 2222.0, 366.0, dayInPeriodY ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateMissingValues00()
    {
        validationRuleService.saveValidationRule( validationRuleG );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateMissingValues01()
    {
        useDataValue( dataElementD, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleG );

        Collection<ValidationResult> reference = new HashSet<>();

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 0.0, 1.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateMissingValues10()
    {
        useDataValue( dataElementC, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleG );

        Collection<ValidationResult> reference = new HashSet<>();

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 1.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateMissingValues11()
    {
        useDataValue( dataElementC, periodA, sourceA, "1" );
        useDataValue( dataElementD, periodA, sourceA, "1" );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateCompulsoryPair00()
    {
        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateCompulsoryPair01()
    {
        useDataValue( dataElementB, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleE, periodA, sourceA, defaultCombo, 0.0, 1.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateCompulsoryPair10()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleE, periodA, sourceA, defaultCombo, 1.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateCompulsoryPair11()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePairWithOtherData00()
    {
        useDataValue( dataElementC, periodA, sourceA, "96" );
        validationRuleService.saveValidationRule( validationRuleG );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 96.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateExclusivePairWithOtherData01()
    {
        useDataValue( dataElementC, periodA, sourceA, "97" );
        validationRuleService.saveValidationRule( validationRuleG );

        useDataValue( dataElementB, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 97.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateExclusivePairWithOtherData10()
    {
        useDataValue( dataElementC, periodA, sourceA, "98" );
        validationRuleService.saveValidationRule( validationRuleG );

        useDataValue( dataElementA, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 98.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateExclusivePairWithOtherData11()
    {
        useDataValue( dataElementC, periodA, sourceA, "99" );
        validationRuleService.saveValidationRule( validationRuleG );

        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleF, periodA, sourceA, defaultCombo, 1.0, 2.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 99.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateExclusivePair00()
    {
        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePair01()
    {
        useDataValue( dataElementB, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePair10()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePair11()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleF, periodA, sourceA, defaultCombo, 1.0, 2.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithAttributeOptions()
    {
        DataElementCategoryOption optionA = new DataElementCategoryOption( "CategoryOptionA" );
        DataElementCategoryOption optionB = new DataElementCategoryOption( "CategoryOptionB" );
        DataElementCategoryOption optionC = new DataElementCategoryOption( "CategoryOptionC" );

        categoryService.addDataElementCategoryOption( optionA );
        categoryService.addDataElementCategoryOption( optionB );
        categoryService.addDataElementCategoryOption( optionC );

        DataElementCategory categoryA = createDataElementCategory( 'A', optionA, optionB );
        DataElementCategory categoryB = createDataElementCategory( 'B', optionC );
        categoryA.setDataDimension( true );
        categoryB.setDataDimension( true );

        categoryService.addDataElementCategory( categoryA );
        categoryService.addDataElementCategory( categoryB );

        DataElementCategoryCombo categoryComboAB = createCategoryCombo( 'A', categoryA, categoryB );

        categoryService.addDataElementCategoryCombo( categoryComboAB );

        DataElementCategoryOptionCombo optionComboAC = createCategoryOptionCombo( 'A', categoryComboAB, optionA,
            optionC );
        DataElementCategoryOptionCombo optionComboBC = createCategoryOptionCombo( 'A', categoryComboAB, optionB,
            optionC );

        categoryService.addDataElementCategoryOptionCombo( optionComboAC );
        categoryService.addDataElementCategoryOptionCombo( optionComboBC );

        useDataValue( dataElementA, periodA, sourceA, "4", optionCombo, optionComboAC );
        useDataValue( dataElementB, periodA, sourceA, "3", optionCombo, optionComboAC );

        useDataValue( dataElementA, periodA, sourceA, "2", optionCombo, optionComboBC );
        useDataValue( dataElementB, periodA, sourceA, "1", optionCombo, optionComboBC );

        validationRuleService.saveValidationRule( validationRuleD ); // deA + deB < deB * 2
        validationRuleService.saveValidationRule( validationRuleX ); // deA + deB = deB * 2

        //
        // optionComboAC
        //
        Collection<ValidationResult> results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA,
            optionComboAC );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleD, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleX, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );

        //
        // All optionCombos
        //
        results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, null );

        reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleD, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleX, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleD, periodA, sourceA, optionComboBC, 3.0, 2.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleX, periodA, sourceA, optionComboBC, 3.0, 2.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );

        //
        // Default optionCombo
        //
        results = validationService.startInteractiveValidationAnalysis( dataSetMonthly, periodA, sourceA, optionCombo );

        assertResultsEmpty( results );
    }
}
