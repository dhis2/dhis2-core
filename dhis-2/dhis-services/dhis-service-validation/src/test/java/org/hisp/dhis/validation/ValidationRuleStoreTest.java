<<<<<<< HEAD
package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class ValidationRuleStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ValidationRuleStore validationRuleStore;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private Set<DataElement> dataElements;

    private Set<CategoryOptionCombo> optionCombos;

    private Expression expressionA;

    private Expression expressionB;

    private Expression expressionC;

    private Expression expressionD;

    private PeriodType periodType;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        dataElements = new HashSet<>();

        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );

        CategoryOptionCombo categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );

        expressionA = new Expression( "expressionA", "descriptionA" );
        expressionB = new Expression( "expressionB", "descriptionB" );
        expressionC = new Expression( "expressionC", "descriptionC" );
        expressionD = new Expression( "expressionD", "descriptionD" );

        periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    @Test
    public void testSaveValidationRule()
    {
        ValidationRule validationRule = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );

        validationRuleStore.save( validationRule );
        long id = validationRule.getId();

        validationRule = validationRuleStore.get( id );

        assertEquals( validationRule.getName(), "ValidationRuleA" );
        assertEquals( validationRule.getDescription(), "DescriptionA" );
        assertEquals( validationRule.getOperator(), equal_to );
        assertNotNull( validationRule.getLeftSide().getExpression() );
        assertNotNull( validationRule.getRightSide().getExpression() );
        assertEquals( validationRule.getPeriodType(), periodType );
    }

    @Test
    public void testUpdateValidationRule()
    {
        ValidationRule validationRule = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );

        validationRuleStore.save( validationRule );
        long id = validationRule.getId();

        validationRule = validationRuleStore.get( id );

        assertEquals( validationRule.getName(), "ValidationRuleA" );
        assertEquals( validationRule.getDescription(), "DescriptionA" );
        assertEquals( validationRule.getOperator(), equal_to );

        validationRule.setName( "ValidationRuleB" );
        validationRule.setDescription( "DescriptionB" );
        validationRule.setOperator( greater_than );

        validationRuleStore.update( validationRule );

        validationRule = validationRuleStore.get( id );

        assertEquals( validationRule.getName(), "ValidationRuleB" );
        assertEquals( validationRule.getDescription(), "DescriptionB" );
        assertEquals( validationRule.getOperator(), greater_than );
    }

    @Test
    public void testDeleteValidationRule()
    {
        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        long idA = validationRuleA.getId();
        validationRuleStore.save( validationRuleB );
        long idB = validationRuleB.getId();

        assertNotNull( validationRuleStore.get( idA ) );
        assertNotNull( validationRuleStore.get( idB ) );

        validationRuleA.clearExpressions();

        validationRuleStore.delete( validationRuleA );

        assertNull( validationRuleStore.get( idA ) );
        assertNotNull( validationRuleStore.get( idB ) );

        validationRuleB.clearExpressions();

        validationRuleStore.delete( validationRuleB );

        assertNull( validationRuleStore.get( idA ) );
        assertNull( validationRuleStore.get( idB ) );
    }

    @Test
    public void testGetAllValidationRules()
    {
        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        validationRuleStore.save( validationRuleB );

        List<ValidationRule> rules = validationRuleStore.getAll();

        assertTrue( rules.size() == 2 );
        assertTrue( rules.contains( validationRuleA ) );
        assertTrue( rules.contains( validationRuleB ) );
    }

    @Test
    public void testGetAllFormValidationRules()
    {
        ValidationRule validationRuleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodType, true );
