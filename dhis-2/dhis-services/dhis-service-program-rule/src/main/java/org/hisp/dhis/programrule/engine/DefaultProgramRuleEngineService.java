/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
<<<<<<< HEAD

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
=======
package org.hisp.dhis.programrule.engine;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.SYSTEM_PROGRAM_RULE_SERVER_EXECUTION;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

import com.google.common.collect.Lists;

/**
 * Created by zubair@dhis2.org on 23.10.17.
 */

@Slf4j
@Service( "org.hisp.dhis.programrule.engine.ProgramRuleEngineService" )
<<<<<<< HEAD
public class DefaultProgramRuleEngineService implements ProgramRuleEngineService
=======
public class DefaultProgramRuleEngineService
    implements ProgramRuleEngineService
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramRuleEngine programRuleEngine;

    private final List<RuleActionImplementer> ruleActionImplementers;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageInstanceService programStageInstanceService;

<<<<<<< HEAD
    public DefaultProgramRuleEngineService( @Qualifier( "oldRuleEngine" ) ProgramRuleEngine programRuleEngine,
=======
    private final ProgramService programService;

    private final ProgramRuleService programRuleService;

    private final DhisConfigurationProvider config;

    public DefaultProgramRuleEngineService(
        @Qualifier( "notificationRuleEngine" ) ProgramRuleEngine programRuleEngine,
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        List<RuleActionImplementer> ruleActionImplementers, ProgramInstanceService programInstanceService,
        ProgramStageInstanceService programStageInstanceService, ProgramRuleService programRuleService,
        ProgramService programService, DhisConfigurationProvider config )
    {
        checkNotNull( programRuleEngine );
        checkNotNull( ruleActionImplementers );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );
        checkNotNull( programRuleService );
        checkNotNull( programService );
        checkNotNull( config );

        this.programRuleEngine = programRuleEngine;
        this.ruleActionImplementers = ruleActionImplementers;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
        this.programRuleService = programRuleService;
        this.programService = programService;
        this.config = config;
    }

    @Override
<<<<<<< HEAD
    public List<RuleEffect> evaluateEnrollmentAndRunEffects( long programInstanceId )
=======
    @Transactional
    public List<RuleEffect> evaluateEnrollmentAndRunEffects( long enrollment )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
<<<<<<< HEAD
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programInstanceId );

        if ( programInstance == null )
        {
            return Lists.newArrayList();
        }

        List<RuleEffect> ruleEffects = getRuleEffects( programInstance, Optional.empty(),
            programInstance.getProgramStageInstances() );
=======
        if ( config.isDisabled( SYSTEM_PROGRAM_RULE_SERVER_EXECUTION ) )
        {
            return Lists.newArrayList();
        }

        ProgramInstance programInstance = programInstanceService.getProgramInstance( enrollment );

        if ( programInstance == null )
        {
            return Lists.newArrayList();
        }
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance,
            programInstance.getProgramStageInstances() );

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
<<<<<<< HEAD
    public List<RuleEffect> evaluateEventAndRunEffects( long programStageInstanceId )
=======
    @Transactional
    public List<RuleEffect> evaluateEventAndRunEffects( String event )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
<<<<<<< HEAD
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( programStageInstanceId );

        if ( psi == null )
        {
            return Lists.newArrayList();
        }

        List<RuleEffect> ruleEffects = getRuleEffects( psi.getProgramInstance(), Optional.of( psi ),
            psi.getProgramInstance().getProgramStageInstances() );
=======
        if ( config.isDisabled( SYSTEM_PROGRAM_RULE_SERVER_EXECUTION ) )
        {
            return Lists.newArrayList();
        }

        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( event );

        return evaluateEventAndRunEffects( psi );
    }

    @Override
    public RuleValidationResult getDescription( String condition, String programId )
    {
        Program program = programService.getProgram( programId );

        return programRuleEngine.getDescription( condition, program );
    }

    private List<RuleEffect> evaluateEventAndRunEffects( ProgramStageInstance psi )
    {
        if ( psi == null )
        {
            return Lists.newArrayList();
        }
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        ProgramInstance programInstance = programInstanceService.getProgramInstance( psi.getProgramInstance().getId() );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance, psi,
            programInstance.getProgramStageInstances() );

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i -> {
                log.debug( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, psi );
            } );
        }

        return ruleEffects;
    }

    private List<RuleEffect> getRuleEffects( ProgramInstance enrollment, Optional<ProgramStageInstance> event,
        Set<ProgramStageInstance> events )
    {
        return programRuleEngine.evaluate( enrollment, event, events );
    }
}
