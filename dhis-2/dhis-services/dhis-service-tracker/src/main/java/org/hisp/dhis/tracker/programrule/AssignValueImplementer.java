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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.springframework.stereotype.Component;

/**
 * @Author Enrico Colasante
 */
@Slf4j
@Component
public class AssignValueImplementer
    implements RuleActionApplier
{
    @Override
    public Class getActionClass()
    {
        return RuleActionAssign.class;
    }

    @Override
    public TrackerBundle executeActions( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> enrollmentEffects = getEffects( bundle.getEnrollmentRuleEffects() );

        enrollmentEffects
            .entrySet()
            .stream()
            .forEach( e -> getTrackedEntityFromEnrollment( bundle, e.getKey() )
                .ifPresent( tei -> setTeiAttribute( tei, e.getValue() ) ) );

        Map<String, List<RuleEffect>> eventEffects = getEffects( bundle.getEventRuleEffects() );

        eventEffects
            .entrySet()
            .stream()
            .forEach( e -> getEvent( bundle, e.getKey() )
                .ifPresent( tei -> setDataElement( tei, e.getValue() ) ) );

        return bundle;
    }

    private void setTeiAttribute( TrackedEntity tei, List<RuleEffect> effects )
    {
        effects.stream()
            .forEach( ruleEffect -> {
                RuleActionAssign action = (RuleActionAssign) ruleEffect.ruleAction();
                String teiAttributeUid = action.field();
                Optional<Attribute> any = tei.getAttributes().stream()
                    .filter( teiAttribute -> teiAttribute.getAttribute().equals( teiAttributeUid ) )
                    .findAny();
                if ( any.isPresent() && !StringUtils.isEmpty( any.get().getValue() ) )
                {
                    any.get().setValue( ruleEffect.data() );
                }
            } );
    }

    private void setDataElement( Event event, List<RuleEffect> effects )
    {
        effects.stream()
            .forEach( ruleEffect -> {
                RuleActionAssign action = (RuleActionAssign) ruleEffect.ruleAction();
                String dataElementUid = action.field();
                Optional<DataValue> dataValue = event.getDataValues().stream()
                    .filter( dv -> dv.getDataElement().equals( dataElementUid ) )
                    .findAny();
                if ( dataValue.isPresent() && !StringUtils.isEmpty( dataValue.get().getValue() ) )
                {
                    dataValue.get().setValue( ruleEffect.data() );
                }
            } );
    }

    private Optional<Event> getEvent( TrackerBundle bundle, String eventUid )
    {
        return bundle.getEvents()
            .stream()
            .filter( e -> e.getEvent().equals( eventUid ) )
            .findAny();
    }

    private Optional<TrackedEntity> getTrackedEntity( TrackerBundle bundle, String teiUid )
    {
        return bundle.getTrackedEntities()
            .stream()
            .filter( e -> e.getTrackedEntity().equals( teiUid ) )
            .findAny();
    }

    private Optional<TrackedEntity> getTrackedEntityFromEnrollment( TrackerBundle bundle, String enrollmentUid )
    {
        return bundle.getEnrollments()
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollmentUid ) )
            .map( e -> e.getTrackedEntity() )
            .findAny()
            .flatMap( tei -> getTrackedEntity( bundle, tei ) );
    }
}
