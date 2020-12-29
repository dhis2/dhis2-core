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

package org.hisp.dhis.tracker.programrule;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.RuleActionAttribute;
import org.hisp.dhis.rules.models.RuleActionMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.hooks.ValidationUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Author Enrico Colasante
 */
public abstract class ErrorWarningImplementer
    implements RuleActionValidator
{
    @Override
    public abstract Class<? extends RuleActionMessage> getActionClass();

    public abstract boolean isOnComplete();

    public abstract IssueType getIssueType();

    @Override
    public Map<String, List<ProgramRuleIssue>> validateEnrollments( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> effects =
            getEffects( bundle.getEnrollmentRuleEffects() );

        List<String> filteredEnrollments = bundle.getEnrollments()
            .stream()
            .filter( filterEnrollment() )
            .map( Enrollment::getEnrollment )
            .collect( Collectors.toList() );

        return effects.entrySet().stream()
            .filter( e -> filteredEnrollments.contains( e.getKey() ) )
            .collect( Collectors.toMap( Map.Entry::getKey,
                e -> parseErrors( e.getValue() ) ) );
    }

    private List<ProgramRuleIssue> parseErrors( List<RuleEffect> effects )
    {
        return effects
            .stream()
            .map( ruleEffect -> {
                RuleActionMessage ruleActionMessage = getActionClass().cast( ruleEffect.ruleAction() );
                String field = ruleActionMessage.field();
                String content = ruleActionMessage.content();
                String data = ruleEffect.data();

                StringBuilder stringBuilder = new StringBuilder( content );
                if ( !StringUtils.isEmpty( data ) )
                {
                    stringBuilder.append( " " ).append( data );
                }
                if ( !StringUtils.isEmpty( field ) )
                {
                    stringBuilder.append( " (" ).append( field ).append( ")" );
                }

                return stringBuilder.toString();
            } )
            .map( message -> new ProgramRuleIssue( message, getIssueType() ) )
            .collect( Collectors.toList() );
    }

    @Override
    public Map<String, List<ProgramRuleIssue>> validateEvents( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> effects = getEffects( bundle.getEventRuleEffects() );

        List<String> filteredEvents = bundle.getEvents()
            .stream()
            .filter( filterEvent() )
            .map( Event::getEvent )
            .collect( Collectors.toList() );

        Map<String, List<RuleEffect>> effectsByEvent = effects.entrySet().stream()
            .filter( e -> filteredEvents.contains( e.getKey() ) )
            .collect( Collectors.toMap( Map.Entry::getKey,
                Map.Entry::getValue ) );

        return filterDataElementEffects( effectsByEvent, bundle.getEvents(), bundle.getPreheat() );
    }

    /**
     * Effects that are linked to a data element shouldn't be applied if the {@link Event} is {@link EventStatus#ACTIVE}
     * and the {@link org.hisp.dhis.program.ValidationStrategy} is {@link org.hisp.dhis.program.ValidationStrategy#ON_COMPLETE}
     */
    private Map<String, List<ProgramRuleIssue>> filterDataElementEffects( Map<String, List<RuleEffect>> effectsByEvent,
        List<Event> events, TrackerPreheat preheat )
    {
        Map<String, List<ProgramRuleIssue>> filteredEffects = Maps.newHashMap();

        for ( Map.Entry<String, List<RuleEffect>> eventWithEffects : effectsByEvent.entrySet() )
        {
            Event event = events.stream().filter( e -> e.getEvent().equals( eventWithEffects.getKey() ) ).findAny()
                .get();
            ProgramStage programStage = preheat.get( ProgramStage.class, event.getProgramStage() );

            boolean needsToValidateDataValues = ValidationUtils.needsToValidateDataValues( event, programStage );

            List<RuleEffect> ruleEffectsToValidate = eventWithEffects.getValue()
                .stream()
                .filter( effect ->
                    ((RuleActionAttribute) effect.ruleAction()).attributeType() != AttributeType.DATA_ELEMENT ||
                        needsToValidateDataValues )
                .collect( Collectors.toList() );

            if ( !ruleEffectsToValidate.isEmpty() )
            {
                filteredEffects.put( eventWithEffects.getKey(), parseErrors( ruleEffectsToValidate ) );
            }
        }

        return filteredEffects;
    }

    protected Predicate<Event> filterEvent()
    {
        if ( isOnComplete() )
        {
            return e -> Objects.equals( EventStatus.COMPLETED, e.getStatus() );
        }
        else
        {
            return e -> true;
        }
    }

    protected Predicate<Enrollment> filterEnrollment()
    {
        if ( isOnComplete() )
        {
            return e -> Objects.equals( EnrollmentStatus.COMPLETED, e.getStatus() );
        }
        else
        {
            return e -> true;
        }
    }
}
