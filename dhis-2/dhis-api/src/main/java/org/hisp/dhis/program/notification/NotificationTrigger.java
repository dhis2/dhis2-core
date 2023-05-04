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
package org.hisp.dhis.program.notification;

import java.util.Set;

import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;

/**
 * @author Halvdan Hoem Grelland
 */
@JacksonXmlRootElement( localName = "notificationTrigger", namespace = DxfNamespaces.DXF_2_0 )
public enum NotificationTrigger
{
    /**
     * Program enrollment.
     */
    ENROLLMENT,

    /**
     * Program or ProgramStage completion.
     */
    COMPLETION,

    /**
     * Triggered by ProgramRule.
     */
    PROGRAM_RULE,

    /**
     * Scheduled days relative to the dueDate of the Event and DataSet
     * completion.
     */
    SCHEDULED_DAYS_DUE_DATE,

    /**
     * Scheduled days relative to the incidentDate of the Enrollment
     * (enrollment).
     */
    SCHEDULED_DAYS_INCIDENT_DATE,

    /**
     * Scheduled days relative to the enrollmentDate of the Enrollment
     * (enrollment).
     */
    SCHEDULED_DAYS_ENROLLMENT_DATE;

    private static final Set<NotificationTrigger> IMMEDIATE_TRIGGERS = new ImmutableSet.Builder<NotificationTrigger>()
        .add( ENROLLMENT, COMPLETION, PROGRAM_RULE ).build();

    private static final Set<NotificationTrigger> SCHEDULED_TRIGGERS = new ImmutableSet.Builder<NotificationTrigger>()
        .add( SCHEDULED_DAYS_DUE_DATE, SCHEDULED_DAYS_INCIDENT_DATE, SCHEDULED_DAYS_ENROLLMENT_DATE ).build();

    private static final Set<NotificationTrigger> APPLICABLE_TO_ENROLLMENT = new ImmutableSet.Builder<NotificationTrigger>()
        .add( ENROLLMENT, COMPLETION, SCHEDULED_DAYS_INCIDENT_DATE, SCHEDULED_DAYS_ENROLLMENT_DATE ).build();

    private static final Set<NotificationTrigger> APPLICABLE_TO_EVENT = new ImmutableSet.Builder<NotificationTrigger>()
        .add( COMPLETION, SCHEDULED_DAYS_DUE_DATE ).build();

    public boolean isImmediate()
    {
        return IMMEDIATE_TRIGGERS.contains( this );
    }

    public boolean isScheduled()
    {
        return SCHEDULED_TRIGGERS.contains( this );
    }

    public static Set<NotificationTrigger> getAllScheduledTriggers()
    {
        return SCHEDULED_TRIGGERS;
    }

    public static Set<NotificationTrigger> getAllImmediateTriggers()
    {
        return IMMEDIATE_TRIGGERS;
    }

    public static Set<NotificationTrigger> getAllApplicableToEnrollment()
    {
        return APPLICABLE_TO_ENROLLMENT;
    }

    public static Set<NotificationTrigger> getAllApplicableToEvent()
    {
        return APPLICABLE_TO_EVENT;
    }

    public boolean isApplicableToEvent()
    {
        return APPLICABLE_TO_EVENT.contains( this );
    }

    public boolean isApplicableToEnrollment()
    {
        return APPLICABLE_TO_ENROLLMENT.contains( this );
    }
}
