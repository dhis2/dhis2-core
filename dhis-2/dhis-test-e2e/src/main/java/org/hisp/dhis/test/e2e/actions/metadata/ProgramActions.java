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
package org.hisp.dhis.test.e2e.actions.metadata;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.hisp.dhis.test.e2e.utils.SharingUtils;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramActions extends RestApiActions {
  public ProgramStageActions programStageActions;
  public ProgramRuleHandler programRuleHandler;

  public ProgramActions() {
    super("/programs");
    this.programStageActions = new ProgramStageActions();
    this.programRuleHandler = new ProgramRuleHandler();
  }

  public ApiResponse createProgram(String programType) {
    JsonObject object = buildProgram(programType);

    if (programType.equalsIgnoreCase("WITH_REGISTRATION")) {
      JsonObjectBuilder.jsonObject(object)
          .addObject("trackedEntityType", new JsonObjectBuilder().addProperty("id", "Q9GufDoplCL"));
    }

    return post(object);
  }

  public ApiResponse createTrackerProgram(String trackedEntityTypeId, String... orgUnitIds) {
    return createProgram("WITH_REGISTRATION", trackedEntityTypeId, orgUnitIds).validateStatus(201);
  }

  public String createProgramWithAccessLevel(String accessLevel, String... orgUnits) {
    String programId =
        this.createTrackerProgram(Constants.TRACKED_ENTITY_TYPE, orgUnits).extractUid();

    JsonObject sharingObject =
        SharingUtils.createSharingObject(null, "rwrw----", Map.of(), Map.of());

    JsonObject program =
        this.get(programId)
            .getBodyAsJsonBuilder()
            .addProperty("accessLevel", accessLevel)
            .addObject("sharing", sharingObject)
            .addProperty("onlyEnrollOnce", "false")
            .build();

    this.update(programId, program).validateStatus(200);
    this.createProgramStage(programId, "Program stage " + DataGenerator.randomString());

    return programId;
  }

  public ApiResponse createEventProgram(String... orgUnitsIds) {
    JsonObject body =
        new JsonObjectBuilder(buildProgram("WITHOUT_REGISTRATION", null, orgUnitsIds)).build();
    ApiResponse response = post(body);

    createProgramStage(response.extractUid(), "DEFAULT STAGE" + response.extractUid());

    return response;
  }

  public ApiResponse createProgram(
      String programType, String trackedEntityTypeId, String... orgUnitIds) {
    JsonObject object = buildProgram(programType, trackedEntityTypeId, orgUnitIds);

    return post(object);
  }

  public ApiResponse addProgramStage(String programId, String programStageId) {
    JsonObject body =
        get(programId)
            .getBodyAsJsonBuilder()
            .addArray(
                "programStages", new JsonObjectBuilder().addProperty("id", programStageId).build())
            .build();

    return update(programId, body);
  }

  /**
   * Creates a program stage and links it to the program.
   *
   * @param programId
   * @param programStageName
   * @return program stage id
   */
  public String createProgramStage(String programId, String programStageName) {
    JsonObject programStage =
        new JsonObjectBuilder()
            .addProperty("name", programStageName)
            .addProperty("code", programStageName)
            .addObject("program", new JsonObjectBuilder().addProperty("id", programId))
            .addObject("sharing", SharingUtils.createSharingObject("rwrw----"))
            .build();

    ApiResponse response = programStageActions.post(programStage);

    response.validate().statusCode(is(oneOf(201, 200)));
    return response.extractUid();
  }

  /**
   * Creates a program rule and links it to the program.
   *
   * @param programId
   * @param programRuleName
   * @return program rule id
   */
  public String createProgramRule(String programId, String programRuleName) {
    JsonObject programRule =
        new JsonObjectBuilder()
            .addProperty("name", programRuleName)
            .addProperty("code", programRuleName)
            .addObject("program", new JsonObjectBuilder().addProperty("id", programId))
            .build();

    ApiResponse response = programRuleHandler.post(programRule);

    response.validate().statusCode(is(oneOf(201, 200)));
    return response.extractUid();
  }

  public ApiResponse addOrganisationUnits(String programId, String... orgUnitIds) {
    JsonObject object = this.get(programId).getBody();

    JsonArray orgUnits =
        Optional.ofNullable(object.getAsJsonArray("organisationUnits")).orElse(new JsonArray());

    for (String ouid : orgUnitIds) {
      JsonObject orgUnit = new JsonObject();
      orgUnit.addProperty("id", ouid);

      orgUnits.add(orgUnit);
    }

    object.add("organisationUnits", orgUnits);

    return this.update(programId, object);
  }

  public ApiResponse addDataElement(
      String programStageId, String dataElementId, boolean isMandatory) {
    JsonObject object =
        programStageActions.get(programStageId, new QueryParamsBuilder().add("fields=*")).getBody();

    JsonObjectBuilder.jsonObject(object)
        .addOrAppendToArray(
            "programStageDataElements",
            new JsonObjectBuilder()
                .addProperty("compulsory", String.valueOf(isMandatory))
                .addObject("dataElement", new JsonObjectBuilder().addProperty("id", dataElementId))
                .build());

    return programStageActions.update(programStageId, object);
  }

  public ApiResponse addAttribute(String programId, String teAttributeId, boolean isMandatory) {
    JsonObject object =
        this.get(programId, new QueryParamsBuilder().add("fields=*"))
            .getBodyAsJsonBuilder()
            .addOrAppendToArray(
                "programTrackedEntityAttributes",
                new JsonObjectBuilder()
                    .addProperty("mandatory", String.valueOf(isMandatory))
                    .addObject(
                        "trackedEntityAttribute",
                        new JsonObjectBuilder().addProperty("id", teAttributeId))
                    .build())
            .build();

    JsonObjectBuilder.jsonObject(object);

    return this.update(programId, object).validateStatus(200);
  }

  /** Adds or replaces program category mappings (idempotent) */
  public ApiResponse addOrReplaceCategoryMappings(String programId, List<JsonObject> mappings) {
    JsonObjectBuilder builder =
        this.get(programId, new QueryParamsBuilder().add("fields=*")).getBodyAsJsonBuilder();

    for (JsonObject mapping : mappings) {
      builder.removeFromArray("categoryMappings", "categoryId", mapping.get("categoryId"));
      builder.addOrAppendToArray("categoryMappings", mapping);
    }

    return this.update(programId, builder.build()).validateStatus(200);
  }

  public JsonObject buildProgram() {
    String random = DataGenerator.randomString();

    JsonObject object =
        JsonObjectBuilder.jsonObject()
            .addProperty("name", "AutoTest program " + random)
            .addProperty("shortName", "AutoTest program " + random)
            .addProperty("code", "TA_PROGRAM_" + random)
            .addObject("sharing", SharingUtils.createSharingObject("rwrw----"))
            .addUserGroupAccess()
            .build();

    return object;
  }

  public JsonObject buildProgram(String programType) {
    return new JsonObjectBuilder(buildProgram())
        .addProperty("programType", programType)
        .addProperty("displayFrontPageList", "true")
        .addObject("sharing", SharingUtils.createSharingObject("rwrw----"))
        .build();
  }

  public JsonObject buildProgram(
      String programType, String trackedEntityTypeId, String... orgUnitIds) {
    JsonObjectBuilder builder = new JsonObjectBuilder(buildProgram(programType));

    for (String ouid : orgUnitIds) {
      builder.addOrAppendToArray(
          "organisationUnits", new JsonObjectBuilder().addProperty("id", ouid).build());
    }

    if (!StringUtils.isEmpty(trackedEntityTypeId)) {
      builder
          .addObject(
              "trackedEntityType", new JsonObjectBuilder().addProperty("id", trackedEntityTypeId))
          .build();
    }

    return builder.build();
  }

  public ApiResponse getOrgUnitsAssociations(String... programUids) {
    return get(
        "/orgUnits",
        new QueryParamsBuilder()
            .add(Arrays.stream(programUids).collect(Collectors.joining(",", "programs=", ""))));
  }
}
