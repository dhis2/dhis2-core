package org.hisp.dhis.jdbc.statementbuilder;

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
public class H2StatementBuilder
    extends AbstractStatementBuilder
{
    @Override
    public String getDoubleColumnType()
    {
        return "double";
    }

    @Override
    public String getColumnQuote()
    {
        return "\"";
    }

    @Override
    public String getVacuum( String table )
    {
        return null;
    }

    @Override
    public String getAnalyze( String table )
    {
        return null;
    }

    @Override
    public String getTableOptions( boolean autoVacuum )
    {
        return "";
    }

    @Override
    public String getRegexpMatch()
    {
        return "regexp";
    }

    @Override
    public String getRegexpWordStart() //TODO test
    {
        return "[[:<:]]";
    }

    @Override
    public String getRegexpWordEnd()
    {
        return "[[:>:]]";
    }

    @Override
    public String position( String substring, String string )
    {
        return ("POSITION(" + substring + ", " + string + ")");
    }

    @Override
    public String getRandom( int n )
    {
        return "cast(trunc(" + n + "*random(),0) as int)";
    }

    @Override
    public String getCharAt( String str, String n )
    {
        return "substring(" + str + "," + n + ",1)";
    }

    @Override
    public String getAddDate( String dateField, int days )
    {
        return "DATEADD('DAY'," + days + "," + dateField + ")";
    }

    @Override
    public String getDaysBetweenDates( String fromColumn, String toColumn )
    {
        return ("DATEDIFF('DAY', " + toColumn + ", " + fromColumn + ")");
    }

    @Override
    public String getNumberOfColumnsInPrimaryKey( String table )
    {
        return "select 0 as c"; //TODO fix
    }
}
