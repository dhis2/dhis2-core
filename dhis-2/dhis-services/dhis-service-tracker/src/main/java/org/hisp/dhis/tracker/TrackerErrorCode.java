package org.hisp.dhis.tracker;

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

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public enum TrackerErrorCode
{
    NONE( "No error message given." ),

    E1000( "User has no write access to organisation unit: {0}" ),
    E1001( "User has no data write access to tracked entity: {0}" ),
    E1002( "Tracked entity instance {0} already exists or was deleted earlier" ),
    E1003( "Enrollment create access error. {0}" ),

    E1004( "Missing required property trackedEntityType" ),
    E1005( "Invalid trackedEntityType {0}" ),
    E1006( "Invalid attribute {0}" ),
    E1007( "Attribute.value {0}" ),
    E1008( "Value does not match the attribute pattern {0}" ),
    E1009( "File resource with uid '{0}' has already been assigned to a different object" ),
    E1010( "No org unit id in tracked entity instance object" ),
    E1011( "Invalid org unit ID: {0}" ),
    E1012( "Geometry does not conform to feature type '{0}'" ),
    E1013( "Could not parse coordinates" ),

    E1014( "Provided program '{0}' s a program without registration. " +
        "An enrollment cannot be created into program without registration." ),

    E1015( "TrackedEntityInstance '{0}'" +
        " already has an active enrollment in program '{1}'" ),

    E1016( "TrackedEntityInstance '{0}'" +
        " already has an active enrollment in program '{1}', and this program only allows enrolling one time" ),

    E1017( "Attribute.value Does not point to a valid attribute." ),
    E1018( "Attribute.value Missing mandatory attribute '{0}'" ),
    E1019( "Attribute.attribute Only program attributes is allowed for enrollment '{0}'" ),
    E1020( "Enrollment.date Enrollment Date can't be future date :'{0}'" ),
    E1021( "Enrollment.incidentDate Incident Date can't be future date :'{0}'" ),
    E1022( "Tracked entity instance must have same tracked entity as program: '{0}'" ),

    E1023( "DisplayIncidentDate is true but IncidentDate is null " ),
    E1024( "Invalid enrollment incident date: '{0}'" ),
    E1025( "Invalid enrollment date: '{0}'" ),
    E1026( "Invalid enrollment created at client date: '{0}'" ),
    E1027( "Invalid enrollment last updated at client date: '{0}'" ),
    E1028( "User does not have access to assign ownership for the entity-program combination '{0}','{1}'" ),
    E1029( "Comment already exist, only create allowed '{0}','{1}'" ),

    E1030( "Event '{0}' already exists or was deleted earlier." ),
    E1031( "Event date is required. " ),
    E1032( "Event.event did not point to a valid event: '{0}'" ),

    E1033( "Event.orgUnit did not point to a valid organisation: '{0}'" ),

    E1034( "Event.program did not point to a valid program: '{0}'" ),
    E1035( "Event.programStage did not point to a valid programStage: '{0}'" ),

    E1036( "Event.trackedEntityInstance does not point to a valid tracked entity instance: '{0}'" ),
    E1037( "Tracked entity instance: '{0}' is not enrolled in program: '{1}'" ),
    E1038( "Tracked entity instance: '{0}' has multiple active enrollments in program: : '{1}'" ),
    E1039( "Program stage is not repeatable and an event already exists" ),

    E1040( "Multiple active program instances exists for program: '{0}'" ),
    E1041( "Program is not assigned to this organisation unit: '{0}'" ),

    E1042( "Event needs to have completed date" ),
    E1043( "The event's completeness date has expired. Not possible to make changes to this event" ),
    E1044( "Event needs to have event date" ),
    E1045( "The program's expiry date has passed. It is not possible to make changes to this event" ),
    E1046( "Event needs to have at least one (event or schedule) date" ),
    E1047( "The event's date belongs to an expired period. It is not possible to create such event" ),

    E1048( "Geometry '{0}' does not conform to the feature type '{1}' specified for the program stage: '{2}'" ),
    E1049( "Invalid longitude or latitude for property 'coordinates'." ),

    E1050( "User don't have access to create program instance. '{0}'" ),

    E1051( "Invalid event due date: '{0}'" ),
    E1052( "Invalid event date: '{0}'" ),
    E1053( "Invalid event created at client date: '{0}'" ),
    E1054( "Invalid event last updated at client date: '{0}'" ),

    E1055( "Default attribute option combo is not allowed since program has non-default category combo." ),
    E1056( "Event date '{0}' is before start date '{1}' for attributeOption '{2}'" ),
    E1057( "Event date '{0}' is after end date '{1}' for attributeOption '{2}'" ),

    E1058( "CategoryOptionCombo write access error '{0}'" ),

    E1059( "Tracked entity instance {0} do not exists either as deleted or not" ),
    E1060( "Tracked entity instance {0} could not be fetched" ),
    E1061( "Tracked entity instance {0} cannot be deleted as it has associated enrollments and user does not have authority {0}" ),
    E1062( "{0} doesn't point to a valid enrollment." ),

    E1063( "Tracked entity instance {0} does not exists" ),

    E9999( "N/A" );

    private String message;

    TrackerErrorCode( String message )
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }
}
