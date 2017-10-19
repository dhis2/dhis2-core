package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by zubair@dhis2.org on 19.10.17.
 */
public class ProgramRuleEntityMapper
{
    private final ImmutableMap<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>> ACTION_MAPPER =
        new ImmutableMap.Builder<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>>()
        .put( ProgramRuleActionType.ASSIGN, pra -> RuleActionAssign.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.CREATEEVENT, pra -> RuleActionCreateEvent.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.DISPLAYKEYVALUEPAIR, pra -> RuleActionDisplayKeyValuePair.createForFeedback( pra.getContent(), pra.getData() ) )
        .put( ProgramRuleActionType.DISPLAYTEXT, pra -> RuleActionDisplayText.createForFeedback( pra.getContent(), pra.getData() ) )
        .put( ProgramRuleActionType.ERRORONCOMPLETE, pra -> RuleActionErrorOnCompletion.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.HIDEFIELD, pra -> RuleActionHideField.create( pra.getContent(), pra.getLocation() ) )
        .put( ProgramRuleActionType.HIDESECTION, pra -> RuleActionHideSection.create( pra.getProgramStageSection().getUid() ) )
        .put( ProgramRuleActionType.SENDMESSAGE, pra -> RuleActionAssign.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.SHOWERROR, pra -> RuleActionShowError.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.SHOWWARNING, pra -> RuleActionShowWarning.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .put( ProgramRuleActionType.SETMANDATORYFIELD, pra -> RuleActionSetMandatoryField.create( pra.getLocation() ) )
        .put( ProgramRuleActionType.WARNINGONCOMPLETE, pra -> RuleActionWarningOnCompletion.create( pra.getContent(), pra.getData(), pra.getLocation() ) )
        .build();
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramRuleService programRuleService;


    public List<Rule> getMappedProgramRules()
    {
        List<ProgramRule> programRules = programRuleService.getAllProgramRule();

        return getMappedProgramRules( programRules );
    }

    public List<Rule> getMappedProgramRules( Program program )
    {
        List<ProgramRule> programRules = programRuleService.getProgramRule( program );

        return getMappedProgramRules( programRules );
    }

    public List<Rule> getMappedProgramRules( List<ProgramRule> programRules )
    {
        List<Rule> rules = programRules.stream().map( pRule -> toMappedRule( pRule ) ).collect( Collectors.toList() );

        return rules;
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private Rule toMappedRule( ProgramRule programRule )
    {
        Set<ProgramRuleAction> programRuleActions = programRule.getProgramRuleActions();

        List<RuleAction> ruleActions = programRuleActions.stream().map( action -> toRuleAction( action ) ).collect( Collectors.toList() );

        Rule rule = Rule.create( programRule.getProgramStage() != null ? programRule.getProgramStage().getUid() : StringUtils.EMPTY, programRule.getPriority(), programRule.getCondition(), ruleActions );

        return rule;
    }

    private RuleAction toRuleAction( ProgramRuleAction programRuleAction )
    {
        RuleAction ruleAction = ACTION_MAPPER.getOrDefault( programRuleAction.getProgramRuleActionType(),
            pra -> RuleActionAssign.create( pra.getContent(), pra.getData(), pra.getLocation() ) ).apply( programRuleAction );

        return ruleAction;
    }
}
