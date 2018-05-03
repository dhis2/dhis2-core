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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.models.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by zubair@dhis2.org on 19.10.17.
 */
public class DefaultProgramRuleEntityMapperService implements ProgramRuleEntityMapperService
{
    private static final Log log = LogFactory.getLog( DefaultProgramRuleEntityMapperService.class );

    private static final String LOCATION_FEEDBACK = "feedback";

    private static final String LOCATION_INDICATOR = "indicators";

    private final ImmutableMap<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>> ACTION_MAPPER =
        new ImmutableMap.Builder<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>>()
        .put( ProgramRuleActionType.ASSIGN, pra -> RuleActionAssign.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.CREATEEVENT, pra -> RuleActionCreateEvent.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.DISPLAYKEYVALUEPAIR, this::getLocationBasedDisplayRuleAction )
        .put( ProgramRuleActionType.DISPLAYTEXT, this::getLocationBasedDisplayRuleAction )
        .put( ProgramRuleActionType.HIDEFIELD, pra -> RuleActionHideField.create( pra.getContent(), getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.HIDEPROGRAMSTAGE, pra -> RuleActionHideProgramStage.create( pra.getProgramStage().getUid() ) )
        .put( ProgramRuleActionType.HIDESECTION, pra -> RuleActionHideSection.create( pra.getProgramStageSection().getUid() ) )
        .put( ProgramRuleActionType.SHOWERROR, pra -> RuleActionShowError.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.SHOWWARNING, pra -> RuleActionShowWarning.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.SETMANDATORYFIELD, pra -> RuleActionSetMandatoryField.create( getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.WARNINGONCOMPLETE, pra -> RuleActionWarningOnCompletion.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.ERRORONCOMPLETE, pra -> RuleActionErrorOnCompletion.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) )
        .put( ProgramRuleActionType.SENDMESSAGE, pra -> RuleActionSendMessage.create( pra.getProgramNotificationTemplate().getUid(), pra.getData() ) )
        .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>> VARIABLE_MAPPER_MAPPER =
        new ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>>()
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, prv -> RuleVariableAttribute.create( prv.getName(), prv.getAttribute().getUid(), toMappedValueType( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, prv -> RuleVariableCurrentEvent.create( prv.getName(), prv.getDataElement().getUid(), toMappedValueType( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, prv -> RuleVariablePreviousEvent.create( prv.getName(), prv.getDataElement().getUid(), toMappedValueType( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, prv -> RuleVariableNewestEvent.create( prv.getName(), prv.getDataElement().getUid(), toMappedValueType( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, prv -> RuleVariableNewestStageEvent.create( prv.getName(),
            prv.getDataElement().getUid(), prv.getProgramStage().getUid() ,RuleValueType.TEXT ) )
        .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>> VALUE_TYPE_MAPPER = new
        ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>>()
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, prv -> prv.getAttribute().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, prv -> prv.getDataElement().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, prv -> prv.getDataElement().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, prv -> prv.getDataElement().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, prv -> prv.getDataElement().getValueType()  )
        .build();
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;


    @Override
    public List<Rule> toMappedProgramRules()
    {
        List<ProgramRule> programRules = programRuleService.getAllProgramRule();

        return toMappedProgramRules( programRules );
    }

    @Override
    public List<Rule> toMappedProgramRules( Program program )
    {
        List<ProgramRule> programRules = programRuleService.getProgramRule( program );

        return toMappedProgramRules( programRules );
    }

    @Override
    public List<Rule> toMappedProgramRules( List<ProgramRule> programRules )
    {
        return programRules.stream().map( this::toRule ).collect( Collectors.toList() );
    }

    @Override
    public List<RuleVariable> toMappedProgramRuleVariables()
    {
        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getAllProgramRuleVariable();

        return toMappedProgramRuleVariables( programRuleVariables );
    }

    @Override
    public List<RuleVariable> toMappedProgramRuleVariables( Program program )
    {
        List<ProgramRuleVariable> programRuleVariables = programRuleVariableService.getProgramRuleVariable( program );

        return toMappedProgramRuleVariables( programRuleVariables );
    }

    @Override
    public List<RuleVariable> toMappedProgramRuleVariables( List<ProgramRuleVariable> programRuleVariables )
    {
        return programRuleVariables.stream().map( this::toRuleVariable ).collect( Collectors.toList() );
    }

    @Override
    public RuleEnrollment toMappedRuleEnrollment( ProgramInstance enrollment )
    {
        return RuleEnrollment.create( enrollment.getUid(), enrollment.getIncidentDate(),
            enrollment.getEnrollmentDate(), RuleEnrollment.Status.valueOf( enrollment.getStatus().toString() ), enrollment.getEntityInstance().getTrackedEntityAttributeValues().stream()
            .map( attr -> RuleAttributeValue.create( attr.getAttribute().getUid(), attr.getValue() ) )
            .collect( Collectors.toList() ) );
    }

    @Override
    public List<RuleEvent> toMappedRuleEvents ( Set<ProgramStageInstance> programStageInstances, ProgramStageInstance psiToEvaluate )
    {
        return programStageInstances.stream()
            .filter( psi -> !psi.getUid().equals( psiToEvaluate.getUid() ) )
            .map( ps -> RuleEvent.create( ps.getUid(), ps.getProgramStage().getUid(),
             RuleEvent.Status.valueOf( ps.getStatus().toString() ), ps.getExecutionDate() != null ? ps.getExecutionDate() : new Date(), ps.getDueDate(), ps.getDataValues().stream()
                .map(dv -> RuleDataValue.create( dv.getCreated(), dv.getProgramStageInstance().getUid(), dv.getDataElement().getUid(), dv.getValue() ) )
                .collect( Collectors.toList() ) ) )
                .collect( Collectors.toList() );
    }

    @Override
    public RuleEvent toMappedRuleEvent( ProgramStageInstance psi )
    {
        return RuleEvent.create( psi.getProgramInstance().getUid(), psi.getUid(), RuleEvent.Status.valueOf( psi.getStatus().toString() ), psi.getExecutionDate(),
         psi.getDueDate(), psi.getDataValues().stream()
            .map(dv -> RuleDataValue.create( dv.getCreated(), psi.getUid(), dv.getDataElement().getUid(), dv.getValue() ) )
            .collect( Collectors.toList() ) );
    }

    @Override
    public List<RuleEvent> toMappedRuleEvents( Set<ProgramStageInstance> programStageInstances )
    {
        return programStageInstances.stream()
            .map( ps -> RuleEvent.create( ps.getUid(), ps.getProgramStage().getUid(),
            RuleEvent.Status.valueOf( ps.getStatus().toString() ), ps.getExecutionDate() != null ? ps.getExecutionDate() : new Date(), ps.getDueDate(), ps.getDataValues().stream()
                .map(dv -> RuleDataValue.create( dv.getCreated(), dv.getProgramStageInstance().getUid(), dv.getDataElement().getUid(), dv.getValue() ) )
                .collect( Collectors.toList() ) ) )
                .collect( Collectors.toList() );
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private Rule toRule( ProgramRule programRule )
    {
        Set<ProgramRuleAction> programRuleActions = programRule.getProgramRuleActions();

        List<RuleAction> ruleActions = programRuleActions.stream().map( this::toRuleAction ).collect( Collectors.toList() );

        return Rule.create( programRule.getProgramStage() != null ? programRule.getProgramStage().getUid() : StringUtils.EMPTY, programRule.getPriority(), programRule.getCondition(), ruleActions );
    }

    private RuleAction toRuleAction( ProgramRuleAction programRuleAction )
    {
        return ACTION_MAPPER.getOrDefault( programRuleAction.getProgramRuleActionType(), pra ->
            RuleActionAssign.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) ).apply( programRuleAction );
    }

    private RuleVariable toRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        return VARIABLE_MAPPER_MAPPER.get( programRuleVariable.getSourceType() ).apply( programRuleVariable );
    }

    private RuleValueType toMappedValueType( ProgramRuleVariable programRuleVariable )
    {
        ValueType valueType = VALUE_TYPE_MAPPER.getOrDefault( programRuleVariable.getSourceType(), prv -> ValueType.TEXT ).apply( programRuleVariable );

        if ( valueType.isBoolean() )
        {
            return RuleValueType.BOOLEAN;
        }

        if ( valueType.isText() )
        {
            return RuleValueType.TEXT;
        }

        if ( valueType.isNumeric() )
        {
            return RuleValueType.NUMERIC;
        }

        return RuleValueType.TEXT;
    }

    private String getAssignedParameter( ProgramRuleAction programRuleAction )
    {
        if ( programRuleAction.hasDataElement() )
        {
            return programRuleAction.getDataElement().getUid();
        }

        if ( programRuleAction.hasTrackedEntityAttribute() )
        {
            return programRuleAction.getAttribute().getUid();
        }

        if ( programRuleAction.hasContent() )
        {
            return programRuleAction.getContent();
        }

        log.warn( String.format( "No location found for ProgramRuleAction: %s", programRuleAction.getUid() ) );

        return StringUtils.EMPTY;
    }

    private RuleAction getLocationBasedDisplayRuleAction( ProgramRuleAction programRuleAction )
    {
        if ( ProgramRuleActionType.DISPLAYTEXT.equals( programRuleAction.getProgramRuleActionType() ) )
        {
            if ( LOCATION_FEEDBACK.equals( programRuleAction.getLocation() ) )
            {
                return RuleActionDisplayText.createForFeedback( programRuleAction.getContent(), programRuleAction.getData() );
            }

            if ( LOCATION_INDICATOR.equals( programRuleAction.getLocation() ) )
            {
                return RuleActionDisplayText.createForIndicators( programRuleAction.getContent(), programRuleAction.getData() );
            }

            return RuleActionDisplayText.createForFeedback( programRuleAction.getContent(), programRuleAction.getData() );
        }
        else
        {
            if ( LOCATION_FEEDBACK.equals( programRuleAction.getLocation() ) )
            {
                return RuleActionDisplayKeyValuePair.createForFeedback( programRuleAction.getContent(), programRuleAction.getData() );
            }

            if ( LOCATION_INDICATOR.equals( programRuleAction.getLocation() ) )
            {
                return RuleActionDisplayKeyValuePair.createForIndicators( programRuleAction.getContent(), programRuleAction.getData() );
            }

            return RuleActionDisplayKeyValuePair.createForFeedback( programRuleAction.getContent(), programRuleAction.getData() );
        }
    }
}
