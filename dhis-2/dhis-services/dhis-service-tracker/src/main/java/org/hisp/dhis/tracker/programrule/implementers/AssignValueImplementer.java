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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.*;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerReportUtils;
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
        Boolean canOverwrite = (Boolean) systemSettingManager
            .getSystemSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        for ( EventActionRule actionRule : eventClasses.getValue() )
        {
            if ( !actionRule.getDataValue().isPresent() ||
                Boolean.TRUE.equals( canOverwrite ) ||
                isTheSameValue( actionRule, bundle.getPreheat() ) )
            {
                addOrOverwriteDataValue( actionRule, bundle );
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1308, actionRule.getDataValue().get().getDataElement(),
                        actionRule.getEvent() ),
                    IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1307, actionRule.getDataValue().get().getDataElement(),
                        actionRule.getEvent() ),
                    IssueType.ERROR ) );
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
        Boolean canOverwrite = (Boolean) systemSettingManager
            .getSystemSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE );

        for ( EnrollmentActionRule actionRule : enrollmentActionRules.getValue() )
        {
            if ( !actionRule.getAttribute().isPresent() ||
                Boolean.TRUE.equals( canOverwrite ) ||
                isTheSameValue( actionRule, bundle.getPreheat() ) )
            {
                addOrOverwriteAttribute( actionRule, bundle );
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1310, actionRule.getAttribute().get().getAttribute(),
                        actionRule.getEnrollment() ),
                    IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1310, actionRule.getAttribute().get().getAttribute(),
                        actionRule.getEnrollment() ),
                    IssueType.ERROR ) );
            }
        }

        return issues;
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
        TrackedEntityAttribute attribute = preheat.get( TrackedEntityAttribute.class, actionRule.getField() );
        String value = actionRule.getValue();
        Optional<Attribute> optionalAttribute = actionRule.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( actionRule.getField() ) )
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
        List<Attribute> attributes = bundle.getEnrollment( actionRule.getEnrollment() )
            .map( Enrollment::getAttributes ).orElse( Lists.newArrayList() );
        Optional<Attribute> optionalAttribute = attributes.stream()
            .filter( at -> at.getAttribute().equals( actionRule.getField() ) )
            .findAny();
        if ( optionalAttribute.isPresent() )
        {
            optionalAttribute.get().setValue( actionRule.getData() );
        }
        else
        {
            attributes.add( createAttribute( actionRule.getField(), actionRule.getData() ) );
        }
    }

    private Attribute createAttribute( String attributeUid, String newValue )
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( attributeUid );
        attribute.setValue( newValue );
        return attribute;
    }

    private DataValue createDataValue( String dataElementUid, String newValue )
    {
        DataValue dataValue = new DataValue();
        dataValue.setDataElement( dataElementUid );
        dataValue.setValue( newValue );
        return dataValue;
    }
}
