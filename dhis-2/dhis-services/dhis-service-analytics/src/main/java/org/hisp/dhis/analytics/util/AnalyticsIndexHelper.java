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
package org.hisp.dhis.analytics.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.analytics.AnalyticsTableManager.TABLE_TEMP_SUFFIX;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.IndexFunction.LOWER;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.removeQuote;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RegExUtils;
import org.hisp.dhis.analytics.AnalyticsIndex;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.CodeGenerator;

import com.google.common.collect.Lists;

/**
 * Helper class that encapsulates methods responsible for supporting the
 * creation of analytics indexes based on very specific needs.
 *
 * @author maikel arabori
 */
public class AnalyticsIndexHelper
{
    private static final String PREFIX_INDEX = "in_";

    private AnalyticsIndexHelper()
    {
    }

    /**
     * Returns a queue of analytics table indexes.
     *
     * @param partitions the list of {@link AnalyticsTablePartition}.
     * @return a {@link java.util.concurrent.ConcurrentLinkedQueue} of indexes.
     */
    public static List<AnalyticsIndex> getIndexes( List<AnalyticsTablePartition> partitions )
    {
        List<AnalyticsIndex> indexes = new ArrayList<>();

        for ( AnalyticsTablePartition partition : partitions )
        {
            List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();

            for ( AnalyticsTableColumn col : columns )
            {
                if ( !col.isSkipIndex() )
                {
                    List<String> indexColumns = col.hasIndexColumns() ? col.getIndexColumns()
                        : Lists.newArrayList( col.getName() );

                    indexes.add( new AnalyticsIndex( partition.getTempTableName(), indexColumns, col.getIndexType() ) );

                    maybeAddTextLowerIndex( indexes, partition.getTempTableName(), col, indexColumns );
                }
            }
        }

        return indexes;
    }

    /**
     * Based on the given arguments, this method will apply specific logic and
     * return the correct SQL statement for the index creation.
     *
     * @param index the {@link AnalyticsIndex}
     * @param tableType the {@link AnalyticsTableType}
     * @return the SQL index statement
     */
    public static String createIndexStatement( AnalyticsIndex index, AnalyticsTableType tableType )
    {
        final String indexName = getIndexName( index, tableType );
        final String indexColumns = maybeApplyFunctionToIndex( index, join( index.getColumns(), "," ) );

        return "create index " + indexName + " " +
            "on " + index.getTable() + " " +
            "using " + index.getType().keyword() + " (" + indexColumns + ");";
    }

    /**
     * Returns index name for column. Purpose of code suffix is to avoid
     * uniqueness collision between indexes for temporary and real tables.
     *
     * @param index the {@link AnalyticsIndex}
     * @param tableType the {@link AnalyticsTableType}
     */
    public static String getIndexName( AnalyticsIndex index, AnalyticsTableType tableType )
    {
        String columnName = join( index.getColumns(), "_" );

        return quote( maybeSuffixIndexName( index,
            PREFIX_INDEX + removeQuote( columnName ) + "_" + shortenTableName( index.getTable(), tableType )
                + "_" + CodeGenerator.generateCode( 5 ) ) );
    }

    /**
     * If the given "index" has an associated function, this method will wrap
     * the given "columns" into the index function.
     *
     * @param index the {@link AnalyticsIndex}
     * @param indexColumns the columns to be used in the function
     * @return the columns inside the respective function
     */
    private static String maybeApplyFunctionToIndex( AnalyticsIndex index, String indexColumns )
    {
        if ( index.hasFunction() )
        {
            return index.getFunction().value() + "(" + indexColumns + ")";
        }

        return indexColumns;
    }

    /**
     * If the conditions are met, this method adds an index, that uses the
     * "lower" function, into the given list of "indexes". A new index will be
     * added in the following rules are matched:
     *
     * Column data type is TEXT AND "indexColumns" has ONLY one element AND the
     * column name is a valid UID.
     *
     * @param indexes list of {@link AnalyticsIndex}
     * @param tableName the table name of the index
     * @param column the {@link AnalyticsTableColumn}
     * @param indexColumns the columns to be used in the function
     */
    private static void maybeAddTextLowerIndex( List<AnalyticsIndex> indexes, String tableName,
        AnalyticsTableColumn column, List<String> indexColumns )
    {
        String columnName = RegExUtils.removeAll( column.getName(), "\"" );
        boolean isSingleColumn = indexColumns.size() == 1;

        if ( column.getDataType() == TEXT && isValidUid( columnName ) && isSingleColumn )
        {
            indexes.add( new AnalyticsIndex( tableName, indexColumns, column.getIndexType(),
                LOWER ) );
        }
    }

    /**
     * Shortens the given table name.
     *
     * @param table the table name
     * @param tableType
     */
    private static String shortenTableName( String table, AnalyticsTableType tableType )
    {
        table = table.replaceAll( tableType.getTableName(), "ax" );
        table = table.replaceAll( TABLE_TEMP_SUFFIX, EMPTY );

        return table;
    }

    /**
     * If the current index object has an associated function, this method will
     * add a suffix using the function name.
     *
     * @param index
     * @param indexName
     * @return the index name plus the function suffix if any
     */
    private static String maybeSuffixIndexName( AnalyticsIndex index, String indexName )
    {
        if ( index.hasFunction() )
        {
            return indexName + "_" + index.getFunction().value();
        }

        return indexName;
    }
}
