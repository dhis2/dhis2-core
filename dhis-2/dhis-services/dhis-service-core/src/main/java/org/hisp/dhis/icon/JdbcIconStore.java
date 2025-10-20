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
import static java.util.function.Function.identity;
import static org.apache.commons.lang3.StringUtils.wrap;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.addIlikeReplacingCharacters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.UserService;
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
      ImmutableMap.<String, String>builder().build();

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
  public int deleteOrphanDefaultIcons() {
    String sql =
        """
      delete from icon i
      where custom = false
      and not exists(select 1 from fileresource fr where fr.fileresourceid = i.fileresourceid );
      """;
    return namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource());
  }

  @Override
  public long count(IconQueryParams params) {
    return createQuery(params, NativeSQL.of(namedParameterJdbcTemplate)).count();
  }

  @Override
  public Icon getIconByKey(String key) {
    List<Icon> icons = getIcons(new IconQueryParams().setKeys(List.of(key)));
    return icons.isEmpty() ? null : icons.get(0);
  }

  @Override
  public List<Icon> getIcons(IconQueryParams params) {
    return createQuery(params, NativeSQL.of(namedParameterJdbcTemplate)).stream(this::toIcon)
        .toList();
  }

  static QueryBuilder createQuery(IconQueryParams params, SQL.QueryAPI api) {
    String sql =
        """
              SELECT
                c.iconkey,
                c.description,
                c.keywords,
                c.created,
                c.lastupdated,
                c.fileresourceid,
                c.createdby,
                c.custom
              FROM icon c
              WHERE 1=1 -- below filters might be erased...
                AND c.lastupdated >= :lastUpdatedStartDate
                AND c.lastupdated <= :lastUpdatedEndDate
                AND c.created >= :createdStartDate
                AND c.created <= :createdEndDate
                AND c.custom = :custom
                AND c.keywords @> cast(:keywords as jsonb)
                AND c.iconkey IN (:keys )
                AND (c.iconkey ilike :search or c.keywords #>> '{}' ilike :search)
              """;

    IconTypeFilter type = params.getType();
    String search = params.getSearch();
    String searchPattern =
        search != null && !search.isEmpty() ? wrap(addIlikeReplacingCharacters(search), '%') : null;
    List<OrderCriteria> orders = params.getOrder();
    if (orders == null || orders.isEmpty())
      orders = List.of(OrderCriteria.of("iconkey", SortDirection.ASC));
    return SQL.selectOf(sql, api)
        .setParameter("lastUpdatedStartDate", params.getLastUpdatedStartDate())
        .setParameter("lastUpdatedEndDate", params.getLastUpdatedEndDate())
        .setParameter("createdStartDate", params.getCreatedStartDate())
        .setParameter("createdEndDate", params.getCreatedEndDate())
        .setParameter("custom", type != IconTypeFilter.ALL ? type == IconTypeFilter.CUSTOM : null)
        .setParameter("keywords", convertKeywordsSetToJsonString(params.getKeywords()))
        .setParameter("keys", params.getKeys(), identity())
        .setParameter("search", searchPattern)
        .eraseNullParameterLines()
        .useEqualsOverInForParameters("keys")
        .setLimit(params.isPaging() ? max(1, min(500, params.getPageSize())) : null)
        .setOffset(params.isPaging() ? (params.getPage() - 1) * params.getPageSize() : null)
        .setOrders(
            orders,
            OrderCriteria::getField,
            o -> o.getDirection().isAscending(),
            Map.of("key", "c.iconkey", "lastUpdated", "c.lastupdated", "created", "c.created"));
  }

  private Icon toIcon(SQL.Row row) {
    Icon icon = new Icon();
    icon.setKey(row.getString(0));
    icon.setDescription(row.getString(1));
    icon.setKeywords(convertKeywordsJsonIntoSet(row.getString(2)));
    icon.setCreated(row.getDate(3));
    icon.setLastUpdated(row.getDate(4));
    icon.setFileResource(fileResourceStore.get(row.getLong(5)));
    icon.setCreatedBy(userService.getUser(row.getLong(6)));
    icon.setCustom(row.getBoolean(7));
    return icon;
  }

  private static Set<String> convertKeywordsJsonIntoSet(String jsonString) {
    try {
      return keywordsMapper.readValue(jsonString, new TypeReference<Set<String>>() {});
    } catch (IOException e) {
      log.error("Parsing keywords json failed, string value: '{}'", jsonString, e);
      throw new IllegalArgumentException(e);
    }
  }

  private static String convertKeywordsSetToJsonString(List<String> keywords) {
    if (keywords == null || keywords.isEmpty()) return null;
    try {
      return keywordsMapper.writeValueAsString(new HashSet<>(keywords));
    } catch (IOException e) {
      log.error("Parsing keywords into json string failed", e);
      throw new IllegalArgumentException(e);
    }
  }
}
