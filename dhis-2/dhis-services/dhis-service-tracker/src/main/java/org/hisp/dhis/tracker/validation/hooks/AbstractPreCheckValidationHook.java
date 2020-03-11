package org.hisp.dhis.tracker.validation.hooks;

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

import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;

import java.util.Iterator;
import java.util.List;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
abstract public class AbstractPreCheckValidationHook
    extends AbstractTrackerValidationHook
    implements TrackerValidationHook
{

    abstract public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event );

    abstract public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity );

    abstract public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle,
        Enrollment enrollment );

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        validateTrackerDTOs( reporter, ( tei, r ) -> validateTrackedEntities( r, bundle, tei ),
            bundle.getTrackedEntities() );

        validateTrackerDTOs( reporter, ( enrollment, r ) -> validateEnrollments( r, bundle, enrollment ),
            bundle.getEnrollments() );

        validateTrackerDTOs( reporter, ( event, r ) -> validateEvents( r, bundle, event ),
            bundle.getEvents() );

        return reporter.getReportList();
    }

    public <T extends TrackerDto> void validateTrackerDTOs( ValidationErrorReporter reporter,
        ValidationFunction<T> function, List<T> dtoInstances )
    {
        Iterator<T> iterator = dtoInstances.iterator();

        while ( iterator.hasNext() )
        {
            T dto = iterator.next();

            // Fork the report in order to be thread-safe so we can support multi-threaded validation in future.
            // Iterator needs to be changed to split variant also...
            ValidationErrorReporter reportFork = reporter.fork( dto );

            function.validateObject( dto, reportFork );

            if ( reportFork.hasErrors() )
            {
                iterator.remove();
            }

            reporter.merge( reportFork );
        }
    }
}
