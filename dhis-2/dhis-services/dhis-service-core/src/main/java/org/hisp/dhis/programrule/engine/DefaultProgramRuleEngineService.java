package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zubair@dhis2.org on 23.10.17.
 */
public class DefaultProgramRuleEngineService implements ProgramRuleEngineService
{
    private static final Log log = LogFactory.getLog( DefaultProgramRuleEngineService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramRuleEngine programRuleEngine;

    @Autowired
    private List<RuleActionImplementer> ruleActionImplementers;

    @Override
    public List<RuleAction> evaluate( ProgramInstance programInstance )
    {
        if ( !containsImplementableActions( programInstance ) )
        {
            return new ArrayList<>();
        }

        log.info( "RuleEngine triggered" );

        List<RuleEffect> ruleEffects = new ArrayList<>();

        try
        {
            ruleEffects = programRuleEngine.evaluateEnrollment( programInstance );
        }
        catch( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            log.error( DebugUtils.getStackTrace( ex.getCause() ) );
        }

        List<RuleAction> ruleActions = ruleEffects.stream().map( RuleEffect::ruleAction ).collect( Collectors.toList() );

        for ( RuleAction action : ruleActions )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( action ) ).forEach( i ->
            {
                log.info( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( action, programInstance );
            } );
        }

        return ruleActions;
    }

    @Override
    public List<RuleAction> evaluate( ProgramStageInstance programStageInstance )
    {
        if ( !containsImplementableActions( programStageInstance.getProgramInstance() ) )
        {
            return new ArrayList<>();
        }

        log.info( "RuleEngine triggered" );

        List<RuleEffect> ruleEffects = new ArrayList<>();

        try
        {
            ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );
        }
        catch( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            log.error( DebugUtils.getStackTrace( ex.getCause() ) );
        }

        List<RuleAction> ruleActions = ruleEffects.stream().map( RuleEffect::ruleAction ).collect( Collectors.toList() );

        for ( RuleAction action : ruleActions )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( action ) ).forEach( i ->
            {
                log.info( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( action, programStageInstance );
            } );
        }

        return ruleActions;
    }

    private boolean containsImplementableActions( ProgramInstance programInstance )
    {
        if ( programInstance == null )
        {
            return false;
        }

        Program program = programInstance.getProgram();

        Set<ProgramRule> programRules = program.getProgramRules();

        List<ProgramRuleAction> programRuleActions = programRules.stream().map( ProgramRule::getProgramRuleActions )
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );

        for ( ProgramRuleAction action : programRuleActions )
        {
            if ( action.getProgramRuleActionType().isImplementable() )
            {
                return true;
            }
        }

        return false;
    }
}
