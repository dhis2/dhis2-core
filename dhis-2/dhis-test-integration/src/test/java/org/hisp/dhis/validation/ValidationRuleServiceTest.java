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

import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @author Jim Grace
 */
class ValidationRuleServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private DataElement dataElementE;

    private Set<CategoryOptionCombo> optionCombos;

    private CategoryOptionCombo optionCombo;

    private Expression expressionA;

    private Expression expressionB;

    private Expression expressionC;

    private Expression expressionD;

    private DataSet dataSetWeekly;

    private DataSet dataSetMonthly;

    private DataSet dataSetYearly;

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

    private PeriodType periodTypeWeekly;

    private PeriodType periodTypeMonthly;

    private PeriodType periodTypeYearly;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
        throws Exception
    {
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
        optionCombo = categoryService.getDefaultCategoryOptionCombo();
        String suffix = SEPARATOR + optionCombo.getUid();
        optionCombos = new HashSet<>();
        optionCombos.add( optionCombo );
        expressionA = new Expression(
            "#{" + dataElementA.getUid() + suffix + "} + #{" + dataElementB.getUid() + suffix + "}", "expressionA" );
        expressionB = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionB" );
        expressionC = new Expression(
            "#{" + dataElementC.getUid() + suffix + "} - #{" + dataElementD.getUid() + suffix + "}", "expressionC" );
        expressionD = new Expression( "#{" + dataElementB.getUid() + suffix + "} * 2", "expressionD" );
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
        // deA
        validationRuleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodTypeMonthly, true );
        // +
        // deB
        // =
        // deC
        // -
        // deD
        validationRuleB = createValidationRule( "B", greater_than, expressionC, expressionD, // deC
                                                                                             // -
                                                                                             // deD
                                                                                             // >
                                                                                             // deB
                                                                                             // *
                                                                                             // 2
            periodTypeMonthly );
    }

    // -------------------------------------------------------------------------
    // CURD functionality tests
    // -------------------------------------------------------------------------
    @Test
    void testSaveValidationRule()
    {
        long id = validationRuleService.saveValidationRule( validationRuleA );
        validationRuleA = validationRuleService.getValidationRule( id );
        assertEquals( "ValidationRuleA", validationRuleA.getName() );
        assertEquals( "DescriptionA", validationRuleA.getDescription() );
        assertEquals( equal_to, validationRuleA.getOperator() );
        assertNotNull( validationRuleA.getLeftSide().getExpression() );
        assertNotNull( validationRuleA.getRightSide().getExpression() );
        assertEquals( periodTypeMonthly, validationRuleA.getPeriodType() );
    }

    @Test
    void testUpdateValidationRule()
    {
        long id = validationRuleService.saveValidationRule( validationRuleA );
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
    void testDeleteValidationRule()
    {
        long idA = validationRuleService.saveValidationRule( validationRuleA );
        long idB = validationRuleService.saveValidationRule( validationRuleB );
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
    void testGetAllValidationRules()
    {
        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );
        Collection<ValidationRule> rules = validationRuleService.getAllValidationRules();
        assertTrue( rules.size() == 2 );
        assertTrue( rules.contains( validationRuleA ) );
        assertTrue( rules.contains( validationRuleB ) );
    }

    @Test
    void testGetAllFormValidationRules()
    {
        validationRuleA.setSkipFormValidation( true );
        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );
        Collection<ValidationRule> rules = validationRuleService.getAllFormValidationRules();
        assertTrue( rules.size() == 1 );
        assertTrue( rules.contains( validationRuleB ) );
    }

    @Test
    void testGetValidationRuleByName()
    {
        long id = validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );
        ValidationRule rule = validationRuleService.getValidationRuleByName( "ValidationRuleA" );
        assertEquals( id, rule.getId() );
        assertEquals( "ValidationRuleA", rule.getName() );
    }

    // -------------------------------------------------------------------------
    // ValidationRuleGroup
    // -------------------------------------------------------------------------
    @Test
    void testAddValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, expressionA, expressionA, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, expressionB, expressionB, periodTypeMonthly );
        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );
        Set<ValidationRule> rules = new HashSet<>();
        rules.add( ruleA );
        rules.add( ruleB );
        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );
        groupA.setMembers( rules );
        groupB.setMembers( rules );
        long idA = validationRuleService.addValidationRuleGroup( groupA );
        long idB = validationRuleService.addValidationRuleGroup( groupB );
        assertEquals( groupA, validationRuleService.getValidationRuleGroup( idA ) );
        assertEquals( groupB, validationRuleService.getValidationRuleGroup( idB ) );
    }

    @Test
    void testUpdateValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, expressionC, expressionD, periodTypeMonthly );
        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );
        Set<ValidationRule> rules = new HashSet<>();
        rules.add( ruleA );
        rules.add( ruleB );
        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );
        groupA.setMembers( rules );
        groupB.setMembers( rules );
        long idA = validationRuleService.addValidationRuleGroup( groupA );
        long idB = validationRuleService.addValidationRuleGroup( groupB );
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
    void testDeleteValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, expressionC, expressionD, periodTypeMonthly );
        validationRuleService.saveValidationRule( ruleA );
        validationRuleService.saveValidationRule( ruleB );
        Set<ValidationRule> rules = new HashSet<>();
        rules.add( ruleA );
        rules.add( ruleB );
        ValidationRuleGroup groupA = createValidationRuleGroup( 'A' );
        ValidationRuleGroup groupB = createValidationRuleGroup( 'B' );
        groupA.setMembers( rules );
        groupB.setMembers( rules );
        long idA = validationRuleService.addValidationRuleGroup( groupA );
        long idB = validationRuleService.addValidationRuleGroup( groupB );
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
    void testGetAllValidationRuleGroup()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, expressionC, expressionD, periodTypeMonthly );
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
    void testGetValidationRuleGroupByName()
    {
        ValidationRule ruleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodTypeMonthly );
        ValidationRule ruleB = createValidationRule( "B", equal_to, expressionC, expressionD, periodTypeMonthly );
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
