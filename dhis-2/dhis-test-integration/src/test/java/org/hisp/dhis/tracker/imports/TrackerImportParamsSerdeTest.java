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
package org.hisp.dhis.tracker.imports;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundleMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luciano Fiandesio
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerImportParamsSerdeTest extends PostgresIntegrationTestBase {

  @Autowired private RenderService renderService;

  @Test
  void testJsonSerialization() throws Exception {
    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .idScheme(TrackerIdSchemeParam.CODE)
            .programIdScheme(TrackerIdSchemeParam.ofAttribute("aaaa"))
            .build();
    TrackerImportParams trackerImportParams =
        TrackerImportParams.builder()
            .idSchemes(identifierParams)
            .atomicMode(AtomicMode.OBJECT)
            .flushMode(FlushMode.OBJECT)
            .skipRuleEngine(true)
            .importStrategy(TrackerImportStrategy.DELETE)
            .validationMode(ValidationMode.SKIP)
            .build();
    String json = renderService.toJsonAsString(trackerImportParams);
    JSONAssert.assertEquals(
        """
        {"importMode":"COMMIT",\
        "idSchemes":{"dataElementIdScheme":{"idScheme":"CODE"},\
        "orgUnitIdScheme":{"idScheme":"CODE"},\
        "programIdScheme":{"idScheme":"ATTRIBUTE","attributeUid":"aaaa"},\
        "programStageIdScheme":{"idScheme":"CODE"},\
        "idScheme":{"idScheme":"CODE"},\
        "categoryOptionComboIdScheme":{"idScheme":"CODE"},\
        "categoryOptionIdScheme":{"idScheme":"CODE"}},\
        "importStrategy":"DELETE",\
        "atomicMode":"OBJECT",\
        "flushMode":"OBJECT",\
        "validationMode":"SKIP",\
        "skipPatternValidation":false,\
        "skipSideEffects":false,\
        "skipRuleEngine":true}""",
        json,
        JSONCompareMode.LENIENT);
  }

  @Test
  void testJsonDeserialization() throws IOException {
    final String json =
        """
        {"importMode":"COMMIT",\
        "idSchemes":{"dataElementIdScheme":{"idScheme":"UID"},\
        "orgUnitIdScheme":{"idScheme":"UID"},\
        "programIdScheme":{"idScheme":"ATTRIBUTE","attributeUid":"aaaa"},\
        "programStageIdScheme":{"idScheme":"UID"},\
        "idScheme":{"idScheme":"CODE"},\
        "categoryOptionComboIdScheme":{"idScheme":"UID"},\
        "categoryOptionIdScheme":{"idScheme":"UID"}},\
        "importStrategy":"DELETE",\
        "atomicMode":"OBJECT",\
        "flushMode":"OBJECT",\
        "validationMode":"SKIP",\
        "skipPatternValidation":true,\
        "skipSideEffects":true,\
        "skipRuleEngine":true}""";

    final TrackerImportParams trackerImportParams =
        renderService.fromJson(json, TrackerImportParams.class);

    assertThat(trackerImportParams.getImportMode(), is(TrackerBundleMode.COMMIT));
    assertThat(trackerImportParams.getImportStrategy(), is(TrackerImportStrategy.DELETE));
    assertThat(trackerImportParams.getAtomicMode(), is(AtomicMode.OBJECT));
    assertThat(trackerImportParams.getFlushMode(), is(FlushMode.OBJECT));
    assertThat(trackerImportParams.getValidationMode(), is(ValidationMode.SKIP));
    assertThat(trackerImportParams.isSkipPatternValidation(), is(true));
    assertThat(trackerImportParams.isSkipSideEffects(), is(true));
    assertThat(trackerImportParams.isSkipRuleEngine(), is(true));
    TrackerIdSchemeParams idSchemes = trackerImportParams.getIdSchemes();
    assertThat(idSchemes.getIdScheme(), is(TrackerIdSchemeParam.CODE));
    assertThat(idSchemes.getProgramIdScheme().getIdScheme(), is(TrackerIdScheme.ATTRIBUTE));
    assertThat(idSchemes.getProgramIdScheme().getAttributeUid(), is("aaaa"));
  }
}
