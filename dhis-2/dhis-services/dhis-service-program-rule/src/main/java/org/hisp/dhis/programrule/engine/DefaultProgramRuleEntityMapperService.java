/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.dhis2.ruleengine.models.AttributeType.DATA_ELEMENT;
import static org.dhis2.ruleengine.models.AttributeType.TRACKED_ENTITY_ATTRIBYTE;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import kotlinx.datetime.LocalDate;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.dhis2.ruleengine.DataItem;
import org.dhis2.ruleengine.ItemValueType;
import org.dhis2.ruleengine.models.AttributeType;
import org.dhis2.ruleengine.models.DisplayLocation;
import org.dhis2.ruleengine.models.Option;
import org.dhis2.ruleengine.models.Rule;
import org.dhis2.ruleengine.models.RuleAction;
import org.dhis2.ruleengine.models.RuleAction.Assign;
import org.dhis2.ruleengine.models.RuleAction.CreateEvent;
import org.dhis2.ruleengine.models.RuleAction.DisplayKeyValuePair;
import org.dhis2.ruleengine.models.RuleAction.DisplayText;
import org.dhis2.ruleengine.models.RuleAction.ErrorOnCompletion;
import org.dhis2.ruleengine.models.RuleAction.HideField;
import org.dhis2.ruleengine.models.RuleAction.HideProgramStage;
import org.dhis2.ruleengine.models.RuleAction.HideSection;
import org.dhis2.ruleengine.models.RuleAction.ScheduleMessage;
import org.dhis2.ruleengine.models.RuleAction.SendMessage;
import org.dhis2.ruleengine.models.RuleAction.SetMandatory;
import org.dhis2.ruleengine.models.RuleAction.ShowError;
import org.dhis2.ruleengine.models.RuleAction.ShowWarning;
import org.dhis2.ruleengine.models.RuleAction.WarningOnCompletion;
import org.dhis2.ruleengine.models.RuleAttributeValue;
import org.dhis2.ruleengine.models.RuleDataValue;
import org.dhis2.ruleengine.models.RuleEnrollment;
import org.dhis2.ruleengine.models.RuleEvent;
import org.dhis2.ruleengine.models.RuleValueType;
import org.dhis2.ruleengine.models.RuleVariable;
import org.dhis2.ruleengine.models.RuleVariable.RuleVariableAttribute;
import org.dhis2.ruleengine.models.RuleVariable.RuleVariableCalculatedValue;
import org.dhis2.ruleengine.models.RuleVariable.RuleVariableCurrentEvent;
import org.dhis2.ruleengine.models.RuleVariable.RuleVariableNewestEvent;
import org.dhis2.ruleengine.models.RuleVariable.RuleVariableNewestStageEvent;
import org.dhis2.ruleengine.models.RuleVariable.RuleVariablePreviousEvent;
import org.dhis2.ruleengine.utils.EnvironmentVariables;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Transactional( readOnly = true )
@Service( "org.hisp.dhis.programrule.engine.ProgramRuleEntityMapperService" )
public class DefaultProgramRuleEntityMapperService implements ProgramRuleEntityMapperService
{
    private static final String LOCATION_FEEDBACK = "feedback";

    private static final String LOCATION_INDICATOR = "indicators";

