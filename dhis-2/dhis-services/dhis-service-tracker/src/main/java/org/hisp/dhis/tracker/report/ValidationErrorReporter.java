package org.hisp.dhis.tracker.report;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

import lombok.Data;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
public class ValidationErrorReporter
{
    private final List<TrackerErrorReport> reportList;

    private final List<TrackerWarningReport> warningsReportList;

    private final boolean isFailFast;

    private final TrackerImportValidationContext validationContext;

    private final Class<?> mainKlass;

    private final AtomicInteger listIndex = new AtomicInteger( 0 );

    private String mainId;

    private TrackerType dtoType;

    private List<TrackerDto> invalidDTOs;

    public ValidationErrorReporter( TrackerImportValidationContext context, Class<?> mainKlass )
    {
        this.validationContext = context;
        this.reportList = new ArrayList<>();
        this.warningsReportList = new ArrayList<>();
        this.isFailFast = validationContext.getBundle().getValidationMode() == ValidationMode.FAIL_FAST;
        this.mainKlass = mainKlass;
        this.invalidDTOs = new ArrayList<>();
    }

    private ValidationErrorReporter( TrackerImportValidationContext context, Class<?> mainKlass, boolean isFailFast,
        int listIndex )
    {
        this.validationContext = context;
        this.reportList = new ArrayList<>();
        this.warningsReportList = new ArrayList<>();
        this.isFailFast = isFailFast;
        this.mainKlass = mainKlass;
        this.listIndex.set( listIndex );
        this.invalidDTOs = new ArrayList<>();
    }

    public boolean hasErrors()
    {
        return !this.reportList.isEmpty();
    }

    public static TrackerErrorReport.TrackerErrorReportBuilder newReport( TrackerErrorCode errorCode )
    {
        return TrackerErrorReport.builder().errorCode( errorCode );
    }

    public static TrackerWarningReport.TrackerWarningReportBuilder newWarningReport( TrackerErrorCode errorCode )
    {
        return TrackerWarningReport.builder().warningCode( errorCode );
    }

    public void addError( TrackerErrorReport.TrackerErrorReportBuilder builder )
    {
        builder.trackerType( this.dtoType );

        if ( this.mainId != null )
        {
            builder.uid( this.mainId );
        }

        getReportList().add( builder.build( this.validationContext.getBundle() ) );

        // make sure the unpersisted payload entities in the reference map are also made invalid
        this.validationContext.getBundle().getPreheat().invalidateReference( this.mainId );

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

    public ValidationErrorReporter fork()
    {
        return fork( null );
    }

    public <T extends TrackerDto> ValidationErrorReporter fork( T dto )
    {
        ValidationErrorReporter fork = new ValidationErrorReporter( this.validationContext, this.mainKlass,
            isFailFast(),
            listIndex.incrementAndGet() );

        if ( dto == null )
        {
            fork.mainId = this.mainId;
            return fork;
        }

        // TODO: Use interface method to build name?
        if ( dto instanceof TrackedEntity )
        {
            TrackedEntity trackedEntity = (TrackedEntity) dto;
            fork.mainId = trackedEntity.getTrackedEntity();
            fork.dtoType = TrackerType.TRACKED_ENTITY;
        }
        else if ( dto instanceof Enrollment )
        {
            Enrollment enrollment = (Enrollment) dto;
            fork.mainId = enrollment.getEnrollment();
            fork.dtoType = TrackerType.ENROLLMENT;
        }
        else if ( dto instanceof Event )
        {
            Event event = (Event) dto;
            fork.mainId = event.getEvent();
            fork.dtoType = TrackerType.EVENT;
        }
        else if ( dto instanceof Relationship )
        {
            Relationship relationship = (Relationship) dto;
            fork.mainId = relationship.getRelationship();
            fork.dtoType = TrackerType.RELATIONSHIP;
        }
        return fork;
    }

    public void merge( ValidationErrorReporter reporter )
    {
        this.reportList.addAll( reporter.getReportList() );
        this.warningsReportList.addAll( reporter.getWarningsReportList() );
    }

    public void addDtosWithErrors( List<TrackerDto> notValidDTOs )
    {
        this.invalidDTOs.addAll( notValidDTOs );
    }
}