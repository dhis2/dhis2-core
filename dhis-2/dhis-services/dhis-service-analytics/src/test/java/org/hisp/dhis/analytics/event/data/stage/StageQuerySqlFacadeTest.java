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
package org.hisp.dhis.analytics.event.data.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ColumnAndAlias;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

class StageQuerySqlFacadeTest {
  private final TestClassifier classifier = new TestClassifier();
  private final TestDateRenderer dateRenderer = new TestDateRenderer();
  private final TestOrgUnitService orgUnitService = new TestOrgUnitService();
  private final DefaultStageQuerySqlFacade subject =
      new DefaultStageQuerySqlFacade(classifier, dateRenderer, orgUnitService);

  @Test
  void shouldResolveStageOrgUnitSelectColumn() {
    QueryItem item = createItem("ou", true);
    classifier.isStageOrgUnit = true;
    EventQueryParams params = new EventQueryParams.Builder().build();

    Optional<ColumnAndAlias> column = subject.resolveSelectColumn(item, params, false, false);

    assertTrue(column.isPresent());
    assertEquals("stage_ou_expr", column.get().getColumn());
    assertEquals(item.getItemName(), column.get().getAlias());
  }

  @Test
  void shouldResolveStageDateSelectColumnOnlyForAggregatedQueries() {
    QueryItem item = createItem("occurreddate", true);
    classifier.isStageDate = true;
    dateRenderer.periodBucket = Optional.of("monthly");

    EventQueryParams params = new EventQueryParams.Builder().build();

    Optional<ColumnAndAlias> nonAggregated =
        subject.resolveSelectColumn(item, params, false, false);
    Optional<ColumnAndAlias> aggregated = subject.resolveSelectColumn(item, params, false, true);

    assertFalse(nonAggregated.isPresent());
    assertTrue(aggregated.isPresent());
    assertEquals("bucket_expr_monthly", aggregated.get().getColumn());
    assertEquals(item.getItemName(), aggregated.get().getAlias());
  }

  @Test
  void shouldReturnEmptyForStageDateWhenNoPeriodBucketResolved() {
    QueryItem item = createItem("occurreddate", true);
    classifier.isStageDate = true;
    dateRenderer.periodBucket = Optional.empty();

    Optional<ColumnAndAlias> column =
        subject.resolveSelectColumn(item, new EventQueryParams.Builder().build(), false, true);

    assertFalse(column.isPresent());
  }

  @Test
  void shouldResolveStageOrgUnitWhereClause() {
    QueryItem item = createItem("ou", true);
    classifier.isStageOrgUnit = true;
    EventQueryParams params = new EventQueryParams.Builder().build();

    Optional<String> where = subject.resolveWhereClause(item, params, AnalyticsType.EVENT);

    assertTrue(where.isPresent());
    assertEquals("stage_ou_where", where.get());
  }

  @Test
  void shouldReturnEmptyWhereClauseForNonStageOrgUnit() {
    QueryItem item = createItem("occurreddate", true);

    Optional<String> where =
        subject.resolveWhereClause(
            item, new EventQueryParams.Builder().build(), AnalyticsType.EVENT);

    assertFalse(where.isPresent());
  }

  @Test
  void shouldDelegateIsStageDateClassification() {
    QueryItem item = createItem("occurreddate", true);
    classifier.isStageDate = true;

    assertTrue(subject.isStageDate(item));
  }

  private QueryItem createItem(String uid, boolean withStage) {
    DataElement dataElement = new DataElement();
    dataElement.setUid(uid);
    QueryItem item = new QueryItem(dataElement);
    if (withStage) {
      ProgramStage stage = new ProgramStage();
      stage.setUid("s1234567890");
      item.setProgramStage(stage);
    }
    return item;
  }

  private static class TestClassifier implements StageQueryItemClassifier {
    private boolean isStageOrgUnit;
    private boolean isStageDate;

    @Override
    public boolean isStageScoped(QueryItem item) {
      return isStageOrgUnit || isStageDate;
    }

    @Override
    public boolean isStageOrgUnit(QueryItem item) {
      return isStageOrgUnit;
    }

    @Override
    public boolean isStageDate(QueryItem item) {
      return isStageDate;
    }

    @Override
    public boolean isStageEventDate(QueryItem item) {
      return false;
    }

    @Override
    public boolean isStageScheduledDate(QueryItem item) {
      return false;
    }

    @Override
    public boolean isStageEventStatus(QueryItem item) {
      return false;
    }
  }

  private static class TestDateRenderer implements StageDatePeriodBucketSqlRenderer {
    private Optional<String> periodBucket = Optional.empty();

    @Override
    public Optional<String> resolvePeriodBucketColumn(QueryItem item) {
      return periodBucket;
    }

    @Override
    public String renderPeriodBucketExpression(QueryItem item, String periodBucketColumn) {
      return "bucket_expr_" + periodBucketColumn;
    }
  }

  private static class TestOrgUnitService implements StageOrgUnitSqlService {
    @Override
    public ColumnAndAlias selectColumn(
        QueryItem item, EventQueryParams params, boolean isGroupByClause) {
      return isGroupByClause
          ? ColumnAndAlias.ofColumn("stage_ou_expr")
          : ColumnAndAlias.ofColumnAndAlias("stage_ou_expr", item.getItemName());
    }

    @Override
    public String whereClause(
        QueryItem item, EventQueryParams params, AnalyticsType analyticsType) {
      return "stage_ou_where";
    }
  }
}
