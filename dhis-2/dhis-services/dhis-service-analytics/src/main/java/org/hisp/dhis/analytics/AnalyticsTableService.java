package org.hisp.dhis.analytics;

import org.hisp.dhis.analytics.table.AnalyticsTableType;

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

import org.hisp.dhis.scheduling.TaskId;

/**
 * Service for analytics table generation and analysis.
 * 
 * @author Lars Helge Overland
 */
public interface AnalyticsTableService
{
    /**
     * Returns the {@link AnalyticsTableType} of analytics table which this manager handles.
     * 
     * @return the type of analytics table.
     */
    AnalyticsTableType getAnalyticsTableType();
    
    /**
     * Rebuilds the analytics tables.
     * 
     * @param lastYears the number of last years of data to include, null if all.
     * @param taskId the {@link TaskId}.
     */
    void update( Integer lastYears, TaskId taskId );
    
    /**
     * Drops main and temporary analytics tables.
     */
    void dropTables();

    /**
     * Performs an SQL analyze operation on all analytics tables.
     */
    void analyzeAnalyticsTables();
}
