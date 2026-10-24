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
package org.hisp.dhis.dataentryform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.Type;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.hibernate.jsonb.type.JsonSetBinaryType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataEntryFormStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataEntryFormStore store;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DataSetStore dataSetStore;

  @Autowired private ProgramStageStore programStageStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("get data entry forms which contain indicator refs in its htmlCode")
  void testAddDataEntryForm() {
    // given 4 data entry forms exist
    DataEntryForm form1 = createDataEntryForm('a');
    DataEntryForm form2 = createDataEntryForm('b');
    DataEntryForm form3 = createDataEntryForm('c');
    DataEntryForm form4 = createDataEntryForm('d');

    // 3 of which have specific search refs in their html code fields
    form1.setHtmlCode("<body><p>findMe1</p></body>");
    form2.setHtmlCode("<body><p>findMe2</p></body>");
    form3.setHtmlCode("<body><p>findMe2</p></body>");

    // 1 of which has a ref which won't be searched for
    form4.setHtmlCode("<body><p>dontFindMe</p></body>");

    manager.save(form1);
    manager.save(form2);
    manager.save(form3);
    manager.save(form4);

    // when searching for forms that contain a specific indicator ref
    List<DataEntryForm> matchedForms1 = store.getDataEntryFormsHtmlContaining("findMe1");

    // then only 1 form is retrieved matching a specific ref
    assertEquals(1, matchedForms1.size());
    assertTrue(matchedForms1.contains(form1));
    assertFalse(matchedForms1.contains(form2));
    assertFalse(matchedForms1.contains(form3));
    assertFalse(matchedForms1.contains(form4));

    // then only 2 forms are retrieved matching a specific ref
    List<DataEntryForm> matchedForms2 = store.getDataEntryFormsHtmlContaining("findMe2");
    assertEquals(2, matchedForms2.size());
    assertTrue(matchedForms2.contains(form2));
    assertTrue(matchedForms2.contains(form3));
    assertFalse(matchedForms2.contains(form1));
    assertFalse(matchedForms2.contains(form4));
  }

  @Test
  @DisplayName("the id is generated by a sequence generator on hibernate_sequence")
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  @Test
  @DisplayName("the persister maps every property with its type, nullability and cache strategy")
  void testPersisterMetadata() {
    EntityPersister persister = persister();
    assertEquals(
        List.of(
            "code",
            "created",
            "format",
            "htmlCode",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "style",
            "translations",
            "uid"),
        List.of(persister.getPropertyNames()).stream().sorted().toList());

    assertInstanceOf(TextType.class, persister.getPropertyType("htmlCode"));
    assertInstanceOf(StringType.class, persister.getPropertyType("name"));
    assertInstanceOf(IntegerType.class, persister.getPropertyType("format"));
    CustomType style = assertInstanceOf(CustomType.class, persister.getPropertyType("style"));
    EnumType<?> styleEnum = assertInstanceOf(EnumType.class, style.getUserType());
    assertEquals(DisplayDensity.class, styleEnum.returnedClass());
    assertFalse(styleEnum.isOrdinal());
    assertInstanceOf(JsonSetBinaryType.class, translationsUserType(persister));
    ManyToOneType lastUpdatedBy =
        assertInstanceOf(ManyToOneType.class, persister.getPropertyType("lastUpdatedBy"));
    assertFalse(lastUpdatedBy.isEager(null));

    for (String property : List.of("uid", "name", "created", "lastUpdated")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of("code", "style", "htmlCode", "format", "lastUpdatedBy", "translations")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    EntityMetamodel metamodel = persister.getEntityMetamodel();
    assertFalse(persister.getPropertyUpdateability()[metamodel.getPropertyIndex("created")]);

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  @DisplayName("every mapped property is written to its column and read back from it")
  void testColumnsRoundTrip() {
    User admin = getAdminUser();
    String htmlCode = "<p>%s</p>".formatted("x".repeat(100_000));
    DataEntryForm form = new DataEntryForm("Round trip form", DisplayDensity.COMPACT, htmlCode);
    form.setCode("CODE_ROUND_TRIP");
    form.setFormat(DataEntryForm.CURRENT_FORMAT);
    form.setLastUpdatedBy(admin);
    form.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Formulaire")));
    DataEntryForm saved = save(form);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, name, style, htmlcode, format, created, lastupdated,"
                + " lastupdatedby, translations::text as translations"
                + " from dataentryform where dataentryformid = ?",
            saved.getId());
    assertEquals(saved.getUid(), row.get("uid"));
    assertEquals("CODE_ROUND_TRIP", row.get("code"));
    assertEquals("Round trip form", row.get("name"));
    assertEquals("COMPACT", row.get("style"));
    assertEquals(htmlCode, row.get("htmlcode"));
    assertEquals(2, ((Number) row.get("format")).intValue());
    assertEquals(saved.getCreated().getTime(), ((Date) row.get("created")).getTime());
    assertEquals(saved.getLastUpdated().getTime(), ((Date) row.get("lastupdated")).getTime());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());
    assertTrue(((String) row.get("translations")).contains("\"Formulaire\""));

    DataEntryForm reloaded = reload(saved);
    assertEquals(saved.getUid(), reloaded.getUid());
    assertEquals("CODE_ROUND_TRIP", reloaded.getCode());
    assertEquals("Round trip form", reloaded.getName());
    assertEquals(DisplayDensity.COMPACT, reloaded.getStyle());
    assertEquals(htmlCode, reloaded.getHtmlCode());
    assertEquals(2, reloaded.getFormat());
    assertEquals(saved.getCreated().getTime(), reloaded.getCreated().getTime());
    assertEquals(saved.getLastUpdated().getTime(), reloaded.getLastUpdated().getTime());
    assertEquals(admin.getUid(), reloaded.getLastUpdatedBy().getUid());
    assertEquals(
        Set.of(new Translation(new Locale("fr"), "NAME", "Formulaire")),
        reloaded.getTranslations());
  }

  @Test
  @DisplayName("a form with only a name is written with null columns and empty translations")
  void testOptionalColumnsAreWrittenAsNull() {
    DataEntryForm form = new DataEntryForm("Name only form");
    form.setTranslations(null);
    DataEntryForm saved = save(form);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select code, style, htmlcode, translations::text as translations"
                + " from dataentryform where dataentryformid = ?",
            saved.getId());
    assertNull(row.get("code"));
    assertNull(row.get("style"));
    assertNull(row.get("htmlcode"));
    assertEquals("[]", row.get("translations"));

    DataEntryForm reloaded = reload(saved);
    assertNull(reloaded.getStyle());
    assertNull(reloaded.getHtmlCode());
    assertTrue(reloaded.getTranslations().isEmpty());
  }

  @Test
  @DisplayName("a form is found by its name after being reloaded from the database")
  void testGetDataEntryFormByName() {
    save(new DataEntryForm("Form by name", DisplayDensity.NORMAL, "<p>by name</p>"));
    save(new DataEntryForm("Other form", DisplayDensity.NORMAL, "<p>other</p>"));
    clearSession();
    entityManagerFactory.getCache().evict(DataEntryForm.class);

    DataEntryForm found = store.getDataEntryFormByName("Form by name");
    assertEquals("<p>by name</p>", found.getHtmlCode());
    assertNull(store.getDataEntryFormByName("No such form"));
  }

  @Test
  @DisplayName("data sets and program stages load their form and are found by it")
  void testDataSetAndProgramStageReferenceForm() {
    DataEntryForm form = save(createDataEntryForm('R', "<p>referenced</p>"));
    DataSet dataSet = createDataSet('R');
    dataSet.setDataEntryForm(form);
    dataSetStore.save(dataSet);
    Program program = createProgram('R');
    manager.save(program);
    ProgramStage stage = createProgramStage('R', program);
    stage.setDataEntryForm(form);
    programStageStore.save(stage);
    entityManager.flush();
    clearSession();
    entityManagerFactory.getCache().evictAll();

    assertEquals(
        "<p>referenced</p>", dataSetStore.get(dataSet.getId()).getDataEntryForm().getHtmlCode());
    assertEquals(
        "<p>referenced</p>", programStageStore.get(stage.getId()).getDataEntryForm().getHtmlCode());
    DataEntryForm reloaded = store.get(form.getId());
    assertEquals(
        List.of(dataSet.getUid()),
        dataSetStore.getDataSetsByDataEntryForm(reloaded).stream().map(DataSet::getUid).toList());
    assertEquals(
        List.of(stage.getUid()),
        programStageStore.getByDataEntryForm(reloaded).stream().map(ProgramStage::getUid).toList());
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(DataEntryForm.class);
  }

  /** The jsonb user type of translations, embedded in a component on annotation mappings. */
  private static Object translationsUserType(EntityPersister persister) {
    Type type = persister.getPropertyType("translations");
    if (type instanceof ComponentType component) {
      type = component.getSubtypes()[0];
    }
    return assertInstanceOf(CustomType.class, type).getUserType();
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private DataEntryForm save(DataEntryForm form) {
    store.save(form);
    entityManager.flush();
    return form;
  }

  /** Reloads the form from the database, bypassing the session and the second-level cache. */
  private DataEntryForm reload(DataEntryForm form) {
    clearSession();
    entityManagerFactory.getCache().evict(DataEntryForm.class);
    return store.get(form.getId());
  }
}
