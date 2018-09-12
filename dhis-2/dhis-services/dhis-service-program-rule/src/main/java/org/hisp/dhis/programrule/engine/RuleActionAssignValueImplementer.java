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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Zubair Asghar.
 */

public class RuleActionAssignValueImplementer implements RuleActionImplementer
{
    private static final Log log = LogFactory.getLog( RuleActionAssignValueImplementer.class );

    private static final String REGEX = "\\w+";

    private static final Pattern PATTERN = Pattern.compile( REGEX, Pattern.CASE_INSENSITIVE );

    @Autowired
    private RuleVariableInMemoryMap variableMap;

    @Override
    public boolean accept( RuleAction ruleAction )
    {
        return ruleAction instanceof RuleActionAssign;
    }

    @Override
    public void implement( RuleEffect ruleEffect, ProgramInstance programInstance )
    {
        assignValue( ruleEffect, programInstance );
    }

    @Override
    public void implement( RuleEffect ruleEffect, ProgramStageInstance programStageInstance )
    {
        ProgramInstance programInstance = programStageInstance.getProgramInstance();

        assignValue( ruleEffect, programInstance );
    }

    private void assignValue( RuleEffect ruleEffect, ProgramInstance programInstance )
    {
        if ( programInstance == null )
        {
            log.info( "No value assigned by AssignValue action" );
            return;
        }

        String value = ruleEffect.data();

        RuleActionAssign assign = (RuleActionAssign) ruleEffect.ruleAction();

        String variable = assign.field();

        Matcher matcher = PATTERN.matcher( variable );

        while ( matcher.find() )
        {
            variable = matcher.group( 0 ).trim();
        }

        log.info( "Assigning " + variable + " with value " + value );

        if ( !variableMap.containsKey( programInstance.getUid() ) )
        {
            Map<String, String> valueMap = new HashMap<>();
            valueMap.put( variable, value );

            variableMap.put( programInstance.getUid(), valueMap );
            return;
        }

        variableMap.get( programInstance.getUid() ).put( variable, value );
        return;
    }
}
