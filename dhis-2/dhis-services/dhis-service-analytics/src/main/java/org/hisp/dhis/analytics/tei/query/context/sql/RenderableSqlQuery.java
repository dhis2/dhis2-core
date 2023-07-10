/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei.query.context.sql;

import static org.hisp.dhis.commons.util.TextUtils.SPACE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.From;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.LeftJoin;
import org.hisp.dhis.analytics.common.query.LeftJoins;
import org.hisp.dhis.analytics.common.query.LimitOffset;
import org.hisp.dhis.analytics.common.query.OrderRenderer;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.common.query.RootConditionRenderer;
import org.hisp.dhis.analytics.common.query.Select;
import org.hisp.dhis.analytics.common.query.Table;
import org.hisp.dhis.analytics.common.query.Where;

/**
 * This class is responsible for rendering the SQL query. each instance of this class will only
 * render each "part" once, and then cache the result. This way we can reuse the same instance of
 * this class for rendering the count query, without affecting the parameters of the original query.
 */
@Data
@Builder(toBuilder = true)
public class RenderableSqlQuery implements Renderable {
  private static final Renderable COUNT_1 = Select.ofUnquoted("count(1)");

  private static final String LIMIT_OFFSET = "LIMIT_OFFSET";

  private static final String ORDER_BY = "ORDER_BY";

  private static final String WHERE = "WHERE";

  private static final String FROM = "FROM";

  private static final String SELECT = "SELECT";

  private final boolean countRequested;

  @Singular private final List<Field> selectFields;

  private final Table mainTable;

  @Singular private final List<LeftJoin> leftJoins;

  @Singular private final List<GroupableCondition> groupableConditions;

  private final LimitOffset limitOffset;

  @Singular private final List<IndexedOrder> orderClauses;

  @Builder.Default private final Map<String, String> renderedParts = new HashMap<>();

  /**
   * transforms the current instance into a count query.
   *
   * @return a new instance of this class, with the same parameters as the current instance, but
   *     with the countRequested flag set to true.
   */
  public RenderableSqlQuery forCount() {
    return toBuilder().countRequested(true).renderedParts(renderedParts).build();
  }

  @Override
  public String render() {
    if (countRequested) {
      return renderSqlCountQuery();
    }
    return renderSqlQuery();
  }

  private String renderSqlQuery() {
    return renderParts(select(), from(), where(), order(), limitOffset());
  }

  private String renderSqlCountQuery() {
    return renderParts(COUNT_1.render(), from(), where());
  }

  private String renderParts(String... parts) {
    return Arrays.stream(parts).filter(Objects::nonNull).collect(Collectors.joining(SPACE));
  }

  private String limitOffset() {
    return getIfPresentOrElse(LIMIT_OFFSET, limitOffset::render);
  }

  private String order() {
    return getIfPresentOrElse(ORDER_BY, () -> OrderRenderer.of(orderClauses).render());
  }

  private String where() {
    return getIfPresentOrElse(
        WHERE, () -> Where.of(RootConditionRenderer.of(groupableConditions)).render());
  }

  private String from() {
    return getIfPresentOrElse(FROM, () -> From.of(mainTable, LeftJoins.of(leftJoins)).render());
  }

  private String select() {
    return getIfPresentOrElse(SELECT, () -> Select.of(selectFields).render());
  }

  private String getIfPresentOrElse(String key, Supplier<String> supplier) {
    if (!renderedParts.containsKey(key)) {
      renderedParts.put(key, supplier.get());
    }

    return renderedParts.get(key);
  }
}
