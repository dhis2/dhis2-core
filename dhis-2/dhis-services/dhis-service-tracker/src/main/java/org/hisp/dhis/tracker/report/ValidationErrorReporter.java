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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import lombok.Value;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

/**
 * A class that collects {@link TrackerErrorReport} during the validation
 * process.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Value
// TODO: should this be "ValidationReporter" since it does not only report
// errors ?
public class ValidationErrorReporter
{
    private final List<TrackerErrorReport> reportList;

    private final List<TrackerWarningReport> warningsReportList;

    private final boolean isFailFast;

    private final TrackerIdSchemeParams idSchemes;

    /*
     * A map that keep tracks of all the invalid Tracker objects encountered
     * during the validation process
     */
    private final Map<TrackerType, List<String>> invalidDTOs;

    /**
     * Create a {@link ValidationErrorReporter} reporting all errors and
     * warnings with identifiers in given idSchemes.
     * {@link #addError(TrackerErrorReport)} will only throw a
     * {@link ValidationFailFastException} if {@code failFast} true is given.
     *
     * @param idSchemes idSchemes in which to report errors and warnings
     * @param failFast
     */
    public ValidationErrorReporter( TrackerIdSchemeParams idSchemes, boolean failFast )
    {
        this.reportList = new ArrayList<>();
        this.warningsReportList = new ArrayList<>();
        this.invalidDTOs = new HashMap<>();
        this.idSchemes = idSchemes;
        this.isFailFast = failFast;
    }

    /**
     * Create a {@link ValidationErrorReporter} reporting all errors and
     * warnings ({@link #isFailFast} = false) with identifiers in given
     * idSchemes. {@link #addError(TrackerErrorReport)} will not throw a
     * {@link ValidationFailFastException}.
     *
     * @param idSchemes idSchemes in which to report errors and warnings
     */
    public ValidationErrorReporter( TrackerIdSchemeParams idSchemes )
    {
        this.reportList = new ArrayList<>();
        this.warningsReportList = new ArrayList<>();
        this.invalidDTOs = new HashMap<>();
        this.idSchemes = idSchemes;
        this.isFailFast = false;
    }

    public boolean hasErrors()
    {
        return !this.reportList.isEmpty();
    }

    public boolean hasErrorReport( Predicate<TrackerErrorReport> test )
    {
        return reportList.stream().anyMatch( test );
    }

    public boolean hasWarningReport( Predicate<TrackerWarningReport> test )
    {
        return warningsReportList.stream().anyMatch( test );
    }

    public boolean hasWarnings()
    {
        return !this.warningsReportList.isEmpty();
    }

    public void addError( TrackerDto dto, TrackerErrorCode code, Object... args )
    {
        TrackerErrorReport error = TrackerErrorReport.builder()
            .uid( dto.getUid() )
            .trackerType( dto.getTrackerType() )
            .errorCode( code )
            .addArgs( args )
            .build( idSchemes );
        addError( error );
    }

    public void addError( TrackerErrorReport error )
    {
        getReportList().add( error );
        this.invalidDTOs.computeIfAbsent( error.getTrackerType(), k -> new ArrayList<>() ).add( error.getUid() );

        if ( isFailFast() )
        {
            throw new ValidationFailFastException( getReportList() );
        }
    }

    public void addWarning( TrackerWarningReport warning )
    {
        getWarningsReportList().add( warning );
    }

    /**
     * Checks if the provided uid and Tracker Type is part of the invalid
     * entities
     */
    public boolean isInvalid( TrackerType trackerType, String uid )
    {
        return this.invalidDTOs.getOrDefault( trackerType, new ArrayList<>() ).contains( uid );
    }

    public boolean isInvalid( TrackerDto dto )
    {
        return this.isInvalid( dto.getTrackerType(), dto.getUid() );
    }

    public void addWarning( TrackerDto dto, TrackerErrorCode code, Object... args )
    {
        TrackerWarningReport warn = TrackerWarningReport.builder()
            .uid( dto.getUid() )
            .trackerType( dto.getTrackerType() )
            .warningCode( code )
            .addArgs( args )
            .build( idSchemes );
        addWarning( warn );
    }

    public void addWarningIf( BooleanSupplier expression, TrackerDto dto, TrackerErrorCode code, Object... args )
    {
        if ( expression.getAsBoolean() )
        {
            addWarning( dto, code, args );
        }
    }

    public void addErrorIf( BooleanSupplier expression, TrackerDto dto, TrackerErrorCode code, Object... args )
    {
        if ( expression.getAsBoolean() )
        {
            addError( dto, code, args );
        }
    }

    public void addErrorIfNull( Object object, TrackerDto dto, TrackerErrorCode code, Object... args )
    {
        if ( object == null )
        {
            addError( dto, code, args );
        }
    }
}