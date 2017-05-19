package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@Transactional
public class DefaultValidationRuleService
    implements ValidationRuleService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleStore validationRuleStore;

    public void setValidationRuleStore( ValidationRuleStore validationRuleStore )
    {
        this.validationRuleStore = validationRuleStore;
    }

    private GenericIdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore;

    public void setValidationRuleGroupStore( GenericIdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore )
    {
        this.validationRuleGroupStore = validationRuleGroupStore;
    }

    private ExpressionService expressionService;

    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    // -------------------------------------------------------------------------
    // ValidationRule CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int saveValidationRule( ValidationRule validationRule )
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

    @Override
    public ValidationRule getValidationRule( int id )
    {
        return validationRuleStore.get( id );
    }

    @Override
    public ValidationRule getValidationRule( String uid )
    {
        return validationRuleStore.getByUid( uid );
    }

    @Override
    public ValidationRule getValidationRuleByName( String name )
    {
        return validationRuleStore.getByName( name );
    }

    @Override
    public List<ValidationRule> getAllValidationRules()
    {
        return validationRuleStore.getAll();
    }

    @Override
    public List<ValidationRule> getValidationRulesByDataElements( Collection<DataElement> dataElements )
    {
        return validationRuleStore.getValidationRulesByDataElements( dataElements );
    }

    @Override
    public int getValidationRuleCount()
    {
        return validationRuleStore.getCount();
    }

    @Override
    public int getValidationRuleCountByName( String name )
    {
        return validationRuleStore.getCountLikeName( name );
    }

    @Override
    public List<ValidationRule> getValidationRulesBetween( int first, int max )
    {
        return validationRuleStore.getAllOrderedName( first, max ) ;
    }

    @Override
    public List<ValidationRule> getValidationRulesBetweenByName( String name, int first, int max )
    {
        return validationRuleStore.getAllLikeName( name, first, max ) ;
    }

    @Override
    public Collection<ValidationRule> getValidationRulesForDataElements( Set<DataElement> dataElements )
    {
        Set<ValidationRule> rulesForDataElements = new HashSet<>();

        Set<String> deIds = dataElements.stream().map( DataElement::getUid ).collect( Collectors.toSet() );

        for ( ValidationRule rule : getAllValidationRules() )
        {
            if ( !Sets.intersection( expressionService.getDataElementIdsInExpression( rule.getLeftSide().getExpression() ), deIds ).isEmpty() ||
                !Sets.intersection( expressionService.getDataElementIdsInExpression( rule.getRightSide().getExpression() ), deIds ).isEmpty() )
            {
                rulesForDataElements.add( rule );
            }
        }

        return rulesForDataElements;
    }

    @Override
    public Set<DataElement> getDataElements( ValidationRule validationRule )
    {
        Set<DataElement> elements = new HashSet<>();
        elements.addAll( expressionService.getDataElementsInExpression( validationRule.getLeftSide().getExpression() ) );
        elements.addAll( expressionService.getDataElementsInExpression( validationRule.getRightSide().getExpression() ) );
        return elements;
    }

    @Override
    public List<ValidationRule> getValidationRulesWithNotificationTemplates()
    {
        return validationRuleStore.getValidationRulesWithNotificationTemplates();
    }
    
    // -------------------------------------------------------------------------
    // ValidationRuleGroup CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int addValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
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

    @Override
    public ValidationRuleGroup getValidationRuleGroup( int id )
    {
        return validationRuleGroupStore.get( id );
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroup( String uid )
    {
        return validationRuleGroupStore.getByUid( uid );
    }

    @Override
    public List<ValidationRuleGroup> getAllValidationRuleGroups()
    {
        return validationRuleGroupStore.getAll();
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroupByName( String name )
    {
        return validationRuleGroupStore.getByName( name );
    }

    @Override
    public int getValidationRuleGroupCount()
    {
        return validationRuleGroupStore.getCount();
    }

    @Override
    public int getValidationRuleGroupCountByName( String name )
    {
        return validationRuleGroupStore.getCountLikeName( name ) ;
    }

    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetween( int first, int max )
    {
        return validationRuleGroupStore.getAllOrderedName( first, max );
    }

    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetweenByName( String name, int first, int max )
    {
        return validationRuleGroupStore.getAllLikeName( name, first, max ) ;
    }
}
