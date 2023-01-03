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
package org.hisp.dhis.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an index on a database table column.
 *
 * @author Lars Helge Overland
 */
public class AnalyticsIndex
{
    /**
     * Table name.
     */
    private String table;

    /**
     * Table column names.
     */
    private List<String> columns = new ArrayList<>();

    /**
     * Index type.
     */
    private IndexType type;

    /**
     * Function to be used by the index, if any
     */
    private IndexFunction function;

    /**
     * @param table table name.
     * @param columns column name.
     * @param type index type.
     */
    public AnalyticsIndex( String table, List<String> columns, IndexType type )
    {
        this.table = table;
        this.columns = columns;
        this.type = type;
    }

    /**
     * @param table table name.
     * @param columns column name.
     * @param type index type.
     */
    public AnalyticsIndex( String table, List<String> columns, IndexType type, IndexFunction function )
    {
        this( table, columns, type );
        this.function = function;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasType()
    {
        return type != null;
    }

    public boolean hasFunction()
    {
        return function != null;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    public String getTable()
    {
        return table;
    }

    public List<String> getColumns()
    {
        return columns;
    }

    public IndexType getType()
    {
        return type;
    }

    public IndexFunction getFunction()
    {
        return function;
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;
        result = prime * result + columns.hashCode();
        result = prime * result + table.hashCode();
        return result;
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

        AnalyticsIndex other = (AnalyticsIndex) object;

        return table.equals( other.table ) && columns.equals( other.columns );
    }
}