    private final ImmutableMap<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>> ACTION_MAPPER = new ImmutableMap.Builder<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>>()
        .put( ProgramRuleActionType.ASSIGN,
            pra -> new Assign( getAssignedParameter( pra ), pra.getData(), pra.getContent() ) )
        .put( ProgramRuleActionType.CREATEEVENT,
            pra -> new CreateEvent( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.DISPLAYKEYVALUEPAIR, this::getLocationBasedDisplayRuleAction )
        .put( ProgramRuleActionType.DISPLAYTEXT, this::getLocationBasedDisplayRuleAction )
        .put( ProgramRuleActionType.HIDEFIELD,
            pra -> new HideField( getAttributeType( pra ), pra.getContent(), getAssignedParameter( pra ),
                pra.getData() ) )
        .put( ProgramRuleActionType.HIDEPROGRAMSTAGE,
            pra -> new HideProgramStage( pra.getProgramStage().getUid(), pra.getData() ) )
        .put( ProgramRuleActionType.HIDESECTION,
            pra -> new HideSection( pra.getProgramStageSection().getUid(), pra.getData() ) )
        .put( ProgramRuleActionType.SHOWERROR,
            pra -> new ShowError( getAttributeType( pra ), pra.getContent(), getAssignedParameter( pra ),
                pra.getData() ) )
        .put( ProgramRuleActionType.SHOWWARNING,
            pra -> new ShowWarning( getAttributeType( pra ), pra.getContent(), getAssignedParameter( pra ),
                pra.getData() ) )
        .put( ProgramRuleActionType.SETMANDATORYFIELD,
            pra -> new SetMandatory( getAttributeType( pra ), getAssignedParameter( pra ), pra.getData() ) )
        .put( ProgramRuleActionType.WARNINGONCOMPLETE,
            pra -> new WarningOnCompletion( getAttributeType( pra ), pra.getContent(), getAssignedParameter( pra ),
                pra.getData() ) )
        .put( ProgramRuleActionType.ERRORONCOMPLETE,
            pra -> new ErrorOnCompletion( getAttributeType( pra ), pra.getContent(), getAssignedParameter( pra ),
                pra.getData() ) )
        .put( ProgramRuleActionType.SENDMESSAGE,
            pra -> new SendMessage( pra.getTemplateUid(), pra.getData() ) )
        .put( ProgramRuleActionType.SCHEDULEMESSAGE,
            pra -> new ScheduleMessage( pra.getTemplateUid(), pra.getData() ) )
        .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>> VARIABLE_MAPPER = new ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>>()
        .put( ProgramRuleVariableSourceType.CALCULATED_VALUE,
            prv -> new RuleVariableCalculatedValue( prv.getName(), prv.getUid(), toMappedValueType( prv ),
                prv.getUseCodeForOptionSet(), List.of() ) )
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            prv -> new RuleVariableAttribute( prv.getName(), prv.getAttribute().getUid(),
                toMappedValueType( prv ), prv.getUseCodeForOptionSet(), getOptions( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            prv -> new RuleVariableCurrentEvent( prv.getName(), prv.getDataElement().getUid(),
                toMappedValueType( prv ), prv.getUseCodeForOptionSet(), getOptions( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT,
            prv -> new RuleVariablePreviousEvent( prv.getName(), prv.getDataElement().getUid(),
                toMappedValueType( prv ), prv.getUseCodeForOptionSet(), getOptions( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
            prv -> new RuleVariableNewestEvent( prv.getName(), prv.getDataElement().getUid(),
                toMappedValueType( prv ), prv.getUseCodeForOptionSet(), getOptions( prv ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
            prv -> new RuleVariableNewestStageEvent( prv.getName(), prv.getDataElement().getUid(),
                toMappedValueType( prv ), prv.getProgramStage().getUid(), prv.getUseCodeForOptionSet(),
                getOptions( prv ) ) )
        .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>> VALUE_TYPE_MAPPER = new ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>>()
        .put( ProgramRuleVariableSourceType.CALCULATED_VALUE, ProgramRuleVariable::getValueType )
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, prv -> prv.getAttribute().getValueType() )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, prv -> prv.getDataElement().getValueType() )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, prv -> prv.getDataElement().getValueType() )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
            prv -> prv.getDataElement().getValueType() )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
            prv -> prv.getDataElement().getValueType() )
        .build();

    private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, DataItem>> DESCRIPTION_MAPPER = new ImmutableMap.Builder<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, DataItem>>()
        .put( ProgramRuleVariableSourceType.TEI_ATTRIBUTE, prv -> {
            TrackedEntityAttribute attribute = prv.getAttribute();

            return new DataItem( ObjectUtils.firstNonNull( attribute.getDisplayName(), attribute.getDisplayFormName(),
                attribute.getName() ), getItemValueType( attribute.getValueType() ) );
        } )
        .put( ProgramRuleVariableSourceType.CALCULATED_VALUE,
            prv -> new DataItem( ObjectUtils.firstNonNull( prv.getDisplayName(), prv.getName() ),
                getItemValueType( prv.getValueType() ) ) )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, this::getDisplayName )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, this::getDisplayName )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, this::getDisplayName )
        .put( ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, this::getDisplayName )
        .build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramRuleService programRuleService;

