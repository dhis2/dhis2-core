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
package org.hisp.dhis.icon;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.StringUtils.wrap;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.addIlikeReplacingCharacters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.user.UserService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Repository("org.hisp.dhis.icon.IconStore")
@RequiredArgsConstructor
public class JdbcIconStore implements IconStore {

  private static final String KEY_COLUMN = "iconkey";
  private static final String DEFAULT_ORDER = KEY_COLUMN + " asc";

  private static final ImmutableMap<String, String> COLUMN_MAPPER =
      ImmutableMap.<String, String>builder()
          .put("key", KEY_COLUMN)
          .put("lastUpdated", "lastupdated")
          .put("created", "created")
          .build();

  private static final ObjectMapper keywordsMapper = new ObjectMapper();

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private final UserService userService;

  private final FileResourceStore fileResourceStore;

  @Override
  public List<String> getAllKeys() {
    String sql = "select iconkey from icon";
    return namedParameterJdbcTemplate.query(sql, Map.of(), (rs, rowNum) -> rs.getString(1));
  }

  @Override
  public long count(IconQueryParams params) {
    String sql =
        """
     select count(*) from icon c
                      """;

    MapSqlParameterSource parameterSource = new MapSqlParameterSource();

    sql = buildIconQuery(params, sql, parameterSource);

    return Optional.ofNullable(
            namedParameterJdbcTemplate.queryForObject(sql, parameterSource, Long.class))
        .orElse(0L);
  }

  @Override
  public Icon getIconByKey(String key) {
    final String sql =
        """
                select c.iconkey as iconkey, c.description as icondescription, c.keywords as keywords, c.created as created, c.lastupdated as lastupdated,
                c.fileresourceid as fileresourceid, c.createdby as createdby, c.custom as custom from icon c
                where iconkey = :key
                """;

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("key", key);

    List<Icon> icons = namedParameterJdbcTemplate.query(sql, params, getIconRowMapper());

    return icons.isEmpty() ? null : icons.get(0);
  }

  @Override
  public void save(Icon icon) {
    String sql =
        """
            INSERT INTO icon (iconkey,description,keywords,fileresourceid,createdby,created,lastupdated,custom)
            VALUES (:key,:description,cast(:keywords as jsonb),:fileresourceid,:createdby,now(),now(),:custom)
            """;

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("key", icon.getKey());
    params.addValue("description", icon.getDescription());
    params.addValue("custom", icon.isCustom());
    params.addValue(
        "keywords", convertKeywordsSetToJsonString(icon.getKeywords().stream().toList()));
    params.addValue("fileresourceid", icon.getFileResource().getId());
    params.addValue("createdby", icon.getCreatedBy() != null ? icon.getCreatedBy().getId() : null);

    namedParameterJdbcTemplate.update(sql, params);
  }

  @Override
  public void delete(Icon icon) {
    String sql = "DELETE FROM icon WHERE iconkey = :key";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("key", icon.getKey());
    namedParameterJdbcTemplate.update(sql, params);
  }

  @Override
  public void update(Icon icon) {
    String sql =
        """
            update icon set description = :description, keywords = cast(:keywords as jsonb), lastupdated = now() where iconkey = :key
            """;

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("description", icon.getDescription());
    params.addValue(
        "keywords", convertKeywordsSetToJsonString(icon.getKeywords().stream().toList()));
    params.addValue("key", icon.getKey());

    namedParameterJdbcTemplate.update(sql, params);
  }

  @Override
  public List<Icon> getIcons(IconQueryParams params) {
    String sql =
        """
              select c.iconkey as iconkey, c.description as icondescription, c.keywords as keywords, c.created as created, c.lastupdated as lastupdated,
              c.fileresourceid as fileresourceid, c.createdby as createdby, c.custom as custom from icon c
              """;

    MapSqlParameterSource parameterSource = new MapSqlParameterSource();

    sql = buildIconQuery(params, sql, parameterSource);
    sql += orderByQuery(params.getOrder());

    if (params.isPaging()) {
      sql = getPaginatedQuery(params.getPage(), params.getPageSize(), sql, parameterSource);
    }

    return namedParameterJdbcTemplate
        .queryForStream(sql, parameterSource, getIconRowMapper())
        .toList();
  }

