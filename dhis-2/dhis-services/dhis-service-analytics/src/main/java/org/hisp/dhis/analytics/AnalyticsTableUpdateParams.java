package org.hisp.dhis.analytics;

import java.util.Set;

import org.hisp.dhis.analytics.table.AnalyticsTableType;
import org.hisp.dhis.scheduling.TaskId;

import com.google.common.base.MoreObjects;

public class AnalyticsTableUpdateParams
{    
    private Integer lastYears;
    
    private boolean skipMasterTable;
    
    boolean skipResourceTables;
    
    private Set<AnalyticsTableType> skipTableTypes;
    
    private TaskId taskId;
    
    // -------------------------------------------------------------------------
    // Get methods
    // -------------------------------------------------------------------------

    public Integer getLastYears()
    {
        return lastYears;
    }

    public boolean isSkipMasterTable()
    {
        return skipMasterTable;
    }

    public boolean isSkipResourceTables()
    {
        return skipResourceTables;
    }

    public Set<AnalyticsTableType> getSkipTableTypes()
    {
        return skipTableTypes;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "last years", lastYears )
            .add( "skip master table", skipMasterTable )
            .add( "skip resource tables", skipResourceTables )
            .add( "skip table types", skipTableTypes )
            .toString();
    }
    
    // -------------------------------------------------------------------------
    // Builder of immutable instances
    // -------------------------------------------------------------------------

    public static Builder newBuilder()
    {
        return new AnalyticsTableUpdateParams.Builder();
    }
    
    /**
     * Builder for {@link DataQueryParams} instances.
     */
    public static class Builder
    {
        private AnalyticsTableUpdateParams params;
        
        protected Builder()
        {
            this.params = new AnalyticsTableUpdateParams();
        }
                
        public Builder withLastYears( Integer lastYears )
        {
            this.params.lastYears = lastYears;
            return this;
        }
        
        public Builder withSkipMasterTable( boolean skipMasterTable )
        {
            this.params.skipMasterTable = true;
            return this;
        }
        
        public Builder withSkipResourceTables( boolean skipResourceTables )
        {
            this.params.skipResourceTables = skipResourceTables;
            return this;
        }
        
        public Builder withSkipTableTypes( Set<AnalyticsTableType> skipTableTypes )
        {
            this.params.skipTableTypes = skipTableTypes;
            return this;
        }
        
        public Builder withTaskId( TaskId taskId )
        {
            this.params.taskId = taskId;
            return this;
        }
        
        public AnalyticsTableUpdateParams build()
        {
            return this.params;
        }
    }
}
