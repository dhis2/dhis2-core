/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
<<<<<<< HEAD

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hisp.dhis.tracker.TrackerType;
=======
package org.hisp.dhis.tracker.report;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

import org.hisp.dhis.tracker.TrackerType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
public class TrackerTypeReport
{
    @JsonProperty
    private final TrackerType trackerType;

    @JsonProperty
    private TrackerStats stats = new TrackerStats();

    private Map<Integer, TrackerObjectReport> objectReportMap = new HashMap<>();

    public TrackerTypeReport( TrackerType trackerType )
    {
        this.trackerType = trackerType;
    }

    @JsonProperty
    public List<TrackerObjectReport> getObjectReports()
    {
        return new ArrayList<>( objectReportMap.values() );
    }

<<<<<<< HEAD
    //-----------------------------------------------------------------------------------
=======
    // -----------------------------------------------------------------------------------
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    // Utility Methods
    // -----------------------------------------------------------------------------------

    /**
     * Are there any errors present?
     *
     * @return true or false depending on any errors found
     */
    public boolean isEmpty()
    {
        return getErrorReports().isEmpty();
    }

    public void addObjectReport( TrackerObjectReport objectReport )
    {
        this.objectReportMap.put( objectReport.getIndex(), objectReport );
    }

    public List<TrackerErrorReport> getErrorReports()
    {
        List<TrackerErrorReport> errorReports = new ArrayList<>();
        objectReportMap.values().forEach( objectReport -> errorReports.addAll( objectReport.getErrorReports() ) );

        return errorReports;
    }

    public Map<Integer, TrackerObjectReport> getObjectReportMap()
    {
        return objectReportMap;
    }
}
