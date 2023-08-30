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
package org.hisp.dhis.tracker;

import static org.hamcrest.CoreMatchers.describedAs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackedEntityAttributeValueGenerationTests extends TrackerApiTest {
  private RestApiActions trackedEntityAttributeActions;

  @BeforeAll
  public void beforeAll() {
    trackedEntityAttributeActions = new RestApiActions("/trackedEntityAttributes");

    new LoginActions().loginAsSuperUser();
  }

  private Stream<Arguments> shouldGenerateAttributeValues() {
    ApiResponse response =
        trackedEntityAttributeActions.get(
            new QueryParamsBuilder()
                .add("filter", "generated:eq:true")
                .add("filter", "pattern:!like:()")
                .add("fields", "id,pattern"));

    List<Map<?, ?>> attributes = response.extractList("trackedEntityAttributes");

    List<Arguments> arguments = new ArrayList<>();

    attributes.forEach(
        att -> {
          arguments.add(
              Arguments.of(att.get("id"), att.get("pattern").toString().replaceAll("[+,\" ]", "")));
        });

    return arguments.stream();
  }

  @MethodSource
  @ParameterizedTest(name = "/generate value for TEA with pattern {1}")
  public void shouldGenerateAttributeValues(String uid, String pattern) {
    int numberOfDigitsInPattern = StringUtils.countMatches(pattern, "#");

    trackedEntityAttributeActions
        .get(uid + "/generate")
        .validate()
        .statusCode(200)
        .body("key", equalTo(pattern))
        .body(
            "value",
            describedAs(
                "Generated value should end with " + numberOfDigitsInPattern + " digits",
                matchesPattern(String.format(".*\\d{%d}$", numberOfDigitsInPattern))));
  }

  @Test
  public void shouldGenerateAttributeValueWithOrgUnitCode() {
    String ouCode = "OU_559";
    String attribute =
        trackedEntityAttributeActions
            .get(new QueryParamsBuilder().add("filter", "pattern:like:ORG_UNIT_CODE()"))
            .extractString("trackedEntityAttributes.id[0]");

    assertNotNull(
        attribute, "Tracked entity attribute with pattern containing OU_CODE was not found");
    trackedEntityAttributeActions
        .get(attribute + "/generate", new QueryParamsBuilder().add("ORG_UNIT_CODE", ouCode))
        .validate()
        .statusCode(200)
        .body("key", startsWith(ouCode))
        .body("value", startsWith(ouCode));
  }
}
