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
package org.hisp.dhis.tracker.report;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerValidationReport
{
    @JsonProperty
    @Builder.Default
    private List<TrackerErrorReport> errorReports = new ArrayList<>();

    @JsonProperty
    @Builder.Default
    private List<TrackerWarningReport> warningReports = new ArrayList<>();

    @JsonIgnore
    @Builder.Default
    private List<TrackerValidationHookTimerReport> performanceReport = new ArrayList<>();

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public void add( TrackerValidationReport validationReport )
    {
        add( validationReport.getErrorReports() );
        addWarnings( validationReport.getWarningReports() );
        addPerfReports( validationReport.getPerformanceReport() );
    }

    public void add( ValidationErrorReporter validationReporter )
    {
        add( validationReporter.getReportList() );
        addWarnings( validationReporter.getWarningsReportList() );
    }

    public void add( List<TrackerErrorReport> errorReports )
    {
        for ( TrackerErrorReport errorReport : errorReports )
        {
            addErrorIfNotExisting( errorReport );
        }
    }

    public void addWarnings( List<TrackerWarningReport> warningReportsReports )
    {
        for ( TrackerWarningReport warningReport : warningReportsReports )
        {
            addWarningIfNotExisting( warningReport );
        }
    }

    public void addPerfReports( List<TrackerValidationHookTimerReport> reports )
    {
        this.performanceReport.addAll( reports );
    }

    public void add( TrackerValidationHookTimerReport report )
    {
        performanceReport.add( report );
    }

    public boolean hasErrors()
    {
        return !errorReports.isEmpty();
    }

    /**
     * Returns the size of all the Tracker DTO that did not pass validation
     */
    public long size()
    {

        return this.getErrorReports().stream().map( TrackerErrorReport::getUid ).distinct().count();
    }

    private void addErrorIfNotExisting( TrackerErrorReport report )
    {
        if ( !this.errorReports.contains( report ) )
        {
            this.errorReports.add( report );
        }
    }

    private void addWarningIfNotExisting( TrackerWarningReport report )
    {
        if ( !this.warningReports.contains( report ) )
        {
            this.warningReports.add( report );
        }
    }
}
