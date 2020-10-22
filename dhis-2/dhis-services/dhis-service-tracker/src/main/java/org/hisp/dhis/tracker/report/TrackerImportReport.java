package org.hisp.dhis.tracker.report;

import org.apache.commons.lang3.StringUtils;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@NoArgsConstructor
public class TrackerImportReport
{

    private TrackerStatus status = TrackerStatus.OK;

    private TrackerTimingsStats timings = new TrackerTimingsStats();

    private TrackerBundleReport bundleReport = new TrackerBundleReport();

    private TrackerValidationReport trackerValidationReport = new TrackerValidationReport();
    
    private String message;

    @JsonProperty
    public TrackerStats getStats()
    {
        TrackerStats stats = bundleReport.getStats();
        stats.setIgnored( calculateIgnored() );
        return stats;
    }
    
    @JsonProperty
    public TrackerStatus getStatus()
    {
        return status;
    }
    
    @JsonProperty
    public TrackerBundleReport getBundleReport()
    {
        return bundleReport;
    }

    @JsonProperty
    public TrackerTimingsStats getTimings()
    {
        return timings;
    }

    @JsonProperty
    public TrackerValidationReport getTrackerValidationReport()
    {
        return trackerValidationReport;
    }
    
    @JsonProperty
    public String message()
    {
        if ( StringUtils.isEmpty( message ) )
        {
            return getStatus().name();
        }
        
        return message;
    }
    
    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    /**
     * Are there any errors present?
     *
     * @return true or false depending on any errors found in bundle report
     */
    public boolean isEmpty()
    {
        return bundleReport.isEmpty();
    }
    
    private int calculateIgnored()
    {
        return (int) getTrackerValidationReport().getErrorReports().stream()
        .map( TrackerErrorReport::getUid )
        .distinct().count();
    }
}
