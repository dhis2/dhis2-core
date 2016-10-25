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

import com.google.common.collect.Sets;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.*;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.Operator.*;

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

    private DataSet dataSetWeekly;

    private DataSet dataSetMonthly;

    private DataSet dataSetYearly;

    private Period periodA;
    private Period periodB;

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
            Sets.newHashSet( dataElementA, dataElementB ) );
        expressionB = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB",
            Sets.newHashSet( dataElementC, dataElementD ) );
        expressionC = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionC", Sets.newHashSet( dataElementB ) );
        expressionD = new Expression( "#{" + dataElementB.getUid() + suffix + "}", "expressionD", Sets.newHashSet( dataElementB ) );
        expressionE = new Expression( "AVG(#{" + dataElementB.getUid() + suffix + "} * 1.5)", "expressionE",
            Sets.newHashSet( dataElementB ), Sets.newHashSet( dataElementB ) );
        expressionF = new Expression(
            "#{" + dataElementB.getUid() + suffix + "} / #{" + dataElementE.getUid() + suffix + "}", "expressionF",
            Sets.newHashSet( dataElementB, dataElementE ) );
        expressionG = new Expression(
            "AVG(#{" + dataElementB.getUid() + suffix + "} * 1.5 / #{" + dataElementE.getUid() + suffix + "})",
            "expressionG", Sets.newHashSet( dataElementB, dataElementE ), Sets.newHashSet( dataElementB, dataElementE ) );
        expressionH = new Expression(
            "AVG(#{" + dataElementB.getUid() + suffix + "}) + 1.5*STDDEV(#{" + dataElementB.getUid() + suffix + "})",
            "expressionH", Sets.newHashSet(), Sets.newHashSet( dataElementB ) );

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
