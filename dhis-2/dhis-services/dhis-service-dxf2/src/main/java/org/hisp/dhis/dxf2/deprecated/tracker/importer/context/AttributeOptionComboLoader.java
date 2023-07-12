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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * @author Luciano Fiandesio
 */
@Component
public class AttributeOptionComboLoader {
  private final JdbcTemplate jdbcTemplate;

  public static final String SQL_GET_CATEGORYOPTIONCOMBO =
      "select coc.categoryoptioncomboid, "
          + "coc.uid, coc.code, coc.ignoreapproval, coc.name, c.uid as cc_uid, c.name as cc_name, "
          + "string_agg(coco.categoryoptionid::text, ',') as cat_ids from categoryoptioncombo coc "
          + "join categorycombos_optioncombos co on coc.categoryoptioncomboid = co.categoryoptioncomboid "
          + "join categoryoptioncombos_categoryoptions coco on coc.categoryoptioncomboid = coco.categoryoptioncomboid "
          + "join categorycombo c on co.categorycomboid = c.categorycomboid "
          + "join categorycombos_categories cc on c.categorycomboid = cc.categorycomboid "
          + "join dataelementcategory dec on cc.categoryid = dec.categoryid where coc."
          + "${resolvedScheme} "
          + "group by coc.categoryoptioncomboid, coc.uid, coc.code, coc.ignoreapproval, coc.name, cc_uid, cc_name";

  public static final String SQL_GET_CATEGORYOPTIONCOMBO_BY_CATEGORYIDS =
      "select * from ( "
          + "select coc.categoryoptioncomboid, "
          + "coc.uid, "
          + "coc.code, "
          + "coc.ignoreapproval, "
          + "coc.name, "
          + "c.uid as cc_uid, "
          + "c.name as cc_name,"
          + "array_to_string(array_agg(distinct coco.categoryoptionid::TEXT), ',') as cat_ids "
          + "from categoryoptioncombo coc "
          + "join categorycombos_optioncombos co on coc.categoryoptioncomboid = co.categoryoptioncomboid "
          + "join categorycombo c on co.categorycomboid = c.categorycomboid "
          + "join categorycombos_categories cc on c.categorycomboid = cc.categorycomboid "
          + "join categoryoptioncombos_categoryoptions coco on coc.categoryoptioncomboid = coco.categoryoptioncomboid "
          + "where c.${resolvedScheme} "
          + "group by coc.categoryoptioncomboid, coc.uid, coc.code, coc.ignoreapproval, coc.name, cc_uid, cc_name "
          + ") as catoptcombo where "
          + "array_length(regexp_split_to_array(cat_ids, ','),1) = array_length(ARRAY[${option_ids}],1) AND "
          + "regexp_split_to_array(cat_ids, ',') @> ARRAY[${option_ids}]";

