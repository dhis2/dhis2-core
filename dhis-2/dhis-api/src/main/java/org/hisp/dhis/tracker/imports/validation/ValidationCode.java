/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.validation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Getter
@RequiredArgsConstructor
public enum ValidationCode {
  /* General */
  E1000("User: `{0}` has no capture scope access to OrganisationUnit: `{1}`."),
  E1001("User: `{0}` has no data write access to TrackedEntityType: `{1}`."),
  E1002("TrackedEntity: `{0}`, already exists."),
  E1003("User: `{0}` has no write access to TrackedEntity: `{1}`."),
  E1005("Could not find TrackedEntityType: `{0}`."),
  E1006("Attribute: `{0}` does not exist."),
  E1007("Error validating attribute value type: `{0}`; Error: `{1}`."),
  E1008("ProgramStage `{0}` has no reference to a Program, check ProgramStage configuration."),
  E1009("File resource: `{0}` has already been assigned to another object."),
  E1010("Could not find Program: `{0}`, linked to Event."),
  E1011("Could not find OrganisationUnit: `{0}`, linked to Event."),
  E1012("Geometry does not conform to FeatureType: `{0}`."),
  E1013("Could not find ProgramStage: `{0}`, linked to Event."),
  E1014(
      "Provided Program: `{0}` is a Program without registration. "
          + "An Enrollment cannot be created into Program without registration."),
  E1015("TrackedEntity: `{0}` already has an active Enrollment in Program `{1}`."),
  E1016(
      "TrackedEntity: `{0}` already has an Enrollment in Program: `{1}`, and this "
          + "Program only allows enrolling once."),
  E1018("Attribute: `{0}` is mandatory in Program `{1}` but not declared in Enrollment `{2}`."),
  E1019("Only ProgramAttributes are allowed for Enrollment, invalid Attribute: `{0}`."),
  E1020("Enrollment date: `{0}` cannot be a future date."),
  E1021("Incident date: `{0}` cannot be a future date."),
  E1022("TrackedEntity: `{0}` must have same TrackedEntityType as Program `{1}`."),
  E1023("DisplayIncidentDate is true but occurredAt is null."),
  E1025("Property enrolledAt is null."),
  E1029("Event OrganisationUnit: `{0}` and Program: `{1}`, do not match."),
  E1030("Event: `{0}` already exists."),
  E1031("Event occurredAt date is missing."),
  E1032("Event: `{0}` do not exist."),
  E1033("Event: `{0}` Enrollment value is null."),
  E1039("ProgramStage: `{0}` is not repeatable and an Event already exists."),
  E1041("Enrollment OrganisationUnit: `{0}` and Program: `{1}` do not match."),
  E1043("Event: `{0}` completeness date has expired, not allowed to make changes to this Event."),
  E1046("Event: `{0}` needs to have at least one Event or schedule date."),
  E1047("Event: `{0}` date belongs to an expired period, not possible to create Event."),
  E1049("Could not find OrganisationUnit: `{0}` linked to TrackedEntity."),
  E1050("Event ScheduledAt date is missing."),
  E1051("Event: `{0}` completedAt must be null when status is `{1}`."),
  E1052("Enrollment: `{0}` completedAt must be null when status is `{1}`."),
  E1054("AttributeOptionCombo `{0}` is not in the Event Program CategoryCombo `{1}`."),
  E1055("Default AttributeOptionCombo is not allowed as Program has non-default CategoryCombo."),
  E1056("Event date: `{0}` is before start date: `{1}` for AttributeOption: `{2}`."),
  E1057("Event date: `{0}` is after end date: `{1}` for AttributeOption: `{2}` in Program: `{3}`."),
  E1063("TrackedEntity: `{0}`, does not exist."),
  E1064("Non-unique attribute value `{0}` for attribute `{1}`."),
  E1068("Could not find TrackedEntity: `{0}` linked to Enrollment."),
  E1069("Could not find Program: `{0}` linked to Enrollment."),
  E1070("Could not find OrganisationUnit: `{0}` linked to Enrollment."),
  E1074("FeatureType is missing."),
  E1075("Attribute: `{0}` UID is missing."),
  E1076("`{0}` `{1}` is mandatory and must be specified."),
  E1077("Attribute: `{0}` text value exceed the maximum length: `{0}`."),
  E1079("Event: `{0}` Program: `{1}` is different from Program defined in Enrollment `{2}`."),
  E1080("Enrollment: `{0}` already exists."),
  E1081("Enrollment: `{0}` do not exist."),
  E1082("Event: `{0}` is already deleted and cannot be modified."),
  E1083("User: `{0}` is not authorized to update completed Events."),
  E1089("Event: `{0}` references ProgramStage `{1}` which do not belong to Program `{2}`."),
  E1090(
      "Attribute: `{0}` is mandatory in TrackedEntityType `{1}` but not found in TrackedEntity `{2}`."),
  E1091("User: `{0}` has no data write access to Program: `{1}`."),
  E1095("User: `{0}` has no data write access to ProgramStage: `{1}`."),
  E1096("User: `{0}` has no data read access to Program: `{1}`."),
  E1099("User: `{0}` has no write access to CategoryOption: `{1}`."),
  E1100("User: `{0}` lacks 'F_TEI_CASCADE_DELETE' authority to delete TrackedEntity: `{1}`."),
  E1102("User: `{0}` does not have access to the TrackedEntity: `{1}` and Program: `{2}`."),
  E1103("User: `{0}` lacks 'F_ENROLLMENT_CASCADE_DELETE' authority to delete Enrollment : `{1}`."),
  E1104("User: `{0}` has no data read access to Program: `{1}`, TrackedEntityType: `{2}`."),
  E1112("AttributeValue: `{0}` is confidential but encryption is not enabled: `{1}`"),
  E1113("Enrollment: `{0}` is already deleted and cannot be modified."),
  E1114("TrackedEntity: `{0}` is already deleted and cannot be modified."),
  E1115("Could not find CategoryOptionCombo: `{0}`."),
  E1116("Could not find CategoryOption: `{0}`."),
  E1117("CategoryOptionCombo not found for CategoryCombo `{0}` and CategoryOptions `{1}`."),
  E1118("Assigned User `{0}` is not valid."),
  E1119("A Note with UID `{0}` already exists."),
  E1120("ProgramStage `{0}` does not allow user assignment."),
  E1121("Missing required trackedEntity property: `{0}`."),
  E1122("Missing required enrollment property: `{0}`."),
  E1123("Missing required event property: `{0}`."),
  E1124("Missing required relationship property: `{0}`."),
  E1125("Value `{0}` is not a valid option code in OptionSet `{1}`."),
  E1126("Not allowed to update trackedEntity property: {0}."),
  E1127("Not allowed to update enrollment property: {0}."),
  E1128("Not allowed to update event property: {0}."),
  E1300("Generated by ProgramRule (`{0}`) - `{1}`."),
  E1301("Generated by ProgramRule (`{0}`) - Mandatory DataElement `{1}` is not present."),
  E1302("DataElement `{0}` is not valid: `{1}`."),
  E1303("Mandatory DataElement `{0}` is not present."),
  E1304("DataElement `{0}` is not a valid DataElement."),
  E1305("DataElement `{0}` is not part of ProgramStage `{1}`."),
  E1306("Generated by ProgramRule (`{0}`) - Mandatory Attribute `{1}` is not present"),
  E1307(
      "Generated by ProgramRule (`{0}`) - Unable to assign value to DataElement `{1}`. "
          + "The provided value must be empty or match the calculated value `{2}`"),
  E1308("Generated by ProgramRule (`{0}`) - DataElement `{1}` is replaced in Event `{2}`."),
  E1309(
      "Generated by ProgramRule (`{0}`) - Unable to assign value to Attribute `{1}`. "
          + "The provided value must be empty or match the calculated value `{2}`."),
  E1310("Generated by ProgramRule (`{0}`) - Attribute `{1}` is being replaced in te `{2}`."),
  E1313("Event `{0}` of an Enrollment does not reference a TrackedEntity."),
  E1314("Generated by ProgramRule (`{0}`) - DataElement `{1}` is mandatory and cannot be deleted."),
  E1315("Status `{0}` does not allow creating data values, allow statuses: {1}."),
  E1316("Event cannot transition from status `{0}` to status `{1}`."),
  E1317("Generated by ProgramRule (`{0}`) - Attribute `{1}` is mandatory and cannot be deleted."),
  E1318("Status `{0}` is not applicable for single events."),
  E1319("Generated by program rule (`{0}`) - Date `{1}` is not valid"),
  E1320("Generated by program rule (`{0}`) - Event (`{1}`) was automatically scheduled at `{2}`."),
  E1321(
      "Generated by program rule (`{0}`) - User (`{1}`) does not have write access to ProgramStage `{2}`, where an event was auto-scheduled using SCHEDULEEVENT rule action."),
  E1322(
      "Generated by program rule (`{0}`) - Event for programStage (`{1}`) and enrollment (`{2}`) already exists"),
  E1323("User: `{0}` has no access to any program."),
  E1324(
      "'User `{0}` has no ownership access to any program for the provided TrackedEntity: `{1}`."),

  /* Relationship */
  E4000("Relationship: `{0}` cannot link to itself."),
  E4001(
      "RelationshipItem `{0}` for Relationship `{1}` is invalid as an item must be associated with exactly one of TrackedEntity, Enrollment, Event."),
  E4006("Could not find RelationshipType: `{0}`."),
  E4010("RelationshipType `{0}` constraint requires a {1} but a {2} was found."),
  E4012("Could not find `{0}`: `{1}`, linked to relationship."),
  E4014("RelationshipType `{0}` constraint requires TrackedEntityType `{1}` but `{2}` was found."),
  E4015("Relationship: `{0}` already exists."),
  E4016("Relationship: `{0}` do not exist."),
  E4017("Relationship: `{0}` is already deleted and cannot be modified."),
  E4018("Relationship: `{0}` linking {1}: `{2}` to {3}: `{4}` already exists."),
  E4020("User: `{0}` has no write access to relationship: `{1}`."),
  E5000("{0} `{1}` cannot be created because {2} `{3}` referenced by it could not be created."),
  E9999("N/A.");

  /** Validation error message. */
  private final String message;
}
