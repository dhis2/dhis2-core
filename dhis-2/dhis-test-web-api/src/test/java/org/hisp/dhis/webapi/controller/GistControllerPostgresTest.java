/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Gist API that cannot run on H2 but require an actual postgres DB.
 *
 * @author Jan Bernitt
 */
class GistControllerPostgresTest extends AbstractGistControllerPostgresTest {

  /**
   * Note: this test was moved here unchanged to verify the functionality in the API hasn't changed.
   * However, when the "name" became a generated column instead of using a from transformation it
   * has to run with an actual postgres DB. The test name is kept to allow seeing the evolution
   * looking back to versions that do use from instead of a generated column to synthesize the name
   * from firstName and surname post DB query.
   */
  @Test
  void testField_UserNameAutomaticFromTransformation() {
    JsonArray users = GET("/users/gist?fields=id,name&headless=true").content();
    assertEquals(
        "FirstNameuserGist SurnameuserGist", users.getObject(1).getString("name").string());
  }

  /** Note: now that user "name" is a generated column we can also filter on it */
  @Test
  void testFilter_UserName() {
    JsonArray users =
        GET("/users/gist?fields=id,name&headless=true&filter=name:like:Gist").content();
    assertFalse(users.isEmpty());
  }

  @Test
  void testFilter_AttributeValuesAttribute() {
    String attr1Id = postNewAttribute("A1attr", ValueType.TEXT, Attribute.ObjectType.USER_GROUP);
    String attr2Id = postNewAttribute("A2attr", ValueType.NUMBER, Attribute.ObjectType.USER_GROUP);
    String group1Id = postNewUserGroupWithAttributeValue("GA1", attr1Id, "hey");
    String group2Id = postNewUserGroupWithAttributeValue("GA2", attr2Id, "42");
    JsonArray groups =
        GET("/userGroups/gist?fields=id,name&headless=true&filter=attributeValues.attribute.name:like:A1")
            .content();
    assertEquals(1, groups.size());
    assertEquals(group1Id, groups.getObject(0).getString("id").string());
  }

  private String postNewAttribute(
      String name, ValueType valueType, Attribute.ObjectType objectType) {
    String body =
        "{'name':'%s', 'valueType':'%s', '%s':true}"
            .formatted(name, valueType.name(), objectType.getPropertyName());
    return assertStatus(HttpStatus.CREATED, POST("/attributes", body));
  }

  private String postNewUserGroupWithAttributeValue(String name, String attrId, String value) {
    // language=JSON
    String json =
        """
        {
        "name":"%s",
        "attributeValues":[{"attribute": {"id":"%s"}, "value":"%s"}]
        }
        """
            .formatted(name, attrId, value);
    return assertStatus(HttpStatus.CREATED, POST("/userGroups/", json));
  }
}
