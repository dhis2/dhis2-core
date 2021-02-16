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

import static java.util.Arrays.asList;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.expression.Operator.greater_than;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class ValidationRuleStoreTest
    extends DhisSpringTest
{

    @Autowired
    private ValidationRuleStore validationRuleStore;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    private final Expression expressionA = new Expression( "expressionA", "descriptionA" );

    private final Expression expressionB = new Expression( "expressionB", "descriptionB" );

    private final Expression expressionC = new Expression( "expressionC", "descriptionC" );

    private final Expression expressionD = new Expression( "expressionD", "descriptionD" );

    private final PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();

    private void assertValidationRule( char uniqueCharacter, ValidationRule actual )
    {
        assertEquals( "ValidationRule" + uniqueCharacter, actual.getName() );
        assertEquals( "Description" + uniqueCharacter, actual.getDescription() );
        assertNotNull( actual.getLeftSide().getExpression() );
        assertNotNull( actual.getRightSide().getExpression() );
        assertEquals( periodType, actual.getPeriodType() );
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    @Test
    public void testSaveValidationRule()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );

        ruleA = validationRuleStore.get( ruleA.getId() );

        assertValidationRule( 'A', ruleA );
        assertEquals( equal_to, ruleA.getOperator() );
    }

    @Test
    public void testUpdateValidationRule()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );

        ruleA = validationRuleStore.get( ruleA.getId() );

        assertValidationRule( 'A', ruleA );
        assertEquals( equal_to, ruleA.getOperator() );

        ruleA.setName( "ValidationRuleB" );
        ruleA.setDescription( "DescriptionB" );
        ruleA.setOperator( greater_than );

        validationRuleStore.update( ruleA );

        ruleA = validationRuleStore.get( ruleA.getId() );

        assertValidationRule( 'B', ruleA );
        assertEquals( greater_than, ruleA.getOperator() );
    }

    @Test
    public void testDeleteValidationRule()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule ruleB = addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        assertNotNull( validationRuleStore.get( ruleA.getId() ) );
        assertNotNull( validationRuleStore.get( ruleB.getId() ) );

        ruleA.clearExpressions();
        validationRuleStore.delete( ruleA );

        assertNull( validationRuleStore.get( ruleA.getId() ) );
        assertNotNull( validationRuleStore.get( ruleB.getId() ) );

        ruleB.clearExpressions();
        validationRuleStore.delete( ruleB );

        assertNull( validationRuleStore.get( ruleA.getId() ) );
        assertNull( validationRuleStore.get( ruleB.getId() ) );
    }

    @Test
    public void testGetAllValidationRules()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule ruleB = addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        assertContainsOnly( validationRuleStore.getAll(), ruleA, ruleB );
    }

    @Test
    public void testGetAllFormValidationRules()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType, true );
        ValidationRule ruleB = addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        assertContainsOnly( validationRuleStore.getAllFormValidationRules(), ruleB );
    }

    @Test
    public void testGetValidationRuleByName()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule ruleB = addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        assertEquals( ruleA, validationRuleStore.getByName( "ValidationRuleA" ) );
        assertEquals( ruleB, validationRuleStore.getByName( "ValidationRuleB" ) );
    }

    @Test
    public void testGetValidationRuleCount()
    {
        addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        assertEquals( 2, validationRuleStore.getCount() );
    }

    protected static Expression createExpression( char uniqueCharacter )
    {
        String expression = "Expression1" + uniqueCharacter;
        return new Expression( expression, expression );
    }

    @Test
    public void testGetValidationRulesWithNotificationTemplates()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule ruleB = addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        // Test empty
        assertContainsOnly( validationRuleStore.getValidationRulesWithNotificationTemplates() );

        // Add template
        ValidationNotificationTemplate template = addValidationNotificationTemplate( 'A', ruleA );
        assertContainsOnly( validationRuleStore.getValidationRulesWithNotificationTemplates(), ruleA );

        // Add one more
        template = addValidationNotificationTemplate( 'B', ruleB );
        assertContainsOnly( validationRuleStore.getValidationRulesWithNotificationTemplates(), ruleA, ruleB );
    }

    @Test
    public void testGetValidationRulesWithoutGroups()
    {
        ValidationRule ruleA = addValidationRule( 'A', equal_to, expressionA, expressionB, periodType );
        ValidationRule ruleB = addValidationRule( 'B', equal_to, expressionC, expressionD, periodType );

        ValidationRuleGroup groupA = addValidationRuleGroup( 'A', ruleA );

        assertContainsOnly( validationRuleStore.getValidationRulesWithoutGroups(), ruleB );
    }

    private ValidationNotificationTemplate addValidationNotificationTemplate( char uniqueCharacter,
        ValidationRule... rules )
    {
        ValidationNotificationTemplate template = createValidationNotificationTemplate( "Template " + uniqueCharacter );
        asList( rules ).forEach( template::addValidationRule );
        idObjectManager.save( template );
        return template;
    }

    private ValidationRule addValidationRule( char uniqueCharacter, Operator operator, Expression leftSide,
        Expression rightSide, PeriodType periodType )
    {
        return addValidationRule( uniqueCharacter, operator, leftSide, rightSide, periodType, false );
    }

    private ValidationRule addValidationRule( char uniqueCharacter, Operator operator, Expression leftSide,
        Expression rightSide, PeriodType periodType, boolean skipFormValidation )
    {
        ValidationRule rule = createValidationRule( Character.toString( uniqueCharacter ), operator, leftSide,
            rightSide, periodType, skipFormValidation );
        validationRuleStore.save( rule );
        return rule;
    }

    private ValidationRuleGroup addValidationRuleGroup( char uniqueCharacter, ValidationRule... rules )
    {
        ValidationRuleGroup group = createValidationRuleGroup( uniqueCharacter );
        asList( rules ).forEach( group::addValidationRule );
        idObjectManager.save( group );
        return group;
    }
}
