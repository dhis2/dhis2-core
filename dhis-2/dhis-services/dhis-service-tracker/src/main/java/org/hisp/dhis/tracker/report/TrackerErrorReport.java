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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.util.ObjectUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
public class TrackerErrorReport
{
    private final TrackerErrorMessage message;

    @JsonProperty
    private final Class<?> mainKlass;

    @JsonProperty
    private String mainId;

    @JsonProperty
    private Class<?> errorKlass;

    @JsonProperty
    private final String[] errorProperties;

    @JsonProperty
    private Object value;

    private final int lineNumber;

    public TrackerErrorReport( Class<?> mainKlass, TrackerErrorMessage message, int line, String mainId,
        Class<?> errorKlass, String[] errorProperties, Object value )
    {
        this.mainKlass = mainKlass;
        this.message = message;
        this.lineNumber = line;
        this.mainId = mainId;
        this.errorKlass = errorKlass;
        this.errorProperties = errorProperties;
        this.value = value;
    }

    @JsonProperty
    public TrackerErrorCode getErrorCode()
    {
        return message.getErrorCode();
    }

    @JsonProperty
    public String getMessage()
    {
        return message.getMessage();
    }

    public static class Builder
    {
        protected TrackerErrorMessage message;

        protected Class<?> mainKlass;

        protected String mainId;

        protected Class<?> errorKlass;

        protected String[] errorProperties = new String[0];

        protected Object value;

        protected int listIndex;

        private TrackerErrorCode errorCode;

        private Object mainObject;

        private final List<Object> arguments = new ArrayList<>();

        public Builder withErrorCode( TrackerErrorCode errorCode )
        {
            this.errorCode = errorCode;
            return this;
        }

        public Builder addArg( Object arg )
        {
            this.arguments.add( arg );
            return this;
        }

        protected Builder withMainKlass( Class<?> mainKlass )
        {
            this.mainKlass = mainKlass;
            return this;
        }

        protected Builder withListIndex( int listIndex )
        {
            this.listIndex = listIndex;
            return this;
        }

        public Builder setMainId( String mainId )
        {
            this.mainId = mainId;
            return this;
        }

        public TrackerErrorReport build( TrackerBundle bundle )
        {
            TrackerIdScheme scheme = bundle.getIdentifier();
            TrackerIdentifier identifier = TrackerIdentifier.builder().idScheme( scheme ).build();

            TrackerErrorMessage trackerErrorMessage = new TrackerErrorMessage( this.errorCode );
            for ( Object argument : arguments )
            {
                String s = parseArgs( identifier, argument );
                trackerErrorMessage.addArgument( s );
            }

            if ( this.mainObject != null )
            {
                this.mainId = parseArgs( identifier, this.mainObject );
            }

            return new TrackerErrorReport( this.mainKlass, trackerErrorMessage, this.listIndex, this.mainId,
                this.mainKlass, this.errorProperties, this.value );
        }

        public static String parseArgs( TrackerIdentifier identifier, Object argument )
        {
            if ( String.class.isAssignableFrom( ObjectUtils.firstNonNull( argument, "NULL" ).getClass() ) )
            {
                return (ObjectUtils.firstNonNull( argument, "NULL" ).toString());
            }
            else if ( IdentifiableObject.class.isAssignableFrom( argument.getClass() ) )
            {
                return (identifier.getIdAndName( (IdentifiableObject) argument ));
            }
            else if ( Date.class.isAssignableFrom( argument.getClass() ) )
            {
                return (DateFormat.getInstance().format( argument ));
            }
            else if ( Enrollment.class.isAssignableFrom( argument.getClass() ) )
            {
                Enrollment enrollment = (Enrollment) argument;
                return (enrollment.getEnrollment() + " (" + enrollment.getClass().getSimpleName() + ")");
            }
            else if ( Event.class.isAssignableFrom( argument.getClass() ) )
            {
                Event event = (Event) argument;
                return (event.getEvent() + " (" + event.getClass().getSimpleName() + ")");
            }
            else if ( TrackedEntity.class.isAssignableFrom( argument.getClass() ) )
            {
                TrackedEntity entity = (TrackedEntity) argument;
                return (entity.getTrackedEntity() + " (" + entity.getClass().getSimpleName() + ")");
            }

            return "";
        }
    }

    @Override
    public String toString()
    {
        return "TrackerErrorReport{" +
            "message=" + message.getMessage() +
            ", errorCode=" + message.getErrorCode() +
            ", mainId='" + mainId + '\'' +
            ", mainKlass=" + mainKlass +
            ", errorKlass=" + errorKlass +
            ", errorProperties=" + Arrays.toString( errorProperties ) +
            ", value=" + value +
            ", lineNumber=" + lineNumber +
            '}';
    }
}
