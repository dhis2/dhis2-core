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
package org.hisp.dhis.tracker.imports.databuilder;

import com.google.gson.JsonObject;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackedEntityDataBuilder implements TrackerImporterDataBuilder {
  private JsonObjectBuilder jsonObjectBuilder;

  public TrackedEntityDataBuilder() {
    jsonObjectBuilder = new JsonObjectBuilder();
    setTrackedEntityType(Constants.TRACKED_ENTITY_TYPE);
  }

  public TrackedEntityDataBuilder setId(String id) {
    this.jsonObjectBuilder.addProperty("trackedEntity", id);

    return this;
  }

  public TrackedEntityDataBuilder setOrgUnit(String ou) {
    jsonObjectBuilder.addProperty("orgUnit", ou);
    return this;
  }

  public TrackedEntityDataBuilder setTrackedEntityType(String teTypeId) {
    jsonObjectBuilder.addProperty("trackedEntityType", teTypeId);

    return this;
  }

  public TrackedEntityDataBuilder addEnrollment(EnrollmentDataBuilder enrollmentDataBuilder) {
    jsonObjectBuilder.addOrAppendToArray("enrollments", enrollmentDataBuilder.single());
    return this;
  }

  public TrackedEntityDataBuilder addEnrollment(String programId, String ouId) {
    return addEnrollment(new EnrollmentDataBuilder().setProgram(programId).setOrgUnit(ouId));
  }

  public TrackedEntityDataBuilder addAttribute(String attributeId, String value) {
    this.jsonObjectBuilder.addOrAppendToArray(
        "attributes",
        new JsonObjectBuilder()
            .addProperty("attribute", attributeId)
            .addProperty("value", value)
            .build());

    return this;
  }

  public TrackedEntityDataBuilder addRelationship(RelationshipDataBuilder builder) {
    jsonObjectBuilder.addOrAppendToArray("relationships", builder.build());

    return this;
  }

  public JsonObject array(String trackedEntityType, String ou) {
    this.setOrgUnit(ou).setTrackedEntityType(trackedEntityType);
    return array();
  }

  public JsonObject buildWithEnrollment(String ou, String program) {
    this.setOrgUnit(ou).addEnrollment(program, ou);

    return array();
  }

  public JsonObject buildWithEnrollment(String trackedEntityType, String ou, String program) {
    this.setOrgUnit(ou).setTrackedEntityType(trackedEntityType).addEnrollment(program, ou);

    return array();
  }

  /**
   * Builds a te with enrollment and event
   *
   * @param trackedEntityType
   * @param ou
   * @param program
   * @param programStage
   * @return
   */
  public JsonObject buildWithEnrollmentAndEvent(
      String trackedEntityType, String ou, String program, String programStage) {
    this.setOrgUnit(ou)
        .setTrackedEntityType(trackedEntityType)
        .addEnrollment(
            new EnrollmentDataBuilder()
                .setProgram(program)
                .setOrgUnit(ou)
                .addEvent(programStage, ou));

    return array();
  }

  public JsonObject buildWithEnrollmentAndEvent(
      String trackedEntityType,
      String ou,
      String program,
      String programStage,
      String eventStatus) {
    this.setOrgUnit(ou)
        .setTrackedEntityType(trackedEntityType)
        .addEnrollment(
            new EnrollmentDataBuilder()
                .setProgram(program)
                .setOrgUnit(ou)
                .addEvent(
                    new EventDataBuilder()
                        .setProgramStage(programStage)
                        .setStatus(eventStatus)
                        .setOrgUnit(ou)));

    return array();
  }

  @Override
  public JsonObject array() {
    return jsonObjectBuilder.wrapIntoArray("trackedEntities");
  }

  @Override
  public JsonObject single() {
    return jsonObjectBuilder.build();
  }
}
