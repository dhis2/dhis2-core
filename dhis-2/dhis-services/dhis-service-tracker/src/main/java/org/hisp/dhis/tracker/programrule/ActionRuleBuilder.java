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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleActionError;
import org.hisp.dhis.rules.models.RuleActionErrorOnCompletion;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleActionShowError;
import org.hisp.dhis.rules.models.RuleActionShowWarning;
import org.hisp.dhis.rules.models.RuleActionWarningOnCompletion;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.programrule.implementers.RuleActionType;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.ActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.AssignActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.ErrorActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.ErrorOnCompleteActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.MandatoryActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.SyntaxErrorActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.WarningActionRule;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.WarningOnCompleteActionRule;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

@Service
@RequiredArgsConstructor
class ActionRuleBuilder
{
    public Map<TrackerDto, List<ActionRule>> buildEnrollmentActionRules( List<RuleEffects> ruleEffects,
        TrackerBundle bundle )
    {
        return buildEnrollmentEffects( filterEnrollments( ruleEffects, bundle ), bundle );
    }

    public Map<TrackerDto, List<EventActionRule>> buildEventActionRules( List<RuleEffects> ruleEffects,
        TrackerBundle bundle )
    {
        return filterEvents( ruleEffects, bundle );
    }

    private List<RuleEffects> filterEnrollments( List<RuleEffects> ruleEffects, TrackerBundle bundle )
    {
        return ruleEffects.stream()
            .filter( RuleEffects::isEnrollment )
            .filter( e -> bundle.findEnrollmentByUid( e.getTrackerObjectUid() ).isPresent() )
            .collect( Collectors.toList() );
    }

    private Map<TrackerDto, List<ActionRule>> buildEnrollmentEffects( List<RuleEffects> ruleEffects,
        TrackerBundle bundle )
    {
        return ruleEffects
            .stream()
            .collect( Collectors.toMap( e -> bundle.findEnrollmentByUid( e.getTrackerObjectUid() ).get(),
                e -> buildEnrollmentActionRules( bundle.findEnrollmentByUid( e.getTrackerObjectUid() ).get(), e,
                    bundle ) ) );
    }

    private Map<TrackerDto, List<EventActionRule>> filterEvents( List<RuleEffects> ruleEffects, TrackerBundle bundle )
    {
        return ruleEffects.stream()
            .filter( RuleEffects::isEvent )
            .filter( e -> bundle.findEventByUid( e.getTrackerObjectUid() ).isPresent() )
            .filter( e -> needsToValidateDataValues( bundle.findEventByUid( e.getTrackerObjectUid() ).get(),
                bundle.getPreheat()
                    .getProgramStage( bundle.findEventByUid( e.getTrackerObjectUid() ).get().getProgramStage() ) ) )
            .collect( Collectors.toMap( e -> bundle.findEventByUid( e.getTrackerObjectUid() ).get(),
                e -> buildEventActionRules( bundle.findEventByUid( e.getTrackerObjectUid() ).get(), e, bundle ) ) );
    }

    private List<ActionRule> buildEnrollmentActionRules( Enrollment enrollment, RuleEffects ruleEffects,
        TrackerBundle bundle )
    {
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

    private ActionRule buildEnrollmentActionRule( String ruleId, String data, RuleAction ruleAction,
        List<Attribute> attributes )
    {
        if ( ruleAction instanceof RuleActionAssign )
        {
            RuleActionAssign action = (RuleActionAssign) ruleAction;
            return new AssignActionRule( ruleId, data, action.field(), attributes );
        }
        if ( ruleAction instanceof RuleActionSetMandatoryField )
        {
            RuleActionSetMandatoryField action = (RuleActionSetMandatoryField) ruleAction;
            return new MandatoryActionRule( ruleId, action.field() );
        }
        if ( ruleAction instanceof RuleActionShowError )
        {
            RuleActionShowError action = (RuleActionShowError) ruleAction;
            return new ErrorActionRule( ruleId, data, action.field(), action.content() );
        }
        if ( ruleAction instanceof RuleActionShowWarning )
        {
            RuleActionShowWarning action = (RuleActionShowWarning) ruleAction;
            return new WarningActionRule( ruleId, data, action.field(), action.content() );
        }
        if ( ruleAction instanceof RuleActionErrorOnCompletion )
        {
            RuleActionErrorOnCompletion action = (RuleActionErrorOnCompletion) ruleAction;
            return new ErrorOnCompleteActionRule( ruleId, data, action.field(), action.content() );
        }
        if ( ruleAction instanceof RuleActionWarningOnCompletion )
        {
            RuleActionWarningOnCompletion action = (RuleActionWarningOnCompletion) ruleAction;
            return new WarningOnCompleteActionRule( ruleId, data, action.field(), action.content() );
        }
        if ( ruleAction instanceof RuleActionError )
        {
            return new SyntaxErrorActionRule( ruleId, data );
        }
        return null;
    }

    private List<EventActionRule> buildEventActionRules( Event event, RuleEffects ruleEffects, TrackerBundle bundle )
    {
        ProgramStage programStage = bundle.getPreheat().getProgramStage( event.getProgramStage() );
        Set<DataValue> dataValues = event.getDataValues();

        return ruleEffects
            .getRuleEffects()
            .stream()
            .map( effect -> buildEventActionRule( effect.ruleId(), effect.data(),
                effect.ruleAction(), dataValues ) )
            .filter( Objects::nonNull )
            .filter( effect -> isDataElementPartOfProgramStage( effect.getField(), programStage ) )
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
}
