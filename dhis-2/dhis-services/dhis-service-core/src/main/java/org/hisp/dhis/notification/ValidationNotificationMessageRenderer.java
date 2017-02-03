package org.hisp.dhis.notification;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.CURRENT_DATE;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.DESCRIPTION;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.IMPORTANCE;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.LEFT_SIDE_DESCRIPTION;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.LEFT_SIDE_VALUE;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.OPERATOR;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.ORG_UNIT_NAME;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.PERIOD;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.RIGHT_SIDE_DESCRIPTION;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.RIGHT_SIDE_VALUE;
import static org.hisp.dhis.validation.notification.ValidationRuleTemplateVariable.RULE_NAME;

/**
 * @author Halvdan Hoem Grelland
 */
public class ValidationNotificationMessageRenderer
    extends BaseNotificationMessageRenderer<ValidationResult>
{
    private static final ImmutableMap<TemplateVariable, Function<ValidationResult, String>> VARIABLE_RESOLVERS =
        new ImmutableMap.Builder<TemplateVariable, Function<ValidationResult, String>>()
            .put( RULE_NAME, vr -> vr.getValidationRule().getDisplayName() )
            .put( DESCRIPTION, vr -> vr.getValidationRule().getDescription() )
            .put( OPERATOR, vr -> vr.getValidationRule().getOperator().getMathematicalOperator() )
            .put( IMPORTANCE, vr -> vr.getValidationRule().getImportance().name() )
            .put( LEFT_SIDE_DESCRIPTION, vr -> vr.getValidationRule().getLeftSide().getDescription() )
            .put( RIGHT_SIDE_DESCRIPTION, vr -> vr.getValidationRule().getRightSide().getDescription() )
            .put( LEFT_SIDE_VALUE, vr -> Double.toString( vr.getLeftsideValue() ) )
            .put( RIGHT_SIDE_VALUE, vr -> Double.toString( vr.getRightsideValue() ) )
            .put( ORG_UNIT_NAME, vr -> vr.getOrganisationUnit().getDisplayName() )
            .put( PERIOD, vr -> vr.getPeriod().getDisplayName() )
            .put( CURRENT_DATE, vr -> formatDate( new Date() ) )
            .build();

    private static final ImmutableSet<ExpressionType> SUPPORTED_EXPRESSION_TYPES = ImmutableSet.of( ExpressionType.VARIABLE );

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ValidationNotificationMessageRenderer()
    {
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @Override
    protected ImmutableMap<TemplateVariable, Function<ValidationResult, String>> getVariableResolvers()
    {
        return VARIABLE_RESOLVERS;
    }

    @Override
    protected Map<String, String> resolveAttributeValues( Set<String> attributeKeys, ValidationResult result )
    {
        // Attributes are not supported for validation notifications
        return Collections.emptyMap();
    }

    @Override
    protected TemplateVariable fromVariableName( String name )
    {
        return ValidationRuleTemplateVariable.fromVariableName( name );
    }

    @Override
    protected Set<ExpressionType> getSupportedExpressionTypes()
    {
        return SUPPORTED_EXPRESSION_TYPES;
    }
}
