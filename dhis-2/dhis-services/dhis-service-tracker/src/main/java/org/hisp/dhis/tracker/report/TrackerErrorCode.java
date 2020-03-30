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

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public enum TrackerErrorCode
{
    NONE( "No error message given." ),

    E1016( "TrackedEntityInstance: `{0}`, already has an active enrollment in program: `{1}`, and this " +
        "program only allows enrolling one time." ),
    E1038( "Tracked entity instance: `{0}`, has multiple active enrollments in program `{1}`." ),
    E1039( "Program stage is not repeatable and an event already exists." ),
    E1045( "Program: `{0}`, expiry date has passed. It is not possible to make changes to this event." ),
    E1040( "Multiple active enrollments exists for program: `{0}`." ),
    E1037( "Tracked entity instance: `{0}`, is not enrolled in program `{1}`." ),
    E1055( "Default attribute option combo is not allowed since program has non-default category combo." ),
    E1064( "Error validating attribute, not unique; Error `{0}`" ),
    E1074( "Feature type is missing." ),
    E1031( "Event date is required." ),
    E1036( "Event: `{0}`, tracked entity instance does not point to a existing object." ),
    E1042( "Event: `{0}`, needs to have completed date." ),
    E1043( "Event: `{0}`, completeness date has expired. Not possible to make changes to this event." ),
    E1044( "Event: `{0}`, needs to have event date." ),
    E1046( "Event: `{0}`, needs to have at least one (event or schedule) date." ),
    E1047( "Event: `{0}`, date belongs to an expired period. It is not possible to create such event." ),
    E1056( "Event date: `{0}`, is before start date: `{1}`, for attributeOption: `{2}`." ),
    E1057( "Event date: `{0}`, is after end date: `{1}`, for attributeOption; `{2}`." ),
    E1082( "Event: `{0}`, was already used and/or deleted. This event can not be modified." ),
    E1051( "Invalid event due date: `{0}`." ),
    E1052( "Invalid event date: `{0}`." ),
    E1053( "Invalid event created at client date: `{0}`." ),
    E1054( "Invalid event last updated at client date: `{0}`." ),
    E1019( "Only program attributes is allowed for enrollment; Non valid attributes: `{0}`." ),
    E1008( "Value: `{0}`, does not match the attribute pattern." ),
    E1007( "Error validating attribute value type; Error `{0}`." ),
    E1018( "Missing mandatory attribute: `{0}`." ),
    E1017( "Attribute: `{0}`, does not exist." ),
    E1075( "Attribute: `{0}`, is missing uid." ),
    E1076( "Attribute: `{0}`, value is null." ),
    E1078( "Attribute: `{0}`, is missing type, is null or type is empty." ),
    E1085( "Attribute: `{0}`, value type is not valid: `{1}`." ),
    E1083( "User: `{0}`, is not authorized to modify completed events." ),
    E1077( "User: `{0}`, do not have access to read enrollment: `{1}`." ),
    E1099( "User: `{0}`, has no write access to category option: `{1}`." ),
    E1101( "User: `{0}`, has no read access to organisation unit: `{1}`." ),
    E1009( "File resource: `{0}`, has already been assigned to a different object." ),
    E1084( "File resource: `{0}`, reference could not be found." ),

    E1002( "Tracked entity instance: `{0}`, already exists or was deleted earlier." ),
    E1015( "TrackedEntityInstance: `{0}`, already has an active enrollment in program `{1}`." ),
    E1022( "Tracked entity instance: `{0}`, must have same tracked entity type as program `{1}`." ),
    E1063( "Tracked entity instance: `{0}`, does not exist." ),
    E1004( "Missing required property: 'trackedEntityType'." ),
    E1005( "Could not find trackedEntityType: `{0}`." ),
    E1006( "Attribute: `{0}`, does not exist." ),
    E1010( "No organization unit id in tracked entity instance object." ),
    E1011( "Could not find organization unit with this uid: `{0}`." ),
    E1012( "Geometry does not conform to feature type: `{0}`." ),
    E1013( "Could not parse coordinates: `{0}`; Error: `{1}`." ),
    E1014( "Provided program: `{0}`, is a program without registration. " +
        "An enrollment cannot be created into program without registration." ),
    E1020( "Enrollment date: `{0}`, can`t be future date." ),
    E1021( "Incident date: `{0}`, can`t be future date." ),
    E1023( "DisplayIncidentDate is true but IncidentDate is null or invalid formatted: `{0}`." ),
    E1025( "Invalid enrollment date: `{0}`." ),
    E1026( "Invalid enrollment created at client date: `{0}`." ),
    E1027( "Invalid enrollment last updated at client date: `{0}`." ),
    E1041( "Enrollment organization unit: `{0}`, and program: `{1}`, organization unit: `{2}`,  don't match." ),
    E1068( "Could not find TrackedEntityInstance: `{0}`, linked to enrollment." ),
    E1069( "Could not find Program: `{0}`, linked to enrollment." ),
    E1070( "Could not find organization unit: `{0}`, linked to enrollment." ),
    E1080( "Enrollment: `{0}`, already exists." ),
    E1081( "Enrollment: `{0}`, do not exist." ),
    E1030( "Event `{0}` already exists or was deleted earlier." ),
    E1032( "Event uid: `{0}` did not point to a valid event." ),
    E1035( "Event: `{0}`, program stage value is NULL." ),
    E1086( "Event: `{0}` has a program: `{1}`, that is a registration but its program stage is not valid or missing." ),
    E1088( "Event: `{0}`, program: `{1}`, and program stage: `{2}` could not be found." ),
    E1089( "Event: `{0}`, program stage program and event program don't match." ),
    E1000( "User: `{0}`, has no write access to organisation unit: `{1}`." ),
    E1001( "User: `{0}`, has no data write access to tracked entity type: `{1}`." ),
    E1091( "User: `{0}`, has no data write access to program: `{1}`." ),
    E1092( "User: `{0}`, has no data read access to tracked entity type: `{1}`." ),
    E1095( "User: `{0}`, has no data write access to program stage: `{1}`." ),
    E1096( "User: `{0}`, has no data read access to program: `{1}`." ),
    E1100( "User: `{0}`, is lacking F_TEI_CASCADE_DELETE authority to delete tracked entity instance: `{1}`." ),
    E1102( "User: `{0}`, does not have access to the tracked entity: `{1}`, program: `{2}`, combination." ),

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
