package org.hisp.dhis.common;

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
import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
public class GridHeader
{
    private static final ImmutableSet<String> NUMERIC_TYPES = 
        ImmutableSet.of( Float.class.getName(), Double.class.getName(), Long.class.getName(), Integer.class.getName() );

    private String name;

    private String column;

    private ValueType valueType;
    
    private String type;

    private boolean hidden;

    private boolean meta;

    private String optionSet;

    private String legendSet;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public GridHeader()
    {
    }

    /**
     * @param name   name
     */
    public GridHeader( String name )
    {
        this.name = name;
        this.type = String.class.getName();
        this.hidden = false;
        this.meta = false;
    }

    /**
     * @param name name
     * @param column column
     */
    public GridHeader( String name, String column )
    {
        this( name );
        this.column = column;
    }

    /**
     * Sets the column property to the name value. Sets the type property to
     * String.
     *
     * @param name name
     * @param hidden hidden
     * @param meta meta
     */
    public GridHeader( String name, boolean hidden, boolean meta )
    {
        this( name );
        this.column = name;
        this.hidden = hidden;
        this.meta = meta;
    }

    /**
     * @param name name
     * @param column column
     * @param valueType valueType
     * @param type type
     * @param hidden hidden
     * @param meta meta
     */
    public GridHeader( String name, String column, ValueType valueType, String type, boolean hidden, boolean meta )
    {
        this( name, column );
        this.valueType = valueType;
        this.type = type;
        this.hidden = hidden;
        this.meta = meta;
    }

    /**
     * @param name name
     * @param column column
     * @param type type
     * @param hidden hidden
     * @param meta meta
     * @param optionSet optionSet
     * @param legendSet legendSet
     */
    public GridHeader( String name, String column, ValueType valueType, String type, boolean hidden, boolean meta, String optionSet, String legendSet )
    {
        this( name, column, valueType, type, hidden, meta );
        this.optionSet = optionSet;
        this.legendSet = legendSet;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isNumeric()
    {
        return type != null && NUMERIC_TYPES.contains( type );
    }

    public boolean hasLegendSet()
    {
        return legendSet != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public String getColumn()
    {
        return column;
    }

    public void setColumn( String column )
    {
        this.column = column;
    }

    @JsonProperty
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    @JsonProperty
    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden( boolean hidden )
    {
        this.hidden = hidden;
    }

    @JsonProperty
    public boolean isMeta()
    {
        return meta;
    }

    public void setMeta( boolean meta )
    {
        this.meta = meta;
    }

    @JsonProperty
    public String getOptionSet()
    {
        return optionSet;
    }

    public void setOptionSet( String optionSet )
    {
        this.optionSet = optionSet;
    }

    @JsonProperty
    public String getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( String legendSet )
    {
        this.legendSet = legendSet;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals, toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        final GridHeader other = (GridHeader) object;

        return name.equals( other.name );
    }

    @Override
    public String toString()
    {
        return "[Name: " + name + ", column: " + column + ", value type: " + valueType + ", type: " + type + "]";
    }
}
