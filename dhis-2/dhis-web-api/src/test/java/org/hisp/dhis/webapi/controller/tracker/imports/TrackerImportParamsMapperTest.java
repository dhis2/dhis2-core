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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.FlushMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundleMode;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class TrackerImportParamsMapperTest {

  @Test
  void testDefaultParams() {
    final TrackerImportParams params = TrackerImportParams.builder().build();
    assertDefaultParams(params);
  }

  @Test
  void testValidationMode() {
    Arrays.stream(ValidationMode.values())
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().validationMode(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(params.getValidationMode(), is(e));
            });
  }

  @Test
  void testImportMode() {
    Arrays.stream(TrackerBundleMode.values())
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().importMode(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(params.getImportMode(), is(e));
            });
  }

  @Test
  void testAtomicMode() {
    Arrays.stream(AtomicMode.values())
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().atomicMode(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(params.getAtomicMode(), is(e));
            });
  }

  @Test
  void testFlushMode() {
    Arrays.stream(FlushMode.values())
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().flushMode(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(params.getFlushMode(), is(e));
            });
  }

  @Test
  void testImportStrategy() {
    Arrays.stream(TrackerImportStrategy.values())
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().importStrategy(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(params.getImportStrategy(), is(e));
            });
  }

  @Test
  void testIdSchemeUsingIdSchemeName() {
    ImportRequestParams importRequestParams =
        ImportRequestParams.builder().idScheme(TrackerIdSchemeParam.NAME).build();
    TrackerImportParams params = TrackerImportParamsMapper.trackerImportParams(importRequestParams);

    TrackerIdSchemeParam expected = TrackerIdSchemeParam.NAME;
    assertEquals(expected, params.getIdSchemes().getIdScheme());
    assertEquals(expected, params.getIdSchemes().getDataElementIdScheme());
    assertEquals(expected, params.getIdSchemes().getOrgUnitIdScheme());
    assertEquals(expected, params.getIdSchemes().getProgramIdScheme());
    assertEquals(expected, params.getIdSchemes().getProgramStageIdScheme());
    assertEquals(expected, params.getIdSchemes().getCategoryOptionComboIdScheme());
    assertEquals(expected, params.getIdSchemes().getCategoryOptionIdScheme());
  }

  @Test
  void testIdSchemeUsingIdSchemeAttribute() {
    ImportRequestParams importRequestParams =
        ImportRequestParams.builder()
            .idScheme(TrackerIdSchemeParam.ofAttribute("WSiOAALYocA"))
            .build();
    TrackerImportParams params = TrackerImportParamsMapper.trackerImportParams(importRequestParams);

    TrackerIdSchemeParam expected = TrackerIdSchemeParam.ofAttribute("WSiOAALYocA");
    assertEquals(expected, params.getIdSchemes().getIdScheme());
    assertEquals(expected, params.getIdSchemes().getDataElementIdScheme());
    assertEquals(expected, params.getIdSchemes().getOrgUnitIdScheme());
    assertEquals(expected, params.getIdSchemes().getProgramIdScheme());
    assertEquals(expected, params.getIdSchemes().getProgramStageIdScheme());
    assertEquals(expected, params.getIdSchemes().getCategoryOptionComboIdScheme());
    assertEquals(expected, params.getIdSchemes().getCategoryOptionIdScheme());
  }

  @Test
  void testOrgUnitIdentifier() {
    idSchemeParams()
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().orgUnitIdScheme(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(
                  params.getIdSchemes().getOrgUnitIdScheme().getIdScheme(), is(e.getIdScheme()));
            });
  }

  @Test
  void testProgramIdentifier() {
    idSchemeParams()
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().programIdScheme(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(
                  params.getIdSchemes().getProgramIdScheme().getIdScheme(), is(e.getIdScheme()));
            });
  }

  @Test
  void testProgramIdentifierUsingIdSchemeAttribute() {
    ImportRequestParams importRequestParams =
        ImportRequestParams.builder()
            .programIdScheme(TrackerIdSchemeParam.ofAttribute("WSiOAALYocA"))
            .build();
    TrackerImportParams params = TrackerImportParamsMapper.trackerImportParams(importRequestParams);

    assertEquals(
        TrackerIdSchemeParam.ofAttribute("WSiOAALYocA"),
        params.getIdSchemes().getProgramIdScheme());
  }

  @Test
  void testProgramStageIdentifier() {
    idSchemeParams()
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().programStageIdScheme(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(
                  params.getIdSchemes().getProgramStageIdScheme().getIdScheme(),
                  is(e.getIdScheme()));
            });
  }

  @Test
  void testDataElementIdentifier() {
    idSchemeParams()
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().dataElementIdScheme(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(
                  params.getIdSchemes().getDataElementIdScheme().getIdScheme(),
                  is(e.getIdScheme()));
            });
  }

  @Test
  void testCategoryOptionComboIdentifier() {
    idSchemeParams()
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().categoryOptionComboIdScheme(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(
                  params.getIdSchemes().getCategoryOptionComboIdScheme().getIdScheme(),
                  is(e.getIdScheme()));
            });
  }

  @Test
  void testCategoryOptionIdentifier() {
    idSchemeParams()
        .forEach(
            e -> {
              ImportRequestParams importRequestParams =
                  ImportRequestParams.builder().categoryOptionIdScheme(e).build();
              TrackerImportParams params =
                  TrackerImportParamsMapper.trackerImportParams(importRequestParams);
              assertThat(
                  params.getIdSchemes().getCategoryOptionIdScheme().getIdScheme(),
                  is(e.getIdScheme()));
            });
  }

  private List<TrackerIdSchemeParam> idSchemeParams() {
    return List.of(
        TrackerIdSchemeParam.UID,
        TrackerIdSchemeParam.CODE,
        TrackerIdSchemeParam.NAME,
        TrackerIdSchemeParam.ofAttribute("attributeUid"));
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
