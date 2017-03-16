package org.hisp.dhis.analytics;

import java.util.Date;

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

/**
 * @author Lars Helge Overland
 */
public class AnalyticsTableColumn
{
    private String name;
    
    private String dataType;
    
    private String alias;
    
    private Date created;
    
    private boolean skipIndex = false;
    
    private String indexType;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param name analytics table column name.
     * @param dataType analytics table column data type.
     * @param alias source table column alias and name.
     */
    public AnalyticsTableColumn( String name, String dataType, String alias )
    {
        this.name = name;
        this.dataType = dataType;
        this.alias = alias;
    }

    /**
     * @param name analytics table column name.
     * @param dataType analytics table column data type.
     * @param alias source table column alias and name.
     * @param created date when column data was created.
     */
    public AnalyticsTableColumn( String name, String dataType, String alias, Date created )
    {
        this.name = name;
        this.dataType = dataType;
        this.alias = alias;
        this.created = created;
    }

    /**
     * @param name analytics table column name.
     * @param dataType analytics table column data type.
     * @param alias source table column alias and name.
     * @param skipIndex indicates whether to skip indexing this column.
     */
    public AnalyticsTableColumn( String name, String dataType, String alias, boolean skipIndex )
    {
        this.name = name;
        this.dataType = dataType;
        this.alias = alias;
        this.skipIndex = skipIndex;
    }

    /**
     * @param name analytics table column name.
     * @param dataType analytics table column data type.
     * @param alias source table column alias and name.
     * @param skipIndex indicates whether to skip indexing this column.
     * @param indexType index type.
     */
    public AnalyticsTableColumn( String name, String dataType, String alias, boolean skipIndex, String indexType )
    {
        this.name = name;
        this.dataType = dataType;
        this.alias = alias;
        this.skipIndex = skipIndex;
        this.indexType = indexType;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public String getDataType()
    {
        return dataType;
    }

    public String getAlias()
    {
        return alias;
    }

    public Date getCreated()
    {
        return created;
    }

    public boolean isSkipIndex()
    {
        return skipIndex;
    }

    public String getIndexType()
    {
        return indexType;
    }
}
