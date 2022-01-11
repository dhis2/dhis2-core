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
package org.hisp.dhis.validation;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.BaseDataDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "validationRule", namespace = DxfNamespaces.DXF_2_0 )
public class ValidationRule
    extends BaseDataDimensionalItemObject implements MetadataObject
{
    /**
     * A description of the ValidationRule.
     */
    private String description;

    /**
     * Instruction to display to user when validation rule is violated.
     */
    private String instruction;

    /**
     * The user-assigned importance of this rule (e.g. high, medium or low).
     */
    private Importance importance = Importance.MEDIUM;

    /**
     * The comparison operator to compare left and right expressions in the
     * rule.
     */
    private Operator operator;

    /**
     * The type of period in which this rule is evaluated.
     */
    private PeriodType periodType;

    /**
     * The left-side expression to be compared against the right side.
     */
    private Expression leftSide;

    /**
     * The right-side expression to be compared against the left side.
     */
    private Expression rightSide;

    /**
     * Skip this rule when validating forms.
     */
    private boolean skipFormValidation;

    /**
     * Validation Rule will only be run for organisation units at these levels
     * (or all levels if set is empty)
     */
    private Set<Integer> organisationUnitLevels = new HashSet<>();

    /**
     * The set of ValidationRuleGroups to which this ValidationRule belongs.
     */
    private Set<ValidationRuleGroup> groups = new HashSet<>();

    /**
     * Notification templates for this ValidationRule
     */
    private Set<ValidationNotificationTemplate> notificationTemplates = new HashSet<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ValidationRule()
    {

    }

    public ValidationRule( String name, String description, Operator operator, Expression leftSide,
        Expression rightSide, boolean skipFormValidation )
    {
        this.name = name;
        this.description = description;
        this.operator = operator;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.skipFormValidation = skipFormValidation;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Clears the left-side and right-side expressions. This can be useful, for
     * example, before changing the validation rule period type, because the
     * data elements allowed in the expressions depend on the period type.
     */
    public void clearExpressions()
    {
        this.leftSide = null;
        this.rightSide = null;
    }

    /**
     * Joins a validation rule group.
     *
     * @param validationRuleGroup the group to join.
     */
    public void addValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        groups.add( validationRuleGroup );
        validationRuleGroup.getMembers().add( this );
    }

    /**
     * Leaves a validation rule group.
     *
     * @param validationRuleGroup the group to leave.
     */
    public void removeValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        groups.remove( validationRuleGroup );
        validationRuleGroup.getMembers().remove( this );
    }

    /**
     * Gets the validation rule description, but returns the validation rule
     * name if there is no description.
     *
     * @return the description (or name).
     */
    public String getDescriptionNameFallback()
    {
        return description != null && !description.trim().isEmpty() ? description : name;
    }

    /**
     * Returns the instruction if it is not null or empty, if not returns the
     * left side description, operator and right side description if not null or
     * empty, if not returns null.
     */
    public String getInstructionFallback()
    {
        if ( instruction != null && !instruction.isEmpty() )
        {
            return instruction;
        }
        else if ( leftSide != null && rightSide != null )
        {
            return leftSide.getDescription() + " " + operator.getMathematicalOperator() + " "
                + rightSide.getDescription();
        }
        else
        {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getInstruction()
    {
        return instruction;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "instruction", key = "INSTRUCTION" )
    public String getDisplayInstruction()
    {
        return getTranslation( "INSTRUCTION", getInstruction() );
    }

    public void setInstruction( String instruction )
    {
        this.instruction = instruction;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Importance getImportance()
    {
        return importance;
    }

    public void setImportance( Importance importance )
    {
        this.importance = importance;
    }

    @JsonProperty
    @JsonSerialize( using = JacksonPeriodTypeSerializer.class )
    @JsonDeserialize( using = JacksonPeriodTypeDeserializer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.TEXT )
    public PeriodType getPeriodType()
    {
        return periodType;
    }

    public void setPeriodType( PeriodType periodType )
    {
        this.periodType = periodType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Operator getOperator()
    {
        return operator;
    }

    public void setOperator( Operator operator )
    {
        this.operator = operator;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.COMPLEX, required = Property.Value.TRUE )
    public Expression getLeftSide()
    {
        return leftSide;
    }

    public void setLeftSide( Expression leftSide )
    {
        this.leftSide = leftSide;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.COMPLEX, required = Property.Value.TRUE )
    public Expression getRightSide()
    {
        return rightSide;
    }

    public void setRightSide( Expression rightSide )
    {
        this.rightSide = rightSide;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipFormValidation()
    {
        return skipFormValidation;
    }

    public void setSkipFormValidation( boolean skipFormValidation )
    {
        this.skipFormValidation = skipFormValidation;
    }

    @JsonProperty( "validationRuleGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "validationRuleGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "validationRuleGroup", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ValidationRuleGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<ValidationRuleGroup> groups )
    {
        this.groups = groups;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlElementWrapper( localName = "notificationTemplates", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ValidationNotificationTemplate> getNotificationTemplates()
    {
        return notificationTemplates;
    }

    public void setNotificationTemplates( Set<ValidationNotificationTemplate> notificationTemplates )
    {
        this.notificationTemplates = notificationTemplates;
    }

    @JsonProperty
    @JsonSerialize( contentAs = IdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlElementWrapper( localName = "organisationUnitLevels", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Integer> getOrganisationUnitLevels()
    {
        return organisationUnitLevels;
    }

    public void setOrganisationUnitLevels(
        Set<Integer> organisationUnitLevels )
    {
        this.organisationUnitLevels = organisationUnitLevels;
    }
}
