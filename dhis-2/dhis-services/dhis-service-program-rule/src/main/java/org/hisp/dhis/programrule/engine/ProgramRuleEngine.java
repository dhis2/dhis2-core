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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
@Transactional( readOnly = true )
@Component( "org.hisp.dhis.programrule.engine.ProgramRuleEngine" )
public class ProgramRuleEngine
{
    private static final Log log = LogFactory.getLog( ProgramRuleEngine.class );

    private static final String USER = "USER";

    private static final String REGEX = "d2:inOrgUnitGroup\\( *(([\\d/\\*\\+\\-%\\. ]+)|" +
        "( *'[^']*'))*( *, *(([\\d/\\*\\+\\-%\\. ]+)|'[^']*'))* *\\)";

    private static final Pattern PATTERN = Pattern.compile( REGEX );

    private final ProgramRuleEntityMapperService programRuleEntityMapperService;

    private final ProgramRuleExpressionEvaluator programRuleExpressionEvaluator;

    private final ProgramRuleService programRuleService;

    private final ProgramRuleVariableService programRuleVariableService;

    private final OrganisationUnitGroupService organisationUnitGroupService;

    private final RuleVariableInMemoryMap inMemoryMap;

    private final CurrentUserService currentUserService;

    private final ConstantService constantService;

    public ProgramRuleEngine( ProgramRuleEntityMapperService programRuleEntityMapperService,
        ProgramRuleExpressionEvaluator programRuleExpressionEvaluator, ProgramRuleService programRuleService,
        ProgramRuleVariableService programRuleVariableService,
        OrganisationUnitGroupService organisationUnitGroupService, RuleVariableInMemoryMap inMemoryMap,
        CurrentUserService currentUserService, ConstantService constantService )
    {

        checkNotNull( programRuleEntityMapperService );
        checkNotNull( programRuleExpressionEvaluator );
        checkNotNull( programRuleService );
        checkNotNull( programRuleVariableService );
        checkNotNull( organisationUnitGroupService );
        checkNotNull( currentUserService );
        checkNotNull( inMemoryMap );
        checkNotNull( constantService );

        this.programRuleEntityMapperService = programRuleEntityMapperService;
        this.programRuleExpressionEvaluator = programRuleExpressionEvaluator;
        this.programRuleService = programRuleService;
        this.programRuleVariableService = programRuleVariableService;
        this.organisationUnitGroupService = organisationUnitGroupService;
        this.inMemoryMap = inMemoryMap;
        this.currentUserService = currentUserService;
        this.constantService = constantService;
    }

    public List<RuleEffect> evaluateEnrollment(ProgramInstance enrollment )
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
                .forEach( action -> log.debug( String.format( "RuleEngine triggered with result: %s", action.toString() ) ) );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
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
                .forEach( action -> log.debug( String.format( "RuleEngine triggered with result: %s", action.toString() ) ) );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
        }

        return ruleEffects;
    }

    private RuleEngine.Builder ruleEngineBuilder( List<ProgramRule> programRules, List<ProgramRuleVariable> programRuleVariables )
    {
        Map<String, List<String>> supplementaryData = new HashMap<>();

        Map<String, String> constantMap = constantService.getConstantMap().entrySet().stream()
            .collect( Collectors.toMap( Map.Entry::getKey, v -> v.getValue().toString() ) );

        List<String> orgUnitGroups = new ArrayList<>();

        List<Rule> rules = new ArrayList<>();

        for ( ProgramRule programRule : programRules )
        {
            Rule rule = programRuleEntityMapperService.toMappedProgramRule( programRule );

            if ( rule != null )
            {
                rules.add( rule );

                Matcher matcher = PATTERN.matcher( StringUtils.defaultIfBlank( programRule.getCondition(), "" ) );

                while ( matcher.find() )
                {
                    orgUnitGroups.add( StringUtils.replace( matcher.group( 1 ), "'", "" ) );
                }
            }
        }

        if ( !orgUnitGroups.isEmpty() )
        {
            supplementaryData = orgUnitGroups.stream().collect( Collectors.toMap( g -> g,  g -> organisationUnitGroupService.getOrganisationUnitGroup( g ).getMembers()
                .stream().map( OrganisationUnit::getUid ).collect( Collectors.toList() ) ) );
        }

        if ( currentUserService.getCurrentUser() != null )
        {
            supplementaryData.put( USER, currentUserService.getCurrentUser().getUserCredentials().getUserAuthorityGroups().stream().map( UserAuthorityGroup::getUid ).collect( Collectors.toList() ) );
        }

        return RuleEngineContext
            .builder( programRuleExpressionEvaluator )
            .supplementaryData( supplementaryData )
            .calculatedValueMap( inMemoryMap.getVariablesMap() )
            .rules( rules )
            .ruleVariables( programRuleEntityMapperService.toMappedProgramRuleVariables( programRuleVariables ) )
            .constantsValue( constantMap )
            .build().toEngineBuilder().triggerEnvironment( TriggerEnvironment.SERVER );
    }

    private List<ProgramRule> getImplementableRules( Program program )
    {
        List<ProgramRule> permittedRules;

        permittedRules = programRuleService.getImplementableProgramRules( program, ProgramRuleActionType.getNotificationLinkedTypes() );

        if ( permittedRules.isEmpty() )
        {
            return permittedRules;
        }

        return programRuleService.getImplementableProgramRules( program, ProgramRuleActionType.getImplementedActions() );
    }
}
