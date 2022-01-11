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
package org.hisp.dhis.programrule.action.validation;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */

@Slf4j
@Component
public class HideOptionProgramRuleActionValidator extends BaseProgramRuleActionValidator
{
    @Override
    public ProgramRuleActionValidationResult validate( ProgramRuleAction programRuleAction,
        ProgramRuleActionValidationContext validationContext )
    {
        // First checking the validity of DataElement and TEA
        ProgramRuleActionValidationResult result = super.validate( programRuleAction,
            validationContext );

        ProgramRule rule = validationContext.getProgramRule();

        if ( !result.isValid() )
        {
            return result;
        }

        if ( !programRuleAction.hasOption() )
        {
            log.debug( String.format( "Option cannot be null for program rule: %s ",
                rule.getName() ) );

            return ProgramRuleActionValidationResult.builder()
                .valid( false )
                .errorReport( new ErrorReport( Option.class, ErrorCode.E4040,
                    rule.getName() ) )
                .build();
        }

        Option option = validationContext.getOption();

        if ( option == null )
        {
            log.debug( String.format( "Option %s associated with program rule %s does not exist",
                programRuleAction.getOption().getUid(), rule.getName() ) );

            return ProgramRuleActionValidationResult.builder()
                .valid( false )
                .errorReport( new ErrorReport( Option.class, ErrorCode.E4041,
                    programRuleAction.getOption().getUid(), rule.getName() ) )
                .build();
        }

        return ProgramRuleActionValidationResult.builder().valid( true ).build();
    }
}
