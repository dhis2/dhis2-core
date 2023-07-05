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
package org.hisp.dhis.db.migration.helper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jan Bernitt
 */
public class UniqueValueUtils
{
    private static final Logger log = LoggerFactory.getLogger( UniqueValueUtils.class );

    @Value
    @Accessors( fluent = true )
    @AllArgsConstructor
    public static class UniqueValueParams
    {
        @Nonnull
        String table;

        @Nonnull
        String idColumn;

        @Nonnull
        String srcColumn;

        @Nonnull
        String destColumn;

        int destColumnMaxLength;

        boolean keepNull;

        public UniqueValueParams( String table, String idColumn, String column, int columnMaxLength )
        {
            this( table, idColumn, column, columnMaxLength, false );
        }

        public UniqueValueParams( String table, String idColumn, String column, int columnMaxLength, boolean keepNull )
        {
            this( table, idColumn, column, column, columnMaxLength, keepNull );
        }

        public UniqueValueParams( String table, String idColumn, String srcColumn,
            String destColumn, int destColumnMaxLength )
        {
            this( table, idColumn, srcColumn, destColumn, destColumnMaxLength, false );
        }
    }

    /**
     * Finds a value with the provided maximum length that is not already
     * contained in the provided set of unique values, adds it to the set and
     * returns it.
     *
     * Preconditions are: the provided set does not contain duplicates and the
     * number of entries in the set is smaller than the largest decimal number
     * that has equal or less digits then the maximum length.
     *
     * @param value the value we would like to add to the set given it is
     *        unique, otherwise we try to make it unique
     * @param maxLength the maximum number of characters the value is allowed to
     *        have
     * @param values the set of unique values the provided value should be added
     *        to
     * @return the found unique value, which also was added to the set
     */
    static String addValue( @CheckForNull String value, int maxLength, @Nonnull Set<String> values )
    {
        String unique = value == null ? "" : value;
        if ( unique.length() > maxLength )
        {
            unique = unique.substring( 0, maxLength );
        }
        if ( !values.contains( unique ) && value != null )
        {
            values.add( unique );
            return unique;
        }
        for ( int i = 1; i <= values.size() + 1; i++ )
        {
            String nr = String.valueOf( i );
            String attempt = unique.length() + nr.length() <= maxLength
                ? unique + nr
                : unique.substring( 0, maxLength - nr.length() ) + nr;
            if ( !values.contains( attempt ) )
            {
                values.add( attempt );
                return attempt;
            }
        }
        return unique;
    }

    public static void updateUniqueValue( Context context, UniqueValueParams params )
        throws SQLException
    {
        Map<Long, String> srcById = new HashMap<>();
        Map<Long, String> destById = new HashMap<>();
        Set<String> uniqueDestValues = new HashSet<>();
        try ( Statement statement = context.getConnection().createStatement() )
        {
            ResultSet results = statement.executeQuery( String.format( "select %s, %s from %s;",
                params.idColumn(), params.destColumn(), params.table() ) );
            while ( results.next() )
            {
                long id = results.getLong( 1 );
                String value = results.getString( 2 );
                srcById.put( id, value );
                if ( value == null && params.keepNull() )
                {
                    destById.put( id, null );
                }
                else if ( value != null && !uniqueDestValues.contains( value ) )
                {
                    uniqueDestValues.add( value );
                    destById.put( id, value );
                }
            }
        }
        updateNonUniqueValues( context, params, srcById, destById, uniqueDestValues );
    }

    /**
     * We cannot change existing script java files because of the checksum
     * flyway computes so this method needs to exist as an adapter to the new
     * {@link UniqueValueParams} variant.
     */
    public static void copyUniqueValue( Context context, String table, String idColumn, String srcColumn,
        String destColumn, int destColumnMaxLength )
        throws SQLException
    {
        copyUniqueValue( context,
            new UniqueValueParams( table, idColumn, srcColumn, destColumn, destColumnMaxLength ) );
    }

    /**
     * Copies values from the source column to a destination column while making
     * sure they are unique and within a maximum length for the destination
     * column.
     */
    public static void copyUniqueValue( Context context, UniqueValueParams params )
        throws SQLException
    {
        if ( params.destColumn().equals( params.srcColumn() ) )
        {
            updateUniqueValue( context, params );
            return;
        }
        Map<Long, String> srcById = new HashMap<>();
        Map<Long, String> destById = new HashMap<>();
        Set<String> uniqueDestValues = new HashSet<>();
        try ( Statement statement = context.getConnection().createStatement() )
        {
            ResultSet results = statement.executeQuery( String.format( "select %s, %s, %s from %s;",
                params.idColumn(), params.srcColumn(), params.destColumn(), params.table() ) );
            while ( results.next() )
            {
                long id = results.getLong( 1 );
                srcById.put( id, results.getString( 2 ) );
                String destValue = results.getString( 3 );
                if ( destValue == null && params.keepNull() )
                {
                    destById.put( id, null );
                }
                else if ( destValue != null && !uniqueDestValues.contains( destValue ) )
                {
                    uniqueDestValues.add( destValue );
                    destById.put( id, destValue );
                }
            }
        }
        updateNonUniqueValues( context, params, srcById, destById, uniqueDestValues );
    }

    @SuppressWarnings( "java:S107" )
    private static void updateNonUniqueValues( Context context, UniqueValueParams params, Map<Long, String> srcById,
        Map<Long, String> destById, Set<String> uniqueDestValues )
        throws SQLException
    {
        if ( srcById.isEmpty() || destById.size() == srcById.size() )
        {
            // either no entries or all entries already have a unique name
            return;
        }
        String updateTemplate = String.format( "update %s set %s = ? where %s = ?",
            params.table(), params.destColumn(), params.idColumn() );
        for ( Entry<Long, String> idAndSrcValue : srcById.entrySet() )
        {
            long id = idAndSrcValue.getKey();
            String srcValue = idAndSrcValue.getValue();
            if ( !destById.containsKey( id ) )
            {
                String destValue = addValue( srcValue, params.destColumnMaxLength(), uniqueDestValues );
                try ( PreparedStatement statement = context.getConnection().prepareStatement( updateTemplate ) )
                {
                    statement.setLong( 2, id );
                    statement.setString( 1, destValue );

                    if ( log.isInfoEnabled() )
                    {
                        log.info( String.format( "Executing %s => %s migration update: [%s]",
                            params.srcColumn(), params.destColumn(), statement ) );
                    }
                    statement.executeUpdate();
                }
                catch ( SQLException ex )
                {
                    log.error( ex.getMessage() );
                    throw ex;
                }
            }
        }
    }

    private UniqueValueUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }

}
