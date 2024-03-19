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
package org.hisp.dhis.tracker.imports.databuilder;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hisp.dhis.helpers.JsonObjectBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EnrollmentDataBuilder implements TrackerImporterDataBuilder {
  private JsonObjectBuilder jsonObjectBuilder;

  public EnrollmentDataBuilder() {
    jsonObjectBuilder = new JsonObjectBuilder();
    // setStatus( "ACTIVE" );
    setEnrollmentDate(Instant.now().minus(1, ChronoUnit.HOURS).toString());
    setIncidentDate(Instant.now().toString());
  }

  public EnrollmentDataBuilder setId(String id) {
    this.jsonObjectBuilder.addProperty("enrollment", id);
    return this;
  }

  public EnrollmentDataBuilder setStatus(String status) {
    jsonObjectBuilder.addProperty("status", status);

    return this;
  }

  public EnrollmentDataBuilder setTei(String tei) {
    jsonObjectBuilder.addProperty("trackedEntity", tei);

    return this;
  }

  public EnrollmentDataBuilder setEnrollmentDate(String date) {
    jsonObjectBuilder.addProperty("enrolledAt", date);
    if (jsonObjectBuilder.build().has("events")) {
      jsonObjectBuilder.addPropertyByJsonPath("events[0].occurredAt", date);
    }
    return this;
  }

  public EnrollmentDataBuilder setIncidentDate(String date) {
    jsonObjectBuilder.addProperty("occurredAt", date);

    return this;
  }

  public EnrollmentDataBuilder setProgram(String programId) {
    jsonObjectBuilder.addProperty("program", programId);

    return this;
  }

  public EnrollmentDataBuilder setOu(String ouId) {
    jsonObjectBuilder.addProperty("orgUnit", ouId);

    return this;
  }

  public EnrollmentDataBuilder addEvent(EventDataBuilder builder) {
    jsonObjectBuilder.addOrAppendToArray("events", builder.single());
    return this;
  }

  public EnrollmentDataBuilder addEvent(String programStage, String orgUnit) {
    return addEvent(programStage, orgUnit, "ACTIVE");
  }

  public EnrollmentDataBuilder addEvent(String programStage, String orgUnit, String status) {
    // String eventDate = this.jsonObjectBuilder.build().get( "enrolledAt"
    // ).getAsString();

    return addEvent(
        new EventDataBuilder().setProgramStage(programStage).setOu(orgUnit).setStatus(status));
  }

  public EnrollmentDataBuilder addAttribute(String attributeId, String value) {
    jsonObjectBuilder.addOrAppendToArray(
        "attributes",
        new JsonObjectBuilder()
            .addProperty("attribute", attributeId)
            .addProperty("value", value)
            .build());

    return this;
  }

  public JsonObject array(String program, String ou) {
    setProgram(program).setOu(ou);
    return array();
  }

  public JsonObject array(String program, String ou, String tei, String status) {
    setProgram(program).setOu(ou).setStatus(status).setTei(tei);

    return array();
  }

  @Override
  public JsonObject array() {
    return jsonObjectBuilder.wrapIntoArray("enrollments");
  }

  @Override
  public JsonObject single() {
    return jsonObjectBuilder.build();
  }
}
