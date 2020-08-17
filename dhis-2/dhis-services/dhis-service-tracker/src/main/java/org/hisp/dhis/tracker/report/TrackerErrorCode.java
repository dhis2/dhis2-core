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

    E1016( "TrackedEntityInstance: `{0}`, already has an active enrollment in Program: `{1}`, and this " +
        "program only allows enrolling one time." ),
    E1038( "TrackedEntityInstance: `{0}`, has multiple active enrollments in Program `{1}`." ),
    E1037( "TrackedEntityInstance: `{0}`, is not enrolled in Program `{1}`." ),
    E1002( "TrackedEntityInstance: `{0}`, already exists." ),
    E1064( "Error validating attribute, not unique; Error `{0}`" ),
    E1074( "FeatureType is missing." ),
    E1031( "Event OccurredAt date is missing." ),
    E1036( "Event: `{0}`, TrackedEntityInstance does not point to a existing object." ),
    E1042( "Event: `{0}`, needs to have completed date." ),
    E1056( "Event date: `{0}`, is before start date: `{1}`, for AttributeOption: `{2}`." ),
    E1057( "Event date: `{0}`, is after end date: `{1}`, for AttributeOption; `{2}`." ),
    E1051( "Invalid event due date: `{0}`." ),
    E1052( "Invalid event date: `{0}`." ),
    E1019( "Only Program attributes is allowed for enrollment; Non valid attribute: `{0}`." ),
    E1008( "Value: `{0}`, does not match the attribute pattern: `{1}`." ),
    E1007( "Error validating attribute value type: `{0}`; Error: `{1}`." ),
    E1018( "Missing mandatory attribute: `{0}`." ),
    E1075( "Attribute: `{0}`, is missing uid." ),
    E1076( "Attribute: `{0}`, value is null." ),
    E1077( "Attribute: `{0}`, text value exceed the maximum allowed length: `{0}`." ),
    E1085( "Attribute: `{0}`, value does not match value type: `{1}`." ),
    E1083( "User: `{0}`, is not authorized to modify completed events." ),
    E1009( "File resource: `{0}`, has already been assigned to a different object." ),
    E1084( "File resource: `{0}`, reference could not be found." ),
    E1015( "TrackedEntityInstance: `{0}`, already has an active Enrollment in Program `{1}`." ),
    E1022( "TrackedEntityInstance: `{0}`, must have same TrackedEntityType as Program `{1}`." ),
    E1063( "TrackedEntityInstance: `{0}`, does not exist." ),
    E1005( "Could not find TrackedEntityType: `{0}`." ),
    E1006( "Attribute: `{0}`, does not exist." ),
    E1011( "Could not find OrganisationUnit: `{0}`, linked to Event." ),
    E1012( "Geometry does not conform to FeatureType: `{0}`." ),
    E1014( "Provided Program: `{0}`, is a Program without registration. " +
        "An Enrollment cannot be created into Program without registration." ),
    E1020( "Enrollment date: `{0}`, can`t be future date." ),
    E1021( "Incident date: `{0}`, can`t be future date." ),
    E1023( "DisplayIncidentDate is true but IncidentDate is null or invalid formatted: `{0}`." ),
    E1025( "Invalid Enrollment date: `{0}`." ),
    E1041( "Enrollment OrganisationUnit: `{0}`, and Program: `{1}`, OrganisationUnit: `{2}`, don't match." ),
    E1068( "Could not find TrackedEntityInstance: `{0}`, linked to Enrollment." ),
    E1069( "Could not find Program: `{0}`, linked to Enrollment." ),
    E1070( "Could not find OrganisationUnit: `{0}`, linked to Enrollment." ),
    E1080( "Enrollment: `{0}`, already exists." ),
    E1081( "Enrollment: `{0}`, do not exist." ),
    E1030( "Event: `{0}`, already exists." ),
    E1032( "Event: `{0}`, do not exist." ),
    E1035( "Event: `{0}`, ProgramStage value is NULL." ),
    E1086( "Event: `{0}`, has a program: `{1}`, that is a registration but its ProgramStage is not valid or missing." ),
    E1088( "Event: `{0}`, program: `{1}`, and ProgramStage: `{2}`, could not be found." ),
    E1089( "Event: `{0}`, ProgramStage Program and Event Program don't match." ),
    E1000( "User: `{0}`, has no write access to OrganisationUnit: `{1}`." ),
    E1001( "User: `{0}`, has no data write access to TrackedEntityType: `{1}`." ),
    E1091( "User: `{0}`, has no data write access to Program: `{1}`." ),
    E1095( "User: `{0}`, has no data write access to ProgramStage: `{1}`." ),
    E1096( "User: `{0}`, has no data read access to Program: `{1}`." ),
    E1100( "User: `{0}`, is lacking 'F_TEI_CASCADE_DELETE' authority to delete TrackedEntityInstance: `{1}`." ),
    E1102( "User: `{0}`, does not have access to the tracked entity: `{1}`, Program: `{2}`, combination." ),
    E1103( "User: `{0}`, is lacking 'F_ENROLLMENT_CASCADE_DELETE' authority to delete Enrollment : `{1}`." ),
    E1104( "User: `{0}`, has no data read access to program: `{1}`, TrackedEntityType: `{2}`." ),
    E1112( "Attribute value: `{0}`, is set to confidential but system is not properly configured to encrypt data." ),
    E1055( "Default AttributeOptionCombo is not allowed since program has non-default CategoryCombo." ),
    E1115( "Could not find CategoryOptionCombo: `{0}`." ),
    E1116( "Could not find CategoryOption: `{0}`." ),
    E1117( "CategoryOptionCombo does not exist for given category combo and category options: `{0}`." ),
    E1099( "User: `{0}`, has no write access to CategoryOption: `{1}`." ),
    E1039( "ProgramStage: `{0}`, is not repeatable and an event already exists." ),
    E1048( "Object: `{0}`, uid: `{1}`, has an invalid uid format." ),
    E1049( "Could not find OrganisationUnit: `{0}`, linked to Tracked Entity." ),
    // TODO: Delete not working yet
    E1082( "Event: `{0}`, is already deleted." ),
    E1113( "Enrollment: `{0}`, is already deleted." ),
    E1114( "TrackedEntity: `{0}`, is already deleted." ),

    //TODO: See TODO on error usage
    E1017( "Attribute: `{0}`, does not exist." ),
    //TODO: See TODO on error usage
    E1093( "User: `{0}`, has no search access to OrganisationUnit: `{1}`." ),
    //TODO: See TODO on error usage
    E1094( "Not allowed to update Enrollment: `{0}`, existing Program `{1}`." ),
    //TODO: See TODO on error usage
    E1110( "Not allowed to update Event: `{0}`, existing Program `{1}`." ),
    //TODO: See TODO on error usage
    E1111( "We have a generated attribute: `{0}`, but no pattern." ),
    //TODO: See TODO on error usage
    E1040( "Multiple active enrollments exists for Program: `{0}`." ),
    // TODO See TODO on error usage
    E1045( "Program: `{0}`, expiry date has passed. It is not possible to make changes to this event." ),
    // TODO See TODO on error usage
    E1043( "Event: `{0}`, completeness date has expired. Not possible to make changes to this event." ),
    // TODO See TODO on error usage
    E1044( "Event: `{0}`, needs to have event date." ),
    // TODO See TODO on error usage
    E1046( "Event: `{0}`, needs to have at least one (event or schedule) date." ),
    // TODO See TODO on error usage
    E1047( "Event: `{0}`, date belongs to an expired period. It is not possible to create such event." ),

    E1200( "Rule engine error: `{0}`" ),

    E4000( "Relationship: `{0}` cannot link to itself" ),
    E4001( "Property `{0}` can not be set when property `{1}` is `{2}`. " ),
    E4002( "Property `{0}` must be set when property `{1}` is `{2}`. " ),
    E4003( "There are duplicated relationships. " ),
    E4004( "Missing required property: 'relationshipType'." ),

    E9999( "N/A" );

    private final String message;

    TrackerErrorCode( String message )
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }
}
