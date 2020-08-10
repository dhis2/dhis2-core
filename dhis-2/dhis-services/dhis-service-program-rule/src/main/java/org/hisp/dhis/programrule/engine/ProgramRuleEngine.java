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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.DataItem;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.RuleEngineContext;
import org.hisp.dhis.rules.RuleEngineIntent;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserAuthorityGroup;

import com.google.api.client.util.Lists;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
@Slf4j
public class ProgramRuleEngine
{
    private static final String USER = "USER";

    private final ProgramRuleEntityMapperService programRuleEntityMapperService;

    private final ProgramRuleVariableService programRuleVariableService;

    private final OrganisationUnitGroupService organisationUnitGroupService;

    private final CurrentUserService currentUserService;

    private final ConstantService constantService;

    private final ImplementableRuleService implementableRuleService;

    public ProgramRuleEngine( ProgramRuleEntityMapperService programRuleEntityMapperService,
        ProgramRuleVariableService programRuleVariableService,
        OrganisationUnitGroupService organisationUnitGroupService,
        CurrentUserService currentUserService,
        ConstantService constantService,
        ImplementableRuleService implementableRuleService )
    {
        checkNotNull( programRuleEntityMapperService );
        checkNotNull( programRuleVariableService );
        checkNotNull( organisationUnitGroupService );
        checkNotNull( currentUserService );
        checkNotNull( constantService );
        checkNotNull( implementableRuleService );

        this.programRuleEntityMapperService = programRuleEntityMapperService;
        this.programRuleVariableService = programRuleVariableService;
        this.organisationUnitGroupService = organisationUnitGroupService;
        this.currentUserService = currentUserService;
        this.constantService = constantService;
        this.implementableRuleService = implementableRuleService;
    }

    public List<RuleEffect> evaluate( ProgramInstance enrollment, Set<ProgramStageInstance> events )
    {
        return evaluateProgramRules( enrollment, null, events );
    }

    public List<RuleEffect> evaluate( ProgramInstance enrollment, ProgramStageInstance programStageInstance,
        Set<ProgramStageInstance> events )
    {
        if ( programStageInstance == null )
        {
            return Lists.newArrayList();
        }
        return evaluateProgramRules( enrollment, programStageInstance, events );
    }

    private List<RuleEffect> evaluateProgramRules( ProgramInstance enrollment,
        ProgramStageInstance programStageInstance, Set<ProgramStageInstance> events )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        List<RuleEvent> ruleEvents = getRuleEvents( events, programStageInstance );

        RuleEnrollment ruleEnrollment = getRuleEnrollment( enrollment );

        try
        {
            RuleEngine ruleEngine = getRuleEngineContext( enrollment.getProgram() )
                .toEngineBuilder()
                .triggerEnvironment( TriggerEnvironment.SERVER )
                .events( ruleEvents )
                .enrollment( ruleEnrollment )
                .build();

            ruleEffects = getRuleEngineEvaluation( ruleEngine, ruleEnrollment,
                programStageInstance );

            ruleEffects
                .stream()
                .map( RuleEffect::ruleAction )
                .forEach(
                    action -> log.debug( String.format( "RuleEngine triggered with result: %s", action.toString() ) ) );
        }
        catch ( Exception e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
        }