  public AttributeOptionComboLoader(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    checkNotNull(jdbcTemplate);

    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Fetches a {@see CategoryOptionCombo} by id, using the provided look-up Scheme
   *
   * @param idScheme an IdScheme
   * @param id the actual id
   * @return a CategoryOptionCombo or null
   */
  public CategoryOptionCombo getCategoryOptionCombo(IdScheme idScheme, String id) {
    return loadCategoryOptionCombo(idScheme, id);
  }

  /**
   * Fetches a {@see CategoryOptionCombo}
   *
   * @param categoryCombo a {@see CategoryCombo}
   * @param categoryOptions a semicolon delimited list of Category Options uid
   * @param idScheme the {@see IdScheme} to use to fetch the entity
   * @return a {@see CategoryOptionCombo}
   */
  public CategoryOptionCombo getAttributeOptionCombo(
      CategoryCombo categoryCombo,
      String categoryOptions,
      String attributeOptionCombo,
      IdScheme idScheme) {
    final Set<String> opts = TextUtils.splitToSet(categoryOptions, TextUtils.SEMICOLON);

    return getAttributeOptionCombo(categoryCombo, opts, attributeOptionCombo, idScheme);
  }

  /**
   * Fetches the default {@see CategoryOptionCombo}
   *
   * @return a {@see CategoryOptionCombo} or null
   */
  public CategoryOptionCombo getDefault() {
    return loadCategoryOptionCombo(IdScheme.NAME, "default");
  }

  /**
   * Fetches a {@see CategoryOption} by uid, using the provided look-up Scheme
   *
   * @param idScheme an IdScheme
   * @param id the actual id
   * @return a CategoryOption or null
   */
  private CategoryOption getCategoryOption(IdScheme idScheme, String id) {
    return loadCategoryOption(idScheme, id);
  }

  private CategoryOptionCombo getAttributeOptionCombo(
      CategoryCombo categoryCombo,
      Set<String> opts,
      String attributeOptionCombo,
      IdScheme idScheme) {
    if (categoryCombo == null) {
      throw new IllegalQueryException("Illegal category combo");
    }

    // ---------------------------------------------------------------------
    // Attribute category options validation
    // ---------------------------------------------------------------------

    CategoryOptionCombo attrOptCombo = null;

    if (opts != null) {
      Set<CategoryOption> categoryOptions = new HashSet<>();

      for (String uid : opts) {
        CategoryOption categoryOption = getCategoryOption(idScheme, uid);

        if (categoryOption == null) {
          throw new IllegalQueryException("Illegal category option identifier: " + uid);
        }

        categoryOptions.add(categoryOption);
      }

      final String id = resolveCategoryComboId(categoryCombo, idScheme);

      attrOptCombo = getAttributeOptionCombo(idScheme, id, categoryOptions);

      if (attrOptCombo == null) {
        throw new IllegalQueryException(
            "Attribute option combo does not exist for given category combo and category options");
      }
    } else if (attributeOptionCombo != null) {
      attrOptCombo = getCategoryOptionCombo(idScheme, attributeOptionCombo);
    }

    // ---------------------------------------------------------------------
    // Fall back to default category option combination
    // ---------------------------------------------------------------------

    if (attrOptCombo == null) {
      attrOptCombo = getDefault();
    }

    if (attrOptCombo == null) {
      throw new IllegalQueryException("Default attribute option combo does not exist");
    }

    return attrOptCombo;
  }

  private String resolveCategoryComboId(CategoryCombo categoryCombo, IdScheme idScheme) {
    String id = null;

    if (idScheme.is(IdentifiableProperty.ID)) {
      id = String.valueOf(categoryCombo.getId());
    } else if (idScheme.is(IdentifiableProperty.UID)) {
      id = categoryCombo.getUid();
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      id = categoryCombo.getCode();
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      id = categoryCombo.getName();
    }

    return id;
  }

  private CategoryOptionCombo getAttributeOptionCombo(
      IdScheme idScheme, String categoryComboId, Set<CategoryOption> categoryOptions) {

    final String key = "categorycomboid";
    final String categoryComboKey = resolveId(idScheme, key, categoryComboId);

    final String optionsId =
        categoryOptions.stream()
            .map(co -> Long.toString(co.getId()))
            .map(s -> "'" + s + "'")
            .collect(Collectors.joining(","));

    StringSubstitutor sub =
        new StringSubstitutor(
            ImmutableMap.<String, String>builder()
                .put("resolvedScheme", Objects.requireNonNull(categoryComboKey))
                .put("option_ids", optionsId)
                .build());

    // TODO use cache
    List<CategoryOptionCombo> categoryOptionCombos =
        jdbcTemplate.query(
            sub.replace(SQL_GET_CATEGORYOPTIONCOMBO_BY_CATEGORYIDS),
            (rs, i) -> bind("categoryoptioncomboid", rs));

    if (categoryOptionCombos.size() == 1) {
      return categoryOptionCombos.get(0);
    } else {
      // TODO throw an error??
      return null;
    }
  }

  /**
   * Fetches a {@see CategoryOptionCombo} by "id" (based on the provided IdScheme)
   *
   * <p>The {@see CategoryOptionCombo} contains tha associated {@see CategoryCombo} and all the
   * associated {@see CategoryOption}
   *
   * @param idScheme a {@see IdScheme}
   * @param id the {@see CategoryOptionCombo} id to use
   * @return a {@see CategoryOptionCombo} or null
   */
  private CategoryOptionCombo loadCategoryOptionCombo(IdScheme idScheme, String id) {
    String key = "categoryoptioncomboid";

    StringSubstitutor sub =
        new StringSubstitutor(
            ImmutableMap.<String, String>builder()
                .put("key", key)
                .put("resolvedScheme", Objects.requireNonNull(resolveId(idScheme, key, id)))
                .build());

    try {
      return jdbcTemplate.queryForObject(
          sub.replace(SQL_GET_CATEGORYOPTIONCOMBO), (rs, i) -> bind(key, rs));
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  private CategoryOptionCombo bind(String key, ResultSet rs) throws SQLException {
    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setId(rs.getLong(key));
    categoryOptionCombo.setUid(rs.getString("uid"));
    categoryOptionCombo.setCode(rs.getString("code"));
    categoryOptionCombo.setIgnoreApproval(rs.getBoolean("ignoreapproval"));
    categoryOptionCombo.setName(rs.getString("name"));

    String cat_ids = rs.getString("cat_ids");
    if (!ObjectUtils.isEmpty(cat_ids)) {
      categoryOptionCombo.setCategoryOptions(
          Arrays.stream(cat_ids.split(","))
              .map(coid -> getCategoryOption(IdScheme.ID, coid))
              .collect(Collectors.toSet()));
    }
    CategoryCombo categoryCombo = new CategoryCombo();
    categoryCombo.setUid(rs.getString("cc_uid"));
    categoryCombo.setName(rs.getString("cc_name"));
    categoryOptionCombo.setCategoryCombo(categoryCombo);
    return categoryOptionCombo;
  }

  private CategoryOption loadCategoryOption(IdScheme idScheme, String id) {
    String key = "categoryoptionid";
    final String sql =
        "select "
            + key
            + ", uid, code, name, startdate, enddate, sharing from dataelementcategoryoption "
            + "where "
            + resolveId(idScheme, key, id);

    try {
      return jdbcTemplate.queryForObject(
          sql,
          (rs, i) -> {
            CategoryOption categoryOption = new CategoryOption();
            categoryOption.setId(rs.getLong(key));
            categoryOption.setUid(rs.getString("uid"));
            categoryOption.setCode(rs.getString("code"));
            categoryOption.setName(rs.getString("name"));
            categoryOption.setStartDate(rs.getDate("startdate"));
            categoryOption.setEndDate(rs.getDate("enddate"));
            categoryOption.setSharing(getSharing(rs.getObject("sharing").toString()));
            return categoryOption;
          });
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  private String resolveId(IdScheme scheme, String primaryKeyColumn, String id) {
    if (scheme.is(IdentifiableProperty.ID)) {
      return primaryKeyColumn + " = " + id;
    } else if (scheme.is(IdentifiableProperty.UID)) {
      return "uid = '" + id + "'";
    } else if (scheme.is(IdentifiableProperty.CODE)) {
      return "code = '" + id + "'";
    } else if (scheme.is(IdentifiableProperty.NAME)) {
      return "name = '" + id + "'";
    }

    return null;
  }

  private Sharing getSharing(String jsonString) {
    try {
      return JacksonObjectMapperConfig.jsonMapper.readValue(jsonString, Sharing.class);
    } catch (JsonProcessingException e) {
      // ignore
      return null;
    }
  }
}
