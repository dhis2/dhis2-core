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

import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class ValidationErrorReporter
{
    private final List<TrackerErrorReport> reportList;

    private final boolean isFailFast;

    private TrackerBundle bundle;

    private Class<?> mainKlass;

    private AtomicInteger listIndex = new AtomicInteger( 0 );

    private String mainId;

    public ValidationErrorReporter( TrackerBundle bundle, Class<?> mainKlass )
    {
        this.bundle = bundle;
        this.reportList = new ArrayList<>();
        this.isFailFast = bundle.getValidationMode() == ValidationMode.FAIL_FAST;
        this.mainKlass = mainKlass;
    }

    private ValidationErrorReporter( TrackerBundle bundle, Class<?> mainKlass, boolean isFailFast, int listIndex )
    {
        this.bundle = bundle;
        this.reportList = new ArrayList<>();
        this.isFailFast = isFailFast;
        this.mainKlass = mainKlass;
        this.listIndex.set( listIndex );
    }

    public List<TrackerErrorReport> getReportList()
    {
        return reportList;
    }

    public int getTotalErrors()
    {
        return reportList.size();
    }

    public boolean isFailFast()
    {
        return isFailFast;
    }

    public static TrackerErrorReport.Builder newReport( TrackerErrorCode errorCode )
    {
        TrackerErrorReport.Builder builder = new TrackerErrorReport.Builder();
        builder.withErrorCode( errorCode );
        return builder;
    }

    public void addError( TrackerErrorReport.Builder builder )
    {
        builder.withMainKlass( this.mainKlass );
        builder.withListIndex( this.listIndex.get() );
        if ( this.mainId != null )
        {
            builder.setMainId( this.mainId );
        }

        getReportList().add( builder.build( this.bundle ) );

        if ( isFailFast() )
        {
            throw new ValidationFailFastException( getReportList() );
        }
    }

    public void increment( Enrollment enrollment )
    {
        listIndex.incrementAndGet();
        this.mainId = (enrollment.getEnrollment() + " (" + enrollment.getClass().getSimpleName() + ")");
    }

    public void increment( Event enrollment )
    {
        listIndex.incrementAndGet();
        this.mainId = (enrollment.getEvent() + " (" + enrollment.getClass().getSimpleName() + ")");
    }

    public void increment( TrackedEntity te )
    {
        listIndex.incrementAndGet();
        this.mainId = (te.getTrackedEntity() + " (" + te.getClass().getSimpleName() + ")");
    }

    public <T extends TrackerDto> void increment( T tei )
    {
        listIndex.incrementAndGet();
        if ( tei instanceof TrackedEntity )
        {
            TrackedEntity te = (TrackedEntity) tei;
            this.mainId = (te.getTrackedEntity() + " (" + te.getClass().getSimpleName() + ")");
        }
    }

    public <T extends TrackerDto> ValidationErrorReporter fork( T dto )
    {
        ValidationErrorReporter fork = new ValidationErrorReporter( this.bundle, this.mainKlass, isFailFast(),
            listIndex.incrementAndGet() );


        // TODO: Use interface method to build name?
        if ( dto instanceof TrackedEntity )
        {
            TrackedEntity te = (TrackedEntity) dto;
            fork.mainId = (te.getTrackedEntity() + " (" + te.getClass().getSimpleName() + ")");
        }
        else if ( dto instanceof Enrollment )
        {
            Enrollment enrollment = (Enrollment) dto;
            fork.mainId = (enrollment.getEnrollment() + " (" + enrollment.getClass().getSimpleName() + ")");
        }
        else if ( dto instanceof Event )
        {
            Event event = (Event) dto;
            fork.mainId = (event.getEvent() + " (" + event.getClass().getSimpleName() + ")");
        }

        return fork;
    }

    public boolean hasErrors()
    {
        return this.reportList.size() > 0;
    }

    public void merge( ValidationErrorReporter reporter )
    {
        this.reportList.addAll( reporter.getReportList() );
    }
}
