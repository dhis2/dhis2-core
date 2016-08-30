package org.hisp.dhis.programrule;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

import java.util.HashSet;
import java.util.Set;

/**
 * @author markusbekken
 */
@JacksonXmlRootElement( localName = "programRule", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramRule
    extends BaseIdentifiableObject
{
    /**
     * The description of the program rule
     */
    private String description;

    /**
     * The program that the rule belongs to
     */
    private Program program;

    /**
     * The programStage that the rule belongs to
     */
    private ProgramStage programStage;

    /**
     * The collection of actions that will be triggered if the the rule is triggered.
     */
    private Set<ProgramRuleAction> programRuleActions = new HashSet<>();

    /**
     * The condition expression, if this expression is evaluated to true, the actions is triggered.
     */
    private String condition;

    /**
     * The priority of the rule within the program. The lower the priority, the earlier the rule is run.
     * Null means that the rule is run last(together will all other null-rules)
     */
    private Integer priority;

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramRule()
    {

    }

    public ProgramRule( String name, String description, Program program, ProgramStage programStage, Set<ProgramRuleAction> programRuleActions, String condition, Integer priority )
    {
        this.name = name;
        this.description = description;
        this.program = program;
        this.programStage = programStage;
        this.programRuleActions = programRuleActions;
        this.condition = condition;
        this.priority = priority;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programRuleActions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programRuleAction", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramRuleAction> getProgramRuleActions()
    {
        return programRuleActions;
    }

    public void setProgramRuleActions( Set<ProgramRuleAction> programRuleActions )
    {
        this.programRuleActions = programRuleActions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCondition()
    {
        return condition;
    }

    public void setCondition( String condition )
    {
        this.condition = condition;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getPriority()
    {
        return priority;
    }

    public void setPriority( Integer priority )
    {
        this.priority = priority;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ProgramRule programRule = (ProgramRule) other;

            if ( mergeMode.isReplace() )
            {
                description = programRule.getDescription();
                priority = programRule.getPriority();
                condition = programRule.getCondition();
                program = programRule.getProgram();
                programStage = programRule.getProgramStage();
            }
            else if ( mergeMode.isMerge() )
            {
                description = programRule.getDescription() == null ? description : programRule.getDescription();
                priority = programRule.getPriority() == null ? priority : programRule.getPriority();
                condition = programRule.getCondition() == null ? condition : programRule.getCondition();
                program = programRule.getProgram() == null ? program : programRule.getProgram();
                programStage = programRule.getProgramStage() == null ? programStage : programRule.getProgramStage();
            }

            programRuleActions.clear();
            programRuleActions.addAll( programRule.getProgramRuleActions() );
        }
    }
}