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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.RuleEngineContext;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
public class ProgramRuleEngine
{
    private static final Log log = LogFactory.getLog( ProgramRuleEngine.class );

    private static final String USER = "USER";

    @Autowired
    private ProgramRuleEntityMapperService programRuleEntityMapperService;

    @Autowired
    private ProgramRuleExpressionEvaluator programRuleExpressionEvaluator;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private RuleVariableInMemoryMap inMemoryMap;

    @Autowired
    private CurrentUserService currentUserService;

    public List<RuleEffect> evaluateEnrollment( ProgramInstance enrollment )
    {
        if ( enrollment == null )
        {
            return new ArrayList<>();
        }

        List<RuleEffect> ruleEffects = new ArrayList<>();
        
        List<ProgramRule> implementableProgramRules = getImplementableRules( enrollment.getProgram() );


        if ( implementableProgramRules.isEmpty() ) // if implementation does not exist on back end side
        {
            return ruleEffects;
        }

        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( enrollment.getProgram() );

        RuleEnrollment ruleEnrollment = programRuleEntityMapperService.toMappedRuleEnrollment( enrollment );

        List<RuleEvent> ruleEvents = programRuleEntityMapperService.toMappedRuleEvents( enrollment.getProgramStageInstances() );

        RuleEngine ruleEngine;

        try
        {
            ruleEngine = ruleEngineBuilder( implementableProgramRules, programRuleVariables ).events( ruleEvents ).build();

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

        List<ProgramRule> implementableProgramRules = getImplementableRules( enrollment.getProgram() );

        if ( implementableProgramRules.isEmpty() )
        {
            return ruleEffects;
        }

        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( enrollment.getProgram() );

        RuleEnrollment ruleEnrollment = programRuleEntityMapperService.toMappedRuleEnrollment( enrollment );

        List<RuleEvent> ruleEvents = programRuleEntityMapperService.toMappedRuleEvents( enrollment.getProgramStageInstances(), programStageInstance );

        RuleEngine ruleEngine;

        try
        {
            ruleEngine = ruleEngineBuilder( implementableProgramRules, programRuleVariables ).enrollment( ruleEnrollment ).events( ruleEvents ).build();

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
        Map<String, List<String>> supplementaryData = new HashMap<>();

        if( currentUserService.getCurrentUser() != null )
        {
            supplementaryData.put( USER, currentUserService.getCurrentUser().getUserCredentials().getUserAuthorityGroups().stream().map( UserAuthorityGroup::getUid ).collect( Collectors.toList() ) );
        }

        List<OrganisationUnitGroup> groups = organisationUnitGroupService.getAllOrganisationUnitGroupWithMembers();

        groups.forEach( group -> supplementaryData.put( group.getUid(), group.getMembers().stream().map( OrganisationUnit::getUid ).collect( Collectors.toList() ) ) );

        return RuleEngineContext
            .builder( programRuleExpressionEvaluator )
            .supplementaryData( supplementaryData )
            .calculatedValueMap( inMemoryMap.getVariablesMap() )
            .rules( programRuleEntityMapperService.toMappedProgramRules( programRules ) )
            .ruleVariables( programRuleEntityMapperService.toMappedProgramRuleVariables( programRuleVariables ) )
            .build().toEngineBuilder().triggerEnvironment( TriggerEnvironment.SERVER );
    }

    private List<ProgramRule> getImplementableRules( Program program )
    {
        List<ProgramRule> rules =  programRuleService.getProgramRule( program );

        return rules.stream().filter( this::isImplementable ).collect( Collectors.toList() );
    }

    private boolean isImplementable( ProgramRule rule )
    {
        Set<ProgramRuleAction> actions = rule.getProgramRuleActions();

        for( ProgramRuleAction action : actions )
        {
            if ( action.getProgramRuleActionType().isImplementable() )
            {
                return true;
            }
        }

        return false;
    }
}
