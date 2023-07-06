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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ATOMIC_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.FLUSH_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.IMPORT_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.IMPORT_STRATEGY_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.VALIDATION_MODE_KEY;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class TrackerImportParamsBuilderTest {

  private Map<String, List<String>> paramMap = new HashMap<>();

  @Test
  void testDefaultParams() {
    final TrackerImportParams params = TrackerImportParamsBuilder.build(null);
    assertDefaultParams(params);
  }

  @Test
  void testValidationMode() {
    Arrays.stream(ValidationMode.values())
        .forEach(
            e -> {
              paramMap.put(VALIDATION_MODE_KEY.getKey(), Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getValidationMode(), is(e));
            });
  }

  @Test
  void testImportMode() {
    Arrays.stream(TrackerBundleMode.values())
        .forEach(
            e -> {
              paramMap.put(IMPORT_MODE_KEY.getKey(), Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getImportMode(), is(e));
            });
  }

  @Test
  void testAtomicMode() {
    Arrays.stream(AtomicMode.values())
        .forEach(
            e -> {
              paramMap.put(ATOMIC_MODE_KEY.getKey(), Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getAtomicMode(), is(e));
            });
  }

  @Test
  void testFlushMode() {
    Arrays.stream(FlushMode.values())
        .forEach(
            e -> {
              paramMap.put(FLUSH_MODE_KEY.getKey(), Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getFlushMode(), is(e));
            });
  }

  @Test
  void testImportStrategy() {
    Arrays.stream(TrackerImportStrategy.values())
        .forEach(
            e -> {
              paramMap.put(IMPORT_STRATEGY_KEY.getKey(), Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getImportStrategy(), is(e));
            });
  }

  @Test
  void testOrgUnitIdentifier() {
    Arrays.stream(TrackerIdScheme.values())
        .forEach(
            e -> {
              paramMap.put("orgUnitIdScheme", Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getIdSchemes().getOrgUnitIdScheme().getIdScheme(), is(e));
            });
  }

  @Test
  void testProgramIdentifier() {
    Arrays.stream(TrackerIdScheme.values())
        .forEach(
            e -> {
              paramMap.put("programIdScheme", Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getIdSchemes().getProgramIdScheme().getIdScheme(), is(e));
            });
  }

  @Test
  void testProgramIdentifierUsingIdSchemeUidAndValue() {
    // only ATTRIBUTE is accompanied by a value; invalid idSchemes default
    // to UID
    paramMap.put("programIdScheme", Collections.singletonList("UID:WSiOAALYocA"));
    TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);

    assertThat(params.getIdSchemes().getProgramIdScheme().getIdScheme(), is(TrackerIdScheme.UID));
    assertNull(params.getIdSchemes().getProgramIdScheme().getValue());
  }

  @Test
  void testProgramIdentifierUsingIdSchemeCodeAndValue() {
    // only ATTRIBUTE is accompanied by a value; invalid idSchemes default
    // to UID
    paramMap.put("programIdScheme", Collections.singletonList("CODE:WSiOAALYocA"));
    TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);

    assertThat(params.getIdSchemes().getProgramIdScheme().getIdScheme(), is(TrackerIdScheme.UID));
    assertNull(params.getIdSchemes().getProgramIdScheme().getValue());
  }

  @Test
  void testProgramIdentifierUsingIdSchemeNameAndValue() {
    // only ATTRIBUTE is accompanied by a value; invalid idSchemes default
    // to UID
    paramMap.put("programIdScheme", Collections.singletonList("NAME:WSiOAALYocA"));
    TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);

    assertThat(params.getIdSchemes().getProgramIdScheme().getIdScheme(), is(TrackerIdScheme.UID));
    assertNull(params.getIdSchemes().getProgramIdScheme().getValue());
  }

  @Test
  void testProgramIdentifierUsingIdSchemeAttribute() {
    paramMap.put("programIdScheme", Collections.singletonList("ATTRIBUTE:WSiOAALYocA"));
    TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);

    assertThat(
        params.getIdSchemes().getProgramIdScheme().getIdScheme(), is(TrackerIdScheme.ATTRIBUTE));
    assertThat(params.getIdSchemes().getProgramIdScheme().getValue(), is("WSiOAALYocA"));
  }

  @Test
  void testProgramStageIdentifier() {
    Arrays.stream(TrackerIdScheme.values())
        .forEach(
            e -> {
              paramMap.put("programStageIdScheme", Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getIdSchemes().getProgramStageIdScheme().getIdScheme(), is(e));
            });
  }

  @Test
  void testDataElementIdentifier() {
    Arrays.stream(TrackerIdScheme.values())
        .forEach(
            e -> {
              paramMap.put("dataElementIdScheme", Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getIdSchemes().getDataElementIdScheme().getIdScheme(), is(e));
            });
  }

  @Test
  void testCategoryOptionComboIdentifier() {
    Arrays.stream(TrackerIdScheme.values())
        .forEach(
            e -> {
              paramMap.put("categoryOptionComboIdScheme", Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(
                  params.getIdSchemes().getCategoryOptionComboIdScheme().getIdScheme(), is(e));
            });
  }

  @Test
  void testCategoryOptionIdentifier() {
    Arrays.stream(TrackerIdScheme.values())
        .forEach(
            e -> {
              paramMap.put("categoryOptionIdScheme", Collections.singletonList(e.name()));
              TrackerImportParams params = TrackerImportParamsBuilder.build(paramMap);
              assertThat(params.getIdSchemes().getCategoryOptionIdScheme().getIdScheme(), is(e));
            });
  }

  private void assertDefaultParams(TrackerImportParams params) {
    assertThat(params.getValidationMode(), is(ValidationMode.FULL));
    assertThat(params.getImportMode(), is(TrackerBundleMode.COMMIT));
    assertThat(params.getImportStrategy(), is(TrackerImportStrategy.CREATE_AND_UPDATE));
    assertThat(params.getAtomicMode(), is(AtomicMode.ALL));
    assertThat(params.getFlushMode(), is(FlushMode.AUTO));
    TrackerIdSchemeParams identifiers = params.getIdSchemes();
    assertThat(identifiers.getOrgUnitIdScheme(), is(TrackerIdSchemeParam.UID));
    assertThat(identifiers.getProgramIdScheme(), is(TrackerIdSchemeParam.UID));
    assertThat(identifiers.getCategoryOptionComboIdScheme(), is(TrackerIdSchemeParam.UID));
    assertThat(identifiers.getCategoryOptionIdScheme(), is(TrackerIdSchemeParam.UID));
    assertThat(identifiers.getDataElementIdScheme(), is(TrackerIdSchemeParam.UID));
    assertThat(identifiers.getProgramStageIdScheme(), is(TrackerIdSchemeParam.UID));
    assertThat(identifiers.getIdScheme(), is(TrackerIdSchemeParam.UID));
  }
}