=======
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
package org.hisp.dhis.validation;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class ValidationRuleStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ValidationRuleStore validationRuleStore;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private Set<DataElement> dataElements;

    private Set<CategoryOptionCombo> optionCombos;

    private Expression expressionA;

    private Expression expressionB;

    private Expression expressionC;

    private Expression expressionD;

    private PeriodType periodType;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        dataElements = new HashSet<>();

        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );

        CategoryOptionCombo categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );

        expressionA = new Expression( "expressionA", "descriptionA" );
        expressionB = new Expression( "expressionB", "descriptionB" );
        expressionC = new Expression( "expressionC", "descriptionC" );
        expressionD = new Expression( "expressionD", "descriptionD" );

        periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    @Test
    public void testSaveValidationRule()
    {
        ValidationRule validationRule = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );

        validationRuleStore.save( validationRule );
        long id = validationRule.getId();

        validationRule = validationRuleStore.get( id );

        assertEquals( validationRule.getName(), "ValidationRuleA" );
        assertEquals( validationRule.getDescription(), "DescriptionA" );
        assertEquals( validationRule.getOperator(), equal_to );
        assertNotNull( validationRule.getLeftSide().getExpression() );
        assertNotNull( validationRule.getRightSide().getExpression() );
        assertEquals( validationRule.getPeriodType(), periodType );
    }

    @Test
    public void testUpdateValidationRule()
    {
        ValidationRule validationRule = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );

        validationRuleStore.save( validationRule );
        long id = validationRule.getId();

        validationRule = validationRuleStore.get( id );

        assertEquals( validationRule.getName(), "ValidationRuleA" );
        assertEquals( validationRule.getDescription(), "DescriptionA" );
        assertEquals( validationRule.getOperator(), equal_to );

        validationRule.setName( "ValidationRuleB" );
        validationRule.setDescription( "DescriptionB" );
        validationRule.setOperator( greater_than );

        validationRuleStore.update( validationRule );

        validationRule = validationRuleStore.get( id );

        assertEquals( validationRule.getName(), "ValidationRuleB" );
        assertEquals( validationRule.getDescription(), "DescriptionB" );
        assertEquals( validationRule.getOperator(), greater_than );
    }

    @Test
    public void testDeleteValidationRule()
    {
        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        long idA = validationRuleA.getId();
        validationRuleStore.save( validationRuleB );
        long idB = validationRuleB.getId();

        assertNotNull( validationRuleStore.get( idA ) );
        assertNotNull( validationRuleStore.get( idB ) );

        validationRuleA.clearExpressions();

        validationRuleStore.delete( validationRuleA );

        assertNull( validationRuleStore.get( idA ) );
        assertNotNull( validationRuleStore.get( idB ) );

        validationRuleB.clearExpressions();

        validationRuleStore.delete( validationRuleB );

        assertNull( validationRuleStore.get( idA ) );
        assertNull( validationRuleStore.get( idB ) );
    }

    @Test
    public void testGetAllValidationRules()
    {
        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        validationRuleStore.save( validationRuleB );

        List<ValidationRule> rules = validationRuleStore.getAll();

        assertTrue( rules.size() == 2 );
        assertTrue( rules.contains( validationRuleA ) );
        assertTrue( rules.contains( validationRuleB ) );
    }

    @Test
    public void testGetAllFormValidationRules()
    {
        ValidationRule validationRuleA = createValidationRule( "A", equal_to, expressionA, expressionB, periodType,
            true );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        validationRuleStore.save( validationRuleB );

        List<ValidationRule> rules = validationRuleStore.getAllFormValidationRules();

        assertTrue( rules.size() == 1 );
        assertTrue( rules.contains( validationRuleB ) );
    }

    @Test
    public void testGetValidationRuleByName()
    {
        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        long id = validationRuleA.getId();
        validationRuleStore.save( validationRuleB );

        ValidationRule rule = validationRuleStore.getByName( "ValidationRuleA" );

        assertEquals( rule.getId(), id );
        assertEquals( rule.getName(), "ValidationRuleA" );
    }

    @Test
    public void testGetValidationRuleCount()
    {
        Set<DataElement> dataElementsA = new HashSet<>();
        dataElementsA.add( dataElementA );
        dataElementsA.add( dataElementB );

        Set<DataElement> dataElementsB = new HashSet<>();
        dataElementsB.add( dataElementC );
        dataElementsB.add( dataElementD );

        Set<DataElement> dataElementsD = new HashSet<>();
        dataElementsD.addAll( dataElementsA );
        dataElementsD.addAll( dataElementsB );

        Expression expression1 = new Expression( "Expression1", "Expression1" );
        Expression expression2 = new Expression( "Expression2", "Expression2" );
        Expression expression3 = new Expression( "Expression3", "Expression3" );
        Expression expression4 = new Expression( "Expression4", "Expression4" );
        Expression expression5 = new Expression( "Expression5", "Expression5" );
        Expression expression6 = new Expression( "Expression6", "Expression6" );

        ValidationRule ruleA = createValidationRule( 'A', equal_to, expression1, expression2, periodType );
        ValidationRule ruleB = createValidationRule( 'B', equal_to, expression3, expression4, periodType );
        ValidationRule ruleC = createValidationRule( 'C', equal_to, expression5, expression6, periodType );

        validationRuleStore.save( ruleA );
        validationRuleStore.save( ruleB );
        validationRuleStore.save( ruleC );

        assertNotNull( validationRuleStore.getCount() );
        assertEquals( 3, validationRuleStore.getCount() );
    }

    @Test
    public void testGetValidationRulesWithNotificationTemplates()
    {
        // Setup
        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        validationRuleStore.save( validationRuleA );
        validationRuleStore.save( validationRuleB );

        // Test empty

        List<ValidationRule> rules = validationRuleStore.getValidationRulesWithNotificationTemplates();

        assertEquals( 0, rules.size() );

        // Add template

        ValidationNotificationTemplate template = createValidationNotificationTemplate( "Template A" );

        template.addValidationRule( validationRuleA );
        idObjectManager.save( template );

        rules = validationRuleStore.getValidationRulesWithNotificationTemplates();

        assertEquals( 1, rules.size() );

        // Add one more

        template.addValidationRule( validationRuleB );
        idObjectManager.update( template );

        rules = validationRuleStore.getValidationRulesWithNotificationTemplates();

        assertEquals( 2, rules.size() );
        assertTrue( rules.containsAll( Sets.newHashSet( validationRuleA, validationRuleB ) ) );
    }
}
