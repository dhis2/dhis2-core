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
package org.hisp.dhis.programrule.engine;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component( "org.hisp.dhis.programrule.engine.RuleActionAssignValueImplementer" )
@Transactional
public class RuleActionAssignValueImplementer implements RuleActionImplementer
{
    private static final String REGEX = "[a-zA-Z0-9]+(?:[\\w -._]*[a-zA-Z0-9]+)*";

    private static final Pattern PATTERN = Pattern.compile( REGEX, Pattern.CASE_INSENSITIVE );

    private RuleVariableInMemoryMap variableMap;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageInstanceService programStageInstanceService;

    public RuleActionAssignValueImplementer( RuleVariableInMemoryMap variableMap,
        ProgramInstanceService programInstanceService, ProgramStageInstanceService programStageInstanceService )
    {
        checkNotNull( variableMap );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );

        this.variableMap = variableMap;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
    }

    @Override
    public boolean accept( RuleAction ruleAction )
    {
        return ruleAction instanceof RuleActionAssign;
    }

    @Override
    public void implement( RuleEffect ruleEffect, ProgramInstance programInstance )
    {
        checkNotNull( ruleEffect, "Rule Effect cannot be null" );
        checkNotNull( programInstance, "ProgramInstance cannot be null" );

        assignValue( ruleEffect, programInstance );
    }

    @Override
    public void implement( RuleEffect ruleEffect, ProgramStageInstance programStageInstance )
    {
        ProgramInstance programInstance = programStageInstance.getProgramInstance();

        assignValue( ruleEffect, programInstance );
    }

    private void assignValue( RuleEffect ruleEffect, ProgramInstance programInstance )
    {
        if ( programInstance == null )
        {
            log.info( "No value assigned by AssignValue action" );
            return;
        }

        String value = ruleEffect.data();

        RuleActionAssign assign = (RuleActionAssign) ruleEffect.ruleAction();

        String variable = assign.field();

        Matcher matcher = PATTERN.matcher( variable );

        while ( matcher.find() )
        {
            variable = matcher.group( 0 ).trim();
        }

        log.debug( "Assigning: " + variable + " with value: " + value );

        if ( !variableMap.containsKey( programInstance.getUid() ) )
        {
            variableMap.put( programInstance.getUid(), new HashMap<>() );
        }

        variableMap.get( programInstance.getUid() ).put( variable, value );
    }
}
