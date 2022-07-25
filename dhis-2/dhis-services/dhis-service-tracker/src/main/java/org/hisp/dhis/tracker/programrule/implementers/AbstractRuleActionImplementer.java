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
package org.hisp.dhis.tracker.programrule.implementers;

import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.rules.models.AttributeType.TRACKED_ENTITY_ATTRIBUTE;
import static org.hisp.dhis.rules.models.AttributeType.UNKNOWN;
import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.needsToValidateDataValues;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAttribute;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.programrule.EnrollmentActionRule;
import org.hisp.dhis.tracker.programrule.EventActionRule;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.programrule.RuleActionImplementer;

import com.google.common.collect.Lists;

// TODO: Verify if we can remove checks on ProgramStage when Program Rule
// validation is in place
public abstract class AbstractRuleActionImplementer<T extends RuleAction>
    implements RuleActionImplementer
{
    /**
     * @return the class of the action that the implementer work with
     */
    abstract Class<T> getActionClass();

    /**
     * Get the field from the action
     *
     * @param ruleAction to get the field from
     * @return the field of the action
     */
    abstract String getField( T ruleAction );

    /**
     * Apply rule actions to events in the bundle
     *
     * @param eventActionRules Actions to be applied to the bundle
     * @param bundle where to get the events from
     * @return A list of program rule issues that can be either warnings or
     *         errors
     */
    abstract List<ProgramRuleIssue> applyToEvents( Event event, List<EventActionRule> eventActionRules,
        TrackerBundle bundle );

    /**
     * Apply rule actions to enrollments in the bundle
     *
     * @param enrollmentActionRules Actions to be applied to the bundle
     * @param bundle where to get the enrollments from
     * @return A list of program rule issues that can be either warnings or
     *         errors
     */
    abstract List<ProgramRuleIssue> applyToEnrollments( Enrollment enrollment,
        List<EnrollmentActionRule> enrollmentActionRules, TrackerBundle bundle );

    /**
     * Get the content from the action.
     *
     * @param ruleAction to get the content from
     * @return the content of the action
     */
    protected String getContent( T ruleAction )
    {
        return null;
    }

    @Override
    public List<ProgramRuleIssue> validateEvent( TrackerBundle bundle, List<RuleEffect> ruleEffects, Event event )
    {
        List<EventActionRule> eventEffects = getEventEffects( event, ruleEffects, bundle );

        return applyToEvents( event, eventEffects, bundle );
    }

    @Override
    public List<ProgramRuleIssue> validateEnrollment( TrackerBundle bundle, List<RuleEffect> ruleEffects,
        Enrollment enrollment )
    {
        List<EnrollmentActionRule> enrollmentEffects = getEnrollmentEffects( enrollment, ruleEffects, bundle );

        return applyToEnrollments( enrollment, enrollmentEffects, bundle );
    }

    /**
     * Filter the actions by - the action class of the implementer - events
     * linked to data values that are part of a different Program Stage - events
     * linked to data values that do not need to be validated
     *
     * @param effects a map of event and effects
     * @param bundle
     * @return A map of actions by event
     */
    public List<EventActionRule> getEventEffects( Event event, List<RuleEffect> effects, TrackerBundle bundle )
    {
        ProgramStage programStage = bundle.getPreheat().getProgramStage( event.getProgramStage() );
        Set<DataValue> dataValues = event.getDataValues();
        return effects
            .stream()
            .filter( effect -> getActionClass().isAssignableFrom( effect.ruleAction().getClass() ) )
            .filter( effect -> getAttributeType( effect.ruleAction() ) == UNKNOWN ||
                getAttributeType( effect.ruleAction() ) == DATA_ELEMENT )
            .map( effect -> new EventActionRule( effect.ruleId(), effect.data(),
                getField( (T) effect.ruleAction() ),
                getAttributeType( effect.ruleAction() ),
                getContent( (T) effect.ruleAction() ), dataValues ) )
            .filter( effect -> effect.getAttributeType() != DATA_ELEMENT ||
                isDataElementPartOfProgramStage( effect.getField(), programStage ) )
            .filter(
                effect -> effect.getAttributeType() != DATA_ELEMENT ||
                    needsToValidateDataValues( event, programStage ) )
            .collect( Collectors.toList() );
    }

    /**
     * Filter the actions by the action class of the implementer
     *
     * @param effects a map of enrollments and effects
     * @param bundle
     * @return A map of actions by enrollment
     */
    @SuppressWarnings( "unchecked" )
    public List<EnrollmentActionRule> getEnrollmentEffects( Enrollment enrollment, List<RuleEffect> effects,
        TrackerBundle bundle )
    {
        List<Attribute> payloadTeiAttributes = getTrackedEntity( bundle, enrollment.getTrackedEntity() )
            .map( TrackedEntity::getAttributes )
            .orElse( Collections.emptyList() );
        List<Attribute> attributes = mergeAttributes( enrollment.getAttributes(),
            payloadTeiAttributes );

        return effects
            .stream()
            .filter( effect -> getActionClass().isAssignableFrom( effect.ruleAction().getClass() ) )
            .filter( effect -> getAttributeType( effect.ruleAction() ) == UNKNOWN ||
                getAttributeType( effect.ruleAction() ) == TRACKED_ENTITY_ATTRIBUTE )
            .map( effect -> new EnrollmentActionRule( effect.ruleId(), effect.data(),
                getField( (T) effect.ruleAction() ),
                getAttributeType( effect.ruleAction() ),
                getContent( (T) effect.ruleAction() ), attributes ) )
            .collect( Collectors.toList() );
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
        return programStage.getDataElements()
            .stream()
            .map( BaseIdentifiableObject::getUid )
            .anyMatch( de -> de.equals( dataElementUid ) );
    }

    private AttributeType getAttributeType( RuleAction ruleAction )
    {
        if ( ruleAction instanceof RuleActionAttribute )
        {
            return ((RuleActionAttribute) ruleAction).attributeType();
        }

        return UNKNOWN;
    }

    protected Optional<Event> getEvent( TrackerBundle bundle, String eventUid )
    {
        return bundle.getEvents()
            .stream()
            .filter( e -> e.getEvent().equals( eventUid ) )
            .findAny();
    }

    private Optional<Enrollment> getEnrollment( TrackerBundle bundle, String enrollmentUid )
    {
        return bundle.getEnrollments()
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollmentUid ) )
            .findAny();
    }

    private Optional<TrackedEntity> getTrackedEntity( TrackerBundle bundle, String teiUid )
    {
        return bundle.getTrackedEntities()
            .stream()
            .filter( e -> e.getTrackedEntity().equals( teiUid ) )
            .findAny();
    }
}
