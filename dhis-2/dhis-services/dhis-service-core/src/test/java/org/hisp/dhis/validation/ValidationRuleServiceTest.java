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

import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.hisp.dhis.expression.Operator.less_than;
import static org.hisp.dhis.expression.Operator.less_than_or_equal_to;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @author Jim Grace
 */
public class ValidationRuleServiceTest
    extends DhisTest
{
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
    private OrganisationUnitService organisationUnitService;

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

    private Expression expressionX;

    private DataSet dataSetWeekly;

    private DataSet dataSetMonthly;

    private DataSet dataSetYearly;

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

    private Period periodW;

    private Period periodX;

    private Period periodY;

    private Period periodZ;

    private OrganisationUnit sourceA;

    private OrganisationUnit sourceB;

    private OrganisationUnit sourceC;

    private OrganisationUnit sourceD;

    private OrganisationUnit sourceE;

    private OrganisationUnit sourceF;

    private OrganisationUnit sourceG;

    private Set<OrganisationUnit> sourcesA = new HashSet<>();

    private Set<OrganisationUnit> allSources = new HashSet<>();

    private ValidationRule validationRuleA;

    private ValidationRule validationRuleB;

    private ValidationRule validationRuleC;

    private ValidationRule validationRuleD;

    private ValidationRule validationRuleX;

    private ValidationRule monitoringRuleE;

    private ValidationRule monitoringRuleF;

    private ValidationRule monitoringRuleG;

    private ValidationRule monitoringRuleH;

    private ValidationRule monitoringRuleI;

    private ValidationRule monitoringRuleIx;

    private ValidationRule monitoringRuleJ;

    private ValidationRule monitoringRuleK;

    private ValidationRule monitoringRuleKx;

    private ValidationRule monitoringRuleL;

    private ValidationRule monitoringRuleLx;

    private ValidationRule monitoringRuleLxx;

    private ValidationRule monitoringRuleM;

    private ValidationRuleGroup group;

    private PeriodType periodTypeWeekly;

    private PeriodType periodTypeMonthly;

    private PeriodType periodTypeYearly;

    private DataElementCategoryOptionCombo defaultCombo;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private void joinDataSetToSource( DataSet dataSet, OrganisationUnit source )
    {
        source.getDataSets().add( dataSet );
        dataSet.getSources().add( source );
    }

    @Override
    public void setUpTest()
        throws Exception
    {
        CurrentUserService currentUserService = new MockCurrentUserService( allSources, null );
        setDependency( validationRuleService, "currentUserService", currentUserService, CurrentUserService.class );

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
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA",
            dataElementSet( dataElementA, dataElementB ) );
        expressionB = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB",
            dataElementSet( dataElementC, dataElementD ) );
        expressionC = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC", dataElementSet( dataElementB ) );
        expressionD = new Expression( "#{" + dataElementB.getUid() + suffix + "}", "expressionD", dataElementSet( dataElementB ) );
        expressionE = new Expression( "AVG(#{" + dataElementB.getUid() + suffix + "} * 1.5)", "expressionE",
            dataElementSet( dataElementB ), dataElementSet( dataElementB ) );
        expressionF = new Expression(
            "#{" + dataElementB.getUid() + suffix + "} / #{" + dataElementE.getUid() + suffix + "}", "expressionF",
            dataElementSet( dataElementB, dataElementE ) );
        expressionG = new Expression(
            "AVG(#{" + dataElementB.getUid() + suffix + "} * 1.5 / #{" + dataElementE.getUid() + suffix + "})",
            "expressionG", dataElementSet( dataElementB, dataElementE ), dataElementSet( dataElementB, dataElementE ) );
        expressionH = new Expression(
            "AVG(#{" + dataElementB.getUid() + suffix + "}) + 1.5*STDDEV(#{" + dataElementB.getUid() + suffix + "})",
            "expressionH", dataElementSet(), dataElementSet( dataElementB ) );
        expressionX = new Expression( "#{" + dataElementB.getUid() + suffix + "}>250",
            "expressionX", dataElementSet( dataElementB ), dataElementSet() );

        expressionService.addExpression( expressionA );
        expressionService.addExpression( expressionB );
        expressionService.addExpression( expressionC );
        expressionService.addExpression( expressionD );
        expressionService.addExpression( expressionE );
        expressionService.addExpression( expressionF );
        expressionService.addExpression( expressionG );
        expressionService.addExpression( expressionH );

        periodA = createPeriod( periodTypeMonthly, getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) );
        periodB = createPeriod( periodTypeMonthly, getDate( 2000, 4, 1 ), getDate( 2000, 4, 30 ) );
        periodC = createPeriod( periodTypeMonthly, getDate( 2000, 5, 1 ), getDate( 2000, 5, 31 ) );
        periodD = createPeriod( periodTypeMonthly, getDate( 2000, 6, 1 ), getDate( 2000, 6, 30 ) );
        periodE = createPeriod( periodTypeMonthly, getDate( 2000, 7, 1 ), getDate( 2000, 7, 31 ) );

        periodF = createPeriod( periodTypeMonthly, getDate( 2001, 3, 1 ), getDate( 2001, 3, 31 ) );
        periodG = createPeriod( periodTypeMonthly, getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) );
        periodH = createPeriod( periodTypeMonthly, getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) );
        periodI = createPeriod( periodTypeMonthly, getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) );
        periodJ = createPeriod( periodTypeMonthly, getDate( 2001, 7, 1 ), getDate( 2001, 7, 31 ) );

        periodK = createPeriod( periodTypeMonthly, getDate( 2002, 3, 1 ), getDate( 2002, 3, 31 ) );
        periodL = createPeriod( periodTypeMonthly, getDate( 2002, 4, 1 ), getDate( 2002, 4, 30 ) );
        periodM = createPeriod( periodTypeMonthly, getDate( 2002, 5, 1 ), getDate( 2002, 5, 31 ) );
        periodN = createPeriod( periodTypeMonthly, getDate( 2002, 6, 1 ), getDate( 2002, 6, 30 ) );
        periodO = createPeriod( periodTypeMonthly, getDate( 2002, 7, 1 ), getDate( 2002, 7, 31 ) );
        periodP = createPeriod( periodTypeMonthly, getDate( 2002, 8, 1 ), getDate( 2002, 8, 31 ) );

        periodW = createPeriod( periodTypeWeekly, getDate( 2002, 4, 1 ), getDate( 2000, 4, 7 ) );

        periodX = createPeriod( periodTypeYearly, getDate( 2000, 1, 1 ), getDate( 2000, 12, 31 ) );
        periodY = createPeriod( periodTypeYearly, getDate( 2001, 1, 1 ), getDate( 2001, 12, 31 ) );
        periodZ = createPeriod( periodTypeYearly, getDate( 2002, 1, 1 ), getDate( 2002, 12, 31 ) );

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

        joinDataSetToSource( dataSetMonthly, sourceA );
        joinDataSetToSource( dataSetMonthly, sourceB );
        joinDataSetToSource( dataSetMonthly, sourceC );
        joinDataSetToSource( dataSetMonthly, sourceD );
        joinDataSetToSource( dataSetMonthly, sourceE );
        joinDataSetToSource( dataSetMonthly, sourceF );

        joinDataSetToSource( dataSetWeekly, sourceB );
        joinDataSetToSource( dataSetWeekly, sourceC );
        joinDataSetToSource( dataSetWeekly, sourceD );
        joinDataSetToSource( dataSetWeekly, sourceE );
        joinDataSetToSource( dataSetWeekly, sourceF );
        joinDataSetToSource( dataSetWeekly, sourceG );

        joinDataSetToSource( dataSetYearly, sourceB );
        joinDataSetToSource( dataSetYearly, sourceC );
        joinDataSetToSource( dataSetYearly, sourceD );
        joinDataSetToSource( dataSetYearly, sourceE );
        joinDataSetToSource( dataSetYearly, sourceF );

        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );
        organisationUnitService.addOrganisationUnit( sourceD );
        organisationUnitService.addOrganisationUnit( sourceE );
        organisationUnitService.addOrganisationUnit( sourceF );
        organisationUnitService.addOrganisationUnit( sourceG );

        dataSetMonthly.getDataElements().add( dataElementA );
        dataSetMonthly.getDataElements().add( dataElementB );
        dataSetMonthly.getDataElements().add( dataElementC );
        dataSetMonthly.getDataElements().add( dataElementD );

        dataSetWeekly.getDataElements().add( dataElementE );

        dataSetYearly.getDataElements().add( dataElementE );

        dataElementA.getDataSets().add( dataSetMonthly );
        dataElementB.getDataSets().add( dataSetMonthly );
        dataElementC.getDataSets().add( dataSetMonthly );
        dataElementD.getDataSets().add( dataSetMonthly );

        dataElementE.getDataSets().add( dataSetWeekly );

        dataElementE.getDataSets().add( dataSetYearly );

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
        validationRuleX = createValidationRule( "X", equal_to, expressionA, expressionC, periodTypeMonthly ); // deA + deB = deB * 2

        // Compare dataElementB with 1.5 times its average for one sequential previous period.
        monitoringRuleE = createMonitoringRule( "E", less_than_or_equal_to, expressionD, expressionE, periodTypeMonthly, 1, 1, 0 );

        // Compare dataElementB with 1.5 times its average for one annual previous period.
        monitoringRuleF = createMonitoringRule( "F", less_than_or_equal_to, expressionD, expressionE, periodTypeMonthly, 1, 0, 1 );

        // Compare dataElementB with 1.5 times its average for one sequential and two annual previous periods.
        monitoringRuleG = createMonitoringRule( "G", less_than_or_equal_to, expressionD, expressionE, periodTypeMonthly, 1, 1, 2 );

        // Compare dataElementB with 1.5 times its average for two sequential and two annual previous periods.
        monitoringRuleH = createMonitoringRule( "H", less_than_or_equal_to, expressionD, expressionE, periodTypeMonthly, 1, 2, 2 );

        // Compare dataElementB with its average plus 1.5 standard deviations for two sequential previous period.
        monitoringRuleI = createMonitoringRule( "I", less_than_or_equal_to, expressionD, expressionH, periodTypeMonthly, 1, 2, 0 );
        // Compare dataElementB with its average plus 1.5 standard deviations for the two periods before the last (window=3,skip=1)
        monitoringRuleIx = createMonitoringRule( "Ix", less_than_or_equal_to, expressionD, expressionH, null,
            periodTypeMonthly, 1, 3, 0, 1 );

        // Compare dataElementB with its average plus 1.5 standard deviations for one annual previous period.
        monitoringRuleJ = createMonitoringRule( "J", less_than_or_equal_to, expressionD, expressionH, periodTypeMonthly, 1, 0, 1 );

        // Compare dataElementB with its average plus 1.5 standard deviations for one sequential and two annual previous periods, skipping the most recent.
        monitoringRuleK = createMonitoringRule( "K", less_than_or_equal_to, expressionD, expressionH, periodTypeMonthly, 1, 1, 2 );
        monitoringRuleKx = createMonitoringRule( "Kx", less_than_or_equal_to, expressionD, expressionH, null, periodTypeMonthly, 1, 1, 2, 1 );

        // Compare dataElementB with its average plus 1.5 standard deviations for two sequential and two annual previous periods.
        monitoringRuleL = createMonitoringRule( "L", less_than_or_equal_to, expressionD, expressionH, periodTypeMonthly, 1, 2, 2 );
        monitoringRuleLx = createMonitoringRule( "Lx", less_than_or_equal_to, expressionD, expressionH, null,
            periodTypeMonthly, 1, 2, 2, 2 );
        monitoringRuleLxx = createMonitoringRule( "Lxx", less_than_or_equal_to, expressionD, expressionH, expressionX,
            periodTypeMonthly, 1, 2, 2, 0 );

        monitoringRuleM = createMonitoringRule( "M", less_than_or_equal_to, expressionF, expressionG, periodTypeMonthly,
            1, 0, 1 );

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
     * @param results collection of ValidationResult to order
     * @return ValidationResults in their natural order
     */
    private List<ValidationResult> orderedList( Collection<ValidationResult> results )
    {
        List<ValidationResult> resultList = new ArrayList<>( results );
        Collections.sort( resultList );
        return resultList;
    }

    private Set<DataElement> dataElementSet()
    {
        return new HashSet<DataElement>();
    }

    private Set<DataElement> dataElementSet( DataElement elt1 )
    {
        HashSet<DataElement> result = new HashSet<DataElement>();
        result.add( elt1 );
        return result;
    }

    private Set<DataElement> dataElementSet( DataElement elt1, DataElement elt2 )
    {
        HashSet<DataElement> result = new HashSet<DataElement>();
        result.add( elt1 );
        result.add( elt2 );
        return result;
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

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2000, 2, 1 ),
            getDate( 2000, 6, 1 ), sourcesA, null, null, false, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodB, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodA, sourceB, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodB, sourceB, defaultCombo, validationRuleA, 3.0, -1.0 ) );

        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleB, -1.0, 4.0 ) );
        reference.add( new ValidationResult( periodB, sourceA, defaultCombo, validationRuleB, -1.0, 4.0 ) );
        reference.add( new ValidationResult( periodA, sourceB, defaultCombo, validationRuleB, -1.0, 4.0 ) );
        reference.add( new ValidationResult( periodB, sourceB, defaultCombo, validationRuleB, -1.0, 4.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 8, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
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

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2000, 2, 1 ),
            getDate( 2000, 6, 1 ), sourcesA, null, group, false, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodB, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodA, sourceB, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodB, sourceB, defaultCombo, validationRuleA, 3.0, -1.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 4, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    @Test
    public void testValidateDateDateSource()
    {
        useDataValue( dataElementA, periodA, sourceA, "1" );
        useDataValue( dataElementB, periodA, sourceA, "2" );
        useDataValue( dataElementC, periodA, sourceA, "3" );
        useDataValue( dataElementD, periodA, sourceA, "4" );

        useDataValue( dataElementA, periodB, sourceA, "1" );
        useDataValue( dataElementB, periodB, sourceA, "2" );
        useDataValue( dataElementC, periodB, sourceA, "3" );
        useDataValue( dataElementD, periodB, sourceA, "4" );

        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );
        validationRuleService.saveValidationRule( validationRuleC );
        validationRuleService.saveValidationRule( validationRuleD );

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2000, 2, 1 ),
            getDate( 2000, 6, 1 ), sourceA );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodB, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleB, -1.0, 4.0 ) );
        reference.add( new ValidationResult( periodB, sourceA, defaultCombo, validationRuleB, -1.0, 4.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 4, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
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

        Collection<ValidationResult> results = validationRuleService.validate( dataSetMonthly, periodA, sourceA, null );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleA, 3.0, -1.0 ) );
        reference.add( new ValidationResult( periodA, sourceA, defaultCombo, validationRuleB, -1.0, 4.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 2, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    @Test
    public void testValidateMonitoringSequential()
    {
        // Note: for some monitoring tests, we enter more data than needed, to
        // be sure the extra data *isn't* used.

        useDataValue( dataElementB, periodA, sourceA, "11" ); // Mar 2000
        useDataValue( dataElementB, periodB, sourceA, "12" ); // Apr 2000
        useDataValue( dataElementB, periodC, sourceA, "13" ); // May 2000
        useDataValue( dataElementB, periodD, sourceA, "14" ); // Jun 2000
        useDataValue( dataElementB, periodE, sourceA, "15" ); // Jul 2000

        useDataValue( dataElementB, periodF, sourceA, "30" ); // Mar 2001
        useDataValue( dataElementB, periodG, sourceA, "35" ); // Apr 2001
        useDataValue( dataElementB, periodH, sourceA, "40" ); // May 2001
        useDataValue( dataElementB, periodI, sourceA, "45" ); // Jun 2001
        useDataValue( dataElementB, periodJ, sourceA, "50" ); // Jul 2001

        useDataValue( dataElementB, periodK, sourceA, "100" ); // Mar 2002
        useDataValue( dataElementB, periodL, sourceA, "200" ); // Apr 2002
        useDataValue( dataElementB, periodM, sourceA, "400" ); // May 2002
        useDataValue( dataElementB, periodN, sourceA, "600" ); // Jun 2002
        useDataValue( dataElementB, periodO, sourceA, "800" ); // Jul 2002
        useDataValue( dataElementB, periodP, sourceA, "700" ); // Aug 2002

        validationRuleService.saveValidationRule( monitoringRuleE );
        validationRuleService.saveValidationRule( monitoringRuleI );
        validationRuleService.saveValidationRule( monitoringRuleIx );


        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2002, 1, 15 ),
            getDate( 2002, 9, 15 ), sourceA );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleE, 200.0,
            150.0 /* 1.5 * 100 */ ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleE, 400.0,
            300.0 /* 1.5 * 200 */ ) );

        // Note that when using AVG*1.5 with a single period
        // (MonitoringRuleI), it just compares the previous period
        // to the current one since the sample size is one and the average is 
        // just the single sample value.
        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleI, 200.0, 100.0 ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleI, 400.0, 225.0 ) );
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleI, 600.0, 450.0 ) );
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleI, 800.0, 650.0 ) );

        // This is the same, but for the skip count (compares with the period before last)
        // reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleI, 200.0, 100.0 ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleIx, 400.0, 100.0 ) );
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleIx, 600.0, 225.0 ) );
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleIx, 800.0, 450.0 ) );
        reference.add( new ValidationResult( periodP, sourceA, defaultCombo, monitoringRuleIx, 700.0, 650.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    @Test
    public void testValidateMonitoringAnnual()
    {
        useDataValue( dataElementB, periodA, sourceA, "11" ); // Mar 2000
        useDataValue( dataElementB, periodB, sourceA, "12" ); // Apr 2000
        useDataValue( dataElementB, periodC, sourceA, "13" ); // May 2000
        useDataValue( dataElementB, periodD, sourceA, "14" ); // Jun 2000
        useDataValue( dataElementB, periodE, sourceA, "15" ); // Jul 2000

        useDataValue( dataElementB, periodF, sourceA, "50" ); // Mar 2001
        useDataValue( dataElementB, periodG, sourceA, "150" ); // Apr 2001
        useDataValue( dataElementB, periodH, sourceA, "200" ); // May 2001
        useDataValue( dataElementB, periodI, sourceA, "600" ); // Jun 2001
        useDataValue( dataElementB, periodJ, sourceA, "400" ); // Jul 2001

        useDataValue( dataElementB, periodK, sourceA, "100" ); // Mar 2002
        useDataValue( dataElementB, periodL, sourceA, "200" ); // Apr 2002
        useDataValue( dataElementB, periodM, sourceA, "400" ); // May 2002
        useDataValue( dataElementB, periodN, sourceA, "700" ); // Jun 2002
        useDataValue( dataElementB, periodO, sourceA, "800" ); // Jul 2002

        validationRuleService.saveValidationRule( monitoringRuleF );
        validationRuleService.saveValidationRule( monitoringRuleJ );

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2002, 1, 15 ),
            getDate( 2002, 8, 15 ), sourceA );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodK, sourceA, defaultCombo, monitoringRuleF, 100.0,
            75.0 /* 1.5 * 50 */ ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleF, 400.0,
            300.0 /* 1.5 * 200 */ ) );
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleF, 800.0,
            600.0 /* 1.5 * 400 */ ) );

        // Note that when using AVG+1.5*SD with just a past year value
        // (monitoringRuleJ), it simply compares the previous year
        // to the current one since the sample size is one, the average is the
        // one sample value and the standard deviation
        // is zero.
        reference.add( new ValidationResult( periodK, sourceA, defaultCombo, monitoringRuleJ, 100.0, 50.0 ) );
        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleJ, 200.0, 150.0 ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleJ, 400.0, 200.0 ) );
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleJ, 700.0, 600.0 ) );
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleJ, 800.0, 400.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 8, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    @Test
    public void testValidateMonitoring1Sequential2Annual()
    {
        useDataValue( dataElementB, periodA, sourceA, "11" ); // Mar 2000
        useDataValue( dataElementB, periodB, sourceA, "12" ); // Apr 2000
        useDataValue( dataElementB, periodC, sourceA, "13" ); // May 2000
        useDataValue( dataElementB, periodD, sourceA, "14" ); // Jun 2000
        useDataValue( dataElementB, periodE, sourceA, "15" ); // Jul 2000

        useDataValue( dataElementB, periodF, sourceA, "50" ); // Mar 2001
        useDataValue( dataElementB, periodG, sourceA, "150" ); // Apr 2001
        useDataValue( dataElementB, periodH, sourceA, "200" ); // May 2001
        useDataValue( dataElementB, periodI, sourceA, "600" ); // Jun 2001
        useDataValue( dataElementB, periodJ, sourceA, "400" ); // Jul 2001

        useDataValue( dataElementB, periodK, sourceA, "100" ); // Mar 2002
        useDataValue( dataElementB, periodL, sourceA, "200" ); // Apr 2002
        useDataValue( dataElementB, periodM, sourceA, "400" ); // May 2002
        useDataValue( dataElementB, periodN, sourceA, "800" ); // Jun 2002
        useDataValue( dataElementB, periodO, sourceA, "800" ); // Jul 2002

        // compare with 1.5*average for 1 sequential and 2 annual periods
        validationRuleService.saveValidationRule( monitoringRuleG );
        // compare with avg+1.5stdev for 1 sequential and 2 annual periods
        validationRuleService.saveValidationRule( monitoringRuleK );
        // compare with avg+1.5stdev for 1 sequential and 2 annual periods,
        // skipping most recent period
        validationRuleService.saveValidationRule( monitoringRuleKx );

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2002, 1, 15 ),
            getDate( 2002, 8, 15 ), sourceA );

        Collection<ValidationResult> reference = new HashSet<>();

        // Samples =
        reference.add( new ValidationResult( periodK, sourceA, defaultCombo, monitoringRuleG, 100.0,
            83.6 /* 1.5 * ( 11 + 12 + 50 + 150 ) / 4 */ ) );
        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleG, 200.0,
            114.9 /* 1.5 * ( 11 + 12 + 13 + 50 + 150 + 200 + 100 ) / 7 */ ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleG, 400.0,
            254.8 /* 1.5 * ( 12 + 13 + 14 + 150 + 200 + 600 + 200 ) / 7 */ ) );
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleG, 800.0,
            351.9 /* 1.5 * ( 13 + 14 + 15 + 200 + 600 + 400 + 400 ) / 7 */ ) );
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleG, 800.0,
            548.7 /* 1.5 * ( 14 + 15 + 600 + 400 + 700 ) / 5 */ ) );

        // Samples=[11,12,13,50,150,200,100], AVG=76.57, SD=70.09, AVG+SD*1.5=181.71
        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleK, 200.0, 181.7 ) );
        // Samples=[13,14,15,200,600,400,400], AVG=234.57, SD=218.90, AVG+SD*1.5=562.92
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleK, 800.0, 562.9 ) );

        // Samples=[11,12,13,50,150,200,100], AVG=76.57, SD=70.09, AVG+SD*1.5=181.71
        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleKx, 200.0, 185.2 ) );
        // Samples=[13,14,15,200,600,400,400], AVG=234.57, SD=218.90, AVG+SD*1.5=562.92
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleKx, 800.0, 544.4 ) );
        // Samples=[14,15,600,400,700], AVG=345.8, SD=287.24, AVG+SD*1.5=776.66
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleKx, 800.0, 636.5 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 10, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    @Test
    public void testValidateMonitoring2Sequential2Annual()
    {
        useDataValue( dataElementB, periodA, sourceA, "11" ); // Mar 2000
        useDataValue( dataElementB, periodB, sourceA, "12" ); // Apr 2000
        useDataValue( dataElementB, periodC, sourceA, "13" ); // May 2000
        useDataValue( dataElementB, periodD, sourceA, "14" ); // Jun 2000
        useDataValue( dataElementB, periodE, sourceA, "15" ); // Jul 2000

        useDataValue( dataElementB, periodF, sourceA, "50" ); // Mar 2001
        useDataValue( dataElementB, periodG, sourceA, "150" ); // Apr 2001
        useDataValue( dataElementB, periodH, sourceA, "200" ); // May 2001
        useDataValue( dataElementB, periodI, sourceA, "600" ); // Jun 2001
        useDataValue( dataElementB, periodJ, sourceA, "400" ); // Jul 2001

        useDataValue( dataElementB, periodK, sourceA, "100" ); // Mar 2002
        useDataValue( dataElementB, periodL, sourceA, "200" ); // Apr 2002
        useDataValue( dataElementB, periodM, sourceA, "400" ); // May 2002
        useDataValue( dataElementB, periodN, sourceA, "800" ); // Jun 2002
        useDataValue( dataElementB, periodO, sourceA, "800" ); // Jul 2002

        validationRuleService.saveValidationRule( monitoringRuleH ); // 2 sequential and 2 annual periods
        validationRuleService.saveValidationRule( monitoringRuleL ); // 2 sequential and 2 annual periods
        validationRuleService.saveValidationRule( monitoringRuleLx ); // 2 sequential and 2 annual periods

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2002, 1, 15 ),
            getDate( 2002, 8, 15 ), sourceA );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodL, sourceA, defaultCombo, monitoringRuleH, 200.0,
            191.7 /*
                   * 1.5 * ( 11 + 12 + 13 + 14 + 50 + 150 + 200 + 600 + 100 ) /
                   * 9
                   */ ) );
        reference.add( new ValidationResult( periodM, sourceA, defaultCombo, monitoringRuleH, 400.0,
            220.6 /*
                   * 1.5 * ( 11 + 12 + 13 + 14 + 15 + 50 + 150 + 200 + 600 + 400
                   * + 100 + 200 ) / 12
                   */ ) );
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleH, 800.0,
            300.6 /*
                   * 1.5 * ( 12 + 13 + 14 + 15 + 150 + 200 + 600 + 400 + 200 +
                   * 400 ) / 10
                   */ ) );
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleH, 800.0,
            457.9 /* 1.5 * ( 13 + 14 + 15 + 200 + 600 + 400 + 400 + 800 ) / 8 */ ) );

        /* Samples=(13,14,15,200,600,400,400,800), AVG=292.75, SD=256.17, AVG+SD*1.5=493.4 */
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleL, 800.0, 493.4 ) );
        /* Samples=(12,13,14,15,150,200,600,400,200,400), AVG=200.4, SD=195.35, AVG+SD*1.5=721.2 */
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleL, 800.0, 721.2 ) );

        /* Samples=(13,14,15,200,600,400,400,800), AVG=292.75, SD=256.17, AVG+SD*1.5=483.3 */
        reference.add( new ValidationResult( periodN, sourceA, defaultCombo, monitoringRuleLx, 800.0, 483.3 ) );
        /* Samples=(12,13,14,15,150,200,600,400,200,400), AVG=200.4, SD=195.35, AVG+SD*1.5=544.4 */
        reference.add( new ValidationResult( periodO, sourceA, defaultCombo, monitoringRuleLx, 800.0, 544.4 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        // assertEquals( 6, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    @Test
    public void testValidateMonitoringWithBaseline()
    {
        useDataValue( dataElementB, periodA, sourceB, "11" ); // Mar 2000
        useDataValue( dataElementB, periodB, sourceB, "12" ); // Apr 2000
        useDataValue( dataElementB, periodC, sourceB, "13" ); // May 2000
        useDataValue( dataElementB, periodD, sourceB, "14" ); // Jun 2000
        useDataValue( dataElementB, periodE, sourceB, "15" ); // Jul 2000

        useDataValue( dataElementB, periodF, sourceB, "50" ); // Mar 2001
        useDataValue( dataElementB, periodG, sourceB, "150" ); // Apr 2001
        useDataValue( dataElementB, periodH, sourceB, "200" ); // May 2001
        useDataValue( dataElementB, periodI, sourceB, "600" ); // Jun 2001
        useDataValue( dataElementB, periodJ, sourceB, "400" ); // Jul 2001

        useDataValue( dataElementB, periodK, sourceB, "100" ); // Mar 2002
        useDataValue( dataElementB, periodL, sourceB, "200" ); // Apr 2002
        useDataValue( dataElementB, periodM, sourceB, "400" ); // May 2002
        useDataValue( dataElementB, periodN, sourceB, "700" ); // Jun 2002
        useDataValue( dataElementB, periodO, sourceB, "800" ); // Jul 2002

        // This weekly baseline data should be ignored because the period length
        // is less than monthly:
        useDataValue( dataElementE, periodW, sourceB, "1000" ); // Week:

        useDataValue( dataElementE, periodX, sourceB, "40" ); // Year: 2000
        useDataValue( dataElementE, periodY, sourceB, "50" ); // Year: 2001
        useDataValue( dataElementE, periodZ, sourceB, "10" ); // Year:

        validationRuleService.saveValidationRule( monitoringRuleM );

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2002, 1, 15 ),
            getDate( 2002, 8, 15 ), sourceB );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodK, sourceB, defaultCombo, monitoringRuleM,
            10.0 /* 100 / 10 */, 1.5 /* 1.5 * 50 / 50 */ ) );
        reference.add( new ValidationResult( periodL, sourceB, defaultCombo, monitoringRuleM,
            20.0 /* 200 / 10 */, 4.5 /* 1.5 * 150 / 50 */ ) );
        reference.add( new ValidationResult( periodM, sourceB, defaultCombo, monitoringRuleM,
            40.0 /* 400 / 10 */, 6.0 /* 1.5 * 200 / 50 */ ) );
        reference.add( new ValidationResult( periodN, sourceB, defaultCombo, monitoringRuleM,
            70.0 /* 700 / 10 */, 18.0 /* 1.5 * 600 / 50 */ ) );
        reference.add( new ValidationResult( periodO, sourceB, defaultCombo, monitoringRuleM,
            80.0 /* 800 / 10 */, 12.0 /* 1.5 * 400 / 50 */ ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 5, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );
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
        Collection<ValidationResult> results = validationRuleService.validate( dataSetMonthly, periodA, sourceA,
            optionComboAC );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodA, sourceA, optionComboAC, validationRuleD, 7.0, 6.0 ) );
        reference.add( new ValidationResult( periodA, sourceA, optionComboAC, validationRuleX, 7.0, 6.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 2, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );

        //
        // All optionCombos
        //
        results = validationRuleService.validate( dataSetMonthly, periodA, sourceA, null );

        reference = new HashSet<>();

        reference.add( new ValidationResult( periodA, sourceA, optionComboAC, validationRuleD, 7.0, 6.0 ) );
        reference.add( new ValidationResult( periodA, sourceA, optionComboAC, validationRuleX, 7.0, 6.0 ) );
        reference.add( new ValidationResult( periodA, sourceA, optionComboBC, validationRuleD, 3.0, 2.0 ) );
        reference.add( new ValidationResult( periodA, sourceA, optionComboBC, validationRuleX, 3.0, 2.0 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( 4, results.size() );
        assertEquals( orderedList( reference ), orderedList( results ) );

        //
        // Default optionCombo
        //
        results = validationRuleService.validate( dataSetMonthly, periodA, sourceA, optionCombo );

        assertEquals( 0, results.size() );
    }

    @Test
    public void testValidateMonitoringSkipTest()
    {
        // Note: for some monitoring tests, we enter more data than needed, to
        // be sure the extra data *isn't* used.

        useDataValue( dataElementB, periodA, sourceA, "11" ); // Mar 2000
        useDataValue( dataElementB, periodB, sourceA, "12" ); // Apr 2000
        useDataValue( dataElementB, periodC, sourceA, "13" ); // May 2000
        useDataValue( dataElementB, periodD, sourceA, "14" ); // Jun 2000
        useDataValue( dataElementB, periodE, sourceA, "15" ); // Jul 2000

        useDataValue( dataElementB, periodF, sourceA, "100" ); // Mar 2001
        useDataValue( dataElementB, periodG, sourceA, "200" ); // Apr 2001
        useDataValue( dataElementB, periodH, sourceA, "400" ); // May 2001
        useDataValue( dataElementB, periodI, sourceA, "600" ); // Jun 2001
        useDataValue( dataElementB, periodJ, sourceA, "800" ); // Jul 2001

        useDataValue( dataElementB, periodK, sourceA, "30" ); // Mar 2002
        useDataValue( dataElementB, periodL, sourceA, "35" ); // Apr 2002
        useDataValue( dataElementB, periodM, sourceA, "40" ); // May 2002
        useDataValue( dataElementB, periodN, sourceA, "45" ); // Jun 2002
        useDataValue( dataElementB, periodO, sourceA, "50" ); // Jul 2002

        validationRuleService.saveValidationRule( monitoringRuleLxx );

        Collection<ValidationResult> results = validationRuleService.validate( getDate( 2002, 1, 15 ),
            getDate( 2002, 9, 15 ), sourceA );

        Collection<ValidationResult> reference = new HashSet<>();

        reference.add( new ValidationResult( periodO, sourceA, optionCombo, monitoringRuleLxx, 50.0, 46.5 ) );

        for ( ValidationResult result : results )
        {
            assertFalse( MathUtils.expressionIsTrue( result.getLeftsideValue(),
                result.getValidationRule().getOperator(), result.getRightsideValue() ) );
        }

        assertEquals( orderedList( reference ), orderedList( results ) );
    }

    // -------------------------------------------------------------------------
    // CURD functionality tests
    // -------------------------------------------------------------------------

    @Test
    public void testSaveValidationRule()
    {
        int id = validationRuleService.saveValidationRule( validationRuleA );

        validationRuleA = validationRuleService.getValidationRule( id );

        assertEquals( "ValidationRuleA", validationRuleA.getName() );
        assertEquals( "DescriptionA", validationRuleA.getDescription() );
        assertEquals( equal_to, validationRuleA.getOperator() );
        assertNotNull( validationRuleA.getLeftSide().getExpression() );
        assertNotNull( validationRuleA.getRightSide().getExpression() );
        assertEquals( periodTypeMonthly, validationRuleA.getPeriodType() );
    }

    @Test
    public void testUpdateValidationRule()
    {
        int id = validationRuleService.saveValidationRule( validationRuleA );
        validationRuleA = validationRuleService.getValidationRuleByName( "ValidationRuleA" );

        assertEquals( "ValidationRuleA", validationRuleA.getName() );
        assertEquals( "DescriptionA", validationRuleA.getDescription() );
        assertEquals( equal_to, validationRuleA.getOperator() );

        validationRuleA.setId( id );
        validationRuleA.setName( "ValidationRuleB" );
        validationRuleA.setDescription( "DescriptionB" );
        validationRuleA.setOperator( greater_than );

        validationRuleService.updateValidationRule( validationRuleA );
        validationRuleA = validationRuleService.getValidationRule( id );

        assertEquals( "ValidationRuleB", validationRuleA.getName() );
        assertEquals( "DescriptionB", validationRuleA.getDescription() );
        assertEquals( greater_than, validationRuleA.getOperator() );
    }

    @Test
    public void testDeleteValidationRule()
    {
        int idA = validationRuleService.saveValidationRule( validationRuleA );
        int idB = validationRuleService.saveValidationRule( validationRuleB );

        assertNotNull( validationRuleService.getValidationRule( idA ) );
        assertNotNull( validationRuleService.getValidationRule( idB ) );

        validationRuleA.clearExpressions();

        validationRuleService.deleteValidationRule( validationRuleA );

        assertNull( validationRuleService.getValidationRule( idA ) );
        assertNotNull( validationRuleService.getValidationRule( idB ) );

        validationRuleB.clearExpressions();

        validationRuleService.deleteValidationRule( validationRuleB );

        assertNull( validationRuleService.getValidationRule( idA ) );
        assertNull( validationRuleService.getValidationRule( idB ) );
    }

    @Test
    public void testGetAllValidationRules()
    {
        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );

        Collection<ValidationRule> rules = validationRuleService.getAllValidationRules();

        assertTrue( rules.size() == 2 );
        assertTrue( rules.contains( validationRuleA ) );
        assertTrue( rules.contains( validationRuleB ) );
    }

    @Test
    public void testGetValidationRuleByName()
    {
        int id = validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );

        ValidationRule rule = validationRuleService.getValidationRuleByName( "ValidationRuleA" );

        assertEquals( id, rule.getId() );
        assertEquals( "ValidationRuleA", rule.getName() );
    }

    // -------------------------------------------------------------------------
    // ValidationRuleGroup
    // -------------------------------------------------------------------------

    @Test
    public void testAddValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, null, null, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, null, null, periodTypeMonthly );

        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );

        Set<ValidationRule> rules = new HashSet<>();

        rules.add( ruleA );
        rules.add( ruleB );

        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );

        groupA.setMembers( rules );
        groupB.setMembers( rules );

        int idA = validationRuleService.addValidationRuleGroup( groupA );
        int idB = validationRuleService.addValidationRuleGroup( groupB );

        assertEquals( groupA, validationRuleService.getValidationRuleGroup( idA ) );
        assertEquals( groupB, validationRuleService.getValidationRuleGroup( idB ) );
    }

    @Test
    public void testUpdateValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, null, null, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, null, null, periodTypeMonthly );

        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );

        Set<ValidationRule> rules = new HashSet<>();

        rules.add( ruleA );
        rules.add( ruleB );

        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );

        groupA.setMembers( rules );
        groupB.setMembers( rules );

        int idA = validationRuleService.addValidationRuleGroup( groupA );
        int idB = validationRuleService.addValidationRuleGroup( groupB );

        assertEquals( groupA, validationRuleService.getValidationRuleGroup( idA ) );
        assertEquals( groupB, validationRuleService.getValidationRuleGroup( idB ) );

        ruleA.setName( "UpdatedValidationRuleA" );
        ruleB.setName( "UpdatedValidationRuleB" );

        validationRuleService.updateValidationRuleGroup( groupA );
        validationRuleService.updateValidationRuleGroup( groupB );

        assertEquals( groupA, validationRuleService.getValidationRuleGroup( idA ) );
        assertEquals( groupB, validationRuleService.getValidationRuleGroup( idB ) );
    }

    @Test
    public void testDeleteValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, null, null, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, null, null, periodTypeMonthly );

        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );

        Set<ValidationRule> rules = new HashSet<>();

        rules.add( ruleA );
        rules.add( ruleB );

        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );

        groupA.setMembers( rules );
        groupB.setMembers( rules );

        int idA = validationRuleService.addValidationRuleGroup( groupA );
        int idB = validationRuleService.addValidationRuleGroup( groupB );

        assertNotNull( validationRuleService.getValidationRuleGroup( idA ) );
        assertNotNull( validationRuleService.getValidationRuleGroup( idB ) );

        validationRuleService.deleteValidationRuleGroup( groupA );

        assertNull( validationRuleService.getValidationRuleGroup( idA ) );
        assertNotNull( validationRuleService.getValidationRuleGroup( idB ) );

        validationRuleService.deleteValidationRuleGroup( groupB );

        assertNull( validationRuleService.getValidationRuleGroup( idA ) );
        assertNull( validationRuleService.getValidationRuleGroup( idB ) );
    }

    @Test
    public void testGetAllValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, null, null, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, null, null, periodTypeMonthly );

        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );

        Set<ValidationRule> rules = new HashSet<>();

        rules.add( ruleA );
        rules.add( ruleB );

        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );

        groupA.setMembers( rules );
        groupB.setMembers( rules );

        validationRuleService.addValidationRuleGroup( groupA );
        validationRuleService.addValidationRuleGroup( groupB );

        Collection<ValidationRuleGroup> groups = validationRuleService.getAllValidationRuleGroups();

        assertEquals( 2, groups.size() );
        assertTrue( groups.contains( groupA ) );
        assertTrue( groups.contains( groupB ) );
    }

    @Test
    public void testGetValidationRuleGroupByName()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, null, null, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, null, null, periodTypeMonthly );

        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );

        Set<ValidationRule> rules = new HashSet<>();

        rules.add( ruleA );
        rules.add( ruleB );

        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );

        groupA.setMembers( rules );
        groupB.setMembers( rules );

        validationRuleService.addValidationRuleGroup( groupA );
        validationRuleService.addValidationRuleGroup( groupB );

        ValidationRuleGroup groupByName = validationRuleService.getValidationRuleGroupByName( groupA.getName() );

        assertEquals( groupA, groupByName );
    }
}