  @Override
  public int deleteOrphanDefaultIcons() {
    String sql =
        """
      delete from icon i
      where custom = false
      and not exists(select 1 from fileresource fr where fr.fileresourceid = i.fileresourceid );
      """;
    return namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource());
  }

  private String buildIconQuery(
      IconQueryParams params, String sql, MapSqlParameterSource parameterSource) {
    SqlHelper hlp = new SqlHelper(true);

    if (params.hasLastUpdatedStartDate()) {
      sql += hlp.whereAnd() + " c.lastupdated >= :lastUpdatedStartDate ";

      parameterSource.addValue(
          ":lastUpdatedStartDate", params.getLastUpdatedStartDate(), Types.TIMESTAMP);
    }

    if (params.hasLastUpdatedEndDate()) {
      sql += hlp.whereAnd() + " c.lastupdated <= :lastUpdatedEndDate ";

      parameterSource.addValue(
          "lastUpdatedEndDate", params.getLastUpdatedEndDate(), Types.TIMESTAMP);
    }

    if (params.hasCreatedStartDate()) {
      sql += hlp.whereAnd() + " c.created >= :createdStartDate";

      parameterSource.addValue("createdStartDate", params.getCreatedStartDate(), Types.TIMESTAMP);
    }

    if (params.hasCreatedEndDate()) {
      sql += hlp.whereAnd() + " c.created <= :createdEndDate ";

      parameterSource.addValue("createdEndDate", params.getCreatedEndDate(), Types.TIMESTAMP);
    }

    if (params.getType() != IconTypeFilter.ALL) {
      sql += hlp.whereAnd() + " c.custom = :custom ";

      parameterSource.addValue("custom", params.getType() == IconTypeFilter.CUSTOM, Types.BOOLEAN);
    }

    if (params.hasKeywords()) {

      sql += hlp.whereAnd() + " keywords @> cast(:keywords as jsonb) ";

      parameterSource.addValue("keywords", convertKeywordsSetToJsonString(params.getKeywords()));
    }

    if (params.hasKeys()) {
      sql += hlp.whereAnd() + " c.iconkey IN (:keys )";

      parameterSource.addValue("keys", params.getKeys());
    }

    if (params.hasSearch()) {
      String searchValue = params.getSearch();

      sql += hlp.whereAnd() + "(c.iconkey ilike :search or c.keywords #>> '{}' ilike :search)";
      parameterSource.addValue("search", wrap(addIlikeReplacingCharacters(searchValue), '%'));
    }

    return sql;
  }

  private String getPaginatedQuery(
      int page, int pageSize, String sql, MapSqlParameterSource mapSqlParameterSource) {

    sql = sql + " LIMIT :limit OFFSET :offset ";

    page = max(1, page);
    pageSize = max(1, min(500, pageSize));
    int offset = (page - 1) * pageSize;

    mapSqlParameterSource.addValue("limit", pageSize);
    mapSqlParameterSource.addValue("offset", offset);

    return sql;
  }

  private Set<String> convertKeywordsJsonIntoSet(String jsonString) {
    try {
      return keywordsMapper.readValue(jsonString, new TypeReference<Set<String>>() {});
    } catch (IOException e) {
      log.error("Parsing keywords json failed, string value: '{}'", jsonString, e);
      throw new IllegalArgumentException(e);
    }
  }

  private String convertKeywordsSetToJsonString(List<String> keywords) {
    try {
      return keywordsMapper.writeValueAsString(new HashSet<>(keywords));
    } catch (IOException e) {
      log.error("Parsing keywords into json string failed", e);
      throw new IllegalArgumentException(e);
    }
  }

  private RowMapper<Icon> getIconRowMapper() {
    return (rs, rowNum) -> {
      Icon icon = new Icon();
      icon.setKey(rs.getString("iconkey"));
      icon.setDescription(rs.getString("icondescription"));
      icon.setCustom(rs.getBoolean("custom"));
      icon.setKeywords(convertKeywordsJsonIntoSet(rs.getString("keywords")));
      icon.setCreated(rs.getDate("created"));
      icon.setLastUpdated(rs.getDate("lastupdated"));
      icon.setCreatedBy(userService.getUser(rs.getLong("createdby")));
      icon.setFileResource(fileResourceStore.get(rs.getLong("fileresourceid")));
      return icon;
    };
  }

  private String orderByQuery(List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return " order by " + DEFAULT_ORDER;
    }

    StringJoiner orderJoiner = new StringJoiner(", ");
    for (OrderCriteria order : orders) {
      orderJoiner.add(
          getColumnNameForOrdering(order.getField())
              + " "
              + (order.getDirection().isAscending() ? "asc" : "desc"));
    }
    return " order by " + orderJoiner;
  }

  private String getColumnNameForOrdering(String column) {
    return COLUMN_MAPPER.getOrDefault(column, KEY_COLUMN);
  }
}
