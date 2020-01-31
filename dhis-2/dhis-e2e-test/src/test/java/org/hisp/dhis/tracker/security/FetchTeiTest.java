package org.hisp.dhis.tracker.security;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.tracker.fields.Attributes;
import org.hisp.dhis.actions.tracker.fields.DataValue;
import org.hisp.dhis.actions.tracker.fields.DataValueFields;
import org.hisp.dhis.actions.tracker.fields.Enrollment;
import org.hisp.dhis.actions.tracker.fields.EnrollmentFields;
import org.hisp.dhis.actions.tracker.fields.Event;
import org.hisp.dhis.actions.tracker.fields.EventFields;
import org.hisp.dhis.actions.tracker.fields.Note;
import org.hisp.dhis.actions.tracker.fields.NoteFields;
import org.hisp.dhis.actions.tracker.fields.Relationship;
import org.hisp.dhis.actions.tracker.fields.RelationshipAttributes;
import org.hisp.dhis.actions.tracker.fields.RelationshipFrom;
import org.hisp.dhis.actions.tracker.fields.RelationshipTo;
import org.hisp.dhis.actions.tracker.TeiFields;
import org.hisp.dhis.actions.tracker.fields.TrackedEntityInstance;
import org.hisp.dhis.actions.tracker.TrackedEntityInstanceActions;
import org.hisp.dhis.actions.tracker.fields.TrackedEntityInstanceAttributes;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hisp.dhis.actions.tracker.fields.TrackedEntityInstanceFields.*;
import static org.hisp.dhis.actions.tracker.fields.TrackedEntityInstanceFields.trackedEntityInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Luciano Fiandesio
 */
public class FetchTeiTest extends ApiTest {


    private TrackedEntityInstanceActions trackedEntityInstanceActions;

    @BeforeAll
    public void before()
    {
        trackedEntityInstanceActions = new TrackedEntityInstanceActions();
    }

    @Test
    public void v1()
    {
        ApiResponse response = trackedEntityInstanceActions.getTrackedEntityInstances("ImspTQPwCqd", "DESCENDANTS", getTeiFields(), true, 50, true, true);
        ApiResponse responseFast = trackedEntityInstanceActions.getTrackedEntityInstances("ImspTQPwCqd", "DESCENDANTS", getTeiFields(), true, 50, true, true);


        JsonObject body = response.getBody();
        assertCount(body, 50);

    }



    private void assertCount(JsonObject response, int count) {

        assertEquals(count, response.getAsJsonArray("trackedEntityInstances").size());

    }


    private TeiFields getTeiFields() {

        return TeiFields.builder()
                .trackedEntityInstance(TrackedEntityInstance.builder()
                        .fields(asList(
                                trackedEntityInstance,
                                created,
                                lastUpdated,
                                orgUnit,
                                trackedEntityType,
                                coordinates,
                                featureType,
                                deleted))
                        .attributes(Attributes.builder().attributes(asList(
                                TrackedEntityInstanceAttributes.attribute,
                                TrackedEntityInstanceAttributes.value,
                                TrackedEntityInstanceAttributes.created,
                                TrackedEntityInstanceAttributes.lastUpdated)).build())
                        .relationships(Relationship.builder()
                                .fields(asList(RelationshipAttributes.trackedEntityInstanceA,
                                        RelationshipAttributes.trackedEntityInstanceB,
                                        RelationshipAttributes.relationship,
                                        RelationshipAttributes.relationshipName,
                                        RelationshipAttributes.relationshipType,
                                        RelationshipAttributes.created,
                                        RelationshipAttributes.lastUpdated))
                                .relationshipFrom(RelationshipFrom.builder()
                                        .trackedEntityInstanceFields(Collections.singletonList(trackedEntityInstance))
                                        .enrollmentFields(Collections.singletonList(EnrollmentFields.enrollment))
                                        .eventFields(Collections.singletonList(EventFields.event))
                                        .build())
                                .relationshipTo(RelationshipTo.builder()
                                        .trackedEntityInstanceFields(Collections.singletonList(trackedEntityInstance))
                                        .enrollmentFields(Collections.singletonList(EnrollmentFields.enrollment))
                                        .eventFields(Collections.singletonList(EventFields.event))
                                        .build())

                                .build())
                        .enrollment(Enrollment.builder()
                                .note(Note.builder()
                                        .fields(asList(NoteFields.note, NoteFields.value,NoteFields.storedBy,NoteFields.storedDate))
                                        .build())
                                .fields(asList(
                                        EnrollmentFields.enrollment,
                                        EnrollmentFields.created,
                                        EnrollmentFields.lastUpdated,
                                        EnrollmentFields.orgUnit,
                                        EnrollmentFields.program,
                                        EnrollmentFields.enrollmentDate,
                                        EnrollmentFields.incidentDate,
                                        EnrollmentFields.followup,
                                        EnrollmentFields.status,
                                        EnrollmentFields.deleted,
                                        EnrollmentFields.trackedEntityInstance,
                                        EnrollmentFields.coordinate))
                                .event(Event.builder()
                                        .fields(asList(EventFields.event,
                                                EventFields.enrollment,
                                                EventFields.created,
                                                EventFields.lastUpdated,
                                                EventFields.status,
                                                EventFields.coordinate,
                                                EventFields.program,
                                                EventFields.programStage,
                                                EventFields.orgUnit,
                                                EventFields.eventDate,
                                                EventFields.completedDate,
                                                EventFields.deleted,
                                                EventFields.dueDate,
                                                EventFields.attributeOptionCombo))
                                        .dataValue(DataValue.builder()
                                                .fields(asList(
                                                        DataValueFields.dataElement,
                                                        DataValueFields.storedBy,
                                                        DataValueFields.value,
                                                        DataValueFields.created,
                                                        DataValueFields.lastUpdated,
                                                        DataValueFields.providedElsewhere))
                                                .build())

                                        .build())
                                .build())
                        .build())
                .build();
    }
}
