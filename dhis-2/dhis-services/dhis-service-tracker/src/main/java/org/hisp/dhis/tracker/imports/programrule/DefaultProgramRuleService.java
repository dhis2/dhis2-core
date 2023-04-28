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
package org.hisp.dhis.tracker.imports.programrule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.RuleEngineConverterService;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final RuleEngineConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event> eventTrackerConverterService;

    private final TrackerConverterService<Attribute, TrackedEntityAttributeValue> attributeValueTrackerConverterService;

    private final RuleActionEnrollmentMapper ruleActionEnrollmentMapper;

    private final RuleActionEventMapper ruleActionEventMapper;

    @Override
    @Transactional( readOnly = true )
    public void calculateRuleEffects( TrackerBundle bundle, TrackerPreheat preheat )
    {
        List<RuleEffects> ruleEffects = ListUtils.union(
            calculateEnrollmentRuleEffects( bundle, preheat ),
            ListUtils.union(
                calculateProgramEventRuleEffects( bundle, preheat ),
                calculateTrackerEventRuleEffects( bundle, preheat ) ) );

        // This is needed for bundle side effects process
        bundle.setRuleEffects( ruleEffects );

        // These are needed for rule engine validation
        bundle.setEnrollmentRuleActionExecutors( ruleActionEnrollmentMapper.mapRuleEffects( ruleEffects, bundle ) );
        bundle.setEventRuleActionExecutors( ruleActionEventMapper.mapRuleEffects( ruleEffects, bundle ) );
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
        Map<Program, List<org.hisp.dhis.tracker.imports.domain.Event>> programEvents = bundle.getEvents()
            .stream()
            .filter( event -> preheat.getProgram( event.getProgram() ).isWithoutRegistration() )
            .collect( Collectors.groupingBy( event -> preheat.getProgram( event.getProgram() ) ) );

        return programEvents
            .entrySet()
            .stream()
            .flatMap( entry -> {
                List<Event> events = eventTrackerConverterService
                    .fromForRuleEngine( preheat, entry.getValue() );

                return programRuleEngine.evaluateProgramEvents( new HashSet<>( events ),
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
            .map( Enrollment::getAttributes )
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
    private Set<Event> getEventsFromEnrollment( String enrollmentUid, TrackerBundle bundle,
        TrackerPreheat preheat )
    {
        Stream<Event> programStageInstances = preheat.getEvents().values()
            .stream()
            .filter( e -> e.getProgramInstance().getUid().equals( enrollmentUid ) )
            .filter( e -> bundle.findEventByUid( e.getUid() ).isEmpty() );

        Stream<Event> bundleEvents = bundle.getEvents()
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollmentUid ) )
            .map( event -> eventTrackerConverterService.fromForRuleEngine( preheat, event ) );

        return Stream.concat( programStageInstances, bundleEvents ).collect( Collectors.toSet() );

    }
}