    private final ProgramRuleVariableService programRuleVariableService;

    private final ConstantService constantService;

    private final I18nManager i18nManager;

    public DefaultProgramRuleEntityMapperService( ProgramRuleService programRuleService,
        ProgramRuleVariableService programRuleVariableService,
        ConstantService constantService, I18nManager i18nManager )
    {
        checkNotNull( programRuleService );
        checkNotNull( programRuleVariableService );
        checkNotNull( constantService );
        checkNotNull( i18nManager );

        this.programRuleService = programRuleService;
        this.programRuleVariableService = programRuleVariableService;
        this.constantService = constantService;
        this.i18nManager = i18nManager;
    }

    @Override
    public List<Rule> toMappedProgramRules()
    {
        List<ProgramRule> programRules = programRuleService.getAllProgramRule();

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
    public List<RuleVariable> toMappedProgramRuleVariables( List<ProgramRuleVariable> programRuleVariables )
    {
        return programRuleVariables
            .stream()
            .filter( Objects::nonNull )
            .map( this::toRuleVariable )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    @Override
    public Map<String, DataItem> getItemStore( List<ProgramRuleVariable> programRuleVariables )
    {
        Map<String, DataItem> itemStore = new HashMap<>();

        // program rule variables
        programRuleVariables
            .forEach( prv -> itemStore.put( ObjectUtils.firstNonNull( prv.getName(), prv.getDisplayName() ),
                DESCRIPTION_MAPPER.get( prv.getSourceType() ).apply( prv ) ) );

        // constants
        constantService.getAllConstants().forEach( constant -> itemStore.put( constant.getUid(),
            new DataItem( ObjectUtils.firstNonNull( constant.getDisplayName(), constant.getDisplayFormName(),
                constant.getName() ), ItemValueType.NUMBER ) ) );

        // program variables
        EnvironmentVariables.INSTANCE.list().forEach( var -> itemStore.put( var, new DataItem(
            ObjectUtils.firstNonNull( i18nManager.getI18n().getString( var ), var ), ItemValueType.TEXT ) ) );

        return itemStore;
    }

    @Override
    public RuleEnrollment toMappedRuleEnrollment( Enrollment enrollment,
        List<TrackedEntityAttributeValue> trackedEntityAttributeValues )
    {
        if ( enrollment == null )
        {
            return null;
        }

        String orgUnit = "";
        String orgUnitCode = "";

        if ( enrollment.getOrganisationUnit() != null )
        {
            orgUnit = enrollment.getOrganisationUnit().getUid();
            orgUnitCode = enrollment.getOrganisationUnit().getCode();
        }

        List<RuleAttributeValue> ruleAttributeValues;

        if ( enrollment.getTrackedEntity() != null )
        {
            ruleAttributeValues = enrollment.getTrackedEntity().getTrackedEntityAttributeValues()
                .stream()
                .filter( Objects::nonNull )
                .map( attr -> new RuleAttributeValue( attr.getAttribute().getUid(),
                    getTrackedEntityAttributeValue( attr ) ) )
                .collect( Collectors.toList() );
        }
        else
        {
            ruleAttributeValues = trackedEntityAttributeValues
                .stream()
                .filter( Objects::nonNull )
                .map( attr -> new RuleAttributeValue( attr.getAttribute().getUid(),
                    getTrackedEntityAttributeValue( attr ) ) )
                .collect( Collectors.toList() );
        }

        return new RuleEnrollment( enrollment.getUid(), enrollment.getProgram().getName(),
            LocalDate.Companion.parse(
                enrollment.getIncidentDate().toInstant().atZone( ZoneId.systemDefault() ).toLocalDate().toString() ),
            LocalDate.Companion.parse(
                enrollment.getEnrollmentDate().toInstant().atZone( ZoneId.systemDefault() ).toLocalDate().toString() ),
            RuleEnrollment.Status.valueOf( enrollment.getStatus().toString() ), orgUnit, orgUnitCode,
            ruleAttributeValues );
    }

    @Override
    public List<RuleEvent> toMappedRuleEvents( Set<Event> events,
        Event eventToEvaluate )
    {
        return events
            .stream()
            .filter( Objects::nonNull )
            .filter( event -> !(eventToEvaluate != null && event.getUid().equals( eventToEvaluate.getUid() )) )
            .map( this::toMappedRuleEvent )
            .collect( Collectors.toList() );
    }

    @Override
    public RuleEvent toMappedRuleEvent( Event event )
    {
        if ( event == null )
        {
            return null;
        }

        String orgUnit = getOrgUnit( event );
        String orgUnitCode = getOrgUnitCode( event );

        return new RuleEvent( event.getUid(), event.getProgramStage().getUid(),
            event.getProgramStage().getName(),
            RuleEvent.Status.valueOf( event.getStatus().toString() ),
            LocalDate.Companion
                .parse(
                    event.getExecutionDate().toInstant().atZone( ZoneId.systemDefault() ).toLocalDate().toString() ),
            event.getDueDate() == null ? null
                : LocalDate.Companion
                    .parse( event.getDueDate().toInstant().atZone( ZoneId.systemDefault() ).toLocalDate().toString() ),
            event.getCompletedDate() == null ? null
                : LocalDate.Companion.parse(
                    event.getCompletedDate().toInstant().atZone( ZoneId.systemDefault() ).toLocalDate().toString() ),
            orgUnit,
            orgUnitCode,
            event.getEventDataValues()
                .stream()
                .filter( Objects::nonNull )
                .filter( dv -> dv.getValue() != null )
                .map(
                    dv -> new RuleDataValue(
                        LocalDate.Companion.parse( event.getExecutionDate().toInstant().atZone( ZoneId.systemDefault() )
                            .toLocalDate().toString() ),
                        event.getProgramStage().getUid(), dv.getDataElement(), dv.getValue() ) )
                .collect( Collectors.toList() ) );
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private String getOrgUnit( Event event )
    {
        if ( event.getOrganisationUnit() != null )
        {
            return event.getOrganisationUnit().getUid();
        }

        return "";
    }

    private String getOrgUnitCode( Event event )
    {
        if ( event.getOrganisationUnit() != null )
        {
            return event.getOrganisationUnit().getCode();
        }

        return "";
    }

    private Rule toRule( ProgramRule programRule )
    {
        if ( programRule == null )
        {
            return null;
        }

        Set<ProgramRuleAction> programRuleActions = programRule.getProgramRuleActions();

        List<RuleAction> ruleActions;

        Rule rule;
        try
        {
            ruleActions = programRuleActions.stream().map( this::toRuleAction ).collect( Collectors.toList() );

            rule = new Rule( programRule.getName(),
                programRule.getProgramStage() != null ? programRule.getProgramStage().getUid() : StringUtils.EMPTY,
                programRule.getPriority(), programRule.getCondition(), ruleActions,
                programRule.getUid() );
        }
        catch ( Exception e )
        {
            log.debug( "Invalid rule action in ProgramRule: " + programRule.getUid() );

            return null;
        }

        return rule;
    }

    private RuleAction toRuleAction( ProgramRuleAction programRuleAction )
    {
        programRuleAction.setData( programRuleAction.getData() == null ? "" : programRuleAction.getData() );

        return ACTION_MAPPER
            .getOrDefault( programRuleAction.getProgramRuleActionType(),
                pra -> new Assign( pra.getContent(), pra.getData(), getAssignedParameter( pra ) ) )
            .apply( programRuleAction );
    }

    private RuleVariable toRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        RuleVariable ruleVariable = null;

        try
        {
            if ( VARIABLE_MAPPER.containsKey( programRuleVariable.getSourceType() ) )
            {
                ruleVariable = VARIABLE_MAPPER.get( programRuleVariable.getSourceType() ).apply( programRuleVariable );
            }
        }
        catch ( Exception e )
        {
            log.debug( "Invalid ProgramRuleVariable: " + programRuleVariable.getUid() );
        }

        return ruleVariable;
    }

    private RuleValueType toMappedValueType( ProgramRuleVariable programRuleVariable )
    {
        ValueType valueType = VALUE_TYPE_MAPPER
            .getOrDefault( programRuleVariable.getSourceType(), prv -> ValueType.TEXT ).apply( programRuleVariable );

        if ( valueType.isBoolean() )
        {
            return RuleValueType.BOOLEAN;
        }

        if ( valueType.isNumeric() )
        {
            return RuleValueType.NUMERIC;
        }

        if ( valueType.isDate() )
        {
            return RuleValueType.DATE;
        }

        return RuleValueType.TEXT;
    }

    private AttributeType getAttributeType( ProgramRuleAction programRuleAction )
    {
        if ( programRuleAction.hasDataElement() )
        {
            return DATA_ELEMENT;
        }

        if ( programRuleAction.hasTrackedEntityAttribute() )
        {
            return TRACKED_ENTITY_ATTRIBYTE;
        }

        return AttributeType.UNKNOWN;
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
            return StringUtils.EMPTY;
        }

        log.warn( String.format( "No location found for ProgramRuleAction: %s in ProgramRule: %s",
            programRuleAction.getProgramRuleActionType(), programRuleAction.getProgramRule().getUid() ) );

        return StringUtils.EMPTY;
    }

    private RuleAction getLocationBasedDisplayRuleAction( ProgramRuleAction programRuleAction )
    {
        if ( ProgramRuleActionType.DISPLAYTEXT.equals( programRuleAction.getProgramRuleActionType() ) )
        {
            if ( LOCATION_FEEDBACK.equals( programRuleAction.getLocation() ) )
            {
                return new DisplayText( programRuleAction.getContent(), DisplayLocation.LOCATION_FEEDBACK_WIDGET,
                    programRuleAction.getData() );
            }

            if ( LOCATION_INDICATOR.equals( programRuleAction.getLocation() ) )
            {
                return new DisplayText( programRuleAction.getContent(), DisplayLocation.LOCATION_INDICATOR_WIDGET,
                    programRuleAction.getData() );
            }

            return new DisplayText( programRuleAction.getContent(), DisplayLocation.LOCATION_FEEDBACK_WIDGET,
                programRuleAction.getData() );
        }
        else
        {
            if ( LOCATION_FEEDBACK.equals( programRuleAction.getLocation() ) )
            {
                return new DisplayKeyValuePair( programRuleAction.getContent(),
                    DisplayLocation.LOCATION_FEEDBACK_WIDGET,
                    programRuleAction.getData() );
            }

            if ( LOCATION_INDICATOR.equals( programRuleAction.getLocation() ) )
            {
                return new DisplayKeyValuePair( programRuleAction.getContent(),
                    DisplayLocation.LOCATION_INDICATOR_WIDGET, programRuleAction.getData() );
            }

            return new DisplayKeyValuePair( programRuleAction.getContent(), DisplayLocation.LOCATION_FEEDBACK_WIDGET,
                programRuleAction.getData() );
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

    private ItemValueType getItemValueType( ValueType valueType )
    {
        if ( valueType.isDate() )
        {
            return ItemValueType.DATE;
        }

        if ( valueType.isNumeric() )
        {
            return ItemValueType.NUMBER;
        }

        if ( valueType.isBoolean() )
        {
            return ItemValueType.BOOLEAN;
        }

        // default
        return ItemValueType.TEXT;
    }

    private DataItem getDisplayName( ProgramRuleVariable prv )
    {
        DataElement dataElement = prv.getDataElement();

        return new DataItem( ObjectUtils.firstNonNull( dataElement.getDisplayFormName(), dataElement.getFormName(),
            dataElement.getName() ), getItemValueType( dataElement.getValueType() ) );
    }

    private List<Option> getOptions( ProgramRuleVariable prv )
    {
        if ( prv.getUseCodeForOptionSet() )
        {
            return List.of();
        }

        if ( prv.hasDataElement() && prv.getDataElement().hasOptionSet() )
        {
            return prv.getDataElement().getOptionSet().getOptions().stream()
                .map( op -> new Option( op.getName(), op.getCode() ) )
                .toList();
        }
        else if ( prv.hasTrackedEntityAttribute() && prv.getAttribute().hasOptionSet() )
        {
            return prv.getAttribute().getOptionSet().getOptions().stream()
                .map( op -> new Option( op.getName(), op.getCode() ) )
                .toList();
        }

        return List.of();
    }
}
