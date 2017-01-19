package org.hisp.dhis.analytics;

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

import java.util.List;
import java.util.Date;

import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsTable
{
    private String baseName;

    private List<AnalyticsTableColumn> dimensionColumns;

    private Period period;

    private Program program;
    
    private Date created;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AnalyticsTable()
    {
        this.created = new Date();
    }

    public AnalyticsTable( String baseName, List<AnalyticsTableColumn> dimensionColumns )
    {
        this.baseName = baseName;
        this.dimensionColumns = dimensionColumns;
        this.created = new Date();
    }

    public AnalyticsTable( String baseName, List<AnalyticsTableColumn> dimensionColumns, Period period )
    {
        this( baseName, dimensionColumns );
        this.period = period;
    }

    public AnalyticsTable( String baseName, List<AnalyticsTableColumn> dimensionColumns, Period period, Program program )
    {
        this( baseName, dimensionColumns, period );
        this.program = program;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public String getTableName()
    {
        String name = baseName;

        if ( period != null )
        {
            name += PartitionUtils.SEP + PeriodType.getCalendar().fromIso( period.getStartDate() ).getYear();
        }

        if ( program != null )
        {
            name += PartitionUtils.SEP + program.getUid().toLowerCase();
        }

        return name;
    }

    public String getTempTableName()
    {
        String name = baseName + AnalyticsTableManager.TABLE_TEMP_SUFFIX;

        if ( period != null )
        {
            name += PartitionUtils.SEP + PeriodType.getCalendar().fromIso( period.getStartDate() ).getYear();
        }

        if ( program != null )
        {
            name += PartitionUtils.SEP + program.getUid().toLowerCase();
        }

        return name;
    }

    public boolean hasPeriod()
    {
        return period != null;
    }

    public boolean hasProgram()
    {
        return program != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getBaseName()
    {
        return baseName;
    }

    public void setBaseName( String baseName )
    {
        this.baseName = baseName;
    }

    public List<AnalyticsTableColumn> getDimensionColumns()
    {
        return dimensionColumns;
    }

    public void setDimensionColumns( List<AnalyticsTableColumn> dimensionColumns )
    {
        this.dimensionColumns = dimensionColumns;
    }

    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    public Date getCreated()
    {
        return created;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals, toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( baseName == null ) ? 0 : baseName.hashCode() );
        result = prime * result + ( ( period == null ) ? 0 : period.hashCode() );
        result = prime * result + ( ( program == null ) ? 0 : program.hashCode() );
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
        
        if ( baseName == null )
        {
            if ( other.baseName != null )
            {
                return false;
            }
        }
        else if ( !baseName.equals( other.baseName ) )
        {
            return false;
        }
        
        if ( period == null )
        {
            if ( other.period != null )
            {
                return false;
            }
        }
        else if ( !period.equals( other.period ) )
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
        
        return true;
    }

    @Override
    public String toString()
    {
        return getTableName();
    }
}
