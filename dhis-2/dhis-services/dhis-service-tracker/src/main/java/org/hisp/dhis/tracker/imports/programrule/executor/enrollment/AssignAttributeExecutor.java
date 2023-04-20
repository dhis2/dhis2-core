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
package org.hisp.dhis.tracker.imports.programrule.executor.enrollment;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.warning;
import static org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor.isEqual;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;

/**
 * This executor assigns a value to a field if it is empty, otherwise returns an
 * error
 *
 * @Author Enrico Colasante
 */
@RequiredArgsConstructor
public class AssignAttributeExecutor implements RuleActionExecutor<Enrollment>
{
    private final SystemSettingManager systemSettingManager;

    private final String ruleUid;

    private final String value;

    private final String attributeUid;

    private final List<Attribute> attributes;

    @Override
    public Optional<ProgramRuleIssue> executeRuleAction( TrackerBundle bundle, Enrollment enrollment )
    {
        Boolean canOverwrite = systemSettingManager
            .getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );
        TrackedEntityAttribute attribute = bundle.getPreheat().getTrackedEntityAttribute( attributeUid );

        Optional<Attribute> payloadAttribute = attributes.stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();

        if ( payloadAttribute.isEmpty() ||
            Boolean.TRUE.equals( canOverwrite ) ||
            isEqual( value, payloadAttribute.get().getValue(), attribute.getValueType() ) )
        {
            addOrOverwriteAttribute( enrollment, bundle );
            return Optional.of( warning( ruleUid, ValidationCode.E1310, attributeUid, value ) );
        }
        return Optional.of(
            error( ruleUid, ValidationCode.E1309, attributeUid, enrollment.getEnrollment() ) );
    }

    private void addOrOverwriteAttribute( Enrollment enrollment, TrackerBundle bundle )
    {
        TrackedEntityAttribute attribute = bundle.getPreheat().getTrackedEntityAttribute( attributeUid );
        Optional<TrackedEntity> trackedEntity = bundle.findTrackedEntityByUid( enrollment.getTrackedEntity() );

        if ( trackedEntity.isPresent() )
        {
            List<Attribute> teiAttributes = trackedEntity.get().getAttributes();
            Optional<Attribute> optionalAttribute = teiAttributes.stream()
                .filter( at -> at.getAttribute().isEqualTo( attribute ) )
                .findAny();
            if ( optionalAttribute.isPresent() )
            {
                optionalAttribute.get().setValue( value );
                return;
            }
        }

        List<Attribute> enrollmentAttributes = enrollment.getAttributes();
        Optional<Attribute> optionalAttribute = enrollmentAttributes.stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
        if ( optionalAttribute.isPresent() )
        {
            optionalAttribute.get().setValue( value );
        }
        else
        {
            enrollmentAttributes
                .add( createAttribute( bundle.getPreheat().getIdSchemes().toMetadataIdentifier( attribute ),
                    value ) );
        }
    }

    private Attribute createAttribute( MetadataIdentifier attribute, String newValue )
    {
        return Attribute.builder()
            .attribute( attribute )
            .value( newValue )
            .build();
    }
}
