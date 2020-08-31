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
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Lists;

/**
 * This implementer check if a field is not empty in the {@link TrackerBundle}
 *
 * @Author Enrico Colasante
 */
@Slf4j
@Component
public class SetMandatoryFieldImplementer
    implements RuleActionValidator
{
    @Override
    public Class getActionClass()
    {
        return RuleActionSetMandatoryField.class;
    }

    @Override
    public Map<String, List<String>> validateEnrollments( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> effects = getEffects( bundle.getEnrollmentRuleEffects() );

        return effects.entrySet().stream()
            .collect( Collectors.toMap( e -> e.getKey(),
                e -> getTrackedEntityFromEnrollment( bundle, e.getKey() )
                    .map( tei -> checkMandatoryTeiAttribute( tei, e.getValue() ) ).orElse( Lists.newArrayList() ) ) );
    }

    @Override
    public Map<String, List<String>> validateEvents( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> effects = getEffects( bundle.getEventRuleEffects() );

        return effects.entrySet().stream()
            .collect( Collectors.toMap( e -> e.getKey(),
                e -> getEvent( bundle, e.getKey() )
                    .map( tei -> checkMandatoryDataElement( tei, e.getValue() ) ).orElse( Lists.newArrayList() ) ) );
    }

    @Override
    public boolean isWarning()
    {
        return false;
    }

    private List<String> checkMandatoryTeiAttribute( TrackedEntity tei, List<RuleEffect> effects )
    {
        return effects.stream()
            .map( ruleEffect -> {
                RuleActionSetMandatoryField action = (RuleActionSetMandatoryField) ruleEffect.ruleAction();
                String teiAttributeUid = action.field();
                Optional<Attribute> any = tei.getAttributes().stream()
                    .filter( teiAttribute -> teiAttribute.getAttribute().equals( teiAttributeUid ) )
                    .findAny();
                if ( any.isPresent() && StringUtils.isEmpty( any.get().getValue() ) )
                {
                    return "TEI Attribute " + teiAttributeUid + " is missing for tei " + tei.getTrackedEntity();
                }
                else
                {
                    return "";
                }
            } )
            .filter( e -> !e.isEmpty() )
            .collect( Collectors.toList() );
    }

    private List<String> checkMandatoryDataElement( Event event, List<RuleEffect> effects )
    {
        return effects.stream()
            .map( ruleEffect -> {
                RuleActionSetMandatoryField action = (RuleActionSetMandatoryField) ruleEffect.ruleAction();
                String dataElementUid = action.field();
                Optional<DataValue> dataValue = event.getDataValues().stream()
                    .filter( dv -> dv.getDataElement().equals( dataElementUid ) )
                    .findAny();
                if ( dataValue.isPresent() && StringUtils.isEmpty( dataValue.get().getValue() ) )
                {
                    return "Data element " + dataElementUid + " is missing for event " + event.getEvent();
                }
                else
                {
                    return "";
                }
            } )
            .filter( e -> !e.isEmpty() )
            .collect( Collectors.toList() );
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
