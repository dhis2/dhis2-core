package org.hisp.dhis.resourcetable;

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

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.CodeGenerator;

/**
 * @author Lars Helge Overland
 */
public abstract class ResourceTable<T>
{
    protected static final Log log = LogFactory.getLog( ResourceTable.class );
    
    protected static final String TEMP_TABLE_SUFFIX = "_temp";
        
    protected List<T> objects;
    
    protected String columnQuote;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected ResourceTable()
    {
    }
    
    protected ResourceTable( List<T> objects, String columnQuote )
    {
        this.objects = objects;
        this.columnQuote = columnQuote;
    }

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------
    
    public final String getTempTableName()
    {
        return getTableName() + TEMP_TABLE_SUFFIX;
    }
    
    public final String getDropTableStatement()
    {
        return "drop table " + getTableName() + ";";
    }
    
    public final String getDropTempTableStatement()
    {
        return "drop table " + getTempTableName() + ";";
    }
    
    public final String getRenameTempTableStatement()
    {
        return "alter table " + getTempTableName() + " rename to " + getTableName() + ";";
    }

    // -------------------------------------------------------------------------
    // Protected methods
    // -------------------------------------------------------------------------

    protected String getRandomSuffix()
    {
        return CodeGenerator.generateCode( 5 );
    }
    
    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    public abstract String getTableName();
    
    public abstract String getCreateTempTableStatement();
    
    public abstract Optional<String> getPopulateTempTableStatement();
    
    public abstract Optional<List<Object[]>> getPopulateTempTableContent();
    
    /**
     * Returns SQL index create statements for the temp table. Note that the 
     * indexes name must have a random component to avoid uniqueness conflicts.
     * 
     * @return a SQL statement.
     */
    public abstract List<String> getCreateIndexStatements();
}
