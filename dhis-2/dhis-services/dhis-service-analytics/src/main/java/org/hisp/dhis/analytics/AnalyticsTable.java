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

import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.springframework.util.Assert;

/**
 * Class representing an analytics database table.
 *
 * @author Lars Helge Overland
 */
public class AnalyticsTable
{
    /**
     * Analytics table type.
     */
    private AnalyticsTableType tableType;

    /**
     * Columns representing dimensions.
     */
    private List<AnalyticsTableColumn> dimensionColumns;

    /**
     * Columns representing values.
     */
    private List<AnalyticsTableColumn> valueColumns;

    /**
     * Program for analytics tables, applies to events and enrollments.
     */
    private Program program;

    private TrackedEntityType trackedEntityType;

    /**
     * Analytics table partitions for this base analytics table.
     */
    private List<AnalyticsTablePartition> tablePartitions = new UniqueArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AnalyticsTable()
    {
    }

    public AnalyticsTable( AnalyticsTableType tableType, List<AnalyticsTableColumn> dimensionColumns,
        List<AnalyticsTableColumn> valueColumns )
    {
        this.tableType = tableType;
        this.dimensionColumns = dimensionColumns;
        this.valueColumns = valueColumns;
    }

    public AnalyticsTable( AnalyticsTableType tableType, List<AnalyticsTableColumn> dimensionColumns,
        List<AnalyticsTableColumn> valueColumns, Program program )
    {
        this( tableType, dimensionColumns, valueColumns );
        this.program = program;
    }

    public AnalyticsTable( AnalyticsTableType tableType, List<AnalyticsTableColumn> dimensionColumns,
        List<AnalyticsTableColumn> valueColumns, TrackedEntityType trackedEntityType )
    {
        this( tableType, dimensionColumns, valueColumns );
        this.trackedEntityType = trackedEntityType;
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
        else if ( trackedEntityType != null )
        {
            name += PartitionUtils.SEP + trackedEntityType.getUid().toLowerCase();
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
        else if ( trackedEntityType != null )
        {
            name += PartitionUtils.SEP + trackedEntityType.getUid().toLowerCase();
        }

        return name;
    }

    public boolean hasProgram()
    {
        return program != null;
    }

    public boolean hasTrackedEntityType()
    {
        return trackedEntityType != null;
    }

    public boolean hasPartitionTables()
    {
        return !tablePartitions.isEmpty();
    }

    public AnalyticsTablePartition getLatestPartition()
    {
        return tablePartitions.stream()
            .filter( AnalyticsTablePartition::isLatestPartition )
            .findAny().orElse( null );
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public AnalyticsTableType getTableType()
    {
        return tableType;
    }

    public List<AnalyticsTableColumn> getDimensionColumns()
    {
        return dimensionColumns;
    }

    public List<AnalyticsTableColumn> getValueColumns()
    {
        return valueColumns;
    }

    public Program getProgram()
    {
        return program;
    }

    public TrackedEntityType getTrackedEntityType()
    {
        return trackedEntityType;
    }

    public List<AnalyticsTablePartition> getTablePartitions()
    {
        return tablePartitions;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals, toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;
        result = prime * result + ((tableType == null) ? 0 : tableType.hashCode());
        result = prime * result + ((program == null) ? 0 : program.hashCode());
        result = prime * result + ((trackedEntityType == null) ? 0 : trackedEntityType.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        AnalyticsTable other = (AnalyticsTable) object;

        if ( tableType == null )
        {
            if ( other.tableType != null )
            {
                return false;
            }
        }
        else if ( tableType != other.tableType )
        {
            return false;
        }

        if ( program == null )
        {
            if ( other.program != null )
            {
                return false;
            }
        }
        else if ( !program.equals( other.program ) )
        {
            return false;
        }

        if ( trackedEntityType == null )
        {
            if ( other.trackedEntityType != null )
            {
                return false;
            }
        }
        else if ( !trackedEntityType.equals( other.trackedEntityType ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "[Table name: " + getTableName() + ", partitions: " + tablePartitions + "]";
    }
}
