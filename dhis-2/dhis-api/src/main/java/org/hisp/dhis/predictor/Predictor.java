package org.hisp.dhis.predictor;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.Set;

/**
 * @author Ken Haase
 */
@JacksonXmlRootElement( localName = "Predictor", namespace = DxfNamespaces.DXF_2_0 )
public class Predictor
    extends BaseNameableObject
{
    /**
     * The data element into which the predictor writes
     */
    private DataElement output;

    /**
     * The generator used to compute the value of the predictor.
     */
    private Expression generator;

    /**
     * The type of period in which this rule is evaluated.
     */
    private PeriodType periodType;

    /**
     * When non-empty, this is a boolean valued generator which
     * indicates when this rule should be skipped
     */
    private Expression sampleSkipTest;

    /**
     * The org unit level for which this predictor is defined, if any
     */
    private Set<OrganisationUnitLevel> organisationUnitLevels;

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

    public Predictor()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

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
     * Gets the data sources needed from the target period to evaluate the predictor
     */
    public Set<DataElement> getPresentDataNeeded()
    {
        return generator.getDataElementsInExpression();
    }

    /**
     * Gets the data sources needed from past sample periods to evaluate the predictor
     */
    public Set<DataElement> getSampleDataNeeded()
    {
        return generator.getSampleElementsInExpression();
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getOutput()
    {
        return output;
    }

    public void setOutput( DataElement writes )
    {
        this.output = writes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Expression getGenerator()
    {
        return generator;
    }

    public void setGenerator( Expression expr )
    {
        this.generator = expr;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnitLevel> getOrganisationUnitLevels()
    {
        return organisationUnitLevels;
    }

    public void setOrganisationUnitLevels( Set<OrganisationUnitLevel> organisationUnitLevels )
    {
        this.organisationUnitLevels = organisationUnitLevels;
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
    public Expression getSampleSkipTest()
    {
        return sampleSkipTest;
    }

    public void setSampleSkipTest( Expression sampleSkipTest )
    {
        this.sampleSkipTest = sampleSkipTest;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        // TODO: what else needs to be merged?

        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            Predictor predictor = (Predictor) other;

            if ( mergeMode.isReplace() )
            {
                description = predictor.getDescription();
                output = predictor.getOutput();
                generator = predictor.getGenerator();
                periodType = predictor.getPeriodType();
                sequentialSampleCount = predictor.getSequentialSampleCount();
                sequentialSkipCount = predictor.getSequentialSkipCount();
                annualSampleCount = predictor.getAnnualSampleCount();
                organisationUnitLevels = predictor.getOrganisationUnitLevels();
            }
            else if ( mergeMode.isMerge() )
            {
                description = predictor.getDescription() == null ? description : predictor.getDescription();
                periodType = predictor.getPeriodType() == null ? periodType : predictor.getPeriodType();
                output = predictor.getOutput() == null ? output : predictor.getOutput();
                sequentialSampleCount = predictor.getSequentialSampleCount() == null ? sequentialSampleCount : predictor.getSequentialSampleCount();
                sequentialSkipCount = predictor.getSequentialSkipCount() == null ? sequentialSkipCount : predictor.getSequentialSkipCount();
                annualSampleCount = predictor.getAnnualSampleCount() == null ? annualSampleCount : predictor.getAnnualSampleCount();
                organisationUnitLevels = predictor.getOrganisationUnitLevels() == null ? organisationUnitLevels : predictor.getOrganisationUnitLevels();
            }

            if ( generator != null && predictor.getGenerator() != null )
            {
                generator.mergeWith( predictor.getGenerator() );
            }

            if ( sampleSkipTest != null && predictor.getSampleSkipTest() != null )
            {
                sampleSkipTest.mergeWith( predictor.getSampleSkipTest() );
            }
        }
    }

    /**
     * Clears the generator and skipTest expressions. This can be useful, for
     * example, before changing the validation rule period type, because the
     * data elements allowed in the expressions depend on the period type.
     */
    public void clearExpressions()
    {
        this.generator = null;
        this.sampleSkipTest = null;
    }
}
