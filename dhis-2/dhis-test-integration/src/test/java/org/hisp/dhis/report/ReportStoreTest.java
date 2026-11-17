/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.TextType;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.SharingJsonBinaryType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.ReportingParams;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ReportStoreTest extends PostgresIntegrationTestBase {
  @Autowired
  @Qualifier("org.hisp.dhis.report.ReportStore")
  private IdentifiableObjectStore<Report> store;

  @Autowired private VisualizationService visualizationService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("the id is generated one at a time from hibernate_sequence")
  void shouldGenerateIdFromHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());
  }

  @Test
  @DisplayName("every mapped property is written to its column")
  void shouldWriteEveryMappedColumn() {
    User admin = getAdminUser();
    Visualization visualization = saveVisualization('A');
    Report report = createFullReport(visualization);
    report.getSharing().setPublicAccess("rw------");
    report.getSharing().setOwner(admin.getUid());
    store.save(report, false);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select code, name, type, designcontent, relativeperiods::text as relativeperiods,"
                + " visualizationid, paramreportingmonth, paramorganisationunit, cachestrategy,"
                + " translations::text as translations, sharing->>'public' as publicaccess,"
                + " sharing->>'owner' as owner from report where reportid = ?",
            report.getId());
    assertEquals("REPORT_A", row.get("code"));
    assertEquals("Report A", row.get("name"));
    assertEquals("HTML", row.get("type"));
    assertEquals("<html>A</html>", row.get("designcontent"));
    assertEquals("[\"THIS_MONTH\", \"LAST_12_MONTHS\"]", row.get("relativeperiods"));
    assertEquals(visualization.getId(), ((Number) row.get("visualizationid")).longValue());
    assertTrue((Boolean) row.get("paramreportingmonth"));
    assertFalse((Boolean) row.get("paramorganisationunit"));
    assertEquals("CACHE_6AM_TOMORROW", row.get("cachestrategy"));
    assertTrue(((String) row.get("translations")).contains("\"Report FR\""));
    assertEquals("rw------", row.get("publicaccess"));
    assertEquals(admin.getUid(), row.get("owner"));
  }

  @Test
  @DisplayName("every mapped property is read back from its column")
  void shouldReadBackEveryMappedColumn() {
    Visualization visualization = saveVisualization('B');
    Report report = createFullReport(visualization);
    report.getSharing().setPublicAccess("r-------");
    store.save(report, false);
    entityManager.flush();

    Report reloaded = reload(report);

    assertEquals("REPORT_A", reloaded.getCode());
    assertEquals("Report A", reloaded.getName());
    assertEquals(ReportType.HTML, reloaded.getType());
    assertEquals("<html>A</html>", reloaded.getDesignContent());
    assertEquals(List.of("THIS_MONTH", "LAST_12_MONTHS"), reloaded.getRawPeriods());
    assertTrue(reloaded.getRelatives().isThisMonth());
    assertTrue(reloaded.getRelatives().isLast12Months());
    assertEquals(visualization.getUid(), reloaded.getVisualization().getUid());
    assertTrue(reloaded.getReportParams().isReportingPeriod());
    assertFalse(reloaded.getReportParams().isOrganisationUnit());
    assertEquals(CacheStrategy.CACHE_6AM_TOMORROW, reloaded.getCacheStrategy());
    assertEquals(
        Set.of(new Translation(new Locale("fr"), "NAME", "Report FR")), reloaded.getTranslations());
    assertEquals("r-------", reloaded.getSharing().getPublicAccess());
  }

  @Test
  @DisplayName("report params without a stored flag are written and read as null")
  void shouldStoreAbsentReportParamsAsNullColumns() {
    Report report = save(createReport("No params"));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select paramreportingmonth, paramorganisationunit from report where reportid = ?",
            report.getId());
    assertNull(row.get("paramreportingmonth"));
    assertNull(row.get("paramorganisationunit"));
    assertNull(reload(report).getReportParams());
  }

  @Test
  @DisplayName("report params with one stored flag load with the other flag false")
  void shouldReadReportParamsWithOneNullColumn() {
    Report report = save(createReport("One flag"));
    jdbcTemplate.update(
        "update report set paramorganisationunit = true where reportid = ?", report.getId());

    ReportingParams params = reload(report).getReportParams();

    assertNotNull(params);
    assertFalse(params.isReportingPeriod());
    assertTrue(params.isOrganisationUnit());
  }

  @Test
  @DisplayName("only the reporting period and organisation unit flags of report params are stored")
  void shouldStoreOnlyReportingPeriodAndOrganisationUnitFlags() {
    Report report = createReport("Parent flags");
    report.setReportParams(new ReportingParams(false, true, true, true));
    save(report);

    ReportingParams params = reload(report).getReportParams();

    assertFalse(params.isReportingPeriod());
    assertFalse(params.isGrandParentOrganisationUnit());
    assertFalse(params.isParentOrganisationUnit());
    assertTrue(params.isOrganisationUnit());
  }

  @Test
  @DisplayName("a change to the report params of a managed report is flushed")
  void shouldFlushChangedReportParamsFlag() {
    Report report = createReport("Changed flag");
    report.setReportParams(new ReportingParams(false, false, false, false));
    save(report);
    Report managed = reload(report);

    managed.getReportParams().setOrganisationUnit(true);
    entityManager.flush();

    assertEquals(
        Boolean.TRUE,
        jdbcTemplate.queryForObject(
            "select paramorganisationunit from report where reportid = ?",
            Boolean.class,
            report.getId()));
  }

  @Test
  @DisplayName("report params are bound to the two report param columns, not as an embeddable")
  void shouldBindReportParamsToTwoColumns() {
    CompositeCustomType type =
        assertInstanceOf(CompositeCustomType.class, persister().getPropertyType("reportParams"));

    assertInstanceOf(ReportParamsUserType.class, type.getUserType());
    assertEquals(
        List.of("paramreportingmonth", "paramorganisationunit"),
        List.of(((AbstractEntityPersister) persister()).getPropertyColumnNames("reportParams")));
  }

  @Test
  @DisplayName("type and cache strategy are bound as enum names")
  void shouldBindEnumsByName() {
    for (String property : List.of("type", "cacheStrategy")) {
      EnumType<?> enumType =
          assertInstanceOf(
              EnumType.class,
              assertInstanceOf(CustomType.class, persister().getPropertyType(property))
                  .getUserType());
      assertFalse(enumType.isOrdinal(), property + " is bound as an ordinal");
    }
  }

  @Test
  @DisplayName("design content is bound as text")
  void shouldBindDesignContentAsText() {
    assertInstanceOf(TextType.class, persister().getPropertyType("designContent"));
  }

  @Test
  @DisplayName("translations, relative periods and sharing are bound to their jsonb types")
  void shouldBindJsonbProperties() {
    assertInstanceOf(ComponentType.class, persister().getPropertyType("translations"));
    assertInstanceOf(
        JsonBinaryType.class,
        assertInstanceOf(CustomType.class, persister().getPropertyType("rawPeriods"))
            .getUserType());
    assertInstanceOf(
        SharingJsonBinaryType.class,
        assertInstanceOf(CustomType.class, persister().getPropertyType("sharing")).getUserType());
  }

  @Test
  @DisplayName("visualization is a lazy many-to-one")
  void shouldMapVisualizationLazily() {
    assertFalse(
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType("visualization"))
            .isEager(null));
  }

  @Test
  @DisplayName("name is mapped not-null, the other own properties nullable")
  void shouldMapOnlyNameNotNull() {
    assertNotNullable("name");
    for (String property :
        List.of(
            "code",
            "type",
            "designContent",
            "rawPeriods",
            "visualization",
            "reportParams",
            "cacheStrategy",
            "translations",
            "sharing")) {
      assertNullable(property);
    }
  }

  @Test
  @DisplayName("the entity is second-level cached with read-write")
  void shouldCacheEntityReadWrite() {
    EntityPersister persister = persister();

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  @DisplayName("null translations and sharing columns are read as empty values")
  void shouldReadNullJsonbColumnsAsEmpty() {
    Report saved = save(createReport("Nulls"));
    jdbcTemplate.update(
        "update report set translations = null, sharing = null where reportid = ?", saved.getId());

    Report reloaded = reload(saved);

    assertTrue(reloaded.getTranslations().isEmpty());
    assertEquals("Nulls", reloaded.getDisplayName());
    assertNotNull(reloaded.getSharing());
    assertNull(reloaded.getSharing().getPublicAccess());
  }

  @Test
  @DisplayName("an uninitialized proxy equals the report loaded in another session")
  void shouldEqualProxyOfSameReport() {
    Report saved = save(createReport("Proxy"));
    Report loaded = reload(saved);
    clearSession();
    entityManagerFactory.getCache().evict(Report.class);

    Report proxy = entityManager.getReference(Report.class, saved.getId());

    assertNotSame(loaded, proxy);
    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(loaded.hashCode(), proxy.hashCode());
    assertEquals(proxy, loaded);
    assertEquals(loaded, proxy);
  }

  private Report createFullReport(Visualization visualization) {
    Report report = createReport("Report A");
    report.setCode("REPORT_A");
    report.setType(ReportType.HTML);
    report.setDesignContent("<html>A</html>");
    report.setRawPeriods(new ArrayList<>(List.of("THIS_MONTH", "LAST_12_MONTHS")));
    report.setVisualization(visualization);
    report.setReportParams(new ReportingParams(true, false, false, false));
    report.setCacheStrategy(CacheStrategy.CACHE_6AM_TOMORROW);
    report.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Report FR")));
    return report;
  }

  private static Report createReport(String name) {
    Report report = new Report();
    report.setName(name);
    report.setAutoFields();
    return report;
  }

  private Visualization saveVisualization(char uniqueChar) {
    Visualization visualization = createVisualization(uniqueChar);
    visualizationService.save(visualization);
    return visualization;
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(Report.class);
  }

  private void assertNotNullable(String property) {
    assertFalse(isNullable(property), property + " is mapped nullable");
  }

  private void assertNullable(String property) {
    assertTrue(isNullable(property), property + " is mapped not-null");
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private Report save(Report report) {
    store.save(report);
    entityManager.flush();
    return report;
  }

  /** Reloads the report from the database, bypassing the session and the L2 cache. */
  private Report reload(Report report) {
    clearSession();
    entityManagerFactory.getCache().evict(Report.class);
    return store.get(report.getId());
  }
}
