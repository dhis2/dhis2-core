package org.hisp.dhis.analytics;

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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.scheduling.JobConfiguration;

import java.util.Set;

/**
 * Class representing parameters for the analytics table generation process.
 * 
 * @author Lars Helge Overland
 */
public class AnalyticsTableUpdateParams
{
    /**
     * Number of last years for which to update tables.
     */
    private Integer lastYears;
    
    /**
     * Indicates whether to skip update of the master analytics table.
     */
    private boolean skipMasterTable;
    
    /**
     * Indicates whether to skip update of resource tables.
     */
    boolean skipResourceTables;
    
    /**
     * Analytics table types to skip.
     */
    private Set<AnalyticsTableType> skipTableTypes;
    
    /**
     * Job ID.
     */
    private JobConfiguration jobId;
    
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

    public JobConfiguration getJobId()
    {
        return jobId;
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
     * Builder for {@link AnalyticsTableUpdateParams} instances.
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
        
        public Builder withJobId( JobConfiguration jobId )
        {
            this.params.jobId = jobId;
            return this;
        }
        
        public AnalyticsTableUpdateParams build()
        {
            return this.params;
        }
    }
}
