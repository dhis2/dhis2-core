package org.hisp.dhis.validation;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
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
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.translation.TranslationProperty;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING;
import static org.hisp.dhis.expression.Operator.compulsory_pair;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.exclusive_pair;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.hisp.dhis.expression.Operator.less_than;
import static org.hisp.dhis.expression.Operator.less_than_or_equal_to;
import static org.hisp.dhis.expression.Operator.not_equal_to;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;

/**
 * @author Jim Grace
 */
public class ValidationServiceTest
    extends DhisTest
{
    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

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

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private UserService injectUserService;

    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private DataElement dataElementD;
    private DataElement dataElementE;

    private Set<CategoryOptionCombo> optionCombos;

    private CategoryCombo categoryComboX;

    private CategoryOptionCombo optionComboX;

    private CategoryOptionCombo optionCombo;

    private DataSet dataSetWeekly;

    private DataSet dataSetMonthly;

    private DataSet dataSetYearly;

    private Period periodA;
    private Period periodB;
    private Period periodC;
    private Period periodY;

    private int dayInPeriodA;
    private int dayInPeriodB;
    private int dayInPeriodC;
    private int dayInPeriodY;

    private OrganisationUnit sourceA;
    private OrganisationUnit sourceB;
    private OrganisationUnit sourceC;
    private OrganisationUnit sourceD;
    private OrganisationUnit sourceE;
    private OrganisationUnit sourceF;
    private OrganisationUnit sourceG;

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
    private ValidationRule validationRuleR;
    private ValidationRule validationRuleS;
    private ValidationRule validationRuleT;
    private ValidationRule validationRuleU;
    private ValidationRule validationRuleX;

    private ValidationRuleGroup group;

    private PeriodType periodTypeWeekly;
    private PeriodType periodTypeMonthly;
    private PeriodType periodTypeYearly;

    private CategoryOptionCombo defaultCombo;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        this.userService = injectUserService;

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

        CategoryOption optionX = createCategoryOption( 'X' );
        Category categoryX = createCategory( 'X', optionX );
        categoryComboX = createCategoryCombo( 'X', categoryX );
        optionComboX = createCategoryOptionCombo( categoryComboX, optionX );

        categoryComboX.getOptionCombos().add( optionComboX );

        categoryService.addCategoryOption( optionX );
        categoryService.addCategory( categoryX );
        categoryService.addCategoryCombo( categoryComboX );
        categoryService.addCategoryOptionCombo( optionComboX );

        optionCombo = categoryService.getDefaultCategoryOptionCombo();

        String suffixX = SEPARATOR + optionComboX.getUid();
        String suffix = SEPARATOR + optionCombo.getUid();

        optionCombos = new HashSet<>();
        optionCombos.add( optionCombo );

        Expression expressionA = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA" );
        Expression expressionA2 = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA2" );
        Expression expressionA3 = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA3" );
        Expression expressionA4 = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA4" );
        Expression expressionA5 = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA5" );
        Expression expressionB = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB" );
        Expression expressionB2 = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB2" );
        Expression expressionB3 = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB3" );
        Expression expressionC = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC" );
        Expression expressionC2 = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC2" );
        Expression expressionC3 = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC3" );
        Expression expressionI = new Expression( "#{" + dataElementA.getUid() + suffix + "}", "expressionI" );
        Expression expressionI2 = new Expression( "#{" + dataElementA.getUid() + suffix + "}", "expressionI2" );
        Expression expressionI3 = new Expression( "#{" + dataElementA.getUid() + suffix + "}", "expressionI3" );
        Expression expressionJ = new Expression( "#{" + dataElementB.getUid() + suffix + "}", "expressionJ" );
        Expression expressionJ2 = new Expression( "#{" + dataElementB.getUid() + suffix + "}", "expressionJ2" );
        Expression expressionK = new Expression( "#{" + dataElementC.getUid() + "}", "expressionK", NEVER_SKIP );
        Expression expressionL = new Expression( "#{" + dataElementD.getUid() + "}", "expressionL", NEVER_SKIP );
        Expression expressionP = new Expression( SYMBOL_DAYS, "expressionP", NEVER_SKIP );
        Expression expressionP2 = new Expression( SYMBOL_DAYS, "expressionP2", NEVER_SKIP );
        Expression expressionQ = new Expression( "#{" + dataElementE.getUid() + "}", "expressionQ", NEVER_SKIP );
        Expression expressionR = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionR" );
        Expression expressionS = new Expression( "#{" + dataElementA.getUid() + suffixX + "} + #{" + dataElementB.getUid() + suffixX + "}", "expressionS" );
        Expression expressionT = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffixX + "}", "expressionT" );
        Expression expressionU = new Expression( "1000", "expressionU" );
        Expression expressionU2 = new Expression( "1000", "expressionU2" );
        Expression expressionU3 = new Expression( "1000", "expressionU3" );
        Expression expressionU4 = new Expression( "1000", "expressionU4" );

        periodA = createPeriod( periodTypeMonthly, getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) );
        periodB = createPeriod( periodTypeMonthly, getDate( 2000, 4, 1 ), getDate( 2000, 4, 30 ) );
        periodC = createPeriod( periodTypeMonthly, getDate( 2000, 5, 1 ), getDate( 2000, 5, 31 ) );
        periodY = createPeriod( periodTypeYearly, getDate( 2000, 1, 1 ), getDate( 2000, 12, 31 ) );

        dayInPeriodA = periodService.getDayInPeriod( periodA, new Date() );
        dayInPeriodB = periodService.getDayInPeriod( periodB, new Date() );
        dayInPeriodC = periodService.getDayInPeriod( periodC, new Date() );
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
        validationRuleB = createValidationRule( "B", greater_than, expressionB2, expressionC,
            periodTypeMonthly ); // deC - deD > deB * 2
        validationRuleC = createValidationRule( "C", less_than_or_equal_to, expressionB3, expressionA2,
            periodTypeMonthly ); // deC - deD <= deA + deB
        validationRuleD = createValidationRule( "D", less_than, expressionA3, expressionC2,
            periodTypeMonthly ); // deA + deB < deB * 2
        validationRuleE = createValidationRule( "E", compulsory_pair, expressionI, expressionJ, periodTypeMonthly ); // deA [Compulsory pair] deB
        validationRuleF = createValidationRule( "F", exclusive_pair, expressionI2, expressionJ2,
            periodTypeMonthly ); // deA [Exclusive pair] deB
        validationRuleG = createValidationRule( "G", equal_to, expressionK, expressionL, periodTypeMonthly ); // deC = deD
        validationRuleP = createValidationRule( "P", equal_to, expressionI3, expressionP,
            periodTypeMonthly ); // deA = [days]
        validationRuleQ = createValidationRule( "Q", equal_to, expressionQ, expressionP2,
            periodTypeYearly ); // deE = [days]
        validationRuleR = createValidationRule( "R", equal_to, expressionR, expressionU, periodTypeMonthly ); // deA(sum) + deB(sum) = 1000
        validationRuleS = createValidationRule( "S", equal_to, expressionS, expressionU2,
            periodTypeMonthly ); // deA.optionComboX + deB.optionComboX = 1000
        validationRuleT = createValidationRule( "T", equal_to, expressionT, expressionU3,
            periodTypeMonthly ); // deA.default + deB.optionComboX = 1000
        validationRuleU = createValidationRule( "U", equal_to, expressionA4, expressionU4,
            periodTypeMonthly ); // deA.default + deB.default = 1000
        validationRuleX = createValidationRule( "X", equal_to, expressionA5, expressionC3,
            periodTypeMonthly ); // deA + deB = deB * 2
        group = createValidationRuleGroup( 'A' );

        defaultCombo = categoryService.getDefaultCategoryOptionCombo();
    }

    // -------------------------------------------------------------------------
    // Local convenience methods
    // -------------------------------------------------------------------------

    private ValidationResult createValidationResult( ValidationRule validationRule, Period period, OrganisationUnit orgUnit,
        CategoryOptionCombo catCombo, double ls, double rs, int dayInPeriod )
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

        StringBuilder sb = new StringBuilder();

        if ( !referenceList.equals( resultsList ) )
        {
            sb.append("\n");

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
        }

        assertEquals( "", sb.toString() );

        for ( ValidationResult result : results )
        {
            String operator = result.getValidationRule().getOperator().getMathematicalOperator();

            if ( !operator.startsWith( "[" ) )
            {
                String test = result.getLeftsideValue()
                    + result.getValidationRule().getOperator().getMathematicalOperator()
                    + result.getRightsideValue();

                assertFalse( (Boolean) expressionService.getExpressionValue( test, SIMPLE_TEST ) );
            }
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
        dataValueService.addDataValue( createDataValue( e, p, s, optionCombo, optionCombo, value ) );
    }

    private void useDataValue( DataElement e, Period p, OrganisationUnit s, String value,
        CategoryOptionCombo oc1, CategoryOptionCombo oc2 )
    {
        dataValueService.addDataValue( createDataValue( e, p, s, oc1, oc2, value ) );
    }

    // -------------------------------------------------------------------------
    // Business logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testValidateDateDateSources()
    {
        useDataValue( dataElementA, periodA, sourceB, "1" );
        useDataValue( dataElementB, periodA, sourceB, "2" );
        useDataValue( dataElementC, periodA, sourceB, "3" );
        useDataValue( dataElementD, periodA, sourceB, "4" );

        useDataValue( dataElementA, periodB, sourceB, "1" );
        useDataValue( dataElementB, periodB, sourceB, "2" );
        useDataValue( dataElementC, periodB, sourceB, "3" );
        useDataValue( dataElementD, periodB, sourceB, "4" );

        useDataValue( dataElementA, periodA, sourceC, "1" );
        useDataValue( dataElementB, periodA, sourceC, "2" );
        useDataValue( dataElementC, periodA, sourceC, "3" );
        useDataValue( dataElementD, periodA, sourceC, "4" );

        useDataValue( dataElementA, periodB, sourceC, "1" );
        useDataValue( dataElementB, periodB, sourceC, "2" );
        useDataValue( dataElementC, periodB, sourceC, "3" );
        useDataValue( dataElementD, periodB, sourceC, "4" );

        validationRuleService.saveValidationRule( validationRuleA ); // Invalid
        validationRuleService.saveValidationRule( validationRuleB ); // Invalid
        validationRuleService.saveValidationRule( validationRuleC ); // Valid
        validationRuleService.saveValidationRule( validationRuleD ); // Valid

        // Note: in this and subsequent tests we insert the validation results
        // collection into a new HashSet. This
        // insures that if they are the same as the reference results, they will
        // appear in the same order.

        ValidationAnalysisParams parameters = validationService.newParamsBuilder(null, sourceB, getDate( 2000, 2, 1 ), getDate( 2000, 6, 1 ) )
            .withIncludeOrgUnitDescendants( true ).build();

        Collection<ValidationResult> results = validationService.validationAnalysis( parameters );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( createValidationResult( validationRuleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB ) );
        reference.add( createValidationResult( validationRuleA, periodA, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleA, periodB, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodB ) );

        reference.add( createValidationResult( validationRuleB, periodA, sourceB, defaultCombo, -1.0, 4.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleB, periodB, sourceB, defaultCombo, -1.0, 4.0, dayInPeriodB ) );
        reference.add( createValidationResult( validationRuleB, periodA, sourceC, defaultCombo, -1.0, 4.0, dayInPeriodA ) );
        reference.add( createValidationResult( validationRuleB, periodB, sourceC, defaultCombo, -1.0, 4.0, dayInPeriodB ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateDateDateSourcesGroup()
    {
        useDataValue( dataElementA, periodA, sourceB, "1" );
        useDataValue( dataElementB, periodA, sourceB, "2" );
        useDataValue( dataElementC, periodA, sourceB, "3" );
        useDataValue( dataElementD, periodA, sourceB, "4" );

        useDataValue( dataElementA, periodB, sourceB, "1" );
        useDataValue( dataElementB, periodB, sourceB, "2" );
        useDataValue( dataElementC, periodB, sourceB, "3" );
        useDataValue( dataElementD, periodB, sourceB, "4" );

        useDataValue( dataElementA, periodA, sourceC, "1" );
        useDataValue( dataElementB, periodA, sourceC, "2" );
        useDataValue( dataElementC, periodA, sourceC, "3" );
        useDataValue( dataElementD, periodA, sourceC, "4" );

        useDataValue( dataElementA, periodB, sourceC, "1" );
        useDataValue( dataElementB, periodB, sourceC, "2" );
        useDataValue( dataElementC, periodB, sourceC, "3" );
        useDataValue( dataElementD, periodB, sourceC, "4" );

        validationRuleService.saveValidationRule( validationRuleA ); // Invalid
        validationRuleService.saveValidationRule( validationRuleB ); // Invalid
        validationRuleService.saveValidationRule( validationRuleC ); // Valid
        validationRuleService.saveValidationRule( validationRuleD ); // Valid

        group.getMembers().add( validationRuleA );
        group.getMembers().add( validationRuleC );

        validationRuleService.addValidationRuleGroup( group );

        ValidationAnalysisParams params = validationService.newParamsBuilder(group, sourceB, getDate( 2000, 2, 1 ), getDate( 2000, 6, 1 ) )
            .withIncludeOrgUnitDescendants( true ).build();

        Collection<ValidationResult> results = validationService.validationAnalysis( params );
        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB ) );
        reference.add( new ValidationResult( validationRuleA, periodA, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodB ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidatePeriodsRulesSources()
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

        useDataValue( dataElementA, periodA, sourceC, "1" );
        useDataValue( dataElementB, periodA, sourceC, "2" );
        useDataValue( dataElementC, periodA, sourceC, "3" );
        useDataValue( dataElementD, periodA, sourceC, "4" );

        useDataValue( dataElementA, periodB, sourceC, "1" );
        useDataValue( dataElementB, periodB, sourceC, "2" );
        useDataValue( dataElementC, periodB, sourceC, "3" );
        useDataValue( dataElementD, periodB, sourceC, "4" );

        validationRuleService.saveValidationRule( validationRuleA ); // Invalid
        validationRuleService.saveValidationRule( validationRuleB ); // Invalid
        validationRuleService.saveValidationRule( validationRuleC ); // Valid
        validationRuleService.saveValidationRule( validationRuleD ); // Valid

        List<ValidationRule> validationRules = Lists.newArrayList( validationRuleA, validationRuleC );
        List<Period> periods = periodService.getPeriodsBetweenDates( getDate( 2000, 2, 1 ), getDate( 2000, 6, 1 ) );

        ValidationAnalysisParams params = validationService.newParamsBuilder(validationRules, null, periods )
            .withIncludeOrgUnitDescendants( true ).build();

        Collection<ValidationResult> results = validationService.validationAnalysis( params );
        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodB ) );
        reference.add( new ValidationResult( validationRuleA, periodA, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceB, defaultCombo, 3.0, -1.0, dayInPeriodB ) );
        reference.add( new ValidationResult( validationRuleA, periodA, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleA, periodB, sourceC, defaultCombo, 3.0, -1.0, dayInPeriodB ) );

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

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );
        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleA, periodA, sourceA, defaultCombo, 3.0, -1.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleB, periodA, sourceA, defaultCombo, -1.0, 4.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateForm()
    {
        validationRuleA.setSkipFormValidation( true );

        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );
        useDataValue( dataElementC, periodA, sourceA, "3" );
        useDataValue( dataElementD, periodA, sourceA, "4" );

        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

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

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleP, periodA, sourceA, defaultCombo, 1111.0, 31.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );

        results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetYearly, sourceB, periodY )
            .build() );

        reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleQ, periodY, sourceB, defaultCombo, 2222.0, 366.0, dayInPeriodY ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateMissingValues00()
    {
        validationRuleService.saveValidationRule( validationRuleG );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateMissingValues01()
    {
        useDataValue( dataElementD, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleG );

        Collection<ValidationResult> reference = new HashSet<>();

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 0.0, 1.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateMissingValues10()
    {
        useDataValue( dataElementC, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleG );

        Collection<ValidationResult> reference = new HashSet<>();

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 1.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateMissingValues11()
    {
        useDataValue( dataElementC, periodA, sourceA, "1" );
        useDataValue( dataElementD, periodA, sourceA, "1" );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateCompulsoryPair00()
    {
        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateCompulsoryPair01()
    {
        useDataValue( dataElementB, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleE, periodA, sourceA, defaultCombo, 0.0, 1.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateCompulsoryPair10()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleE );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

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

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePairWithOtherData00()
    {
        useDataValue( dataElementC, periodA, sourceA, "96" );
        validationRuleService.saveValidationRule( validationRuleG );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

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

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

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

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

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

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleF, periodA, sourceA, defaultCombo, 1.0, 2.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleG, periodA, sourceA, defaultCombo, 99.0, 0.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateExclusivePair00()
    {
        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePair01()
    {
        useDataValue( dataElementB, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePair10()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateExclusivePair11()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );

        validationRuleService.saveValidationRule( validationRuleF );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleF, periodA, sourceA, defaultCombo, 1.0, 2.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithCategoryOptions()
    {
        CategoryOption optionA = new CategoryOption( "CategoryOptionA" );
        CategoryOption optionB = new CategoryOption( "CategoryOptionB" );

        categoryService.addCategoryOption( optionA );
        categoryService.addCategoryOption( optionB );

        Category categoryA = createCategory( 'A', optionA, optionB );

        categoryService.addCategory( categoryA );

        CategoryCombo categoryComboA = createCategoryCombo( 'A', categoryA );

        categoryService.addCategoryCombo( categoryComboA );

        CategoryOptionCombo optionComboA = createCategoryOptionCombo( categoryComboA, optionA );
        CategoryOptionCombo optionComboB = createCategoryOptionCombo( categoryComboA, optionB );

        categoryService.addCategoryOptionCombo( optionComboA );
        categoryService.addCategoryOptionCombo( optionComboB );

        useDataValue( dataElementD, periodA, sourceA, "3", optionComboA, optionCombo );
        useDataValue( dataElementD, periodA, sourceA, "4", optionComboB, optionCombo );

        Expression expressionZ = new Expression( "#{" + dataElementD.getUid() + "." + optionComboA.getUid() + "} * 2"
            + " + #{" + dataElementD.getUid() + "." + optionComboB.getUid() + "}",
            "expressionZ", NEVER_SKIP );
        Expression expressionV = new Expression( "#{" + dataElementD.getUid() + "}", "expressionV", NEVER_SKIP );

        ValidationRule validationRuleZ = createValidationRule( "Z", equal_to, expressionV, expressionZ,
            periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRuleZ ); // deD[all] = deD.optionComboA * 2 + deD.optionComboB

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleZ, periodA, sourceA, defaultCombo, 7.0, 10.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithDataSetElements()
    {
        DataSet dataSetA = createDataSet( 'A', periodTypeMonthly );
        DataSet dataSetB = createDataSet( 'B', periodTypeMonthly );
        DataSet dataSetC = createDataSet( 'C', periodTypeMonthly );
        DataSet dataSetD = createDataSet( 'D', periodTypeMonthly );

        dataSetA.addDataSetElement( dataElementA );
        dataSetA.addDataSetElement( dataElementB );

        dataSetB.addDataSetElement( dataElementA, categoryComboX );
        dataSetB.addDataSetElement( dataElementB );

        dataSetC.addDataSetElement( dataElementA );
        dataSetC.addDataSetElement( dataElementB, categoryComboX );

        dataSetD.addDataSetElement( dataElementA, categoryComboX );
        dataSetD.addDataSetElement( dataElementB, categoryComboX );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        dataSetService.addDataSet( dataSetC );
        dataSetService.addDataSet( dataSetD );

        validationRuleService.saveValidationRule( validationRuleR );
        validationRuleService.saveValidationRule( validationRuleS );
        validationRuleService.saveValidationRule( validationRuleT );
        validationRuleService.saveValidationRule( validationRuleU );

        useDataValue( dataElementA, periodA, sourceA, "1", optionCombo, optionCombo );
        useDataValue( dataElementB, periodA, sourceA, "2", optionCombo, optionCombo );
        useDataValue( dataElementA, periodA, sourceA, "4", optionComboX, optionCombo );
        useDataValue( dataElementB, periodA, sourceA, "8", optionComboX, optionCombo );

        ValidationResult r = new ValidationResult( validationRuleR, periodA, sourceA, defaultCombo, 15.0, 1000.0, dayInPeriodA );
        ValidationResult s = new ValidationResult( validationRuleS, periodA, sourceA, defaultCombo, 12.0, 1000.0, dayInPeriodA );
        ValidationResult t = new ValidationResult( validationRuleT, periodA, sourceA, defaultCombo, 9.0, 1000.0, dayInPeriodA );
        ValidationResult u = new ValidationResult( validationRuleU, periodA, sourceA, defaultCombo, 3.0, 1000.0, dayInPeriodA );

        assertResultsEquals( Lists.newArrayList( r, t, u ), validationService.validationAnalysis( validationService.newParamsBuilder( dataSetA, sourceA, periodA ).build() ) );
        assertResultsEquals( Lists.newArrayList( r, s, u ), validationService.validationAnalysis( validationService.newParamsBuilder( dataSetB, sourceA, periodA ).build() ) );
        assertResultsEquals( Lists.newArrayList( r, s, t, u ), validationService.validationAnalysis( validationService.newParamsBuilder( dataSetC, sourceA, periodA ).build() ) );
        assertResultsEquals( Lists.newArrayList( r, s, t ), validationService.validationAnalysis( validationService.newParamsBuilder( dataSetD, sourceA, periodA ).build() ) );
    }

    @Test
    public void testValidateWithAttributeOptions()
    {
        CategoryOption optionA = new CategoryOption( "CategoryOptionA" );
        CategoryOption optionB = new CategoryOption( "CategoryOptionB" );
        CategoryOption optionC = new CategoryOption( "CategoryOptionC" );

        categoryService.addCategoryOption( optionA );
        categoryService.addCategoryOption( optionB );
        categoryService.addCategoryOption( optionC );

        Category categoryA = createCategory( 'A', optionA, optionB );
        Category categoryB = createCategory( 'B', optionC );
        categoryA.setDataDimension( true );
        categoryB.setDataDimension( true );

        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );

        CategoryCombo categoryComboAB = createCategoryCombo( 'A', categoryA, categoryB );

        categoryService.addCategoryCombo( categoryComboAB );

        CategoryOptionCombo optionComboAC = createCategoryOptionCombo( categoryComboAB, optionA,
            optionC );
        CategoryOptionCombo optionComboBC = createCategoryOptionCombo( categoryComboAB, optionB,
            optionC );

        categoryService.addCategoryOptionCombo( optionComboAC );
        categoryService.addCategoryOptionCombo( optionComboBC );

        useDataValue( dataElementA, periodA, sourceA, "4", optionCombo, optionComboAC );
        useDataValue( dataElementB, periodA, sourceA, "3", optionCombo, optionComboAC );

        useDataValue( dataElementA, periodA, sourceA, "2", optionCombo, optionComboBC );
        useDataValue( dataElementB, periodA, sourceA, "1", optionCombo, optionComboBC );

        validationRuleService.saveValidationRule( validationRuleD ); // deA + deB < deB * 2
        validationRuleService.saveValidationRule( validationRuleX ); // deA + deB = deB * 2

        //
        // optionComboAC
        //
        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .withAttributeOptionCombo( optionComboAC )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleD, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleX, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );

        //
        // All optionCombos
        //
        results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        reference = new HashSet<>();

        reference.add( new ValidationResult( validationRuleD, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleX, periodA, sourceA, optionComboAC, 7.0, 6.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleD, periodA, sourceA, optionComboBC, 3.0, 2.0, dayInPeriodA ) );
        reference.add( new ValidationResult( validationRuleX, periodA, sourceA, optionComboBC, 3.0, 2.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );

        //
        // Default optionCombo
        //
        results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .withAttributeOptionCombo( optionCombo )
            .build() );

        assertResultsEmpty( results );
    }

    @Test
    public void testValidateNeverSkip()
    {
        useDataValue( dataElementA, periodB, sourceA, "1" );

        useDataValue( dataElementA, periodC, sourceA, "2" );
        useDataValue( dataElementB, periodC, sourceA, "3" );

        Expression expressionLeft = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionLeft", NEVER_SKIP );
        Expression expressionRight = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionRight", NEVER_SKIP );

        ValidationRule validationRule = createValidationRule( "R", not_equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder(
            Sets.newHashSet( validationRule ), sourceA, Sets.newHashSet( periodA, periodB, periodC ) )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodB, sourceA, defaultCombo, 1.0, 1.0, dayInPeriodB ) );
        reference.add( new ValidationResult( validationRule, periodC, sourceA, defaultCombo, 5.0, 5.0, dayInPeriodC ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateSkipIfAllValuesAreMissing()
    {
        useDataValue( dataElementA, periodB, sourceA, "1" );

        useDataValue( dataElementA, periodC, sourceA, "2" );
        useDataValue( dataElementB, periodC, sourceA, "3" );

        Expression expressionLeft = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionLeft", SKIP_IF_ALL_VALUES_MISSING );
        Expression expressionRight = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionRight", SKIP_IF_ALL_VALUES_MISSING );

        ValidationRule validationRule = createValidationRule( "R", not_equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder(
            Sets.newHashSet( validationRule ), sourceA, Sets.newHashSet( periodA, periodB, periodC ) )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodB, sourceA, defaultCombo, 1.0, 1.0, dayInPeriodB ) );
        reference.add( new ValidationResult( validationRule, periodC, sourceA, defaultCombo, 5.0, 5.0, dayInPeriodC ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateSkipIfAnyValueIsMissing()
    {
        useDataValue( dataElementA, periodB, sourceA, "1" );

        useDataValue( dataElementA, periodC, sourceA, "2" );
        useDataValue( dataElementB, periodC, sourceA, "3" );

        Expression expressionLeft = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionLeft", SKIP_IF_ANY_VALUE_MISSING );
        Expression expressionRight = new Expression( "#{" + dataElementA.getUid() + "} + #{" + dataElementB.getUid() + "}", "expressionRight", SKIP_IF_ANY_VALUE_MISSING );

        ValidationRule validationRule = createValidationRule( "R", not_equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder(
            Sets.newHashSet( validationRule ), sourceA, Sets.newHashSet( periodA, periodB, periodC ) )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodC, sourceA, defaultCombo, 5.0, 5.0, dayInPeriodC ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithIf()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        Expression expressionLeft = new Expression( "if(#{" + dataElementA.getUid() + "}==1,5,6)", "expressionLeft" );
        Expression expressionRight = new Expression( "if(#{" + dataElementA.getUid() + "}==2,7,8)", "expressionRight" );

        ValidationRule validationRule = createValidationRule( "R", equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodA, sourceA, defaultCombo, 5.0, 8.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithIsNull()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        Expression expressionLeft = new Expression( "if(isNull(#{" + dataElementA.getUid() + "}),5,6)", "expressionLeft" );
        Expression expressionRight = new Expression( "if(isNull(#{" + dataElementB.getUid() + "}),7,8)", "expressionRight" );

        ValidationRule validationRule = createValidationRule( "R", equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodA, sourceA, defaultCombo, 6.0, 7.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithIsNotNull()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );

        Expression expressionLeft = new Expression( "if(isNotNull(#{" + dataElementA.getUid() + "}),5,6)", "expressionLeft" );
        Expression expressionRight = new Expression( "if(isNotNull(#{" + dataElementB.getUid() + "}),7,8)", "expressionRight" );

        ValidationRule validationRule = createValidationRule( "R", equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodA, sourceA, defaultCombo, 5.0, 8.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithFirstNonNull()
    {
        useDataValue( dataElementA, periodA, sourceA, "3" );

        Expression expressionLeft = new Expression( "firstNonNull( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )", "expressionLeft" );
        Expression expressionRight = new Expression( "firstNonNull( #{" + dataElementB.getUid() + "}, #{" + dataElementA.getUid() + "} )", "expressionRight" );

        ValidationRule validationRule = createValidationRule( "R", not_equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodA, sourceA, defaultCombo, 3.0, 3.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testValidateWithGreatestAndLeast()
    {
        useDataValue( dataElementA, periodA, sourceA, "10" );
        useDataValue( dataElementB, periodA, sourceA, "20" );

        Expression expressionLeft = new Expression( "greatest( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )", "expressionLeft" );
        Expression expressionRight = new Expression( "least( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )", "expressionRight" );

        ValidationRule validationRule = createValidationRule( "R", equal_to, expressionLeft, expressionRight, periodTypeMonthly );

        validationRuleService.saveValidationRule( validationRule );

        Collection<ValidationResult> results = validationService.validationAnalysis( validationService.newParamsBuilder( dataSetMonthly, sourceA, periodA )
            .build() );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( validationRule, periodA, sourceA, defaultCombo, 20.0, 10.0, dayInPeriodA ) );

        assertResultsEquals( reference, results );
    }

    @Test
    public void testInstructionTranslation()
    {
         User user = createUserAndInjectSecurityContext( true );

        Locale locale = Locale.FRENCH;
        UserContext.setUser( user );
        UserContext.setUserSetting( UserSettingKey.DB_LOCALE, locale );

        useDataValue( dataElementA, periodA, sourceA, "10" );
        useDataValue( dataElementB, periodA, sourceA, "20" );

        Expression expressionLeft = new Expression( "greatest( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )", "expressionLeft" );
        Expression expressionRight = new Expression( "least( #{" + dataElementA.getUid() + "}, #{" + dataElementB.getUid() + "} )", "expressionRight" );

        ValidationRule validationRule = createValidationRule( "R", equal_to, expressionLeft, expressionRight, periodTypeMonthly );
        validationRule.setInstruction( "Validation rule instruction" );

        validationRuleService.saveValidationRule( validationRule );

        String instructionTranslated = "Validation rule instruction translated";

        Set<Translation> listObjectTranslation = new HashSet<>( validationRule.getTranslations() );
        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.INSTRUCTION, instructionTranslated ) );


        identifiableObjectManager.updateTranslations( validationRule, listObjectTranslation );

        Assert.assertEquals( instructionTranslated, validationRule.getDisplayInstruction() );
    }
}
