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

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.EnrollmentActionRule;
import org.hisp.dhis.tracker.programrule.EventActionRule;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * This implementer assign a value to a field if it is empty, otherwise returns
 * an error
 *
 * @Author Enrico Colasante
 */
@Component
@RequiredArgsConstructor
public class AssignValueImplementer
    extends AbstractRuleActionImplementer<RuleActionAssign>
{
    private final SystemSettingManager systemSettingManager;

    @Override
    public Class<RuleActionAssign> getActionClass()
    {
        return RuleActionAssign.class;
    }

    @Override
    public String getField( RuleActionAssign ruleAction )
    {
        return ruleAction.field();
    }

    @Override
    public List<ProgramRuleIssue> applyToEvents( Event event, List<EventActionRule> eventActionRules,
        TrackerBundle bundle )
    {
        List<ProgramRuleIssue> issues = Lists.newArrayList();
        Boolean canOverwrite = systemSettingManager
            .getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        TrackerPreheat preheat = bundle.getPreheat();
        for ( EventActionRule actionRule : eventActionRules )
        {
            if ( getDataValue( actionRule, preheat ).isEmpty() ||
                Boolean.TRUE.equals( canOverwrite ) ||
                isTheSameValue( actionRule, bundle.getPreheat() ) )
            {
                addOrOverwriteDataValue( event, actionRule, bundle );
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1308,
                    Lists.newArrayList( actionRule.getField(), event.getEvent() ), IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1307,
                    Lists.newArrayList( actionRule.getField(), actionRule.getData() ), IssueType.ERROR ) );
            }
        }

        return issues;
    }

    public Optional<DataValue> getDataValue( EventActionRule actionRule, TrackerPreheat preheat )
    {
        DataElement dataElement = preheat.getDataElement( actionRule.getField() );
        return actionRule.getDataValues()
            .stream()
            .filter( dv -> dv.getDataElement().isEqualTo( dataElement ) )
            .findAny();
    }

    @Override
    public List<ProgramRuleIssue> applyToEnrollments( Enrollment enrollment,
        List<EnrollmentActionRule> enrollmentActionRules, TrackerBundle bundle )
    {
        List<ProgramRuleIssue> issues = Lists.newArrayList();
        Boolean canOverwrite = systemSettingManager
            .getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        for ( EnrollmentActionRule actionRule : enrollmentActionRules )
        {
            if ( getAttribute( actionRule, bundle.getPreheat() ).isEmpty() ||
                Boolean.TRUE.equals( canOverwrite ) ||
                isTheSameValue( actionRule, bundle.getPreheat() ) )
            {
                addOrOverwriteAttribute( enrollment, actionRule, bundle );
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1310,
                    Lists.newArrayList( actionRule.getField(), actionRule.getData() ),
                    IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1309,
                    Lists.newArrayList( actionRule.getField(), enrollment.getEnrollment() ),
                    IssueType.ERROR ) );
            }
        }

        return issues;
    }

    private Optional<Attribute> getAttribute( EnrollmentActionRule actionRule, TrackerPreheat preheat )
    {
        TrackedEntityAttribute attribute = preheat.getTrackedEntityAttribute( actionRule.getField() );
        return actionRule.getAttributes()
            .stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
    }

    private boolean isTheSameValue( EventActionRule actionRule, TrackerPreheat preheat )
    {
        DataElement dataElement = preheat.getDataElement( actionRule.getField() );
        String dataValue = actionRule.getData();
        Optional<DataValue> optionalDataValue = actionRule.getDataValues().stream()
            .filter( dv -> dv.getDataElement().isEqualTo( dataElement ) )
            .findAny();
        if ( optionalDataValue.isPresent() )
        {
            return isEqual( dataValue, optionalDataValue.get().getValue(), dataElement.getValueType() );
        }

        return false;
    }

    private boolean isTheSameValue( EnrollmentActionRule actionRule, TrackerPreheat preheat )
    {
        TrackedEntityAttribute attribute = preheat.getTrackedEntityAttribute( actionRule.getField() );
        String value = actionRule.getData();
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

    private void addOrOverwriteDataValue( Event event, EventActionRule actionRule, TrackerBundle bundle )
    {
        DataElement dataElement = bundle.getPreheat().getDataElement( actionRule.getField() );

        Optional<DataValue> dataValue = event.getDataValues().stream()
            .filter( dv -> dv.getDataElement().isEqualTo( dataElement ) )
            .findAny();

        if ( dataValue.isPresent() )
        {
            dataValue.get().setValue( actionRule.getData() );
        }
        else
        {
            event.getDataValues()
                .add( createDataValue( bundle.getPreheat().getIdSchemes().toMetadataIdentifier( dataElement ),
                    actionRule.getData() ) );
        }
    }

    private void addOrOverwriteAttribute( Enrollment enrollment, EnrollmentActionRule actionRule, TrackerBundle bundle )
    {
        TrackedEntityAttribute attribute = bundle.getPreheat().getTrackedEntityAttribute( actionRule.getField() );
        Optional<TrackedEntity> trackedEntity = bundle.getTrackedEntity( enrollment.getTrackedEntity() );
        List<Attribute> attributes;

        if ( trackedEntity.isPresent() )
        {
            attributes = trackedEntity.get().getAttributes();
            Optional<Attribute> optionalAttribute = attributes.stream()
                .filter( at -> at.getAttribute().isEqualTo( attribute ) )
                .findAny();
            if ( optionalAttribute.isPresent() )
            {
                optionalAttribute.get().setValue( actionRule.getData() );
                return;
            }
        }

        attributes = enrollment.getAttributes();
        Optional<Attribute> optionalAttribute = attributes.stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
        if ( optionalAttribute.isPresent() )
        {
            optionalAttribute.get().setValue( actionRule.getData() );
        }
        else
        {
            attributes.add( createAttribute( bundle.getPreheat().getIdSchemes().toMetadataIdentifier( attribute ),
                actionRule.getData() ) );
        }
    }

    private Attribute createAttribute( MetadataIdentifier attribute, String newValue )
    {
        return Attribute.builder()
            .attribute( attribute )
            .value( newValue )
            .build();
    }

    private DataValue createDataValue( MetadataIdentifier dataElement, String newValue )
    {
        return DataValue.builder()
            .dataElement( dataElement )
            .value( newValue )
            .build();
    }
}
