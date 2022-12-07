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

import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.program.Program;
import org.springframework.util.Assert;

/**
 * Class representing an analytics database table.
 *
 * @author Lars Helge Overland
 */
@Data
@RequiredArgsConstructor
public class AnalyticsTable
{
    /**
     * Analytics table type.
     */
    private final AnalyticsTableType tableType;

    /**
     * Columns representing dimensions.
     */
    private final List<AnalyticsTableColumn> dimensionColumns;

    /**
     * Columns representing values.
     */
    private final List<AnalyticsTableColumn> valueColumns;

    /**
     * Program for analytics tables, applies to events and enrollments.
     */
    private final Program program;

    /**
     * Analytics table partitions for this base analytics table.
     */
    private final List<AnalyticsTablePartition> tablePartitions = new UniqueArrayList<>();

    /**
     * Analytics table views for this base analytics table.
     */
    private final List<AnalyticsTableView> tableViews = new UniqueArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AnalyticsTable()
    {
        this( null, null, null );
    }

    public AnalyticsTable( AnalyticsTableType tableType, List<AnalyticsTableColumn> dimensionColumns,
        List<AnalyticsTableColumn> valueColumns )
    {
        this.tableType = tableType;
        this.dimensionColumns = dimensionColumns;
        this.valueColumns = valueColumns;
        this.program = null;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Adds an analytics partition table to this master table.
     *
     * @param year the year.
     * @param startDate the start date.
     * @param endDate the end date.
     * @return this analytics table.
     */
    public AnalyticsTable addPartitionTable( Integer year, Date startDate, Date endDate )
    {
        Assert.notNull( year, "Year must be specified" );

        AnalyticsTablePartition tablePartition = new AnalyticsTablePartition( this, year, startDate, endDate, false ); // TODO
                                                                                                                       // approval
        this.tablePartitions.add( tablePartition );

        return this;
    }

    /**
     * Adds an analytics view to this master table.
     *
     * @param year the year.
     * @return this analytics table.
     */
    public AnalyticsTable addView( Integer year )
    {
        Assert.notNull( year, "Year must be specified" );

        AnalyticsTableView tableView = new AnalyticsTableView( this, year );

        this.tableViews.add( tableView );

        return this;
    }

    public String getBaseName()
    {
        return tableType.getTableName();
    }

    public String getTableName()
    {
        String name = getBaseName();

        if ( program != null )
        {
            name = PartitionUtils.getTableName( name, program );
        }

        return name;
    }

    public String getTempTableName()
    {
        String name = getBaseName() + AnalyticsTableManager.TABLE_TEMP_SUFFIX;

        if ( program != null )
        {
            name = PartitionUtils.getTableName( name, program );
        }

        return name;
    }

    public boolean hasPartitionTables()
    {
        return !tablePartitions.isEmpty();
    }

    public boolean hasViews()
    {
        return !tableViews.isEmpty();
    }

    public AnalyticsTablePartition getLatestPartition()
    {
        return tablePartitions.stream()
            .filter( AnalyticsTablePartition::isLatestPartition )
            .findAny().orElse( null );
    }

}
