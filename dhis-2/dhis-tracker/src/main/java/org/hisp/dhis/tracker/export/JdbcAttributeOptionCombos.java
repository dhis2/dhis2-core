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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * JdbcAttributeOptionCombos holds the SQL the tracker export stores share for attribute option
 * combos (AOCs).
 *
 * <p>The AOCs a user may read data of are resolved up front with {@link
 * CategoryOptionComboStore#getDataReadableAttributeOptionComboIds}, the same lookup the data value
 * export uses, and bound as one array parameter named {@link #READABLE_AOCS}. The export query
 * filters on them with {@link #readableCondition(String)}, a hash semi join PostgreSQL estimates
 * like no filter at all. Joining a subquery that groups all category option combos instead gives
 * the planner a result size it cannot estimate, which inflates the row estimate of the whole export
 * query and can flip it to a plan that reads millions of rows (DHIS2-22400).
 *
 * <p>The export query does not join {@code categoryoptioncombo} either. PostgreSQL would merge that
 * join with the filter on the readable AOCs and estimate the matches from the number of all
 * category option combos, an underestimate that grows with it. The AOC UID, only needed in SQL to
 * order by it, comes from {@link #uidSubquery(String)} instead, and the AOCs are loaded with {@link
 * #fetchAttributeOptionCombos} after the export query, once per distinct AOC.
 */
public class JdbcAttributeOptionCombos {
  /** Name of the array parameter holding the internal ids of the readable AOCs. */
  public static final String READABLE_AOCS = "dataReadableAocs";

  /** The order field for the AOC UID, selected as {@code coc_uid}. */
  public static final String ORDER_BY_UID = "attributeOptionCombo.uid";

  private JdbcAttributeOptionCombos() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * @param aocColumn qualified AOC id column to filter, e.g. {@code ev.attributeoptioncomboid}
   * @return a condition limiting the column to the AOC ids bound as {@link #READABLE_AOCS}
   */
  @Nonnull
  public static String readableCondition(@Nonnull String aocColumn) {
    return " exists (select 1 from unnest(cast(:%s as bigint[])) data_readable_aoc(id) where data_readable_aoc.id = %s) "
        .formatted(READABLE_AOCS, aocColumn);
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
   * Sets the identifiers, attribute values and category options of the given option combos, with a
   * single query for all of them.
   *
   * @param combos the option combos to fill, keyed by their internal id
   */
  public static void fetchAttributeOptionCombos(
      @Nonnull NamedParameterJdbcTemplate jdbcTemplate,
      @Nonnull Map<Long, CategoryOptionCombo> combos) {
    if (combos.isEmpty()) return;
    combos.values().forEach(coc -> coc.setCategoryOptions(new HashSet<>()));
    String sql =
        """
        select coc.categoryoptioncomboid as coc_id, coc.uid as coc_uid, coc.code as coc_code,
          coc.name as coc_name, coc.attributevalues as coc_attributevalues,
          co.uid as co_uid, co.code as co_code, co.name as co_name,
          co.attributevalues as co_attributevalues
        from categoryoptioncombo coc
        left join categoryoptioncombos_categoryoptions cocco
          on cocco.categoryoptioncomboid = coc.categoryoptioncomboid
        left join categoryoption co on co.categoryoptionid = cocco.categoryoptionid
        where coc.categoryoptioncomboid in (:ids)""";
    jdbcTemplate.query(
        sql,
        new MapSqlParameterSource("ids", combos.keySet()),
        (RowCallbackHandler)
            rs -> {
              CategoryOptionCombo coc = combos.get(rs.getLong("coc_id"));
              coc.setUid(rs.getString("coc_uid"));
              coc.setCode(rs.getString("coc_code"));
              coc.setName(rs.getString("coc_name"));
              coc.setAttributeValues(attributeValues(rs.getString("coc_attributevalues")));
              if (rs.getString("co_uid") == null) return;
              CategoryOption option = new CategoryOption();
              option.setUid(rs.getString("co_uid"));
              // as before, when the options came aggregated as JSON: no code or name is ""
              option.setCode(Objects.requireNonNullElse(rs.getString("co_code"), ""));
              option.setName(Objects.requireNonNullElse(rs.getString("co_name"), ""));
              option.setAttributeValues(attributeValues(rs.getString("co_attributevalues")));
              coc.getCategoryOptions().add(option);
            });
  }

  private static AttributeValues attributeValues(@CheckForNull String json) {
    return AttributeValues.of(Objects.requireNonNullElse(json, "{}"));
  }
}
