/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.validateMandatoryDataValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.programrule.*;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * This implementer check if a field is not empty in the {@link TrackerBundle}
 *
 * @Author Enrico Colasante
 */
@Component
public class SetMandatoryFieldValidator
    extends AbstractRuleActionImplementer<RuleActionSetMandatoryField>
    implements RuleActionImplementer
{
    @Override
    public Class<RuleActionSetMandatoryField> getActionClass()
    {
        return RuleActionSetMandatoryField.class;
    }

    @Override
    public String getField( RuleActionSetMandatoryField ruleAction )
    {
        return ruleAction.field();
    }

    @Override
    List<ProgramRuleIssue> applyToEvents( Map.Entry<String, List<EventActionRule>> eventClasses, TrackerBundle bundle )
    {
        return checkMandatoryDataElement( getEvent( bundle, eventClasses.getKey() ).get(), eventClasses.getValue(),
            bundle );
    }

    @Override
    List<ProgramRuleIssue> applyToEnrollments( Map.Entry<String, List<EnrollmentActionRule>> enrollmentActionRules,
        TrackerBundle bundle )
    {
        return enrollmentActionRules.getValue().stream()
            .flatMap( actionRule -> checkMandatoryEnrollmentAttribute(
                bundle.getEnrollment( actionRule.getEnrollment() ).get(),
                enrollmentActionRules.getValue() ).stream() )
            .collect( Collectors.toList() );
    }

    private List<ProgramRuleIssue> checkMandatoryEnrollmentAttribute( Enrollment enrollment,
        List<EnrollmentActionRule> effects )
    {
        return effects.stream()
            .map( action -> {
                String attributeUid = action.getField();
                Optional<Attribute> any = enrollment.getAttributes().stream()
                    .filter( attribute -> attribute.getAttribute().equals( attributeUid ) )
                    .findAny();
                if ( !any.isPresent() || StringUtils.isEmpty( any.get().getValue() ) )
                {
                    return new ProgramRuleIssue( action.getRuleUid(),
                        TrackerErrorCode.E1306,
                        Lists.newArrayList( attributeUid ), IssueType.ERROR );
                }
                else
                {
                    return null;
                }
            } )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<ProgramRuleIssue> checkMandatoryDataElement( Event event, List<EventActionRule> actionRules,
        TrackerBundle bundle )
    {
        ProgramStage programStage = bundle.getPreheat().get( ProgramStage.class, event.getProgramStage() );

        Map<String, EventActionRule> mandatoryDataElementsByActionRule = actionRules.stream()
            .filter( eventActionRule -> eventActionRule.getAttributeType() == AttributeType.DATA_ELEMENT )
            .collect( Collectors.toMap( EventActionRule::getField, Function.identity() ) );

        return validateMandatoryDataValue( programStage, event,
            Lists.newArrayList( mandatoryDataElementsByActionRule.keySet() ) )
                .stream()
                .map( e -> new ProgramRuleIssue( mandatoryDataElementsByActionRule.get( e ).getRuleUid(),
                    TrackerErrorCode.E1301,
                    Lists.newArrayList( e ), IssueType.ERROR ) )
                .collect( Collectors.toList() );
    }
}
