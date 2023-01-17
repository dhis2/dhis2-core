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
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.validation.ValidationCode;

import com.google.common.collect.Lists;

/**
 * This executor assign a value to a field if it is empty, otherwise returns an
 * error
 *
 * @Author Enrico Colasante
 */
@RequiredArgsConstructor
public class AssignValueExecutor implements RuleActionExecutor
{
    private final SystemSettingManager systemSettingManager;

    private final AssignValueRuleAction actionRule;

    @Override
    public Optional<ProgramRuleIssue> validateEnrollment( TrackerBundle bundle, Enrollment enrollment )
    {
        Boolean canOverwrite = systemSettingManager
            .getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        if ( getAttribute( actionRule, bundle.getPreheat() ).isEmpty() ||
            Boolean.TRUE.equals( canOverwrite ) ||
            isTheSameValue( actionRule, bundle.getPreheat() ) )
        {
            addOrOverwriteAttribute( enrollment, actionRule, bundle );
            return Optional.of( new ProgramRuleIssue( actionRule.getRuleUid(), ValidationCode.E1310,
                Lists.newArrayList( actionRule.getAttribute(), actionRule.getValue() ),
                IssueType.WARNING ) );
        }
        else
        {
            return Optional.of( new ProgramRuleIssue( actionRule.getRuleUid(), ValidationCode.E1309,
                Lists.newArrayList( actionRule.getAttribute(), enrollment.getEnrollment() ),
                IssueType.ERROR ) );
        }
    }

    private Optional<Attribute> getAttribute( AssignValueRuleAction actionRule, TrackerPreheat preheat )
    {
        TrackedEntityAttribute attribute = preheat.getTrackedEntityAttribute( actionRule.getAttribute() );
        return actionRule.getAttributes()
            .stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
    }

    private boolean isTheSameValue( AssignValueRuleAction actionRule, TrackerPreheat preheat )
    {
        TrackedEntityAttribute attribute = preheat.getTrackedEntityAttribute( actionRule.getAttribute() );
        String value = actionRule.getValue();
        Optional<Attribute> optionalAttribute = actionRule.getAttributes().stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
        if ( optionalAttribute.isPresent() )
        {
            return isEqual( value, optionalAttribute.get().getValue(), attribute.getValueType() );
        }

        return false;
    }

    /**
     * Tests whether the given values are equal. If the given value type is
     * numeric, the values are converted to doubles before being checked for
     * equality.
     *
     * @param value1 the first value.
     * @param value2 the second value.
     * @param valueType the value type.
     * @return true if the values are equal, false if not.
     */
    protected boolean isEqual( String value1, String value2, ValueType valueType )
    {
        if ( valueType.isNumeric() )
        {
            return NumberUtils.isParsable( value1 ) && NumberUtils.isParsable( value2 ) &&
                MathUtils.isEqual( Double.parseDouble( value1 ), Double.parseDouble( value2 ) );
        }
        else
        {
            return value1 != null && value1.equals( value2 );
        }
    }

    private void addOrOverwriteAttribute( Enrollment enrollment, AssignValueRuleAction actionRule,
        TrackerBundle bundle )
    {
        TrackedEntityAttribute attribute = bundle.getPreheat().getTrackedEntityAttribute( actionRule.getAttribute() );
        Optional<TrackedEntity> trackedEntity = bundle.findTrackedEntityByUid( enrollment.getTrackedEntity() );
        List<Attribute> attributes;

        if ( trackedEntity.isPresent() )
        {
            attributes = trackedEntity.get().getAttributes();
            Optional<Attribute> optionalAttribute = attributes.stream()
                .filter( at -> at.getAttribute().isEqualTo( attribute ) )
                .findAny();
            if ( optionalAttribute.isPresent() )
            {
                optionalAttribute.get().setValue( actionRule.getValue() );
                return;
            }
        }

        attributes = enrollment.getAttributes();
        Optional<Attribute> optionalAttribute = attributes.stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
        if ( optionalAttribute.isPresent() )
        {
            optionalAttribute.get().setValue( actionRule.getValue() );
        }
        else
        {
            attributes.add( createAttribute( bundle.getPreheat().getIdSchemes().toMetadataIdentifier( attribute ),
                actionRule.getValue() ) );
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
