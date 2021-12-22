/*
 * Copyright (c) 2004-2021, University of Oslo
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

import lombok.Data;

import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

/**
 * A class that collects {@link TrackerErrorReport} during the validation
 * process.
 *
 * Each {@link TrackerErrorReport} collection is connected to a specific Tracker
 * entity (Tracked Entity, Enrollment, etc.) via the "mainUid" attribute
 *
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
// TODO: should this be "ValidationReporter" since it does not only report
// errors ?
public class ValidationErrorReporter
{
    private final List<TrackerErrorReport> reportList;

    private final List<TrackerWarningReport> warningsReportList;

    private final boolean isFailFast;

    private final TrackerImportValidationContext validationContext;

    /*
     * The Tracker object uid to which this reporter is associated.
     */
    private String mainId;

    /*
     * The type of object associated to this report
     */
    private TrackerType dtoType;

    /*
     * A map that keep tracks of all the invalid Tracker objects encountered
     * during the validation process
     */
    private Map<TrackerType, List<String>> invalidDTOs;

    public static ValidationErrorReporter emptyReporter()
    {
        return new ValidationErrorReporter();
    }

    private ValidationErrorReporter()
    {
        this.warningsReportList = new ArrayList<>();
        this.reportList = new ArrayList<>();
        this.isFailFast = false;
        this.validationContext = null;
        this.invalidDTOs = new HashMap<>();
    }

    public ValidationErrorReporter( TrackerImportValidationContext context )
    {
        this.validationContext = context;
        this.reportList = new ArrayList<>();
        this.warningsReportList = new ArrayList<>();
        this.isFailFast = validationContext.getBundle().getValidationMode() == ValidationMode.FAIL_FAST;
        this.invalidDTOs = new HashMap<>();
    }

    public ValidationErrorReporter( TrackerImportValidationContext context, TrackerDto dto, TrackerType trackerType )
    {
        this( context );
        this.dtoType = trackerType;
        this.mainId = dto.getUid();
    }

    public ValidationErrorReporter( TrackerImportValidationContext context, TrackedEntity trackedEntity )
    {
        this( context );
        this.dtoType = TrackerType.TRACKED_ENTITY;
        this.mainId = trackedEntity.getTrackedEntity();
    }

    public ValidationErrorReporter( TrackerImportValidationContext context, Enrollment enrollment )
    {
        this( context );
        this.dtoType = TrackerType.ENROLLMENT;
        this.mainId = enrollment.getEnrollment();
    }

    public ValidationErrorReporter( TrackerImportValidationContext context, Event event )
    {
        this( context );
        this.dtoType = TrackerType.EVENT;
        this.mainId = event.getEvent();
    }

    public ValidationErrorReporter( TrackerImportValidationContext context, Relationship relationship )
    {
        this( context );
        this.dtoType = TrackerType.RELATIONSHIP;
        this.mainId = relationship.getRelationship();
    }

    public boolean hasErrors()
    {
        return !this.reportList.isEmpty();
    }

    public boolean hasWarnings()
    {
        return !this.warningsReportList.isEmpty();
    }

    public void addError( TrackerErrorReport.TrackerErrorReportBuilder builder )
    {
        builder.trackerType( this.dtoType );

        if ( this.mainId != null )
        {
            builder.uid( this.mainId );
        }

        getReportList().add( builder.build( this.validationContext.getBundle() ) );

        if ( isFailFast() )
        {
            throw new ValidationFailFastException( getReportList() );
        }
    }

    public void addWarning( TrackerWarningReport.TrackerWarningReportBuilder builder )
    {
        builder.trackerType( this.dtoType );

        if ( this.mainId != null )
        {
            builder.uid( this.mainId );
        }
        getWarningsReportList().add( builder.build( this.validationContext.getBundle() ) );
    }

    public void merge( ValidationErrorReporter reporter )
    {
        // add the root invalid object to the map, if invalid
        if ( reporter.getReportList().size() > 0 )
        {
            this.invalidDTOs.computeIfAbsent( reporter.dtoType, k -> new ArrayList<>() ).add( reporter.mainId );

            this.reportList.addAll( reporter.getReportList() );
        }
        this.warningsReportList.addAll( reporter.getWarningsReportList() );
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

    public TrackerPreheat getPreheat()
    {
        return this.getValidationContext().getBundle().getPreheat();
    }
}