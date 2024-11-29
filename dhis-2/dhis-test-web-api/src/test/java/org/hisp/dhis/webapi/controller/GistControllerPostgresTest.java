/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
