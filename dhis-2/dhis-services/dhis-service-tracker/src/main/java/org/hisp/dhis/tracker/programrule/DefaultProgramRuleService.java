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
package org.hisp.dhis.tracker.programrule;

import static org.hisp.dhis.tracker.validation.validator.ValidationUtils.needsToValidateDataValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleActionError;
import org.hisp.dhis.rules.models.RuleActionErrorOnCompletion;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleActionShowError;
import org.hisp.dhis.rules.models.RuleActionShowWarning;
import org.hisp.dhis.rules.models.RuleActionWarningOnCompletion;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.RuleEngineConverterService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.implementers.RuleActionType;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.ActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.AssignActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.ErrorActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.ErrorOnCompleteActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.MandatoryActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.SyntaxErrorActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.WarningActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.WarningOnCompleteActionRule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
@Service
@RequiredArgsConstructor
class DefaultProgramRuleService
    implements ProgramRuleService
{
    @Qualifier( "serviceTrackerRuleEngine" )
    private final ProgramRuleEngine programRuleEngine;

    private final RuleEngineConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService;

    private final RuleEngineConverterService<Event, ProgramStageInstance> eventTrackerConverterService;

    private final TrackerConverterService<Attribute, TrackedEntityAttributeValue> attributeValueTrackerConverterService;

    @Override
    @Transactional( readOnly = true )
    public void calculateRuleEffects( TrackerBundle bundle, TrackerPreheat preheat )
    {
        List<RuleEffects> ruleEffects = ListUtils.union(
            calculateEnrollmentRuleEffects( bundle, preheat ),
            ListUtils.union(
                calculateProgramEventRuleEffects( bundle, preheat ),
                calculateTrackerEventRuleEffects( bundle, preheat ) ) );

        // The following converting/filtering process should be responsibility of a different class

        // This is needed for bunlde side effects process
        bundle.setRuleEffects( ruleEffects );

        // These are needed for rule engine validation
        bundle.setEnrollmentActionRules( filterEnrollments( ruleEffects, bundle ) );
        bundle.setEventActionRules( filterEvents( ruleEffects, bundle ) );
    }

    private Map<TrackerDto, List<ActionRule>> filterEnrollments( List<RuleEffects> ruleEffects, TrackerBundle bundle )
    {
        return ruleEffects.stream()
            .filter( RuleEffects::isEnrollment )
            .filter( e -> bundle.findEnrollmentByUid( e.getTrackerObjectUid() ).isPresent() )
            .collect( Collectors.toMap( e -> bundle.findEnrollmentByUid( e.getTrackerObjectUid() ).get(),
                e -> buildEnrollmentActionRules( e, bundle ) ) );
    }

    private Map<TrackerDto, List<EventActionRule>> filterEvents( List<RuleEffects> ruleEffects, TrackerBundle bundle )
    {
        return ruleEffects.stream()
            .filter( RuleEffects::isEvent )
            .filter( e -> bundle.findEventByUid( e.getTrackerObjectUid() ).isPresent() )
            .collect( Collectors.toMap( e -> bundle.findEventByUid( e.getTrackerObjectUid() ).get(),
                e -> buildEventActionRules( e, bundle ) ) );
    }

    private List<ActionRule> buildEnrollmentActionRules( RuleEffects ruleEffects, TrackerBundle bundle )
    {
        Enrollment enrollment = bundle.findEnrollmentByUid( ruleEffects.getTrackerObjectUid() ).get();
        List<Attribute> payloadTeiAttributes = bundle.findTrackedEntityByUid( enrollment.getTrackedEntity() )
            .map( TrackedEntity::getAttributes )
            .orElse( Collections.emptyList() );
        List<Attribute> attributes = mergeAttributes( enrollment.getAttributes(),
            payloadTeiAttributes );

        return ruleEffects
            .getRuleEffects()
            .stream()
            .map( effect -> buildEnrollmentActionRule( effect.ruleId(), effect.data(),
                effect.ruleAction(), attributes ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    // TODO: Every action rule will be changed to be more expressive and convert the data-field-content
    // rule engine model to something that make sense for the validators
    // i.e. Not all actions need attributes
    private ActionRule buildEnrollmentActionRule( String ruleId, String data, RuleAction ruleAction,
        List<Attribute> attributes )
    {
        if ( ruleAction instanceof RuleActionAssign )
        {
            RuleActionAssign action = (RuleActionAssign) ruleAction;
            return new AssignActionRule( ruleId, data, action.field(), action.content(), attributes );
        }
        if ( ruleAction instanceof RuleActionSetMandatoryField )
        {
            RuleActionSetMandatoryField action = (RuleActionSetMandatoryField) ruleAction;
            return new MandatoryActionRule( ruleId, data, action.field(), null, attributes );
        }
        if ( ruleAction instanceof RuleActionShowError )
        {
            RuleActionShowError action = (RuleActionShowError) ruleAction;
            return new ErrorActionRule( ruleId, data, action.field(), action.content(), attributes );
        }
        if ( ruleAction instanceof RuleActionShowWarning )
        {
            RuleActionShowWarning action = (RuleActionShowWarning) ruleAction;
            return new WarningActionRule( ruleId, data, action.field(), action.content(), attributes );
        }
        if ( ruleAction instanceof RuleActionErrorOnCompletion )
        {
            RuleActionErrorOnCompletion action = (RuleActionErrorOnCompletion) ruleAction;
            return new ErrorOnCompleteActionRule( ruleId, data, action.field(), action.content(), attributes );
        }
        if ( ruleAction instanceof RuleActionWarningOnCompletion )
        {
            RuleActionWarningOnCompletion action = (RuleActionWarningOnCompletion) ruleAction;
            return new WarningOnCompleteActionRule( ruleId, data, action.field(), action.content(), attributes );
        }
        if ( ruleAction instanceof RuleActionError )
        {
            return new SyntaxErrorActionRule( ruleId, data, null, null, attributes );
        }
        return null;
    }

    private List<EventActionRule> buildEventActionRules( RuleEffects ruleEffects, TrackerBundle bundle )
    {

        Event event = bundle.findEventByUid( ruleEffects.getTrackerObjectUid() ).get();
        ProgramStage programStage = bundle.getPreheat().getProgramStage( event.getProgramStage() );
        Set<DataValue> dataValues = event.getDataValues();

        return ruleEffects
            .getRuleEffects()
            .stream()
            .map( effect -> buildEventActionRule( effect.ruleId(), effect.data(),
                effect.ruleAction(), dataValues ) )
            .filter( Objects::nonNull )
            .filter( effect -> isDataElementPartOfProgramStage( effect.getField(), programStage ) )
            .filter( effect -> needsToValidateDataValues( event, programStage ) )
            .collect( Collectors.toList() );
    }

    private EventActionRule buildEventActionRule( String ruleId, String data, RuleAction ruleAction,
        Set<DataValue> dataValues )
    {
        if ( ruleAction instanceof RuleActionAssign )
        {
            RuleActionAssign action = (RuleActionAssign) ruleAction;
            return new EventActionRule( ruleId, data, action.field(), action.content(), RuleActionType.ASSIGN,
                dataValues );
        }
        if ( ruleAction instanceof RuleActionSetMandatoryField )
        {
            RuleActionSetMandatoryField action = (RuleActionSetMandatoryField) ruleAction;
            return new EventActionRule( ruleId, data, action.field(), null, RuleActionType.MANDATORY_VALUE,
                dataValues );
        }
        if ( ruleAction instanceof RuleActionShowError )
        {
            RuleActionShowError action = (RuleActionShowError) ruleAction;
            return new EventActionRule( ruleId, data, action.field(), action.content(), RuleActionType.ERROR,
                dataValues );
        }
        if ( ruleAction instanceof RuleActionShowWarning )
        {
            RuleActionShowWarning action = (RuleActionShowWarning) ruleAction;
            return new EventActionRule( ruleId, data, action.field(), action.content(), RuleActionType.WARNING,
                dataValues );
        }
        if ( ruleAction instanceof RuleActionErrorOnCompletion )
        {
            RuleActionErrorOnCompletion action = (RuleActionErrorOnCompletion) ruleAction;
            return new EventActionRule( ruleId, data, action.field(), action.content(),
                RuleActionType.ERROR_ON_COMPLETE, dataValues );
        }
        if ( ruleAction instanceof RuleActionWarningOnCompletion )
        {
            RuleActionWarningOnCompletion action = (RuleActionWarningOnCompletion) ruleAction;
            return new EventActionRule( ruleId, data, action.field(), action.content(),
                RuleActionType.WARNING_ON_COMPLETE, dataValues );
        }
        if ( ruleAction instanceof RuleActionError )
        {
            return new EventActionRule( ruleId, data, null, null, RuleActionType.SYNTAX_ERROR, dataValues );
        }
        return null;
    }

    private List<Attribute> mergeAttributes( List<Attribute> enrollmentAttributes, List<Attribute> attributes )
    {

        List<Attribute> mergedAttributes = Lists.newArrayList();
        mergedAttributes.addAll( attributes );
        mergedAttributes.addAll( enrollmentAttributes );
        return mergedAttributes;
    }

    private boolean isDataElementPartOfProgramStage( String dataElementUid, ProgramStage programStage )
    {
        if ( StringUtils.isEmpty( dataElementUid ) )
        {
            return true;
        }

        return programStage.getDataElements()
            .stream()
            .map( BaseIdentifiableObject::getUid )
            .anyMatch( de -> de.equals( dataElementUid ) );
    }

    private List<RuleEffects> calculateEnrollmentRuleEffects( TrackerBundle bundle, TrackerPreheat preheat )
    {
        return bundle.getEnrollments()
            .stream()
            .flatMap( e -> {
                ProgramInstance programInstance = enrollmentTrackerConverterService.fromForRuleEngine( preheat, e );

                return programRuleEngine
                    .evaluateEnrollmentAndEvents( programInstance,
                        getEventsFromEnrollment( programInstance.getUid(), bundle, preheat ),
                        getAttributes( e.getEnrollment(), e.getTrackedEntity(), bundle, preheat ) )
                    .stream();
            } )
            .collect( Collectors.toList() );
    }

    private List<RuleEffects> calculateTrackerEventRuleEffects( TrackerBundle bundle, TrackerPreheat preheat )
    {
        Set<ProgramInstance> programInstances = bundle.getEvents()
            .stream()
            .filter( event -> bundle.findEnrollmentByUid( event.getEnrollment() ).isEmpty() )
            .filter( event -> preheat.getProgram( event.getProgram() ).isRegistration() )
            .map( event -> preheat.getEnrollment( event.getEnrollment() ) )
            .collect( Collectors.toSet() );

        return programInstances
            .stream()
            .flatMap( enrollment -> programRuleEngine.evaluateEnrollmentAndEvents(
                enrollment,
                getEventsFromEnrollment( enrollment.getUid(), bundle, preheat ),
                getAttributes( enrollment.getUid(), enrollment.getEntityInstance().getUid(), bundle, preheat ) )
                .stream() )
            .collect( Collectors.toList() );
    }

    private List<RuleEffects> calculateProgramEventRuleEffects( TrackerBundle bundle, TrackerPreheat preheat )
    {
        Map<Program, List<Event>> programEvents = bundle.getEvents()
            .stream()
            .filter( event -> preheat.getProgram( event.getProgram() ).isWithoutRegistration() )
            .collect( Collectors.groupingBy( event -> preheat.getProgram( event.getProgram() ) ) );

        return programEvents
            .entrySet()
            .stream()
            .flatMap( entry -> {
                List<ProgramStageInstance> programStageInstances = eventTrackerConverterService
                    .fromForRuleEngine( preheat, entry.getValue() );

                return programRuleEngine.evaluateProgramEvents( new HashSet<>( programStageInstances ),
                    entry.getKey() )
                    .stream();
            } )
            .collect( Collectors.toList() );
    }

    // Get all the attributes linked to enrollment from the payload and the DB,
    // using the one from payload
    // if they are present in both places
    private List<TrackedEntityAttributeValue> getAttributes( String enrollmentUid, String teiUid, TrackerBundle bundle,
        TrackerPreheat preheat )
    {
        List<TrackedEntityAttributeValue> attributeValues = bundle.findEnrollmentByUid( enrollmentUid )
            .map( e -> e.getAttributes() )
            .map( attributes -> attributeValueTrackerConverterService.from( preheat, attributes ) )
            .orElse( new ArrayList<>() );

        List<TrackedEntityAttributeValue> payloadAttributeValues = bundle
            .findTrackedEntityByUid( teiUid )
            .map( tei -> attributeValueTrackerConverterService.from( preheat, tei.getAttributes() ) )
            .orElse( Collections.emptyList() );
        attributeValues.addAll( payloadAttributeValues );

        TrackedEntityInstance trackedEntity = preheat.getTrackedEntity( teiUid );

        if ( trackedEntity != null )
        {
            List<String> payloadAttributeValuesIds = payloadAttributeValues.stream()
                .map( av -> av.getAttribute().getUid() )
                .collect( Collectors.toList() );

            attributeValues.addAll( trackedEntity.getTrackedEntityAttributeValues().stream()
                .filter( av -> !payloadAttributeValuesIds.contains( av.getAttribute().getUid() ) )
                .collect( Collectors.toList() ) );
        }

        return attributeValues;
    }

    // Get all the events linked to enrollment from the payload and the DB,
    // using the one from payload
    // if they are present in both places
    private Set<ProgramStageInstance> getEventsFromEnrollment( String enrollmentUid, TrackerBundle bundle,
        TrackerPreheat preheat )
    {
        Stream<ProgramStageInstance> programStageInstances = preheat.getEvents().values()
            .stream()
            .filter( e -> e.getProgramInstance().getUid().equals( enrollmentUid ) )
            .filter( e -> bundle.findEventByUid( e.getUid() ).isEmpty() );

        Stream<ProgramStageInstance> bundleEvents = bundle.getEvents()
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollmentUid ) )
            .map( event -> eventTrackerConverterService.fromForRuleEngine( preheat, event ) );

        return Stream.concat( programStageInstances, bundleEvents ).collect( Collectors.toSet() );

    }
}
