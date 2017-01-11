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

import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.system.deletion.DeletionHandler;

import java.util.Iterator;

/**
 * @author Lars Helge Overland
 */
public class ValidationRuleDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return ValidationRule.class.getSimpleName();
    }

    @Override
    public void deleteExpression( Expression expression )
    {
        Iterator<ValidationRule> iterator = validationRuleService.getAllValidationRules().iterator();
        
        while ( iterator.hasNext() )
        {
            ValidationRule rule = iterator.next();
            
            Expression leftSide = rule.getLeftSide();
            Expression rightSide = rule.getRightSide();

            if ( (leftSide != null && leftSide.equals( expression )) ||
                 (rightSide != null && rightSide.equals( expression )) )
            {
                iterator.remove();
                validationRuleService.deleteValidationRule( rule );
            }
        }
    }
    
    @Override
    public void deleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        for ( ValidationRule rule : validationRuleGroup.getMembers() )
        {
            rule.getGroups().remove( validationRuleGroup );
            validationRuleService.updateValidationRule( rule );
        }
    }
}