        return ruleEffects;
    }

    /**
     * To getDescription rule condition in order to fetch its description
     * 
     * @param condition of program rule
     * @param programRule {@link ProgramRule} which the condition is associated
     *        with.
     * @return RuleValidationResult contains description of program rule condition
     *         or errorMessage
     */
    public RuleValidationResult getDescription( String condition, ProgramRule programRule )
    {
        if ( programRule == null )
        {
            log.error( "ProgramRule cannot be null" );
            return RuleValidationResult.builder().isValid( false ).errorMessage( "ProgramRule cannot be null" ).build();
        }

        Program program = programRule.getProgram();

        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( program );

        RuleEngine ruleEngine = ruleEngineBuilder( ListUtils.newList( programRule ), programRuleVariables,
            RuleEngineIntent.DESCRIPTION ).build();

        return ruleEngine.evaluate( condition );
    }

    private RuleEngineContext getRuleEngineContext( Program program )
    {
        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService
            .getProgramRuleVariable( program );
        List<ProgramRule> programRules = implementableRuleService
            .getImplementableRules( program );

        Map<String, String> constantMap = constantService.getConstantMap().entrySet()
            .stream()
            .collect( Collectors.toMap( Map.Entry::getKey, v -> v.getValue().toString() ) );

        Map<String, List<String>> supplementaryData = organisationUnitGroupService.getAllOrganisationUnitGroups()
            .stream()
            .collect( Collectors.toMap( BaseIdentifiableObject::getUid,
                g -> g.getMembers().stream().map( OrganisationUnit::getUid ).collect( Collectors.toList() ) ) );

        if ( currentUserService.getCurrentUser() != null )
        {
            supplementaryData.put( USER, currentUserService.getCurrentUser().getUserCredentials()
                .getUserAuthorityGroups().stream().map( UserAuthorityGroup::getUid ).collect( Collectors.toList() ) );
        }

        return RuleEngineContext.builder()
            .supplementaryData( supplementaryData )
            .rules( programRuleEntityMapperService.toMappedProgramRules( programRules ) )
            .ruleVariables( programRuleEntityMapperService.toMappedProgramRuleVariables( programRuleVariables ) )
            .constantsValue( constantMap )
            .build();
    }

    private RuleEngine.Builder ruleEngineBuilder( List<ProgramRule> programRules,
        List<ProgramRuleVariable> programRuleVariables, RuleEngineIntent intent )
    {
        Map<String, String> constantMap = constantService.getConstantMap().entrySet()
            .stream()
            .collect( Collectors.toMap( Map.Entry::getKey, v -> v.getValue().toString() ) );

        Map<String, List<String>> supplementaryData = organisationUnitGroupService.getAllOrganisationUnitGroups()
            .stream()
            .collect( Collectors.toMap( BaseIdentifiableObject::getUid,
                g -> g.getMembers().stream().map( OrganisationUnit::getUid ).collect( Collectors.toList() ) ) );

        if ( currentUserService.getCurrentUser() != null )
        {
            supplementaryData.put( USER, currentUserService.getCurrentUser().getUserCredentials()
                .getUserAuthorityGroups().stream().map( UserAuthorityGroup::getUid ).collect( Collectors.toList() ) );
        }
        if ( RuleEngineIntent.DESCRIPTION == intent )
        {
            Map<String, DataItem> itemStore = programRuleEntityMapperService.getItemStore( programRuleVariables );

            return RuleEngineContext.builder()
                .supplementaryData( supplementaryData )
                .rules( programRuleEntityMapperService.toMappedProgramRules( programRules ) )
                .ruleVariables( programRuleEntityMapperService.toMappedProgramRuleVariables( programRuleVariables ) )
                .constantsValue( constantMap ).ruleEngineItent( intent ).itemStore( itemStore )
                .build()
                .toEngineBuilder()
                .triggerEnvironment( TriggerEnvironment.SERVER );
        }
        else
        {
            return RuleEngineContext.builder()
                .supplementaryData( supplementaryData )
                .rules( programRuleEntityMapperService.toMappedProgramRules( programRules ) )
                .ruleVariables( programRuleEntityMapperService.toMappedProgramRuleVariables( programRuleVariables ) )
                .constantsValue( constantMap ).ruleEngineItent( intent )
                .build()
                .toEngineBuilder()
                .triggerEnvironment( TriggerEnvironment.SERVER );
        }
    }

    private RuleEvent getRuleEvent( ProgramStageInstance programStageInstance )
    {
        return programRuleEntityMapperService.toMappedRuleEvent( programStageInstance );
    }

    private List<RuleEvent> getRuleEvents( Set<ProgramStageInstance> events,
        ProgramStageInstance programStageInstance )
    {
        return programRuleEntityMapperService.toMappedRuleEvents( events, programStageInstance );
    }

    private RuleEnrollment getRuleEnrollment( ProgramInstance enrollment )
    {
        return programRuleEntityMapperService.toMappedRuleEnrollment( enrollment );
    }

    private List<RuleEffect> getRuleEngineEvaluation( RuleEngine ruleEngine, RuleEnrollment enrollment,
        ProgramStageInstance event )
        throws Exception
    {
        if ( event == null )
        {
            return ruleEngine.evaluate( enrollment ).call();
        }
        else
        {
            return ruleEngine.evaluate( getRuleEvent( event ) ).call();
        }
    }
}
