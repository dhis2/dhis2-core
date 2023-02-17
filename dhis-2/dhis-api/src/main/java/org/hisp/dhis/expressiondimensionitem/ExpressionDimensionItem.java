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
package org.hisp.dhis.expressiondimensionitem;

import org.hisp.dhis.common.BaseDataDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.expression.MissingValueStrategy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * An Expression is the expression of e.g. a validation rule. It consist of a
 * String representation of the rule as well as references to the data elements
 * and category option combos included in the expression.
 * <p/>
 * The expression can contain numbers and mathematical operators and contain
 * references to data elements and category option combos on the form:
 * <p/>
 * i) [1.2] where 1 refers to the data element identifier and 2 refers to the
 * category option combo identifier.
 * <p/>
 * ii) [1] where 1 refers to the data element identifier, in this case the
 * formula represents the total value for all category option combos for that
 * data element.
 *
 */
@JacksonXmlRootElement( localName = "expressionDimensionItem", namespace = DxfNamespaces.DXF_2_0 )
public class ExpressionDimensionItem
    extends BaseDataDimensionalItemObject
    implements MetadataObject
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 8342548380669463589L;

    public static final String SEPARATOR = ".";

    /**
     * The Expression.
     */
    private String expression;

    /**
     * This expression should be given sliding window based data
     */
    private Boolean slidingWindow = false;

    /**
     * Indicates whether the expression should evaluate to null if all or any
     * data values are missing in the expression.
     */
    private MissingValueStrategy missingValueStrategy = MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default empty Expression
     */
    public ExpressionDimensionItem()
    {
    }

    /**
     * @param expression The expression as a String
     * @param description A description of the Expression.
     */
    public ExpressionDimensionItem( String expression, String description )
    {
        this.expression = expression;
        this.description = description;
    }

    /**
     * Constructor with all the parameters.
     *
     * @param expression The expression as a String
     * @param description A description of the Expression.
     * @param missingValueStrategy Strategy for handling missing values.
     */
    public ExpressionDimensionItem( String expression, String description,
        MissingValueStrategy missingValueStrategy )
    {

        this.expression = expression;
        this.description = description;
        this.missingValueStrategy = missingValueStrategy;
    }

    // -------------------------------------------------------------------------
    // Equals and hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final ExpressionDimensionItem other = (ExpressionDimensionItem) obj;

        if ( description == null )
        {
            if ( other.description != null )
            {
                return false;
            }
        }
        else if ( !description.equals( other.description ) )
        {
            return false;
        }

        if ( expression == null )
        {
            return other.expression == null;
        }
        else
        {
            return expression.equals( other.expression );
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((expression == null) ? 0 : expression.hashCode());

        return result;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + id + "\", " +
            "\"expression\":\"" + expression + "\", " +
            "\"description\":\"" + description + "\" " +
            "}";
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MissingValueStrategy getMissingValueStrategy()
    {
        return missingValueStrategy;
    }

    public void setMissingValueStrategy( MissingValueStrategy missingValueStrategy )
    {
        this.missingValueStrategy = missingValueStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getSlidingWindow()
    {
        return slidingWindow;
    }

    public void setSlidingWindow( Boolean slidingWindow )
    {
        this.slidingWindow = slidingWindow;
    }

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.EXPRESSION_DIMENSION_ITEM;
    }
}
