/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
<<<<<<< HEAD
import lombok.Data;
import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerErrorMessage;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
<<<<<<< HEAD
=======
@Builder
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
public class TrackerErrorReport
{
    @JsonProperty
    private final Class<?> mainKlass;

<<<<<<< HEAD
    private final TrackerErrorMessage message;
=======
    private final String errorMessage;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

    @JsonProperty
    private String mainId;

    @JsonProperty
    private Class<?> errorKlass;

    @JsonProperty
<<<<<<< HEAD
    private String errorProperty;
=======
    private final String[] errorProperties;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

    @JsonProperty
    private Object value;

    private final int lineNumber;

    private TrackerErrorCode errorCode;

    private Object mainObject;

    protected int listIndex;

    public TrackerErrorReport( Class<?> mainKlass, String errorMessage, TrackerErrorCode errorCode, int line,
        String mainId,
        Class<?> errorKlass, String[] errorProperties, Object value )
    {
        this.mainKlass = mainKlass;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;

        this.lineNumber = line;
        this.mainId = mainId;
        this.errorKlass = errorKlass;
        this.errorProperties = errorProperties;
        this.value = value;

    }

    public TrackerErrorReport( String errorMessage, TrackerErrorCode errorCode )
    {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.mainKlass = null;
        this.lineNumber = 0;
        this.errorProperties = null;

    }

    @JsonProperty
    public TrackerErrorCode getErrorCode()
    {
        return errorCode;
    }

    @JsonProperty
    public String getMessage()
    {
        return errorMessage;
    }
<<<<<<< HEAD
=======

    public static class TrackerErrorReportBuilder
    {
        private final List<Object> arguments = new ArrayList<>();

        public TrackerErrorReportBuilder addArg( Object arg )
        {
            this.arguments.add( arg );
            return this;
        }

        public TrackerErrorReport build( TrackerBundle bundle )
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

            String errorMessage = MessageFormat.format( errorCode.getMessage(), args.toArray( new Object[0] ) );

            return new TrackerErrorReport( this.mainKlass, errorMessage, this.errorCode, this.listIndex, this.mainId,
                this.mainKlass, this.errorProperties, this.value );
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
        return "TrackerErrorReport{" +
            "message=" + errorMessage +
            ", errorCode=" + errorCode +
            ", mainId='" + mainId + '\'' +
            ", mainClass=" + mainKlass +
            ", errorClass=" + errorKlass +
            ", errorProperties=" + Arrays.toString( errorProperties ) +
            ", value=" + value +
            ", objectIndex=" + lineNumber +
            '}';
    }
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
}
