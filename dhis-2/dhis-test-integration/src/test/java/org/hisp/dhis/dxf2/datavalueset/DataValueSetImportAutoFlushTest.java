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
package org.hisp.dhis.dxf2.datavalueset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Reproduces the auto-flush cost measured in the Uganda Glowroot traces (UGANDA-8 §3): a {@code
 * dataValueSets} import runs as one long transaction with one Hibernate session, loads a large
 * read-only metadata graph into the persistence context, and then pays a <em>full dirty check of
 * that whole context on every query it issues</em>.
 *
 * <p>The traced request imported 166 data values, loaded 2148 organisation units, and burned 28.3 s
 * of CPU out of 29.7 s wall (only 1.33 s of JDBC), with the same query costing 9–20× more at the
 * end of the request than at the start.
 *
 * <p><b>Why this test counts {@code FLUSH_ENTITY} events rather than flushes.</b> {@code
 * DefaultAutoFlushEventListener.onAutoFlush} calls {@code flushEverythingToExecutions}
 * <em>before</em> it checks {@code flushIsReallyNeeded}, and only increments {@code
 * Statistics.getFlushCount()} inside the "really needed" branch. A pure-read import therefore does
 * the entire dirty check on every query while Hibernate reports <b>zero flushes</b> — which is
 * exactly why this defect is invisible in ordinary metrics. The dirty check itself fires one {@code
 * FlushEntityEvent} per managed entity ({@code TypeHelper.findDirty}, the largest self-time leaf in
 * the traces), so counting those events measures the wasted work directly and deterministically.
 *
 * <p>The quantity that matters is the <em>shape</em>: dirty-check work must be O(entities), not
 * O(values × entities). Without the fix it is the product; with it, the read-only lookups skip the
 * flush path entirely because {@code flushMightBeNeeded} short-circuits on {@code
 * FlushMode.COMMIT}.
 */
class DataValueSetImportAutoFlushTest extends IntegrationTestBase {

  /**
   * Organisation units restricted onto the attribute option combo's category option. The traced
   * request had 2148; a few hundred is enough to separate O(n+m) from O(n*m) while keeping the test
   * quick.
   */
  private static final int ORG_UNITS = 300;

  /** Data values in the imported payload. The traced request had 166. */
  private static final int VALUES = 40;

  @Autowired private CategoryService categoryService;
  @Autowired private DataElementService dataElementService;
  @Autowired private DataSetService dataSetService;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private PeriodService periodService;
  @Autowired private DataValueSetService dataValueSetService;
  @Autowired private IdentifiableObjectManager idObjectManager;
  @Autowired private EntityManager em;

  private final List<OrganisationUnit> orgUnits = new ArrayList<>();
  private DataElement dataElement;
  private CategoryOptionCombo attributeOptionCombo;
  private Period period;

  /** Counts one event per managed entity dirty-checked, i.e. the work the fix removes. */
  private final AtomicLong dirtyChecks = new AtomicLong();

  @BeforeEach
  void setUp() {
    OrganisationUnit root = createOrganisationUnit('R');
    organisationUnitService.addOrganisationUnit(root);

    for (int i = 0; i < ORG_UNITS; i++) {
      OrganisationUnit ou = createOrganisationUnit("OU" + i, root);
      organisationUnitService.addOrganisationUnit(ou);
      orgUnits.add(ou);
    }

    // The attribute option combo's category option is restricted to every one of those org units.
    // This is the traced shape: CategoryOptionCombo.getOrganisationUnits() walks its options and
    // initialises each option's organisationUnits set, pulling the whole set into the session.
    CategoryOption restricted = createCategoryOption('X');
    restricted.setOrganisationUnits(new HashSet<>(orgUnits));
    categoryService.addCategoryOption(restricted);

    Category attributeCategory = createCategory('X', restricted);
    attributeCategory.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategory(attributeCategory);

    CategoryCombo attributeCombo = createCategoryCombo('X', attributeCategory);
    attributeCombo.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategoryCombo(attributeCombo);

    attributeOptionCombo = createCategoryOptionCombo(attributeCombo, restricted);
    categoryService.addCategoryOptionCombo(attributeOptionCombo);
    attributeCombo.getOptionCombos().add(attributeOptionCombo);
    categoryService.updateCategoryCombo(attributeCombo);

    dataElement = createDataElement('A', categoryService.getDefaultCategoryCombo());
    dataElementService.addDataElement(dataElement);

    DataSet dataSet = createDataSet('A', new MonthlyPeriodType());
    dataSet.setCategoryCombo(attributeCombo);
    dataSet.addDataSetElement(dataElement);
    orgUnits.forEach(dataSet::addOrganisationUnit);
    dataSetService.addDataSet(dataSet);

    period = createPeriod(new MonthlyPeriodType(), getDate(2026, 1, 1), getDate(2026, 1, 31));
    periodService.addPeriod(period);

    // A normal (non-super) importing user, as in production: the per-value ACL check is part of the
    // workload being measured, and a superuser would short-circuit it.
    User importer = createAndAddUser("importer", root);
    importer.addOrganisationUnits(Set.of(root));
    userService.updateUser(importer);
    enableDataSharing(importer, dataSet, AccessStringHelper.DATA_READ_WRITE);
    enableDataSharing(importer, restricted, AccessStringHelper.DATA_READ_WRITE);
    enableDataSharing(
        importer, categoryService.getDefaultCategoryOption(), AccessStringHelper.DATA_READ_WRITE);
    userService.updateUser(importer);
    injectSecurityContextUser(importer);

    idObjectManager.flush();
    idObjectManager.clear();

    countDirtyChecksFromNowOn();
  }

