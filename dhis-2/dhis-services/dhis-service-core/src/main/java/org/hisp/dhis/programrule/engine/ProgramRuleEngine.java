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
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.RuleEngineContext;
import org.hisp.dhis.rules.models.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
public class ProgramRuleEngine
{
    private static final Log log = LogFactory.getLog( ProgramRuleEngine.class );

    @Autowired
    private ProgramRuleEntityMapperService programRuleEntityMapperService;

    @Autowired
    private ProgramRuleExpressionEvaluator programRuleExpressionEvaluator;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    public List<RuleEffect> evaluateEnrollment( ProgramInstance enrollment )
    {
        if ( enrollment == null )
        {
            return new ArrayList<>();
        }

        List<RuleEffect> ruleEffects = new ArrayList<>();

        List<ProgramRule> programRules = programRuleService.getProgramRule( enrollment.getProgram() );

        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( enrollment.getProgram() );

        RuleEnrollment ruleEnrollment = programRuleEntityMapperService.toMappedRuleEnrollment( enrollment );

        List<RuleEvent> ruleEvents = programRuleEntityMapperService.toMappedRuleEvents( enrollment.getProgramStageInstances() );

        RuleEngine ruleEngine = ruleEngineBuilder( programRules, programRuleVariables ).events( ruleEvents ).build();

        try
        {
            ruleEffects = ruleEngine.evaluate( ruleEnrollment  ).call();

            ruleEffects.stream().map( RuleEffect::ruleAction )
                .forEach( action -> log.info( String.format( "RuleEngine triggered with result: %s", action.toString() ) ) );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
            log.error( DebugUtils.getStackTrace( e.getCause() ) );
        }

        return ruleEffects;
    }

    public List<RuleEffect> evaluateEvent( ProgramStageInstance programStageInstance )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        if ( programStageInstance == null )
        {
            return ruleEffects;
        }

        ProgramInstance enrollment = programStageInstance.getProgramInstance();

        List<ProgramRule> programRules = programRuleService.getProgramRule( enrollment.getProgram() );

        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( enrollment.getProgram() );

        RuleEnrollment ruleEnrollment = programRuleEntityMapperService.toMappedRuleEnrollment( enrollment );

        List<RuleEvent> ruleEvents = programRuleEntityMapperService.toMappedRuleEvents( enrollment.getProgramStageInstances(), programStageInstance );

        RuleEngine ruleEngine = ruleEngineBuilder( programRules, programRuleVariables ).enrollment( ruleEnrollment ).events( ruleEvents ).build();

        try
        {
            ruleEffects = ruleEngine.evaluate( programRuleEntityMapperService.toMappedRuleEvent( programStageInstance )  ).call();

            ruleEffects.stream().map( RuleEffect::ruleAction )
                .forEach( action -> log.info( String.format( "RuleEngine triggered with result: %s", action.toString() ) ) );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
            log.error( DebugUtils.getStackTrace( e.getCause() ) );
        }

        return ruleEffects;
    }

    private RuleEngine.Builder ruleEngineBuilder( List<ProgramRule> programRules, List<ProgramRuleVariable> programRuleVariables )
    {
        return RuleEngineContext
            .builder( programRuleExpressionEvaluator )
            .rules( programRuleEntityMapperService.toMappedProgramRules( programRules ) )
            .ruleVariables( programRuleEntityMapperService.toMappedProgramRuleVariables( programRuleVariables ) )
            .build().toEngineBuilder();
    }
}
