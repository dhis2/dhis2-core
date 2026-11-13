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
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;

/**
 * The attribute option combo (AOC) in the SQL of tracker exports.
 *
 * <p>The export queries do not join {@code categoryoptioncombo}. PostgreSQL would merge that join
 * with the filter on the readable AOCs and estimate the matches from the number of all category
 * option combos, an underestimate that grows with it (DHIS2-22400). A query selects only the AOC
 * id, and the AOC UID when it orders by it ({@link #uidSubquery(String)}). The stores set a {@link
 * #placeholder(long)} on each row, which the service replaces by the option combo after the query
 * ({@link CategoryOptionComboService#loadAttributeOptionCombos}).
 */
public class JdbcAttributeOptionComboUid {

  /** The order field for the AOC UID, selected as {@code coc_uid}. */
  public static final String ORDER_BY_UID = "attributeOptionCombo.uid";

  private JdbcAttributeOptionComboUid() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * @return true when the export is ordered by the AOC UID, the only reason to select it in SQL
   */
  public static boolean isOrderedByUid(@Nonnull List<Order> orders) {
    return orders.stream().anyMatch(order -> ORDER_BY_UID.equals(order.getField()));
  }

  /**
   * @param aocColumn qualified AOC id column, e.g. {@code ev.attributeoptioncomboid}
   * @return a scalar subquery for the UID of the AOC, to select and order by
   */
  @Nonnull
  public static String uidSubquery(@Nonnull String aocColumn) {
    return "(select coc.uid from categoryoptioncombo coc where coc.categoryoptioncomboid = %s)"
        .formatted(aocColumn);
  }

  /**
   * @param id internal id of the AOC of a row
   * @return an option combo that only carries the id, to be replaced by the option combo itself
   */
  @Nonnull
  public static CategoryOptionCombo placeholder(long id) {
    CategoryOptionCombo combo = new CategoryOptionCombo();
    combo.setId(id);
    return combo;
  }
}
