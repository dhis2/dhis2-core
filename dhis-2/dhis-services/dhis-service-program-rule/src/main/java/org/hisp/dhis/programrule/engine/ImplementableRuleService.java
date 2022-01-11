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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;

import com.google.common.collect.ImmutableList;

abstract class ImplementableRuleService
{
    private final ProgramRuleService programRuleService;

    public ImplementableRuleService( ProgramRuleService programRuleService )
    {
        this.programRuleService = programRuleService;
    }

    abstract List<ProgramRule> getProgramRulesByActionTypes( Program program, String programStageUid );

    abstract Cache<Boolean> getProgramHasRulesCache();

    protected List<ProgramRule> getProgramRulesByActionTypes( Program program,
        Set<ProgramRuleActionType> types, String programStageUid )
    {
        if ( programStageUid == null )
        {
            return programRuleService.getProgramRulesByActionTypes( program, types );
        }
        else
        {
            return programRuleService.getProgramRulesByActionTypes( program, types, programStageUid );
        }

    }

    public List<ProgramRule> getProgramRules( Program program, String programStageUid )
    {
        Optional<Boolean> optionalCacheValue = getProgramHasRulesCache().get( program.getUid() );

        if ( optionalCacheValue.isPresent() && Boolean.FALSE.equals( optionalCacheValue.get() ) )
        {
            return ImmutableList.of();
        }

        List<ProgramRule> programRulesByActionTypes = getProgramRulesByActionTypes( program, programStageUid );

        if ( programStageUid == null ) // To populate programHasRulesCache at
                                       // enrollment
        {
            getProgramHasRulesCache().put( program.getUid(), !programRulesByActionTypes.isEmpty() );

            // At enrollment, only those rules should be selected for execution
            // which are not associated with any ProgramStage.
            return programRulesByActionTypes.stream().filter( rule -> rule.getProgramStage() == null )
                .collect( Collectors.toList() );
        }
        return programRulesByActionTypes;
    }

}
