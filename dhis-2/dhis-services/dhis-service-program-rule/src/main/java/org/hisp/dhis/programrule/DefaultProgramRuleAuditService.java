package org.hisp.dhis.programrule;

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

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @Author Zubair Asghar.
 */
public class DefaultProgramRuleAuditService implements ProgramRuleAuditService
{
    private static final String SEPARATOR_ID = "\\.";
    private static final String ENVIRONMENT_VARIABLE = "V";
    private static final String EXPRESSION_PREFIX_REGEXP = "#" + "|" + "A" + "|" + ENVIRONMENT_VARIABLE;
    private static final String EXPRESSION_REGEXP = "(" + EXPRESSION_PREFIX_REGEXP + ")\\{([\\w\\_]+)" + SEPARATOR_ID + "?(\\w*)\\}";
    private static final Set<String> KEYS = Sets.newHashSet( "A", "#" );

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( EXPRESSION_REGEXP );

    private ProgramRuleAuditStore programRuleAuditStore;

    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    public DefaultProgramRuleAuditService( ProgramRuleAuditStore programRuleAuditStore, ProgramRuleVariableService ruleVariableService )
    {
        checkNotNull( programRuleAuditStore );
        checkNotNull( ruleVariableService );

        this.programRuleAuditStore = programRuleAuditStore;
        this.programRuleVariableService = ruleVariableService;
    }

    @Override
    public void addProgramRuleAudit( ProgramRuleAudit ruleAudit )
    {
        programRuleAuditStore.addProgramRuleAudit( ruleAudit );
    }

    @Override
    public void updateProgramRuleAudit( ProgramRuleAudit ruleAudit )
    {
        programRuleAuditStore.updateProgramRuleAudit( ruleAudit );
    }

    @Override
    public void deleteProgramRuleAudit( ProgramRuleAudit ruleAudit )
    {
        programRuleAuditStore.deleteProgramRuleAudit( ruleAudit );
    }

    @Override
    public ProgramRuleAudit getProgramRuleAudit( ProgramRule programRule )
    {
        return programRuleAuditStore.getProgramRuleAudit( programRule );
    }

    @Override
    public List<ProgramRuleAudit> getAllProgramRuleAudits()
    {
        return programRuleAuditStore.getAllProgramRuleAudits();
    }

    @Override
    public ProgramRuleAudit createOrUpdateProgramRuleAudit( ProgramRuleAudit audit, ProgramRule programRule )
    {
        Map<String, Set<String>> variableCollection = extractVariableCollection( programRule );

        Set<ProgramRuleVariable> ruleVariables = getProgramRuleVariables( variableCollection.getOrDefault( ProgramRule.KEY_RULE_VARIABLES, new HashSet<>() ), programRule );
        Set<String> environmentVariables = variableCollection.getOrDefault( ProgramRule.KEY_ENVIRONMENT_VARIABLES, new HashSet<>() );

        if ( audit == null )
        {
            audit = new ProgramRuleAudit( programRule );
        }

        audit.setProgramRuleVariables( ruleVariables );
        audit.setEnvironmentVariables( new HashSet<>( environmentVariables ) );
        audit.setAuditFields();

        return audit;
    }

    private Map<String, Set<String>> extractVariableCollection( ProgramRule programRule )
    {
        Map<String, Set<String>> variableCollection = new HashMap<>();

        if ( programRule.getCondition().isEmpty() )
        {
            return variableCollection;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher( programRule.getCondition() );

        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String name = matcher.group( 2 );

            if ( KEYS.contains( key ) )
            {
                if ( !variableCollection.containsKey( ProgramRule.KEY_RULE_VARIABLES ) )
                {
                    variableCollection.put( ProgramRule.KEY_RULE_VARIABLES, new HashSet<>() );
                }

                variableCollection.get( ProgramRule.KEY_RULE_VARIABLES ).add( name );
            }

            if ( ENVIRONMENT_VARIABLE.equals( key ) )
            {
                if ( !variableCollection.containsKey( ProgramRule.KEY_ENVIRONMENT_VARIABLES ) )
                {
                    variableCollection.put( ProgramRule.KEY_ENVIRONMENT_VARIABLES, new HashSet<>() );
                }

                variableCollection.get( ProgramRule.KEY_ENVIRONMENT_VARIABLES ).add( name );
            }
        }

        return variableCollection;
    }

    private Set<ProgramRuleVariable> getProgramRuleVariables(  Set<String> ruleVariableNames, ProgramRule programRule )
    {
        return ruleVariableNames.stream()
            .filter( Objects::nonNull )
            .flatMap( name -> programRuleVariableService.getProgramRuleVariable( programRule.getProgram(), name ).stream() )
            .collect( Collectors.toSet() );
    }
}
