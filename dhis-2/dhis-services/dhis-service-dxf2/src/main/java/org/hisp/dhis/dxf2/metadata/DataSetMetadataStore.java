/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.dxf2.metadata;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * Read-only projection store backing {@link DataSetMetadataExportService#writeDataSetMetadata}. It
 * loads the data-entry category graph (category combos, category option combos, categories,
 * category options and their id associations) with a fixed, small number of flat SQL queries,
 * returning plain row/association records rather than managed Hibernate entities. This avoids both
 * the per-parent N+1 selects and the memory cost of hydrating the (potentially hundreds of
 * thousands of) entities in that graph.
 *
 * <p>Every query is keyed on a bounded set of category-combo (or option) ids and never on the large
 * option-combo id set, and each id set is bound as a single SQL array parameter, so the number of
 * statements and bind parameters does not grow with the size of the exported graph.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
class DataSetMetadataStore {

  private final JdbcTemplate jdbcTemplate;

  /** A category combo row. {@code translations} is the raw jsonb text (never null; may be "[]"). */
  record ComboRow(
      long id,
      String uid,
      String code,
      String name,
      Date created,
      Date lastUpdated,
      String dataDimensionType,
      boolean skipTotal,
      String translations) {}

  /** A category option combo row, linked to its owning combo. */
  record CocRow(long comboId, long id, String uid, String code, String name, String translations) {}

  /** A category row. */
  record CategoryRow(
      long id,
      String uid,
      String code,
      String name,
      String shortName,
      Date created,
      Date lastUpdated,
      boolean dataDimension,
      String dataDimensionType,
      String translations) {}

  /** A category option row. */
  record OptionRow(
      long id,
      String uid,
      String code,
      String name,
      String shortName,
      String formName,
      Date created,
      Date lastUpdated,
      String translations) {}

  /**
   * A parent-to-child association row carrying the child's uid, returned in the exact iteration
   * order the JSON document requires (SQL {@code ORDER BY}).
   */
  record Assoc(long parentId, long childId, String childUid) {}

  private static final RowMapper<Assoc> ASSOC =
      (rs, i) -> new Assoc(rs.getLong(1), rs.getLong(2), rs.getString(3));

  // --- category combos --------------------------------------------------------------------------

  List<ComboRow> getCombos(long[] comboIds) {
    if (comboIds.length == 0) return List.of();
    return query(
        """
        select categorycomboid, uid, code, name, created, lastupdated,
               datadimensiontype, skiptotal, translations
        from categorycombo
        where categorycomboid = any(?)
        order by categorycomboid
        """,
        comboIds,
        (rs, i) ->
            new ComboRow(
                rs.getLong("categorycomboid"),
                rs.getString("uid"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getTimestamp("created"),
                rs.getTimestamp("lastupdated"),
                rs.getString("datadimensiontype"),
                rs.getBoolean("skiptotal"),
                translations(rs, "translations")));
  }

  /** Combo -> category uids, ordered by the combo's category {@code sort_order} (a list). */
  List<Assoc> getComboCategories(long[] comboIds) {
    if (comboIds.length == 0) return List.of();
    return query(
        """
        select cc.categorycomboid, c.categoryid, c.uid
        from categorycombos_categories cc
        join category c on c.categoryid = cc.categoryid
        where cc.categorycomboid = any(?)
        order by cc.categorycomboid, cc.sort_order
        """,
        comboIds,
        ASSOC);
  }

  // --- category option combos (only for the data-element combos, which expose their COCs) --------

  List<CocRow> getOptionCombos(long[] comboIds) {
    if (comboIds.length == 0) return List.of();
    return query(
        """
        select link.categorycomboid, coc.categoryoptioncomboid, coc.uid, coc.code,
               coc.name, coc.translations
        from categorycombos_optioncombos link
        join categoryoptioncombo coc on coc.categoryoptioncomboid = link.categoryoptioncomboid
        where link.categorycomboid = any(?)
        order by link.categorycomboid, coc.categoryoptioncomboid
        """,
        comboIds,
        (rs, i) ->
            new CocRow(
                rs.getLong("categorycomboid"),
                rs.getLong("categoryoptioncomboid"),
                rs.getString("uid"),
                rs.getString("code"),
                rs.getString("name"),
                translations(rs, "translations")));
  }

  /**
   * Option-combo -> category-option uids for the given combos. Set-valued in the domain model, so
   * emitted in a deterministic option-id order.
   */
  List<Assoc> getOptionComboOptions(long[] comboIds) {
    if (comboIds.length == 0) return List.of();
    return query(
        """
        select coc.categoryoptioncomboid, o.categoryoptionid, o.uid
        from categorycombos_optioncombos link
        join categoryoptioncombo coc on coc.categoryoptioncomboid = link.categoryoptioncomboid
        join categoryoptioncombos_categoryoptions cocco
          on cocco.categoryoptioncomboid = coc.categoryoptioncomboid
        join categoryoption o on o.categoryoptionid = cocco.categoryoptionid
        where link.categorycomboid = any(?)
        order by coc.categoryoptioncomboid, o.categoryoptionid
        """,
        comboIds,
        ASSOC);
  }

  // --- categories -------------------------------------------------------------------------------

  List<CategoryRow> getCategories(long[] categoryIds) {
    if (categoryIds.length == 0) return List.of();
    return query(
        """
        select categoryid, uid, code, name, shortname, created, lastupdated,
               datadimension, datadimensiontype, translations
        from category
        where categoryid = any(?)
        order by categoryid
        """,
        categoryIds,
        (rs, i) ->
            new CategoryRow(
                rs.getLong("categoryid"),
                rs.getString("uid"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("shortname"),
                rs.getTimestamp("created"),
                rs.getTimestamp("lastupdated"),
                rs.getBoolean("datadimension"),
                rs.getString("datadimensiontype"),
                translations(rs, "translations")));
  }

  /**
   * Category -> category-option (id + uid), ordered by the category's option {@code sort_order}.
   */
  List<Assoc> getCategoryOptions(long[] categoryIds) {
    if (categoryIds.length == 0) return List.of();
    return query(
        """
        select cco.categoryid, o.categoryoptionid, o.uid
        from categories_categoryoptions cco
        join categoryoption o on o.categoryoptionid = cco.categoryoptionid
        where cco.categoryid = any(?)
        order by cco.categoryid, cco.sort_order
        """,
        categoryIds,
        ASSOC);
  }

  // --- category options -------------------------------------------------------------------------

  List<OptionRow> getOptions(long[] optionIds) {
    if (optionIds.length == 0) return List.of();
    return query(
        """
        select categoryoptionid, uid, code, name, shortname, formname,
               created, lastupdated, translations
        from categoryoption
        where categoryoptionid = any(?)
        order by categoryoptionid
        """,
        optionIds,
        (rs, i) ->
            new OptionRow(
                rs.getLong("categoryoptionid"),
                rs.getString("uid"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("shortname"),
                rs.getString("formname"),
                rs.getTimestamp("created"),
                rs.getTimestamp("lastupdated"),
                translations(rs, "translations")));
  }

  /** Option -> organisation-unit uids. Set-valued, emitted in deterministic org-unit-id order. */
  List<Assoc> getOptionOrgUnits(long[] optionIds) {
    if (optionIds.length == 0) return List.of();
    return query(
        """
        select coou.categoryoptionid, ou.organisationunitid, ou.uid
        from categoryoption_organisationunits coou
        join organisationunit ou on ou.organisationunitid = coou.organisationunitid
        where coou.categoryoptionid = any(?)
        order by coou.categoryoptionid, ou.organisationunitid
        """,
        optionIds,
        ASSOC);
  }

  // --- helpers ----------------------------------------------------------------------------------

  private <T> List<T> query(String sql, long[] ids, RowMapper<T> mapper) {
    return jdbcTemplate.query(
        sql,
        ps -> {
          Array array = ps.getConnection().createArrayOf("bigint", box(ids));
          ps.setArray(1, array);
        },
        mapper);
  }

  private static Long[] box(long[] ids) {
    Long[] boxed = new Long[ids.length];
    for (int i = 0; i < ids.length; i++) {
      boxed[i] = ids[i];
    }
    return boxed;
  }

  private static String translations(ResultSet rs, String column) throws SQLException {
    String value = rs.getString(column);
    return value == null ? "[]" : value;
  }

  static long[] toArray(Collection<Long> ids) {
    long[] array = new long[ids.size()];
    int i = 0;
    for (Long id : ids) {
      array[i++] = id;
    }
    return array;
  }
}
