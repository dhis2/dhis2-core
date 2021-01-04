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

package org.hisp.dhis.tracker.programrule.implementers;

import com.google.common.collect.Lists;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.programrule.*;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerReportUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * This implementer assign a value to a field if it is empty, otherwise returns an error
 *
 * @Author Enrico Colasante
 */
@Component
public class AssignValueImplementer
    extends AbstractRuleActionImplementer<RuleActionAssign>
    implements RuleActionImplementer
{
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

        for ( EventActionRule actionRule : eventClasses.getValue() )
        {
            if ( !actionRule.getDataValue().isPresent() )
            {
                actionRule.getEvent().getDataValues()
                    .add( createDataValue( actionRule.getField(), actionRule.getData() ) );
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1308, actionRule.getDataValue().get().getDataElement(),
                        actionRule.getEvent().getEvent() ), IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1307, actionRule.getDataValue().get().getDataElement(),
                        actionRule.getEvent().getEvent() ), IssueType.ERROR ) );
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

        for ( EnrollmentActionRule actionRule : enrollmentActionRules.getValue() )
        {
            if ( !actionRule.getAttribute().isPresent() )
            {
                actionRule.getEnrollment().getAttributes()
                    .add( createAttribute( actionRule.getField(), actionRule.getData() ) );
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1310, actionRule.getAttribute().get().getAttribute(),
                        actionRule.getEnrollment().getEnrollment() ), IssueType.WARNING ) );
            }
            else
            {
                issues.add( new ProgramRuleIssue( TrackerReportUtils
                    .formatMessage( TrackerErrorCode.E1310, actionRule.getAttribute().get().getAttribute(),
                        actionRule.getEnrollment().getEnrollment() ), IssueType.ERROR ) );
            }
        }

        return issues;
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
