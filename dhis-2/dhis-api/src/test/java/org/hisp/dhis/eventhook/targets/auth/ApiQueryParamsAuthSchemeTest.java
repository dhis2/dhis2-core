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
package org.hisp.dhis.eventhook.targets.auth;

import java.util.Map;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.junit.jupiter.api.Test;

class ApiQueryParamsAuthSchemeTest extends AbstractAuthSchemeTest {

  @Test
  void testEncrypt() {
    assertEncrypt(
        new ApiQueryParamsAuthScheme()
            .setQueryParams(
                Map.of(
                    "token", "T5pvst37VedtsoD70KlbumzI30Mo4pzzyAY0M6Ia8uYyPBLPeXlYzr4d3LPQD6oS")),
        apiQueryParamsAuthScheme -> apiQueryParamsAuthScheme.getQueryParams().get("token"));
  }

  @Test
  void testDecrypt() {
    assertDecrypt(
        new ApiQueryParamsAuthScheme()
            .setQueryParams(
                Map.of(
                    "token", "3PB06m2bcr0blf81OEpcIDUMUYQYHJcdQsBJyOwbmelTYBQ6fuskAGJReGgM30Cv")),
        apiQueryParamsAuthScheme -> apiQueryParamsAuthScheme.getQueryParams().get("token"));
  }
}
