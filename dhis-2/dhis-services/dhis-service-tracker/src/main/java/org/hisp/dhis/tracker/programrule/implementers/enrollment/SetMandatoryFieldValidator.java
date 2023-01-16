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
package org.hisp.dhis.tracker.programrule.implementers.enrollment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * This implementer check if a field is not empty in the {@link TrackerBundle}
 *
 * @Author Enrico Colasante
 */
@Component( "org.hisp.dhis.tracker.programrule.implementers.enrollment.SetMandatoryFieldValidator" )
public class SetMandatoryFieldValidator implements RuleActionEnrollmentValidator<MandatoryActionRule>
{

    @Override
    public List<MandatoryActionRule> filter( List<ActionRule> actionRules )
    {
        return actionRules
            .stream()
            .filter( a -> a instanceof MandatoryActionRule )
            .map( a -> (MandatoryActionRule) a )
            .collect( Collectors.toList() );
    }

    @Override
    public List<ProgramRuleIssue> validateEnrollment( TrackerBundle bundle,
        List<MandatoryActionRule> enrollmentActionRules, Enrollment enrollment )
    {
        return enrollmentActionRules.stream()
            .flatMap( actionRule -> checkMandatoryEnrollmentAttribute(
                enrollment, enrollmentActionRules, bundle.getPreheat() ).stream() )
            .collect( Collectors.toList() );
    }

    private List<ProgramRuleIssue> checkMandatoryEnrollmentAttribute( Enrollment enrollment,
        List<MandatoryActionRule> effects, TrackerPreheat preheat )
    {
        TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
        return effects.stream()
            .map( action -> {
                TrackedEntityAttribute ruleAttribute = preheat.getTrackedEntityAttribute( action.getAttribute() );
                Optional<Attribute> any = enrollment.getAttributes().stream()
                    .filter( attribute -> attribute.getAttribute().isEqualTo( ruleAttribute ) )
                    .findAny();
                if ( any.isEmpty() || StringUtils.isEmpty( any.get().getValue() ) )
                {
                    return new ProgramRuleIssue( action.getRuleUid(),
                        ValidationCode.E1306,
                        Lists.newArrayList(
                            idSchemes.toMetadataIdentifier( ruleAttribute ).getIdentifierOrAttributeValue() ),
                        IssueType.ERROR );
                }
                else
                {
                    return null;
                }
            } )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }
}
