package org.hisp.dhis.expression;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;

import java.io.Serializable;

/**
 * An Expression is the expression of e.g. a validation rule. It consist of a
 * String representation of the rule as well as references to the data elements
 * and category option combos included in the expression.
 * <p/>
 * The expression can contain numbers and mathematical operators and contain references
 * to data elements and category option combos on the form:
 * <p/>
 * i) [1.2] where 1 refers to the data element identifier and 2 refers to the
 * category option combo identifier.
 * <p/>
 * ii) [1] where 1 refers to the data element identifier, in this case the formula
 * represents the total value for all category option combos for that data element.
 *
 * @author Margrethe Store
 * @version $Id: Expression.java 5011 2008-04-24 20:41:28Z larshelg $
 */
@JacksonXmlRootElement( localName = "expression", namespace = DxfNamespaces.DXF_2_0 )
public class Expression
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -4868682510629094282L;

    public static final String SEPARATOR = ".";
    public static final String EXP_OPEN = "#{";
    public static final String EXP_CLOSE = "}";
    public static final String PAR_OPEN = "(";
    public static final String PAR_CLOSE = ")";

    /**
     * The unique identifier for this Expression.
     */
    private int id;

    /**
     * The Expression.
     */
    private String expression;

    /**
     * A description of the Expression.
     */
    private String description;

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
    public Expression()
    {
    }

    /**
     * @param expression  The expression as a String
     * @param description A description of the Expression.
     */
    public Expression( String expression, String description )
    {
        this.expression = expression;
        this.description = description;
    }

    /**
     * Constructor with all the parameters.
     *
     * @param expression           The expression as a String
     * @param description          A description of the Expression.
     * @param missingValueStrategy Strategy for handling missing values.
     */
    public Expression( String expression, String description,
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

        final Expression other = (Expression) obj;

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
            if ( other.expression != null )
            {
                return false;
            }
        }
        else if ( !expression.equals( other.expression ) )
        {
            return false;
        }

        return true;
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

    public static int matchExpression( String s, int start )
    {
        int i = start, depth = 0, len = s.length();

        while ( i < len )
        {
            char c = s.charAt( i );

            if ( (c == ')') || (c == ']') )
            {
                if ( depth == 0 )
                {
                    return i;
                }
                else
                {
                    depth--;
                }
            }
            else if ( (c == '(') || (c == '[') )
            {
                depth++;
            }
            else if ( c == ',' )
            {
                if ( depth == 0 )
                {
                    return i;
                }
            }
            else
            {
            }

            i++;
        }

        return -1;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
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
}
