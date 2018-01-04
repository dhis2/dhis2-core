package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.rules.models.*;

import java.util.List;
import java.util.Set;

/**
 * RuleEngine has its own domain model. This service is responsible for converting DHIS domain objects to
 * RuleEngine domain objects and vice versa.
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
     *@param program The program which provides ProgramRule.
     * @return A list of mapped Rules for a specific program.
     */
    List<Rule> toMappedProgramRules( Program program );

    /**
     * @param programRules The list of program rules to be mapped
     * @return A list of mapped Rules for list of programs.
     */
    List<Rule> toMappedProgramRules( List<ProgramRule> programRules);

    /**
     * @param program The program which provides ProgramRuleVariable.
     * @return A list of mapped RuleVariables for a specific program.
     */
    List<RuleVariable> toMappedProgramRuleVariables( Program program );

    /***
     * @return A list of mapped RuleVariables for all programs.
     */
    List<RuleVariable> toMappedProgramRuleVariables();

    /**
     *@param programRuleVariables The list of ProgramRuleVariable to be mapped.
     * @return A list of mapped RuleVariables for list of programs.
     */
    List<RuleVariable> toMappedProgramRuleVariables( List<ProgramRuleVariable> programRuleVariables );

    /*
     * @param programStageInstances list of events
     * @param psiToEvaluate event to filter out from the resulting list.
     *
     * @return A list of mapped events for the list of DHIS events.
     */
    List<RuleEvent> toMappedRuleEvents( Set<ProgramStageInstance> programStageInstances, ProgramStageInstance psiToEvaluate );

    /*
     * @param programStageInstances list of events
     * @return A list of mapped events for the list of DHIS events.
     */
    List<RuleEvent> toMappedRuleEvents( Set<ProgramStageInstance> programStageInstances );

    /*
     * @param psiToEvaluate event to converted.
     * @return A mapped event for corresponding DHIS event.
     */
    RuleEvent toMappedRuleEvent( ProgramStageInstance psiToEvaluate );

    /*
     * @return A mapped RuleEnrollment for DHIS enrollment i.e ProgramInstance.
     */
    RuleEnrollment toMappedRuleEnrollment( ProgramInstance programInstance );
}
