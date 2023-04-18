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
package org.hisp.dhis.tracker.imports.programrule.executor;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.warning;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.programrule.IssueType;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;

/**
 * This executor checks if there are errors or warnings in the
 * {@link TrackerBundle}
 *
 * @Author Enrico Colasante
 */
public interface ValidationExecutor<T> extends RuleActionExecutor<T>
{
    IssueType getIssueType();

    boolean needsToRun( T t );

    default Optional<ProgramRuleIssue> execute( ValidationRuleAction ruleAction, T t )
    {
        if ( needsToRun( t ) )
        {
            return mapToIssue( ruleAction );
        }
        return Optional.empty();
    }

    private Optional<ProgramRuleIssue> mapToIssue( ValidationRuleAction ruleAction )
    {
        StringBuilder validationMessage = new StringBuilder( ruleAction.getContent() );
        String data = ruleAction.getData();
        if ( !StringUtils.isEmpty( data ) )
        {
            validationMessage.append( " " ).append( data );
        }
        String field = ruleAction.getField();
        if ( !StringUtils.isEmpty( field ) )
        {
            validationMessage.append( " (" ).append( field ).append( ")" );
        }

        switch ( getIssueType() )
        {
        case WARNING:
            return Optional.of( warning( ruleAction.getRuleUid(), ValidationCode.E1300,
                validationMessage.toString() ) );
        case ERROR:
            return Optional.of(
                error( ruleAction.getRuleUid(), ValidationCode.E1300, validationMessage.toString() ) );
        default:
            return Optional.empty();
        }
    }
}
