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

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.util.ObjectUtils;

/**
 * @author Luciano Fiandesio
 */
class TrackerReportUtils
{

    private TrackerReportUtils()
    {
        // not meant to be inherited from
    }

    protected static List<String> buildArgumentList( TrackerBundle bundle, List<Object> arguments )
    {
        final TrackerIdentifier identifier = TrackerIdentifier.builder().idScheme( bundle.getIdentifier() ).build();
        return arguments.stream().map( arg -> parseArgs( identifier, arg ) ).collect( Collectors.toList() );
    }

    private static String parseArgs( TrackerIdentifier identifier, Object argument )
    {
        if ( String.class.isAssignableFrom( ObjectUtils.firstNonNull( argument, "NULL" ).getClass() ) )
        {
            return ObjectUtils.firstNonNull( argument, "NULL" ).toString();
        }
        else if ( IdentifiableObject.class.isAssignableFrom( argument.getClass() ) )
        {
            return identifier.getIdAndName( (IdentifiableObject) argument );
        }
        else if ( Date.class.isAssignableFrom( argument.getClass() ) )
        {
            return (DateFormat.getInstance().format( argument ));
        }
        else if ( Instant.class.isAssignableFrom( argument.getClass() ) )
        {
            return DateFormat.getInstance().format( Date.from( (Instant) argument ) );
        }
        else if ( Enrollment.class.isAssignableFrom( argument.getClass() ) )
        {
            Enrollment enrollment = (Enrollment) argument;
            return enrollment.getClass().getSimpleName() + " (" + enrollment.getEnrollment() + ")";
        }
        else if ( Event.class.isAssignableFrom( argument.getClass() ) )
        {
            Event event = (Event) argument;
            return event.getClass().getSimpleName() + " (" + event.getEvent() + ")";
        }
        else if ( TrackedEntity.class.isAssignableFrom( argument.getClass() ) )
        {
            TrackedEntity entity = (TrackedEntity) argument;
            return entity.getClass().getSimpleName() + " (" + entity.getTrackedEntity() + ")";
        }

        return StringUtils.EMPTY;
    }
}