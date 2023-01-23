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

import java.util.Optional;

import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.programrule.implementers.enrollment.AssignValueExecutor;
import org.hisp.dhis.tracker.report.ValidationReport;
import org.hisp.dhis.tracker.report.Warning;

/**
 * A {@link RuleActionExecutor} execute a rule action for an event or an
 * enrollment. The execution can produce a {@link ProgramRuleIssue} that will be
 * converted into an {@link org.hisp.dhis.tracker.report.Error} or a
 * {@link Warning} and presented to the client in the {@link ValidationReport}.
 *
 * {@link AssignValueExecutor} can also mutate the Bundle, as it can add or
 * change the value of an attribute.
 * {@link org.hisp.dhis.tracker.programrule.implementers.event.AssignValueExecutor}
 * can do the same for a data element.
 */
public interface RuleActionExecutor<T>
{
    /**
     * A rule action can be associated with an attribute or a data element. When
     * it is associated with a data element we need to make sure the data
     * element is part of the {@link ProgramStage} of the event otherwise we do
     * not need to execute the action.
     *
     * @return the attribute/dataElement Uid the rule action is associated with.
     */
    String getField();

    Optional<ProgramRuleIssue> executeRuleAction( TrackerBundle bundle, T entity );
}
