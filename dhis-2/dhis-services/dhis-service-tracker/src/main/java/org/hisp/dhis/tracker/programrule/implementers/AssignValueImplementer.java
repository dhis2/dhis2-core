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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
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
import org.hisp.dhis.tracker.programrule.RuleActionImplementer;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
    implements RuleActionImplementer
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
    public List<ProgramRuleIssue> applyToEvents( Map.Entry<String, List<EventActionRule>> eventClasses,
        TrackerBundle bundle )
    {
        List<ProgramRuleIssue> issues = Lists.newArrayList();
        Boolean canOverwrite = systemSettingManager
            .getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        for ( EventActionRule actionRule : eventClasses.getValue() )
        {
            if ( !actionRule.getDataValue().isPresent() ||
                Boolean.TRUE.equals( canOverwrite ) ||
                isTheSameValue( actionRule, bundle.getPreheat() ) )
            {
                addOrOverwriteDataValue( actionRule, bundle );
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1308,
                    Lists.newArrayList( actionRule.getField(), actionRule.getEvent() ), IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1307,
                    Lists.newArrayList( actionRule.getField(), actionRule.getValue() ), IssueType.ERROR ) );
            }
        }

        return issues;
    }

    @Override
    public List<ProgramRuleIssue> applyToEnrollments(
        Map.Entry<String, List<EnrollmentActionRule>> enrollmentActionRules,
        TrackerBundle bundle )
    {
        List<ProgramRuleIssue> issues = Lists.newArrayList();
        Boolean canOverwrite = systemSettingManager
            .getBooleanSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        for ( EnrollmentActionRule actionRule : enrollmentActionRules.getValue() )
        {
            if ( !getAttribute( actionRule, bundle.getPreheat() ).isPresent() ||
                Boolean.TRUE.equals( canOverwrite ) ||
                isTheSameValue( actionRule, bundle.getPreheat() ) )
            {
                addOrOverwriteAttribute( actionRule, bundle );
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1310,
                    Lists.newArrayList( actionRule.getField(), actionRule.getValue() ),
                    IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( actionRule.getRuleUid(), TrackerErrorCode.E1309,
                    Lists.newArrayList( actionRule.getField(), actionRule.getEnrollment() ),
                    IssueType.ERROR ) );
            }
        }

        return issues;
    }

    private Optional<Attribute> getAttribute( EnrollmentActionRule actionRule, TrackerPreheat preheat )
    {
        TrackedEntityAttribute attribute = preheat.getTrackedEntityAttribute( actionRule.getField() );

        if ( AttributeType.TRACKED_ENTITY_ATTRIBUTE == actionRule.getAttributeType() )
        {
            return actionRule.getAttributes()
                .stream()
                .filter( at -> at.getAttribute().isEqualTo( attribute ) )
                .findAny();
        }

        return Optional.empty();

    }

    private boolean isTheSameValue( EventActionRule actionRule, TrackerPreheat preheat )
    {
        DataElement dataElement = preheat.get( DataElement.class, actionRule.getField() );
        String dataValue = actionRule.getValue();
        Optional<DataValue> optionalDataValue = actionRule.getDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( actionRule.getField() ) )
            .findAny();
        if ( optionalDataValue.isPresent() )
        {
            return areEquals( dataValue, optionalDataValue.get().getValue(), dataElement.getValueType() );
        }

        return false;
    }

    private boolean isTheSameValue( EnrollmentActionRule actionRule, TrackerPreheat preheat )
    {
        TrackedEntityAttribute attribute = preheat.getTrackedEntityAttribute( actionRule.getField() );
        String value = actionRule.getValue();
        Optional<Attribute> optionalAttribute = actionRule.getAttributes().stream()
            .filter( at -> at.getAttribute().isEqualTo( attribute ) )
            .findAny();
        if ( optionalAttribute.isPresent() )
        {
            return areEquals( value, optionalAttribute.get().getValue(), attribute.getValueType() );
        }

        return false;
    }

    private boolean areEquals( String dataValue, String value, ValueType valueType )
    {
        if ( valueType.isNumeric() )
        {
            return NumberUtils.isParsable( dataValue ) &&
                Double.parseDouble( value ) == Double.parseDouble( dataValue );
        }
        else
        {
            return value.equals( dataValue );
        }
    }

    private void addOrOverwriteDataValue( EventActionRule actionRule, TrackerBundle bundle )
    {
        Set<DataValue> dataValues = bundle.getEvent( actionRule.getEvent() )
            .map( Event::getDataValues ).orElse( Sets.newHashSet() );
        Optional<DataValue> dataValue = dataValues.stream()
            .filter( dv -> dv.getDataElement().equals( actionRule.getField() ) )
            .findAny();

        if ( dataValue.isPresent() )
        {
            dataValue.get().setValue( actionRule.getValue() );
        }
        else
        {
            dataValues.add( createDataValue( actionRule.getField(), actionRule.getValue() ) );
        }
    }

    private void addOrOverwriteAttribute( EnrollmentActionRule actionRule, TrackerBundle bundle )
    {
        Enrollment enrollment = bundle.getEnrollment( actionRule.getEnrollment() ).get();
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

    private DataValue createDataValue( String dataElementUid, String newValue )
    {
        DataValue dataValue = new DataValue();
        dataValue.setDataElement( dataElementUid );
        dataValue.setValue( newValue );
        return dataValue;
    }
}