  /**
   * Appends a counting listener to the FLUSH_ENTITY chain. The default listener still runs; this
   * only observes. Registering on the shared SessionFactory is fine because the count is read
   * across a single import within one test method.
   */
  private void countDirtyChecksFromNowOn() {
    SessionFactoryImplementor sessionFactory =
        em.getEntityManagerFactory()
            .unwrap(SessionFactory.class)
            .unwrap(SessionFactoryImplementor.class);
    sessionFactory
        .getServiceRegistry()
        .getService(EventListenerRegistry.class)
        .appendListeners(
            EventType.FLUSH_ENTITY,
            new FlushEntityEventListener() {
              @Override
              public void onFlushEntity(FlushEntityEvent event) {
                dirtyChecks.incrementAndGet();
              }
            });
  }

  @Test
  @DisplayName(
      "a dataValueSets import must not dirty-check the whole persistence context on every query")
  void importDoesNotDirtyCheckPerQuery() {
    dirtyChecks.set(0);

    long startedAt = System.nanoTime();
    ImportSummary summary =
        dataValueSetService.importDataValueSetJson(payload(), new ImportOptions());
    long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

    assertEquals(
        ImportStatus.SUCCESS,
        summary.getStatus(),
        () -> "import must succeed, conflicts: " + summary.getConflicts());
    assertEquals(
        0,
        summary.getImportCount().getIgnored(),
        () -> "no value may be rejected, conflicts: " + summary.getConflicts());
    assertEquals(VALUES, summary.getImportCount().getImported(), "all values must be imported");

    long observed = dirtyChecks.get();
    // Printed so a reviewer can confirm the gain themselves: raise ORG_UNITS/VALUES towards the
    // traced
    // 2148/166 and watch dirtyCheckEvents and importMs move together on an unfixed build.
    System.out.printf(
        "[UGANDA-9 B] values=%d orgUnits=%d imported=%d dirtyCheckEvents=%d importMs=%d%n",
        VALUES, ORG_UNITS, summary.getImportCount().getImported(), observed, elapsedMs);

    // The bound is the mechanism, not a magic number. Each managed entity may legitimately be
    // dirty-checked a small constant number of times (the commit flush, plus any query that
    // genuinely
    // must see pending writes). What must NOT happen is one full pass per imported value: that is
    // the
    // O(values * entities) product the traces measured as a 9-20x within-request degradation.
    long entities = ORG_UNITS + 32L; // org units plus the handful of metadata objects
    long onePassPerValue = entities * VALUES;
    long bound = entities * 4;

    assertTrue(
        observed <= bound,
        () ->
            "dirty-check passes must be O(entities), not O(values x entities). Observed "
                + observed
                + " FlushEntityEvents for "
                + VALUES
                + " values over ~"
                + entities
                + " managed entities (bound "
                + bound
                + "; even a single full pass per value would already be ~"
                + onePassPerValue
                + ")");
  }

  private InputStream payload() {
    StringBuilder json = new StringBuilder();
    json.append("{\"dataValues\":[");
    for (int i = 0; i < VALUES; i++) {
      if (i > 0) {
        json.append(',');
      }
      json.append("{\"dataElement\":\"")
          .append(dataElement.getUid())
          .append("\",\"period\":\"")
          .append(period.getIsoDate())
          .append("\",\"orgUnit\":\"")
          .append(orgUnits.get(i % orgUnits.size()).getUid())
          .append("\",\"attributeOptionCombo\":\"")
          .append(attributeOptionCombo.getUid())
          .append("\",\"value\":\"")
          .append(i + 1) // never 0: a zero value is skipped as zero-and-insignificant
          .append("\"}");
    }
    json.append("]}");
    return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
  }
}
