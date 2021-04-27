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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidationContext;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * @author Zubair Asghar
 */

@Slf4j
@Component( "programRuleActionObjectBundle" )
public class ProgramRuleActionObjectBundleHook extends AbstractObjectBundleHook
{
    @NonNull
    @Qualifier( "programRuleActionValidatorMap" )
    private final Map<ProgramRuleActionType, Class<? extends ProgramRuleActionValidator>> validatorMap;

    public ProgramRuleActionObjectBundleHook(
        @NonNull Map<ProgramRuleActionType, Class<? extends ProgramRuleActionValidator>> validatorMap )
    {
        this.validatorMap = validatorMap;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( !ProgramRuleAction.class.isInstance( object ) )
        {
            return ImmutableList.of();
        }

        ProgramRuleAction programRuleAction = (ProgramRuleAction) object;

        ProgramRuleActionValidationResult validationResult = validateProgramRuleAction( programRuleAction, bundle );

        if ( !validationResult.isValid() )
        {
            return ImmutableList.of( validationResult.getErrorReport() );
        }

        return ImmutableList.of();
    }

    private ProgramRuleActionValidationResult validateProgramRuleAction( ProgramRuleAction ruleAction,
        ObjectBundle bundle )
    {
        ProgramRuleActionValidationResult validationResult;

        ProgramRuleActionValidationContext validationContext = ProgramRuleActionValidationContext
            .load( bundle.getPreheat(), bundle.getPreheatIdentifier(), ruleAction );

        try
        {
            ProgramRuleActionValidator validator = validatorMap.get( ruleAction.getProgramRuleActionType() )
                .newInstance();

            validationResult = validator.validate( ruleAction, validationContext );

            return validationResult;
        }
        catch ( InstantiationException | IllegalAccessException e )
        {
            log.error( "An error occurred during program rule action validation", e );
        }

        return ProgramRuleActionValidationResult.builder().valid( false )
            .errorReport(
                new ErrorReport( ProgramRuleAction.class, ErrorCode.E4033, ruleAction.getProgramRuleActionType().name(),
                    validationContext.getProgramRule().getName() ) )
            .build();
    }
}
