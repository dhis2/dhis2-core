package org.hisp.dhis.jdbc.statementbuilder;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Collection;
import java.util.List;

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
        return "vacuum " + table + ";";
    }

    @Override
    public String getAnalyze( String table )
    {
        return "analyze " + table + ";";
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

    /**
     * Generates a derived table containing one column of literal strings.
     *
     * The PostgreSQL implementation returns the following form:
     * <code>
     *     (values ('s1'),('s2'),('s3')) table (column)
     * </code>
     *
     * @param values (non-empty) String values for the derived table
     * @param table the desired table name alias
     * @param column the desired column name
     * @return the derived literal table
     */
    @Override
    public String literalStringTable( Collection<String> values, String table, String column )
    {
        StringBuilder sb = new StringBuilder("(values ");

        for ( String value : values )
        {
            sb.append( "('" ).append( value ).append( "')," );
        }

        return sb.deleteCharAt(sb.length()-1) // Remove the final ','.
            .append(") ").append( table )
            .append(" (").append(column).append(")")
            .toString();
    }

    /**
     * Generates a derived table containing literals in two columns: integer
     * and string.
     *
     * The generic implementation, which works in all supported database
     * types, returns a subquery in the following form:
     * <code>
     *     (values (i1, 's1'),(i2, 's2'),(i3, 's3')) table (intColumn, strColumn)
     * </code>
     *
     * @param intValues (non-empty) Integer values for the derived table
     * @param strValues (same size) String values for the derived table
     * @param table the desired table name alias
     * @param intColumn the desired integer column name
     * @param strColumn the desired string column name
     * @return the derived literal table
     */
    @Override
    public String literalIntStringTable( List<Integer> intValues,
        List<String> strValues, String table, String intColumn, String strColumn )
    {
        StringBuilder sb = new StringBuilder( "(values " );

        for ( int i = 0; i < intValues.size(); i++ )
        {
            sb.append( "(" ).append( intValues.get( i ) ).append( ", '" )
                .append( strValues.get( i ) ).append( "')," );
        }

        return sb.deleteCharAt(sb.length()-1) // Remove the final ','.
            .append( ") " ).append( table )
            .append( " (" ).append( intColumn ).append( ", " )
            .append( strColumn ).append( ")" )
            .toString();
    }

    /**
     * Generates a derived table containing literals in two columns: integer
     * and integer.
     *
     * @param int1Values (non-empty) 1st integer column values for the table
     * @param int2Values (same size) 2nd integer column values for the table
     * @param table the desired table name alias
     * @param int1Column the desired 1st integer column name
     * @param int2Column the desired 2nd integer column name
     * @return the derived literal table
     *
     * The generic implementation, which works in all supported database
     * types, returns a subquery in the following form:
     * <code>
     *     (values (i1_1, i2_1),(i1_2, i2_2),(i1_3, i2_3)) table (int1Column, int2Column)
     * </code>
     */
    @Override
    public String literalIntIntTable( List<Integer> int1Values,
        List<Integer> int2Values, String table, String int1Column, String int2Column )
    {
        StringBuilder sb = new StringBuilder("(values ");

        for ( int i = 0; i < int1Values.size(); i++ )
        {
            sb.append( "(" ).append( int1Values.get( i ) ).append( ", ")
                .append( int2Values.get( i ) ).append( ")," );
        }

        return sb.deleteCharAt(sb.length()-1) // Remove the final ','.
            .append( ") " ).append( table )
            .append( " (" ).append( int1Column ).append( ", " )
            .append( int2Column ).append( ")" )
            .toString();
    }

    @Override
    public boolean supportsPartialIndexes()
    {
        return true;
    }
}
