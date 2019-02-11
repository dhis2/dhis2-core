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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

/**
 * Created by zubair@dhis2.org on 19.10.17.
 */

public class DefaultProgramRuleEntityMapperService
    implements ProgramRuleEntityMapperService
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
        .put( ProgramRuleActionType.SENDMESSAGE, pra -> RuleActionSendMessage.create( pra.getTemplateUid(), pra.getData() ) )
        .put( ProgramRuleActionType.SCHEDULEMESSAGE, pra -> RuleActionScheduleMessage.create( pra.getTemplateUid(), pra.getData() ) )
            .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>> VARIABLE_MAPPER_MAPPER =
        new ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>>()
            .put( ProgramRuleVariableSourceType.CALCULATED_VALUE, prv -> RuleVariableCalculatedValue.create( prv.getName(), "", RuleValueType.TEXT ) )
            .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, prv -> RuleVariableAttribute.create( prv.getName(), prv.getAttribute().getUid(), toMappedValueType( prv ) ) )
            .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, prv -> RuleVariableCurrentEvent.create( prv.getName(), prv.getDataElement().getUid(), toMappedValueType( prv ) ) )
            .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, prv -> RuleVariablePreviousEvent.create( prv.getName(), prv.getDataElement().getUid(), toMappedValueType( prv ) ) )
            .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, prv -> RuleVariableNewestEvent.create( prv.getName(), prv.getDataElement().getUid(), toMappedValueType( prv ) ) )
            .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, prv -> RuleVariableNewestStageEvent.create( prv.getName(),
                prv.getDataElement().getUid(), prv.getProgramStage().getUid() , toMappedValueType( prv ) ) )
            .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>> VALUE_TYPE_MAPPER = new
        ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>>()
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, prv -> prv.getAttribute().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, prv -> prv.getDataElement().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, prv -> prv.getDataElement().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, prv -> prv.getDataElement().getValueType()  )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, prv -> prv.getDataElement().getValueType()  )
        .build();

    private final CachingMap<String, ValueType> dataElementToValueTypeCache = new CachingMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private DataElementService dataElementService;


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
        return programRules.stream().map( this::toRule ).filter( Objects::nonNull ).collect( Collectors.toList() );
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
        return programRuleVariables.stream().map( this::toRuleVariable ).filter( Objects::nonNull ).collect( Collectors.toList() );
    }

    @Override
    public Rule toMappedProgramRule( ProgramRule programRule )
    {
        return toRule( programRule );
    }

    @Override
    public RuleEnrollment toMappedRuleEnrollment( ProgramInstance enrollment )
    {
        if( enrollment == null )
        {
            return null;
        }

        return RuleEnrollment.create( enrollment.getUid(), enrollment.getIncidentDate(),
            enrollment.getEnrollmentDate(), RuleEnrollment.Status.valueOf( enrollment.getStatus().toString() ), enrollment.getOrganisationUnit() != null ? enrollment.getOrganisationUnit().getUid() : "",
            enrollment.getEntityInstance().getTrackedEntityAttributeValues().stream().filter( Objects::nonNull )
                .map( attr -> RuleAttributeValue.create( attr.getAttribute().getUid(), getTrackedEntityAttributeValue( attr ) ) )
                .collect( Collectors.toList() ), enrollment.getProgram().getName() );
    }

    @Override
    public List<RuleEvent> toMappedRuleEvents ( Set<ProgramStageInstance> programStageInstances, ProgramStageInstance psiToEvaluate )
    {
        return programStageInstances.stream().filter( Objects::nonNull )
            .filter( psi -> !psi.getUid().equals( psiToEvaluate.getUid() ) )
            .map( psi -> RuleEvent.create( psi.getUid(), psi.getProgramStage().getUid(),
                RuleEvent.Status.valueOf( psi.getStatus().toString() ), psi.getExecutionDate() != null ? psi.getExecutionDate() : psi.getDueDate(), psi.getDueDate(), psi.getOrganisationUnit() != null ? psi.getOrganisationUnit().getUid() : "",
                    psi.getEventDataValues().stream().filter( Objects::nonNull )
                    .map( dv -> RuleDataValue.create( psi.getExecutionDate() != null ? psi.getExecutionDate() : psi.getDueDate(), psi.getProgramStage().getUid(), dv.getDataElement(), getEventDataValue( dv ) ) )
                    .collect( Collectors.toList() ), psi.getProgramStage().getName() ) ).collect( Collectors.toList() );
    }

    @Override
    public RuleEvent toMappedRuleEvent( ProgramStageInstance psi )
    {
        if ( psi == null )
        {
            return null;
        }

        return RuleEvent.create( psi.getUid(), psi.getProgramStage().getUid(), RuleEvent.Status.valueOf( psi.getStatus().toString() ), psi.getExecutionDate() != null ? psi.getExecutionDate() : psi.getDueDate(),
            psi.getDueDate(), psi.getOrganisationUnit() != null ? psi.getOrganisationUnit().getUid() : "", psi.getEventDataValues().stream().filter( Objects::nonNull )
                .map( dv -> RuleDataValue.create( psi.getExecutionDate() != null ? psi.getExecutionDate() : psi.getDueDate(), psi.getProgramStage().getUid(), dv.getDataElement(), getEventDataValue( dv ) ) )
                .collect( Collectors.toList() ), psi.getProgramStage().getName() );
    }

    @Override
    public List<RuleEvent> toMappedRuleEvents( Set<ProgramStageInstance> programStageInstances )
    {
        return programStageInstances.stream().filter( Objects::nonNull )
            .map( psi -> RuleEvent.create( psi.getUid(), psi.getProgramStage().getUid(),
                RuleEvent.Status.valueOf( psi.getStatus().toString() ), psi.getExecutionDate() != null ? psi.getExecutionDate() : psi.getDueDate(), psi.getDueDate(), psi.getOrganisationUnit() != null ? psi.getOrganisationUnit().getUid() : "",
                psi.getEventDataValues().stream().filter( Objects::nonNull )
                    .map( dv -> RuleDataValue.create( psi.getExecutionDate() != null ? psi.getExecutionDate() : psi.getDueDate(), psi.getProgramStage().getUid(), dv.getDataElement(), getEventDataValue( dv ) ) )
                    .collect( Collectors.toList() ), psi.getProgramStage().getName() ) ).collect( Collectors.toList() );
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private Rule toRule( ProgramRule programRule )
    {
        if ( programRule ==  null )
        {
            return null;
        }

        Set<ProgramRuleAction> programRuleActions = programRule.getProgramRuleActions();

        List<RuleAction> ruleActions;

        Rule rule;
        try
        {
            ruleActions = programRuleActions.stream().map( this::toRuleAction ).collect( Collectors.toList() );

            rule = Rule.create( programRule.getProgramStage() != null ? programRule.getProgramStage().getUid() : StringUtils.EMPTY, programRule.getPriority(), programRule.getCondition(), ruleActions, programRule.getName() );
        }
        catch ( Exception e )
        {
            log.debug( "Invalid rule action" );

            return null;
        }

        return rule;
    }

    private RuleAction toRuleAction( ProgramRuleAction programRuleAction )
    {
        return ACTION_MAPPER.getOrDefault( programRuleAction.getProgramRuleActionType(), pra ->
            RuleActionAssign.create( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) ).apply( programRuleAction );
    }

    private RuleVariable toRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        RuleVariable ruleVariable = null;

        try
        {
            ruleVariable = VARIABLE_MAPPER_MAPPER.get( programRuleVariable.getSourceType() ).apply( programRuleVariable );
        }
        catch ( Exception e )
        {
            log.debug( "Invalid rule variable" );
        }

        return ruleVariable;
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

    private String getTrackedEntityAttributeValue( TrackedEntityAttributeValue attributeValue )
    {
        ValueType valueType = attributeValue.getAttribute().getValueType();

        if ( valueType.isBoolean() )
        {
            return attributeValue.getValue() != null ? attributeValue.getValue() : "false";
        }

        if ( valueType.isNumeric() )
        {
            return attributeValue.getValue() != null ? attributeValue.getValue() : "0";
        }

        return attributeValue.getValue() != null ? attributeValue.getValue() : "";
    }

    private String getEventDataValue( EventDataValue dataValue )
    {
        ValueType valueType = getValueTypeForDataElement( dataValue.getDataElement() );

        if ( valueType.isBoolean() )
        {
            return dataValue.getValue() != null ? dataValue.getValue() : "false";
        }

        if ( valueType.isNumeric() )
        {
            return dataValue.getValue() != null ? dataValue.getValue() : "0";
        }

        return dataValue.getValue() != null ? dataValue.getValue() : "";
    }

    private ValueType getValueTypeForDataElement( String dataElementUid ) {
        return dataElementToValueTypeCache.get( dataElementUid, () -> {
            DataElement dataElement = dataElementService.getDataElement( dataElementUid );

            if ( dataElement == null ) {
                log.error( "DataElement " + dataElementUid + " was not found." );
                throw new IllegalStateException( "Required DataElement(" + dataElementUid + ") was not found." );
            }

            return dataElement.getValueType();
        } );
    }
}
