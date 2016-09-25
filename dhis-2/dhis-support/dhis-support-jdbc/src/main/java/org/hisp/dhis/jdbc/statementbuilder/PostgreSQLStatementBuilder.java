package org.hisp.dhis.jdbc.statementbuilder;

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

/**
 * @author Lars Helge Overland
 */
public class PostgreSQLStatementBuilder
    extends AbstractStatementBuilder
{
    @Override
    public String getDoubleColumnType()
    {
        return "double precision";
    }

    @Override
    public String getLongVarBinaryType()
    {
        return "BYTEA";
    }

    @Override
    public String getColumnQuote()
    {
        return "\"";
    }

    @Override
    public String getVacuum( String table )
    {
        return "vacuum analyze " + table + ";";
    }
    
    @Override
    public String getAutoIncrementValue()
    {
        return "nextval('hibernate_sequence')";
    }

    @Override
    public String getTableOptions( boolean autoVacuum )
    {
        String sql = "";
        
        if ( !autoVacuum )
        {
            sql += "autovacuum_enabled = false";
        }
        
        if ( !sql.isEmpty() )
        {
            sql = "with (" + sql + ")";
        }
        
        return sql;
    }
    
    @Override
    public String getRegexpMatch()
    {
        return "~*";
    }
    
    @Override
    public String getRegexpWordStart()
    {
        return "\\m";
    }

    @Override
    public String getRegexpWordEnd()
    {
        return "\\M";
    }

    @Override
    public String getRandom( int n )
    {
        return "cast(floor(" + n + "*random()) as int)";
    }

    @Override
    public String getCharAt( String str, String n )
    {
        return "substring(" + str + " from " + n + " for 1)";
    }

    @Override
    public String getAddDate( String dateField, int days )
    {
        return "(" + dateField + "+" + days + ")";
    }
    
    @Override
    public String getDaysBetweenDates( String fromColumn, String toColumn )
    {
        return toColumn + " - " + fromColumn;
    }

    @Override
    public String getDropPrimaryKey( String table )
    {
        return "alter table " + table + " drop constraint " + table + "_pkey;";
    }

    @Override
    public String getAddPrimaryKeyToExistingTable( String table, String column )
    {
        return
            "alter table " + table + " add column " + column + " integer;" +
            "update " + table + " set " + column + " = nextval('hibernate_sequence') where " + column + " is null;" +
            "alter table " + table + " alter column " + column + " set not null;" +
            "alter table " + table + " add primary key(" + column + ");";
    }

    @Override
    public String getDropNotNullConstraint( String table, String column, String type )
    {
        return "alter table " + table + " alter column " + column + " drop not null;";
    }
}
