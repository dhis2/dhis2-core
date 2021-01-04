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

import com.google.api.client.util.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.RuleActionAttribute;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerReportUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.validateMandatoryDataValue;

/**
 * This implementer check if a field is not empty in the {@link TrackerBundle}
 *
 * @Author Enrico Colasante
 */
@Component
public class SetMandatoryFieldValidator
    implements RuleActionValidator
{
    @Override
    public Class<?> getActionClass()
    {
        return RuleActionSetMandatoryField.class;
    }

    @Override
    public Map<String, List<ProgramRuleIssue>> validateEnrollments( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> effects = getEffects( bundle.getEnrollmentRuleEffects() );

        return effects.entrySet().stream()
            .collect( Collectors.toMap( Map.Entry::getKey,
                e -> getTrackedEntityFromEnrollment( bundle, e.getKey() )
                    .map( tei -> checkMandatoryTeiAttribute( tei, e.getValue() ) ).orElse( Lists.newArrayList() ) ) );
    }

    @Override
    public Map<String, List<ProgramRuleIssue>> validateEvents( TrackerBundle bundle )
    {
        Map<String, List<RuleEffect>> effects = getEffects( bundle.getEventRuleEffects() );

        return effects.entrySet().stream()
            .collect( Collectors.toMap( Map.Entry::getKey,
                e -> getEvent( bundle, e.getKey() )
                    .map( event -> checkMandatoryDataElement( event, e.getValue(), bundle ) )
                    .orElse( Lists.newArrayList() ) ) );
    }

    private List<ProgramRuleIssue> checkMandatoryTeiAttribute( TrackedEntity tei, List<RuleEffect> effects )
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
            .map( e -> new ProgramRuleIssue( e, IssueType.ERROR ) )
            .collect( Collectors.toList() );
    }

    private List<ProgramRuleIssue> checkMandatoryDataElement( Event event, List<RuleEffect> effects,
        TrackerBundle bundle )
    {
        ProgramStage programStage = bundle.getPreheat().get( ProgramStage.class, event.getProgramStage() );

        List<String> mandatoryDataElements = effects.stream()
            .filter( effect -> ((RuleActionAttribute) effect.ruleAction()).attributeType() ==
                AttributeType.DATA_ELEMENT )
            .filter(
                effect -> isDataElementPartOfProgramStage( ((RuleActionSetMandatoryField) effect.ruleAction()).field(),
                    programStage ) )
            .map( effect -> ((RuleActionSetMandatoryField) effect.ruleAction()).field() )
            .collect( Collectors.toList() );

        return validateMandatoryDataValue( programStage, event, mandatoryDataElements )
            .stream()
            .map( e -> new ProgramRuleIssue( TrackerReportUtils.formatMessage( TrackerErrorCode.E1303, e ),
                IssueType.ERROR ) )
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
            .map( Enrollment::getTrackedEntity )
            .findAny()
            .flatMap( tei -> getTrackedEntity( bundle, tei ) );
    }
}
