package org.hisp.dhis.validation;

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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "validationRule", namespace = DxfNamespaces.DXF_2_0 )
public class ValidationRule
    extends BaseIdentifiableObject
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
     * Whether this is a VALIDATION or MONITORING type rule.
     */
    private RuleType ruleType = RuleType.VALIDATION;

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
     * When non-empty, this is a boolean valued expression which
     * indicates when this rule should be skipped
     */
    private Expression sampleSkipTest;

    /**
     * The set of ValidationRuleGroups to which this ValidationRule belongs.
     */
    private Set<ValidationRuleGroup> groups = new HashSet<>();

    /**
     * The organisation unit level at which this rule is evaluated
     * (Monitoring-type rules only).
     */
    private Integer organisationUnitLevel;

    /**
     * The number of sequential periods from which to collect samples
     * to average (Monitoring-type rules only). Sequential periods are those
     * immediately preceding (or immediately following in previous years) the
     * selected period.
     */
    private Integer sequentialSampleCount;

    /**
     * The number of annual periods from which to collect samples to
     * average (Monitoring-type rules only). Annual periods are from previous
     * years. Samples collected from previous years can also include sequential
     * periods adjacent to the equivalent period in previous years.
     */
    private Integer annualSampleCount;

    /**
     * The number of immediate sequential periods to skip (in the current year)
     * when collecting samples for aggregate functions
     */
    private Integer sequentialSkipCount;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ValidationRule()
    {

    }

    public ValidationRule( String name, String description, Operator operator, Expression leftSide,
        Expression rightSide )
    {
        this.name = name;
        this.description = description;
        this.operator = operator;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.sampleSkipTest = null;
    }

    public ValidationRule( String name, String description, Operator operator, Expression leftSide,
        Expression rightSide, Expression skipTest )
    {
        this.name = name;
        this.description = description;
        this.operator = operator;
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.sampleSkipTest = skipTest;
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
        this.sampleSkipTest = null;
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
     * Gets the data elements to evaluate for the current period. For
     * validation-type rules this means all data elements. For monitoring-type
     * rules this means just the left side elements.
     *
     * @return the data elements to evaluate for the current period.
     */
    public Set<DataElement> getCurrentDataElements()
    {
        Set<DataElement> currentDataElements =
            new HashSet<>( leftSide.getDataElementsInExpression() );

        currentDataElements.addAll( rightSide.getDataElementsInExpression() );
        if ( sampleSkipTest != null )
        {
            currentDataElements.addAll( sampleSkipTest.getDataElementsInExpression() );
        }

        return currentDataElements;
    }

    /**
     * Gets the data elements to compare against for past periods. For
     * validation-type rules this returns null. For monitoring-type rules this
     * is just the right side elements.
     *
     * @return the data elements to evaluate for past periods.
     */
    public Set<DataElement> getPastDataElements()
    {
        HashSet<DataElement> past = new HashSet<DataElement>();
        Set<DataElement> elts = leftSide.getSampleElementsInExpression();

        if ( elts != null )
        {
            past.addAll( elts );
        }

        elts = rightSide.getSampleElementsInExpression();

        if ( elts != null )
        {
            past.addAll( elts );
        }

        if ( sampleSkipTest == null )
        {
            elts = null;
        }
        else
        {
            elts = sampleSkipTest.getSampleElementsInExpression();
        }

        if ( elts != null )
        {
            past.addAll( elts );
        }

        return past;
    }

    /**
     * Indicates whether this validation rule has user groups to alert.
     */
    public boolean hasUserGroupsToAlert()
    {
        for ( ValidationRuleGroup group : groups )
        {
            if ( group.hasUserGroupsToAlert() )
            {
                return true;
            }
        }

        return false;
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 1, max = 999 )
    public Integer getOrganisationUnitLevel()
    {
        return organisationUnitLevel;
    }

    public void setOrganisationUnitLevel( Integer organisationUnitLevel )
    {
        this.organisationUnitLevel = organisationUnitLevel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public RuleType getRuleType()
    {
        return ruleType;
    }

    public void setRuleType( RuleType ruleType )
    {
        this.ruleType = ruleType;
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
    public Integer getSequentialSampleCount()
    {
        return sequentialSampleCount;
    }

    public void setSequentialSampleCount( Integer sequentialSampleCount )
    {
        this.sequentialSampleCount = sequentialSampleCount;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 0, max = 10 )
    public Integer getAnnualSampleCount()
    {
        return annualSampleCount;
    }

    public void setAnnualSampleCount( Integer annualSampleCount )
    {
        this.annualSampleCount = annualSampleCount;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSequentialSkipCount()
    {
        return sequentialSkipCount;
    }

    public void setSequentialSkipCount( Integer sequentialSkipCount )
    {
        this.sequentialSkipCount = sequentialSkipCount;
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
    public Expression getSampleSkipTest()
    {
        return sampleSkipTest;
    }

    public void setSampleSkipTest( Expression sampleSkipTest )
    {
        this.sampleSkipTest = sampleSkipTest;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Expression getRightSide()
    {
        return rightSide;
    }

    public void setRightSide( Expression rightSide )
    {
        this.rightSide = rightSide;
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

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ValidationRule validationRule = (ValidationRule) other;

            if ( mergeMode.isReplace() )
            {
                description = validationRule.getDescription();
                operator = validationRule.getOperator();
                periodType = validationRule.getPeriodType();
            }
            else if ( mergeMode.isMerge() )
            {
                description = validationRule.getDescription() == null ? description : validationRule.getDescription();
                operator = validationRule.getOperator() == null ? operator : validationRule.getOperator();
                periodType = validationRule.getPeriodType() == null ? periodType : validationRule.getPeriodType();
            }

            if ( leftSide != null && validationRule.getLeftSide() != null )
            {
                leftSide.mergeWith( validationRule.getLeftSide() );
            }

            if ( rightSide != null && validationRule.getRightSide() != null )
            {
                rightSide.mergeWith( validationRule.getRightSide() );
            }

            groups.clear();
        }
    }
}
