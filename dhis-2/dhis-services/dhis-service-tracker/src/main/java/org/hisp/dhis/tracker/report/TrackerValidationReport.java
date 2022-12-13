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
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ToString
@EqualsAndHashCode
public class TrackerValidationReport
{
    @JsonProperty
    private final List<TrackerErrorReport> errorReports;

    @JsonProperty
    private final List<TrackerWarningReport> warningReports;

    @JsonIgnore
    private final List<Timing> timings;

    public TrackerValidationReport()
    {
        this.errorReports = new ArrayList<>();
        this.warningReports = new ArrayList<>();
        this.timings = new ArrayList<>();
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public void addValidationReport( TrackerValidationReport report )
    {
        addErrors( report.getErrors() );
        addWarnings( report.getWarnings() );
    }

    public List<TrackerErrorReport> getErrors()
    {
        return Collections.unmodifiableList( errorReports );
    }

    public List<TrackerWarningReport> getWarnings()
    {
        return Collections.unmodifiableList( warningReports );
    }

    public TrackerValidationReport addError( TrackerErrorReport error )
    {
        addErrorIfNotExisting( error );
        return this;
    }

    public TrackerValidationReport addErrors( List<TrackerErrorReport> errors )
    {
        for ( TrackerErrorReport error : errors )
        {
            addErrorIfNotExisting( error );
        }
        return this;
    }

    public TrackerValidationReport addWarning( TrackerWarningReport warning )
    {
        addWarningIfNotExisting( warning );
        return this;
    }

    public TrackerValidationReport addWarnings( List<TrackerWarningReport> warnings )
    {
        for ( TrackerWarningReport warning : warnings )
        {
            addWarningIfNotExisting( warning );
        }
        return this;
    }

    public boolean hasErrors()
    {
        return !errorReports.isEmpty();
    }

    public boolean hasError( Predicate<TrackerErrorReport> test )
    {
        return errorReports.stream().anyMatch( test );
    }

    public boolean hasWarnings()
    {
        return !warningReports.isEmpty();
    }

    public boolean hasWarning( Predicate<TrackerWarningReport> test )
    {
        return warningReports.stream().anyMatch( test );
    }

    /**
     * Returns the size of all the Tracker DTO that did not pass validation
     */
    public long size()
    {

        return this.getErrors().stream().map( TrackerErrorReport::getUid ).distinct().count();
    }

    private void addErrorIfNotExisting( TrackerErrorReport error )
    {
        if ( !this.errorReports.contains( error ) )
        {
            this.errorReports.add( error );
        }
    }

    private void addWarningIfNotExisting( TrackerWarningReport warning )
    {
        if ( !this.warningReports.contains( warning ) )
        {
            this.warningReports.add( warning );
        }
    }
}
