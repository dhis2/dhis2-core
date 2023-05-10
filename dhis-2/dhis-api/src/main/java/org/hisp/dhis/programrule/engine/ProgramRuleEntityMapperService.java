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
package org.hisp.dhis.programrule.engine;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dhis2.ruleengine.DataItem;
import org.dhis2.ruleengine.models.Rule;
import org.dhis2.ruleengine.models.RuleEnrollment;
import org.dhis2.ruleengine.models.RuleEvent;
import org.dhis2.ruleengine.models.RuleVariable;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;

/**
 * RuleEngine has its own domain model. This service is responsible for
 * converting DHIS domain objects to RuleEngine domain objects and vice versa.
 *
 * Created by zubair@dhis2.org on 19.10.17.
 */
public interface ProgramRuleEntityMapperService
{
    /***
     * @return A list of mapped Rules for all programs
     */
    List<Rule> toMappedProgramRules();

    /**
     * @param programRules The list of program rules to be mapped
     * @return A list of mapped Rules for list of programs.
     */
    List<Rule> toMappedProgramRules( List<ProgramRule> programRules );

    /***
     * @return A list of mapped RuleVariables for all programs.
     */
    List<RuleVariable> toMappedProgramRuleVariables();

    /**
     * @param programRuleVariables The list of ProgramRuleVariable to be mapped.
     * @return A list of mapped RuleVariables for list of programs.
     */
    List<RuleVariable> toMappedProgramRuleVariables( List<ProgramRuleVariable> programRuleVariables );

    /**
     * @param events list of events
     * @param eventToEvaluate event to filter out from the resulting list.
     *
     * @return A list of mapped events for the list of DHIS events.
     */
    List<RuleEvent> toMappedRuleEvents( Set<Event> events, Event eventToEvaluate );

    /**
     * @param eventToEvaluate event to converted.
     * @return A mapped event for corresponding DHIS event.
     */
    RuleEvent toMappedRuleEvent( Event eventToEvaluate );

    /**
     * @return A mapped RuleEnrollment for DHIS enrollment.
     */
    RuleEnrollment toMappedRuleEnrollment( Enrollment enrollment,
        List<TrackedEntityAttributeValue> trackedEntityAttributeValues );

    /**
     * Fetch display name for {@link ProgramRuleVariable},
     * {@link org.hisp.dhis.constant.Constant}
     *
     * @return map containing item description
     */
    Map<String, DataItem> getItemStore( List<ProgramRuleVariable> programRuleVariables );
}
