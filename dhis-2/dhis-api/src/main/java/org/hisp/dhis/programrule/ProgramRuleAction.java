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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Markus Bekken
 */
@JacksonXmlRootElement( localName = "programRuleAction", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramRuleAction
    extends BaseIdentifiableObject implements MetadataObject
{
    /**
     * The programRule that the action belongs to
     */
    private ProgramRule programRule;

    /**
     * The type of action that is performed when the action is effectuated
     * <p>
     * The actual action the ruleaction row is performing. Allowed values are:
     * <ul>
     * <li>{@code displaytext}
     * Shows a text in the rulebound widget with the matching location string.
     * location: the location code of the widget to display data in
     * content: A hardcoded string to display
     * data: a variable to be evaluated and displayed at the end of the string, can be null.</li>
     *
     * <li>{@code displaykeyvaluepair}
     * Shows a key data box with a hardcoded name and a variable value.
     * location: the location code of the widget to display data in
     * content: A hardcoded string to display as title in the key data box
     * data: The variable to be evaluated and display in the lower half of the key data box.</li>
     *
     * <li>{@code hidefield}
     * Hides a dataelement from the page, as long as the dataelement is not containing a value.
     * dataelement: the dataelement to hide.</li>
     *
     * <li>{@code assignvariable}
     * Assigns/calculates a value that can be further used by other rules. Make sure the priorities is set so the rules
     * that depend on the calculation is run after the assignvariable-rule.
     * content: the variable name to be assigned. “$severeanemia” for example.
     * data: the expression to be evaluated and assigned to the content field. Can contain a hardcoded value(f.ex. “true”)
     * or an expression that is evaluated(for exampple “$hemoglobin < 7”).</li>
     *
     * <li>{@code showwarning}
     * Shows a validation warning connected to a designated dataelement
     * dataelement: the dataelement to show validationerror for
     * content: the validation error itself</li>
     *
     * <li>{@code showerror}
     * Shows a validation error connected to a designated dataelement
     * dataelement: the dataelement to show validationerror for
     * content: the validation error itself</li>
     * </ul>
     */
    private ProgramRuleActionType programRuleActionType;

    /**
     * The data element that is affected by the rule action.
     * Used for:
     * <p>
     * <ul>
     * <li>hidefield</li>
     * <li>showwarning</li>
     * <li>showerror</li>
     * </ul>
     */
    private DataElement dataElement;

    /**
     * The data element that is affected by the rule action.
     * Used for:
     * <p>
     * <ul>
     * <li>hidefield</li>
     * <li>showwarning</li>
     * <li>showerror</li>
     * </ul>
     */
    private TrackedEntityAttribute attribute;

    /**
     * The program indicator that is affected by the rule action.
     * Used for:
     * <ul>
     * <li>hidefield</li>
     * </ul>
     */
    private ProgramIndicator programIndicator;

    /**
     * The program stage section that is affected by the rule action.
     */
    private ProgramStageSection programStageSection;

    /**
     * The program stage  that is affected by the rule action.
     */
    private ProgramStage programStage;

    /**
     * The program notification that will be triggered by the rule action.
     * Used for:
     * <ul>
     * <li>sendmessage</li>
     * </ul>
     */
    private String templateUid;

    /**
     * Used to determine which widget to display data for the two action types:
     * <p>
     * <ul>
     * <li>displaytext</li>
     * <li>displaykeydata</li>
     * </ul>
     */
    private String location;

    /**
     * Used by all the different actions. See “actions”
     */
    private String content;

    /**
     * Used by all the different actions. See “actions”. The data field will be evaluated, so it can contain a rich expression.
     */
    private String data;

    /**
     * Option affected by the rule action
     */
    private Option option;

    /**
     * Option group affected by the rule action
     */
    private OptionGroup optionGroup;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramRuleAction()
    {

    }

    public ProgramRuleAction( String name, ProgramRule programRule, ProgramRuleActionType programRuleActionType,
        DataElement dataElement, TrackedEntityAttribute attribute,
        ProgramStageSection programStageSection, ProgramStage programStage,
        ProgramIndicator programIndicator, String location, String content, String data,
        Option option, OptionGroup optionGroup)
    {
        this.name = name;
        this.programRule = programRule;
        this.programRuleActionType = programRuleActionType;
        this.dataElement = dataElement;
        this.attribute = attribute;
        this.programStageSection = programStageSection;
        this.programStage = programStage;
        this.programIndicator = programIndicator;
        this.location = location;
        this.content = content;
        this.data = data;
        this.option = option;
        this.optionGroup = optionGroup;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasDataElement()
    {
        return this.dataElement != null;
    }

    public boolean hasTrackedEntityAttribute()
    {
        return this.attribute != null;
    }

    public boolean hasContent()
    {
        return this.content != null && !this.content.isEmpty();
    }

    public boolean hasNotification()
    {
        return StringUtils.isNotBlank( this.templateUid );
    }

    public boolean hasOption()
    {
        return this.option != null;
    }

    public boolean hasOptionGroup()
    {
        return this.optionGroup != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramRule getProgramRule()
    {
        return programRule;
    }

    public void setProgramRule( ProgramRule programRule )
    {
        this.programRule = programRule;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramRuleActionType getProgramRuleActionType()
    {
        return programRuleActionType;
    }

    public void setProgramRuleActionType( ProgramRuleActionType programRuleActionType )
    {
        this.programRuleActionType = programRuleActionType;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTemplateUid()
    {
        return templateUid;
    }

    public void setTemplateUid( String programNotificationTemplate )
    {
        this.templateUid = programNotificationTemplate;
    }

    @JsonProperty( "trackedEntityAttribute" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttribute getAttribute()
    {
        return attribute;
    }

    public void setAttribute( TrackedEntityAttribute attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    public void setProgramIndicator( ProgramIndicator programIndicator )
    {
        this.programIndicator = programIndicator;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStageSection getProgramStageSection()
    {
        return programStageSection;
    }

    public void setProgramStageSection( ProgramStageSection programStageSection )
    {
        this.programStageSection = programStageSection;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLocation()
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getContent()
    {
        return content;
    }

    public void setContent( String content )
    {
        this.content = content;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getData()
    {
        return data;
    }

    public void setData( String data )
    {
        this.data = data;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Option getOption()
    {
        return option;
    }

    public void setOption( Option option )
    {
        this.option = option;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OptionGroup getOptionGroup()
    {
        return optionGroup;
    }

    public void setOptionGroup( OptionGroup optionGroup )
    {
        this.optionGroup = optionGroup;
    }
}
