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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.jdbc.StatementBuilder;

/**
 * @author Lars Helge Overland
 */
public abstract class AbstractStatementBuilder
    implements StatementBuilder
{
    static final String AZaz = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static final String AZaz09 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    static final String AZaz_QUOTED = QUOTE + AZaz + QUOTE;
    static final String AZaz09_QUOTED = QUOTE + AZaz09 + QUOTE;

    @Override
    public String encode( String value )
    {
        return encode( value, true );
    }

    @Override
    public String encode( String value, boolean quote )
    {
        if ( value != null )
        {
            value = value.endsWith( "\\" ) ? value.substring( 0, value.length() - 1 ) : value;
            value = value.replaceAll( QUOTE, QUOTE + QUOTE );
        }

        return quote ? (QUOTE + value + QUOTE) : value;
    }

    @Override
    public String columnQuote( String column )
    {
        String qte = getColumnQuote();
        
        column = column.replaceAll( qte, ( qte + qte ) );
        
        return column != null ? ( qte + column + qte ) : null;
    }

    @Override
    public String limitRecord( int offset, int limit )
    {
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String getAutoIncrementValue()
    {
        return "null";
    }

    @Override
    public String getLongVarBinaryType()
    {
        return "VARBINARY(1000000)";
    }

    @Override
    public String concatenate( String... s )
    {
        return "CONCAT(" + StringUtils.join( s, ", " ) + ")";
    }

    @Override
    public String position( String substring, String string )
    {
        return ("POSITION(" + substring + " in " + string + ")");
    }

    @Override
    public String getUid()
    {
        return concatenate(
            getCharAt( AZaz_QUOTED , "1 + " + getRandom( AZaz.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ),
            getCharAt( AZaz09_QUOTED, "1 + " + getRandom( AZaz09.length() ) ) );
    }

    @Override
    public String getNumberOfColumnsInPrimaryKey( String table )
    {
        return
            "select count(cu.column_name) from information_schema.key_column_usage cu " +
                "inner join information_schema.table_constraints tc  " +
                "on cu.constraint_catalog=tc.constraint_catalog " +
                "and cu.constraint_schema=tc.constraint_schema " +
                "and cu.constraint_name=tc.constraint_name " +
                "and cu.table_schema=tc.table_schema " +
                "and cu.table_name=tc.table_name " +
                "where tc.constraint_type='PRIMARY KEY' " +
                "and cu.table_name='" + table + "';";
    }
    
    @Override
    public String getCastToDate( String column )
    {
        return "cast(" + column + " as date)";
    }
    
    @Override
    public String getDaysBetweenDates( String fromColumn, String toColumn )
    {
        return "datediff(" + toColumn + ", " + fromColumn + ")";
    }

    @Override
    public String getDropPrimaryKey( String table )
    {
        return "alter table " + table + " drop primary key;";
    }

    @Override
    public String getAddPrimaryKeyToExistingTable( String table, String column )
    {
        return "alter table " + table + " add column " + column + " integer auto_increment primary key not null;";
    }

    @Override
    public String getDropNotNullConstraint( String table, String column, String type )
    {
        return "alter table " + table + " modify column " + column + " " + type + " null;";
    }
}
