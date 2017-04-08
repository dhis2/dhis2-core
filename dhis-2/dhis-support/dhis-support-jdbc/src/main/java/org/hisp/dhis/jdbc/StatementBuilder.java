package org.hisp.dhis.jdbc;

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
 * @version $Id: StatementBuilder.java 5715 2008-09-17 14:05:28Z larshelg $
 */
public interface StatementBuilder
{
    final String QUOTE = "'";

    //--------------------------------------------------------------------------
    // General
    //--------------------------------------------------------------------------
    
    /**
     * Encodes the provided SQL value. Value will be wrapped in quotes.
     * 
     * @param value the value.
     * @return the SQL encoded value.
     */
    String encode( String value );

    /**
     * Encodes the provided SQL value.
     * 
     * @param value the value.
     * @param quote whether to wrap the resulting value in quotes.
     * @return the SQL encoded value.
     */
    String encode( String value, boolean quote );
    
    /**
     * Returns the character used to quote database table and column names.
     * 
     * @return a quote character.
     */
    String getColumnQuote();
    
    /**
     * Wraps the given column or table in quotes.
     * 
     * @param column the column or table name.
     * @return the column or table name wrapped in quotes.
     */
    String columnQuote( String column );
    
    /**
     * Returns a limit and offset clause.
     * 
     * @param offset the offset / start position for the records to return.
     * @param limit the limit on max number of records to return.
     * @return a limit and offset clause.
     */
    String limitRecord( int offset, int limit );
    
    /**
     * Returns the value to use in insert statements for auto-increment columns.
     * @return value to use in insert statements for auto-increment columns.
     */
    String getAutoIncrementValue();
    
    /**
     * Returns statement for vacuum operation for a table. Returns null if 
     * such statement is not relevant.
     * 
     * @param table the table to vacuum.
     * @return vacuum and analyze operations for a table.
     */
    String getVacuum( String table );
    
    /**
     * Returns statement for analytics operation for a table. Returns null if 
     * such statement is not relevant.
     * 
     * @param table the table to analyze.
     * @return statement for analytics operation for a table.
     */
    String getAnalyze( String table );
    
    /**
     * Returns an SQL statement to include in create table statements with applies
     * options to the table. Returns an empty string if all options are set to the
     * default value.
     * 
     * @param autoVacuum whether to enable automatic vacuum, default is true.
     * @return statement part with applies options to the table.
     */
    String getTableOptions( boolean autoVacuum );
    
    /**
     * Returns the name of a double column type.
     */
    String getDoubleColumnType();

    /**
     * Returns the name of a longvar column type.
     */
    String getLongVarBinaryType();

    /**
     * Returns the value used to match a column to a regular expression. Matching
     * is case insensitive.
     */
    String getRegexpMatch();

    /**
     * Returns the regular expression marker for end of a word.
     */
    String getRegexpWordStart();
    
    /**
     * Returns the regular expression marker for start of a word.
     */
    String getRegexpWordEnd();

    /**
     * Returns an expression to concatenate strings
     *
     * @param s the array of strings to concatenate
     * @return the strings, concatenated
     */
    String concatenate( String... s );

    /**
     * Returns the position of substring in string, or 0 if not there.
     *
     * @param substring string to search for
     * @param string string in which to search
     * @return position, or 0 if not found
     */
    String position( String substring, String string );

    /**
     * Returns a function to get a random integer between 0 and n
     *
     * @param n the maximum random value
     * @return the function to return the random integer
     */
    String getRandom( int n );

    /**
     * Returns a function to return 1 character from a string at position n
     *
     * @param str the string to return the character from
     * @param n the position to find the character at
     * @return the function to return the character
     */
    String getCharAt( String str, String n );

    /**
     * Generates a random 11-character UID where the first character is an
     * upper/lower case letter and the remaining 10 characters are a digit
     * or an upper/lower case letter.
     *
     * @return randomly-generated UID.
     */
    String getUid();

    /**
     * Returns the number of columns part of the primary key for the given table.
     */
    String getNumberOfColumnsInPrimaryKey( String table );
    
    /**
     * Returns a cast to timestamp statement for the given column.
     * 
     * @param column the column name.
     * @return a cast to timestamp statement for the given column.
     */
    String getCastToDate( String column );
    
    /**
     * Returns a statement which calculates the number of days between the two
     * given dates or columns of type date.
     * 
     * @param fromColumn the from date column.
     * @param toColumn the to date column.
     * @return statement which calculates the number of days between the given dates.
     */
    String getDaysBetweenDates( String fromColumn, String toColumn );
    
    String getAddDate( String dateField, int days );
    
    String getDropPrimaryKey( String table );
    
    String getAddPrimaryKeyToExistingTable( String table, String column );
    
    String getDropNotNullConstraint( String table, String column, String type );
}
