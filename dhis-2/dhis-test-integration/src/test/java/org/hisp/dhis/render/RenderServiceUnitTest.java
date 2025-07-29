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
package org.hisp.dhis.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.category.CategoryCombo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class RenderServiceUnitTest {

  @Test
  @DisplayName("import cc with cocs test")
  void importCcWithCocsTest() throws JsonProcessingException {
    // given
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    JsonNode jsonNode = mapper.readTree(getCatCombo());

    // when
    CategoryCombo categoryCombo = mapper.treeToValue(jsonNode, CategoryCombo.class);

    // then
    assertEquals(2, categoryCombo.getOptionCombos().size());
  }

  private String getCatCombo() {
    return """
    {
      "created": "2016-10-10T15:41:06.551",
      "lastUpdated": "2016-10-10T15:41:06.719",
      "createdBy": {
        "id": "GOLswS44mh8",
        "code": null,
        "name": "Tom Wakiki",
        "displayName": "Tom Wakiki",
        "username": "system"
      },
      "code": "Gender",
      "name": "Gender",
      "categories": [
        {
          "id": "cX5k9anHEHd"
        }
      ],
      "dataDimensionType": "DISAGGREGATION",
      "skipTotal": false,
      "sharing": {
        "owner": "GOLswS44mh8",
        "external": false,
        "users": {},
        "userGroups": {}
      },
      "translations": [],
      "displayName": "Gender",
      "access": {
        "manage": true,
        "externalize": false,
        "write": true,
        "read": true,
        "update": true,
        "delete": true
      },
      "user": {
        "id": "GOLswS44mh8",
        "code": null,
        "name": "Tom Wakiki",
        "displayName": "Tom Wakiki",
        "username": "system"
      },
      "id": "dPmavA0qloX",
      "categoryOptionCombos": [
        {
            "id": "qk6n4eMAdtK"
        },
        {
            "id": "KQ50BVoUrd6"
        }
      ],
      "isDefault": false
    }
    """;
  }
}
