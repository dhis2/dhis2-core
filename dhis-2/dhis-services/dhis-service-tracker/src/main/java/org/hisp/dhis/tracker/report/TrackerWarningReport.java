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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * @author Enrico Colasante
 */
@Data
@Builder
public class TrackerWarningReport
{
    @JsonProperty
    private final Class<?> mainKlass;

    private final String warningMessage;

    @JsonProperty
    private String mainId;

    @JsonProperty
    private Class<?> warningKlass;

    @JsonProperty
    private final String[] warningProperties;

    @JsonProperty
    private Object value;

    private final int lineNumber;

    private TrackerErrorCode warningCode;

    private Object mainObject;

    protected int listIndex;

    public TrackerWarningReport( Class<?> mainKlass, String warningMessage, TrackerErrorCode warningCode, int line,
        String mainId, Class<?> warningKlass, String[] warningProperties, Object value )
    {
        this.mainKlass = mainKlass;
        this.warningMessage = warningMessage;
        this.warningCode = warningCode;

        this.lineNumber = line;
        this.mainId = mainId;
        this.warningKlass = warningKlass;
        this.warningProperties = warningProperties;
        this.value = value;

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

    public static class TrackerWarningReportBuilder
    {
        private final List<Object> arguments = new ArrayList<>();

        public TrackerWarningReportBuilder addArg( Object arg )
        {
            this.arguments.add( arg );
            return this;
        }

        public TrackerWarningReport build( TrackerBundle bundle )
        {
            TrackerIdScheme scheme = bundle.getIdentifier();
            TrackerIdentifier identifier = TrackerIdentifier.builder().idScheme( scheme ).build();

            List<String> args = new ArrayList<>();
            for ( Object argument : this.arguments )
            {
                String s = parseArgs( identifier, argument );
                args.add( s );
            }

            if ( this.mainObject != null )
            {
                this.mainId = parseArgs( identifier, this.mainObject );
            }

            String warningMessage = MessageFormat.format( warningCode.getMessage(), args.toArray( new Object[0] ) );

            return new TrackerWarningReport( this.mainKlass, warningMessage, this.warningCode, this.listIndex, this.mainId,
                this.mainKlass, this.warningProperties, this.value );
        }

        public static String parseArgs( TrackerIdentifier identifier, Object argument )
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

            return "";
        }
    }

    @Override
    public String toString()
    {
        return "TrackerWarningReport{" +
            "message=" + warningMessage +
            ", warningCode=" + warningCode +
            ", mainId='" + mainId + '\'' +
            ", mainClass=" + mainKlass +
            ", warningClass=" + warningKlass +
            ", warningProperties=" + Arrays.toString( warningProperties ) +
            ", value=" + value +
            ", objectIndex=" + lineNumber +
            '}';
    }
}
