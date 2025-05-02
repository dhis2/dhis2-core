/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis;

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.test.e2e.dependsOn.DependencyFile;
import org.hisp.dhis.test.e2e.dependsOn.DependencySetupException;
import org.hisp.dhis.test.e2e.dependsOn.ResourceType;
import org.hisp.dhis.test.e2e.dependsOn.JsonDependencyLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonDependencyLoaderTest {

  @Test
  @DisplayName("Valid PI file loads successfully")
  void loadValidPi() {
    DependencyFile df = JsonDependencyLoader.load("dependencies/pi-valid.json");

    assertEquals(ResourceType.PROGRAM_INDICATOR, df.type(), "Type should be PI");
    assertEquals("PI_TEST_001", df.payload().get("code").asText(), "Code should match JSON");
  }

  @Test
  @DisplayName("Missing \"type\" attribute raises DependencySetupException")
  void missingTypeThrows() {
    assertThrows(
        DependencySetupException.class,
        () -> JsonDependencyLoader.load("dependencies/pi-missing-type.json"));
  }
}
