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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Enrico Colasante
 */
@Data
@Builder
public class TrackerWarningReport
{
    private final String warningMessage;

    private TrackerErrorCode warningCode;

    private final TrackerType trackerType;

    private final String uid;

    @JsonCreator
    public TrackerWarningReport( @JsonProperty( "message" ) String warningMessage,
        @JsonProperty( "errorCode" ) TrackerErrorCode warningCode,
        @JsonProperty( "trackerType" ) TrackerType trackerType, @JsonProperty( "uid" ) String uid )
    {
        this.warningMessage = warningMessage;
        this.warningCode = warningCode;
        this.trackerType = trackerType;
        this.uid = uid;
    }

    @JsonProperty
    public TrackerErrorCode getWarningCode()
    {
        return warningCode;
    }

    @JsonProperty
    public String getMessage()
    {
        return warningMessage;
    }

    @JsonProperty
    public TrackerType getTrackerType()
    {
        return trackerType;
    }

    @JsonProperty
    public String getUid()
    {
        return uid;
    }

    public static class TrackerWarningReportBuilder
    {
        private final List<Object> arguments = new ArrayList<>();

        public TrackerWarningReportBuilder addArg( Object arg )
        {
            this.arguments.add( arg );
            return this;
        }

        public TrackerWarningReportBuilder addArg( String arg )
        {
            this.arguments.add( arg );
            return this;
        }

        public TrackerWarningReportBuilder addArg( Instant instant )
        {
            // TODO EnrollmendDateValidationHook uses E1025 and E1025 for
            // malformed and null occuredAt, enrolledAt
            // EventDateValidationHook uses errors like E1031 without args to
            // report a required instant that is null.
            if ( instant == null )
            {
                this.arguments.add( "" );
                return this;
            }
            this.arguments.add( DateFormat.getInstance().format( Date.from( instant ) ) );
            return this;
        }

        public TrackerWarningReportBuilder addArg( Date date )
        {
            this.arguments.add( DateFormat.getInstance().format( date ) );
            return this;
        }

        public TrackerWarningReportBuilder addArg( TrackerIdScheme idScheme, IdentifiableObject arg )
        {
            final TrackerIdentifier identifier = TrackerIdentifier.builder().idScheme( idScheme ).build();
            this.arguments.add( identifier.getIdAndName( arg ) );
            return this;
        }

        public TrackerWarningReportBuilder addArg( TrackerDto dto )
        {
            this.arguments.add( dto.getClass().getSimpleName() + " (" + dto.getUid() + ")" );
            return this;
        }

        public TrackerWarningReportBuilder addArgs( Object... args )
        {
            this.arguments.addAll( Arrays.asList( args ) );
            return this;
        }

        public TrackerWarningReport build()
        {
            return new TrackerWarningReport(
                MessageFormat.format( warningCode.getMessage(), arguments.toArray( new Object[0] ) ),
                this.warningCode, this.trackerType, this.uid );
        }
    }

    @Override
    public String toString()
    {
        return "TrackerWarningReport{" +
            "message=" + warningMessage +
            ", warningCode=" + warningCode +
            '}';
    }
}
