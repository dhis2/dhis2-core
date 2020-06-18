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

import lombok.Data;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    public ValidationErrorReporter( TrackerImportValidationContext context, Class<?> mainKlass )
    {
        this.validationContext = context;
        this.reportList = new ArrayList<>();
        this.warningsReportList = new ArrayList<>();
        this.isFailFast = validationContext.getBundle().getValidationMode() == ValidationMode.FAIL_FAST;
        this.mainKlass = mainKlass;
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
        builder.mainKlass( this.mainKlass );
        builder.listIndex( this.listIndex.get() );

        if ( this.mainId != null )
        {
            builder.mainId( this.mainId );
        }

        getReportList().add( builder.build( this.validationContext.getBundle() ) );

        if ( isFailFast() )
        {
            throw new ValidationFailFastException( getReportList() );
        }
    }

    public void addWarning( TrackerWarningReport.TrackerWarningReportBuilder builder )
    {
        builder.mainKlass( this.mainKlass );
        builder.listIndex( this.listIndex.get() );

        if ( this.mainId != null )
        {
            builder.mainId( this.mainId );
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
            fork.mainId = (trackedEntity.getClass().getSimpleName() + " (" + trackedEntity.getTrackedEntity() + ")");
        }
        else if ( dto instanceof Enrollment )
        {
            Enrollment enrollment = (Enrollment) dto;
            fork.mainId = (enrollment.getClass().getSimpleName() + " (" + enrollment.getEnrollment() + ")");
        }
        else if ( dto instanceof Event )
        {
            Event event = (Event) dto;
            fork.mainId = (event.getClass().getSimpleName() + " (" + event.getEvent() + ")");
        }

        return fork;
    }

    public void merge( ValidationErrorReporter reporter )
    {
        this.reportList.addAll( reporter.getReportList() );
        this.warningsReportList.addAll( reporter.getWarningsReportList() );
    }
}