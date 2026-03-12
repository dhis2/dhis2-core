/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.SortDirection;

/**
 * Builds an {@code order by} clause from user-specified order columns or a default order. Appends a
 * tie-breaker column for deterministic pagination. The tie-breaker direction matches the first
 * user-specified order so PG can use a single forward or backward index scan.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderJdbcClause {

  /** Order with a resolved SQL column name (e.g. {@code "te.created"}). */
  public record SqlOrder(String column, SortDirection direction) {

    /** Creates a {@code SqlOrder} keeping the direction from the given {@code order}. */
    public static SqlOrder of(String column, Order order) {
      return new SqlOrder(column, order.getDirection());
    }
  }

  /**
   * Returns an {@code order by ...} clause including the {@code order by} prefix. When {@code
   * orders} is empty the {@code defaultOrder} is used. Otherwise the resolved columns are joined
   * and the tie-breaker is appended with the same direction as the first user-specified order.
   *
   * @param orders resolved SQL orders
   * @param defaultOrder full default order clause without {@code order by} prefix
   * @param tieBreaker unique column without direction (e.g. {@code "te.trackedentityid"})
   */
  public static String of(List<SqlOrder> orders, String defaultOrder, String tieBreaker) {
    if (orders.isEmpty()) {
      return " order by " + defaultOrder + " ";
    }

    String cols =
        orders.stream()
            .map(o -> o.column() + " " + o.direction())
            .collect(Collectors.joining(", "));
    return " order by " + cols + ", " + tieBreaker + " " + orders.get(0).direction() + " ";
  }
}
