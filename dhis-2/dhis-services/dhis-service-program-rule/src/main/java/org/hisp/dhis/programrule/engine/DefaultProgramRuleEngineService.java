package org.hisp.dhis.programrule.engine;

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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Created by zubair@dhis2.org on 23.10.17.
 */

@Slf4j
@Service( "org.hisp.dhis.programrule.engine.ProgramRuleEngineService" )
public class DefaultProgramRuleEngineService implements ProgramRuleEngineService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramRuleEngine programRuleEngine;

    private final List<RuleActionImplementer> ruleActionImplementers;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageInstanceService programStageInstanceService;

    public DefaultProgramRuleEngineService( @Qualifier( "oldRuleEngine" ) ProgramRuleEngine programRuleEngine,
        List<RuleActionImplementer> ruleActionImplementers, ProgramInstanceService programInstanceService,
        ProgramStageInstanceService programStageInstanceService )
    {
        checkNotNull( programRuleEngine );
        checkNotNull( ruleActionImplementers );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );

        this.programRuleEngine = programRuleEngine;
        this.ruleActionImplementers = ruleActionImplementers;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
    }

    @Override
    public List<RuleEffect> evaluateEnrollmentAndRunEffects( long programInstanceId )
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programInstanceId );
        List<RuleEffect> ruleEffects = getRuleEffects( programInstance );

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i -> {
                log.debug( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, programInstance );
            } );
        }
        return ruleEffects;
    }

    @Override
    public List<RuleEffect> evaluateEnrollment( String programInstanceUid )
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programInstanceUid );
        return getRuleEffects( programInstance );
    }

    private List<RuleEffect> getRuleEffects( ProgramInstance programInstance )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        try
        {
            ruleEffects = programRuleEngine.evaluateEnrollment( programInstance );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            log.error( DebugUtils.getStackTrace( ex.getCause() ) );
        }

        return ruleEffects;
    }

    @Override
    public List<RuleEffect> evaluateEventAndRunEffects( long programStageInstanceId )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( programStageInstanceId );
        List<RuleEffect> ruleEffects = getRuleEffects( psi );

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i -> {
                log.debug( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, psi );
            } );
        }

        return ruleEffects;
    }

    @Override
    public List<RuleEffect> evaluateEvent( String programStageInstanceUid )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( programStageInstanceUid );
        return getRuleEffects( psi );
    }

    private List<RuleEffect> getRuleEffects( ProgramStageInstance psi )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        try
        {
            ruleEffects = programRuleEngine.evaluateEvent( psi );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            log.error( DebugUtils.getStackTrace( ex.getCause() ) );
        }

        return ruleEffects;
    }

}
