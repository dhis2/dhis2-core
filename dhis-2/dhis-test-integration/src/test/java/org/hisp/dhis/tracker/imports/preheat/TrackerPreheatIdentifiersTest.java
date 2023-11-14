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
package org.hisp.dhis.tracker.imports.preheat;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.imports.TrackerIdSchemeParams.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class TrackerPreheatIdentifiersTest extends TrackerTest {
  @Autowired private TrackerPreheatService trackerPreheatService;

  @Autowired private IdentifiableObjectManager manager;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/identifier_metadata.json");
  }

  @Test
  void testOrgUnitIdentifiers() {
    List<Pair<String, TrackerIdSchemeParam>> data = buildDataSet("PlKwabX2xRW", "COU1", "Country");
    for (Pair<String, TrackerIdSchemeParam> pair : data) {
      String id = pair.getLeft();
      TrackerIdSchemeParam param = pair.getRight();
      Event event = Event.builder().orgUnit(param.toMetadataIdentifier(id)).build();
      TrackerImportParams params = buildParams(event, builder().orgUnitIdScheme(param).build());

      TrackerPreheat preheat = trackerPreheatService.preheat(params);

      assertPreheatedObjectExists(preheat, OrganisationUnit.class, param, id);
    }
  }

  @Test
  void testProgramStageIdentifiers() {
    List<Pair<String, TrackerIdSchemeParam>> data = buildDataSet("NpsdDv6kKSO", "PRGA", "ProgramA");
    for (Pair<String, TrackerIdSchemeParam> pair : data) {
      String id = pair.getLeft();
      TrackerIdSchemeParam param = pair.getRight();
      Event event = Event.builder().programStage(param.toMetadataIdentifier(id)).build();
      TrackerImportParams params =
          buildParams(event, builder().programStageIdScheme(param).build());

      TrackerPreheat preheat = trackerPreheatService.preheat(params);

      assertPreheatedObjectExists(preheat, ProgramStage.class, param, id);
    }
  }

  @Test
  void testDataElementIdentifiers() {
    List<Pair<String, TrackerIdSchemeParam>> data = buildDataSet("DSKTW8qFP0z", "DEAGE", "DE Age");
    for (Pair<String, TrackerIdSchemeParam> pair : data) {
      String id = pair.getLeft();
      TrackerIdSchemeParam param = pair.getRight();
      DataValue dv1 =
          DataValue.builder().dataElement(param.toMetadataIdentifier(id)).value("val1").build();
      Event event =
          Event.builder()
              .programStage(MetadataIdentifier.ofUid("NpsdDv6kKSO"))
              .dataValues(Collections.singleton(dv1))
              .build();
      TrackerImportParams params = buildParams(event, builder().dataElementIdScheme(param).build());

      TrackerPreheat preheat = trackerPreheatService.preheat(params);

      assertPreheatedObjectExists(preheat, DataElement.class, param, id);
    }
  }

  @Test
  void testCategoryOptionIdentifiers() {
    List<Pair<String, TrackerIdSchemeParam>> data = buildDataSet("XXXrKDKCefk", "COA", "COAname");
    for (Pair<String, TrackerIdSchemeParam> pair : data) {
      String id = pair.getLeft();
      TrackerIdSchemeParam param = pair.getRight();
      Event event =
          Event.builder().attributeCategoryOptions(Set.of(param.toMetadataIdentifier(id))).build();
      TrackerImportParams params =
          buildParams(event, builder().categoryOptionIdScheme(param).build());

      TrackerPreheat preheat = trackerPreheatService.preheat(params);

      assertPreheatedObjectExists(preheat, CategoryOption.class, param, id);
    }
  }

  @Test
  void testCategoryOptionComboIdentifiers() {
    List<Pair<String, TrackerIdSchemeParam>> data = buildDataSet("XXXvX50cXC0", "COCA", "COCAname");
    for (Pair<String, TrackerIdSchemeParam> pair : data) {
      String id = pair.getLeft();
      TrackerIdSchemeParam param = pair.getRight();
      Event event = Event.builder().attributeOptionCombo(param.toMetadataIdentifier(id)).build();
      TrackerImportParams params =
          buildParams(event, builder().categoryOptionComboIdScheme(param).build());

      TrackerPreheat preheat = trackerPreheatService.preheat(params);

      assertPreheatedObjectExists(preheat, CategoryOptionCombo.class, param, id);
    }
  }

  @Test
  void testDefaultsWithIdSchemeUID() {

    TrackerImportParams params =
        TrackerImportParams.builder().user(currentUserService.getCurrentUser()).build();

    TrackerPreheat preheat = trackerPreheatService.preheat(params);

    assertPreheatHasDefault(preheat, Category.class);
    assertPreheatHasDefault(preheat, CategoryCombo.class);
    assertPreheatHasDefault(preheat, CategoryOption.class);
    assertPreheatHasDefault(preheat, CategoryOptionCombo.class);
  }

  @Test
  void testDefaultsWithIdSchemesOtherThanUID() {

    Event event = new Event();

    TrackerImportParams params =
        buildParams(
            event,
            builder()
                .idScheme(TrackerIdSchemeParam.NAME)
                .categoryOptionIdScheme(
                    TrackerIdSchemeParam.ofAttribute(CodeGenerator.generateUid()))
                .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
                .build());

    TrackerPreheat preheat = trackerPreheatService.preheat(params);

    assertPreheatHasDefault(preheat, Category.class);
    assertPreheatHasDefault(preheat, CategoryCombo.class);
    assertPreheatHasDefault(preheat, CategoryOption.class);
    assertPreheatHasDefault(preheat, CategoryOptionCombo.class);
  }

  private TrackerImportParams buildParams(Event event, TrackerIdSchemeParams idParams) {
    TrackerImportParams params =
        TrackerImportParams.builder()
            .events(Collections.singletonList(event))
            .user(currentUserService.getCurrentUser())
            .idSchemes(idParams)
            .build();
    return params;
  }

  private List<Pair<String, TrackerIdSchemeParam>> buildDataSet(
      String uid, String code, String name) {
    List<Pair<String, TrackerIdSchemeParam>> data = new ArrayList<>();
    data.add(ImmutablePair.of(uid, TrackerIdSchemeParam.UID));
    data.add(ImmutablePair.of(code, TrackerIdSchemeParam.CODE));
    data.add(ImmutablePair.of(name, TrackerIdSchemeParam.NAME));
    return data;
  }

  private void assertPreheatedObjectExists(
      TrackerPreheat preheat,
      Class<? extends IdentifiableObject> klazz,
      TrackerIdSchemeParam idSchemeParam,
      String id) {
    assertThat(
        "Expecting a preheated object for idSchemeParam: "
            + idSchemeParam.getIdScheme().name()
            + " with value: "
            + id,
        preheat.get(klazz, id),
        is(notNullValue()));
  }

  private <T extends IdentifiableObject> void assertPreheatHasDefault(
      TrackerPreheat preheat, Class<T> klass) {
    T actual = preheat.getDefault(klass);
    T expected = manager.getByName(klass, "default");
    assertNotNull(actual);
    assertNotNull(expected);
    // since these are mapped entities, not all fields are mapped
    // we should at least get the identifiers
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getUid(), actual.getUid());
    assertEquals(expected.getCode(), actual.getCode());
    assertEquals(expected.getName(), actual.getName());
  }
}
