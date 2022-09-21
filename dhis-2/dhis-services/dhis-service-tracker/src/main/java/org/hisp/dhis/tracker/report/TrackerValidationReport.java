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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
=======
package org.hisp.dhis.tracker.report;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.tracker.domain.TrackerDto;

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

<<<<<<< HEAD
    //-----------------------------------------------------------------------------------
=======
    @JsonProperty
    @Builder.Default
    private List<TrackerWarningReport> warningReports = new ArrayList<>();

    @JsonProperty
    @Builder.Default
    private List<TrackerValidationHookTimerReport> performanceReport = new ArrayList<>();

    @JsonProperty
    @Builder.Default
    private List<TrackerDto> invalidDtos = new ArrayList<>();

    // -----------------------------------------------------------------------------------
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public void add( TrackerValidationReport validationReport )
    {
        add( validationReport.getErrorReports() );
        this.warningReports.addAll( validationReport.getWarningReports() );
        addPerfReports( validationReport.getPerformanceReport() );
        this.invalidDtos.addAll( validationReport.getInvalidDtos() );
    }

    public void add( ValidationErrorReporter validationReporter )
    {
        this.errorReports.addAll( validationReporter.getReportList() );
        this.warningReports.addAll( validationReporter.getWarningsReportList() );
        this.invalidDtos.addAll( validationReporter.getInvalidDTOs() );
    }

    public void add( List<TrackerErrorReport> errorReports )
    {
        this.errorReports.addAll( errorReports );
    }

    public void addPerfReports( List<TrackerValidationHookTimerReport> reports )
    {
        this.performanceReport.addAll( reports );
    }
<<<<<<< HEAD
=======

    public void add( TrackerValidationHookTimerReport report )
    {
        performanceReport.add( report );
    }

    public boolean hasErrors()
    {
        return !errorReports.isEmpty();
    }
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
}
