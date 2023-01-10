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

import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.validation.ValidationRuleService" )
@Transactional
public class DefaultValidationRuleService
    implements ValidationRuleService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ValidationRuleStore validationRuleStore;

    @Qualifier( "org.hisp.dhis.validation.ValidationRuleGroupStore" )
    private final IdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore;

    private final ExpressionService expressionService;

    private final IdentifiableObjectManager idObjectManager;

    // -------------------------------------------------------------------------
    // ValidationRule CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public long saveValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.save( validationRule );

        return validationRule.getId();
    }

    @Override
    public void updateValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.update( validationRule );
    }

    @Override
    public void deleteValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.delete( validationRule );
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationRule getValidationRule( long id )
    {
        return validationRuleStore.get( id );
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationRule getValidationRule( String uid )
    {
        return validationRuleStore.getByUid( uid );
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationRule getValidationRuleByName( String name )
    {
        return validationRuleStore.getByName( name );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getAllValidationRules()
    {
        return validationRuleStore.getAll();
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getAllFormValidationRules()
    {
        return validationRuleStore.getAllFormValidationRules();
    }

    @Transactional( readOnly = true )
    @Override
    public int getValidationRuleCount()
    {
        return validationRuleStore.getCount();
    }

    @Transactional( readOnly = true )
    @Override
    public int getValidationRuleCountByName( String name )
    {
        return validationRuleStore.getCountLikeName( name );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getValidationRulesBetween( int first, int max )
    {
        return validationRuleStore.getAllOrderedName( first, max );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getValidationRulesBetweenByName( String name, int first, int max )
    {
        return validationRuleStore.getAllLikeName( name, first, max );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getValidationRulesByUid( @Nonnull Collection<String> uids )
    {
        return validationRuleStore.getByUid( uids );
    }

    @Transactional( readOnly = true )
    @Override
    public Set<ValidationRule> getValidationRulesForDataSet( DataSet dataSet )
    {
        Set<String> elementsAndOptionCombos = new HashSet<>();

        for ( DataSetElement dataSetElement : dataSet.getDataSetElements() )
        {
            DataElement dataElement = dataSetElement.getDataElement();

            elementsAndOptionCombos.add( dataElement.getUid() );

            CategoryCombo catCombo = dataSetElement.hasCategoryCombo()
                ? dataSetElement.getCategoryCombo()
                : dataElement.getCategoryCombo();

            for ( CategoryOptionCombo optionCombo : catCombo.getOptionCombos() )
            {
                elementsAndOptionCombos.add( dataElement.getUid() + Expression.SEPARATOR + optionCombo.getUid() );
            }
        }

        Set<ValidationRule> rulesForDataSet = new HashSet<>();

        for ( ValidationRule rule : getAllFormValidationRules() )
        {
            Set<String> leftSideElementsAndCombos = expressionService.getExpressionElementAndOptionComboIds(
                rule.getLeftSide().getExpression(), VALIDATION_RULE_EXPRESSION );
            Set<String> rightSideElementsAndCombos = expressionService.getExpressionElementAndOptionComboIds(
                rule.getRightSide().getExpression(), VALIDATION_RULE_EXPRESSION );

            if ( !Sets.intersection( leftSideElementsAndCombos, elementsAndOptionCombos ).isEmpty() ||
                !Sets.intersection( rightSideElementsAndCombos, elementsAndOptionCombos ).isEmpty() )
            {
                rulesForDataSet.add( rule );
            }
        }

        return rulesForDataSet;
    }

    @Transactional( readOnly = true )
    @Override
    public Set<DataElement> getDataElements( ValidationRule validationRule )
    {
        Set<String> uids = new HashSet<>();
        uids.addAll( expressionService.getExpressionDataElementIds( validationRule.getLeftSide().getExpression(),
            VALIDATION_RULE_EXPRESSION ) );
        uids.addAll( expressionService.getExpressionDataElementIds( validationRule.getRightSide().getExpression(),
            VALIDATION_RULE_EXPRESSION ) );
        return new HashSet<>( idObjectManager.getByUid( DataElement.class, uids ) );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getValidationRulesWithNotificationTemplates()
    {
        return validationRuleStore.getValidationRulesWithNotificationTemplates();
    }

    // -------------------------------------------------------------------------
    // ValidationRuleGroup CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public long addValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.save( validationRuleGroup );

        return validationRuleGroup.getId();
    }

    @Override
    public void deleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.delete( validationRuleGroup );
    }

    @Override
    public void updateValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.update( validationRuleGroup );
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationRuleGroup getValidationRuleGroup( long id )
    {
        return validationRuleGroupStore.get( id );
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationRuleGroup getValidationRuleGroup( String uid )
    {
        return validationRuleGroupStore.getByUid( uid );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRuleGroup> getAllValidationRuleGroups()
    {
        return validationRuleGroupStore.getAll();
    }

    @Transactional( readOnly = true )
    @Override
    public ValidationRuleGroup getValidationRuleGroupByName( String name )
    {
        return validationRuleGroupStore.getByName( name );
    }

    @Transactional( readOnly = true )
    @Override
    public int getValidationRuleGroupCount()
    {
        return validationRuleGroupStore.getCount();
    }

    @Transactional( readOnly = true )
    @Override
    public int getValidationRuleGroupCountByName( String name )
    {
        return validationRuleGroupStore.getCountLikeName( name );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetween( int first, int max )
    {
        return validationRuleGroupStore.getAllOrderedName( first, max );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetweenByName( String name, int first, int max )
    {
        return validationRuleGroupStore.getAllLikeName( name, first, max );
    }

    @Transactional( readOnly = true )
    @Override
    public List<ValidationRule> getValidationRulesWithoutGroups()
    {
        return validationRuleStore.getValidationRulesWithoutGroups();
    }

}
