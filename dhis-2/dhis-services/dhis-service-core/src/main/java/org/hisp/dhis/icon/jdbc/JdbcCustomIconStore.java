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
package org.hisp.dhis.icon.jdbc;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.icon.CustomIcon;
import org.hisp.dhis.icon.CustomIconStore;
import org.hisp.dhis.icon.IconOperationParams;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.icon.CustomIconStore")
@RequiredArgsConstructor
public class JdbcCustomIconStore implements CustomIconStore {
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private static final RowMapper<CustomIcon> customIconRowMapper =
      (rs, rowNum) -> {
        CustomIcon customIcon = new CustomIcon();

        customIcon.setKey(rs.getString("iconkey"));
        customIcon.setDescription(rs.getString("icondescription"));
        customIcon.setKeywords((String[]) rs.getArray("keywords").getArray());
        customIcon.setFileResourceUid(rs.getString("fileresourceuid"));
        customIcon.setCreatedByUserUid(rs.getString("useruid"));
        customIcon.setCreated(rs.getTimestamp("created"));
        customIcon.setLastUpdated(rs.getTimestamp("lastupdated"));

        return customIcon;
      };

  @Override
  public CustomIcon getIconByKey(String key) {
    final String sql =
        """
            select c.key as iconkey, c.description as icondescription, c.keywords as keywords, c.created as created, c.lastupdated as lastupdated,
            f.uid as fileresourceuid, u.uid as useruid
            from customicon c join fileresource f on f.fileresourceid = c.fileresourceid
            join userinfo u on u.userinfoid = c.createdby
            where key = ?
            """;

    List<CustomIcon> customIcons = jdbcTemplate.query(sql, customIconRowMapper, key);

    return customIcons.isEmpty() ? null : customIcons.get(0);
  }

  @Override
  public Stream<CustomIcon> getIcons(IconOperationParams iconOperationParams) {

    String sql =
        """
                  select c.key as iconkey, c.description as icondescription, c.keywords as keywords, c.created as created, c.lastupdated as lastupdated,
                  f.uid as fileresourceuid, u.uid as useruid
                  from customicon c join fileresource f on f.fileresourceid = c.fileresourceid
                  join userinfo u on u.userinfoid = c.createdby
                  """;

    MapSqlParameterSource parameterSource = new MapSqlParameterSource();

    sql = buildIconQuery(iconOperationParams, sql, parameterSource);

    if (iconOperationParams.isPaging()) {
      sql =
          getPaginatedQuery(
              iconOperationParams.getPager().getPage(),
              iconOperationParams.getPager().getPageSize(),
              sql,
              parameterSource);
    }

    return namedParameterJdbcTemplate.query(sql, parameterSource, customIconRowMapper).stream();
  }

  @Override
  public long count(IconOperationParams iconOperationParams) {

    String sql = """
     select count(*) from customicon c
                      """;

    MapSqlParameterSource parameterSource = new MapSqlParameterSource();

    sql = buildIconQuery(iconOperationParams, sql, parameterSource);

    return Optional.ofNullable(
            namedParameterJdbcTemplate.queryForObject(sql, parameterSource, Long.class))
        .orElse(0L);
  }

  @Override
  public Set<String> getKeywords() {
    return Set.copyOf(
        jdbcTemplate.queryForList(
            "select distinct unnest(keywords) from customicon", String.class));
  }

  @Override
  public void save(CustomIcon customIcon, FileResource fileResource, UserDetails createdByUser) {
    jdbcTemplate.update(
        "INSERT INTO customicon (key, description, keywords, fileresourceid, createdby, created, lastupdated) VALUES (?, ?, ?, ?,?,?,?)",
        customIcon.getKey(),
        customIcon.getDescription(),
        customIcon.getKeywords(),
        fileResource.getId(),
        createdByUser.getId(),
        customIcon.getCreated(),
        customIcon.getLastUpdated());
  }

  @Override
  public void delete(String customIconKey) {
    jdbcTemplate.update("delete from customicon where key = ?", customIconKey);
  }

  @Override
  public void update(CustomIcon customIcon) {
    jdbcTemplate.update(
        "update customicon set description = ?, keywords = ?, lastupdated = ? where key = ?",
        customIcon.getDescription(),
        customIcon.getKeywords(),
        customIcon.getLastUpdated(),
        customIcon.getKey());
  }

  private String buildIconQuery(
      IconOperationParams iconOperationParams, String sql, MapSqlParameterSource parameterSource) {
    SqlHelper hlp = new SqlHelper(true);

    if (iconOperationParams.hasLastUpdatedStartDate()) {
      sql += hlp.whereAnd() + " c.lastupdated >= :lastUpdatedStartDate ";

      parameterSource.addValue(
          ":lastUpdatedStartDate", iconOperationParams.getLastUpdatedStartDate(), Types.TIMESTAMP);
    }

    if (iconOperationParams.hasLastUpdatedEndDate()) {
      sql += hlp.whereAnd() + " c.lastupdated <= :lastUpdatedEndDate ";

      parameterSource.addValue(
          "lastUpdatedEndDate", iconOperationParams.getLastUpdatedEndDate(), Types.TIMESTAMP);
    }

    if (iconOperationParams.hasCreatedStartDate()) {
      sql += hlp.whereAnd() + " c.created >= :createdStartDate";

      parameterSource.addValue(
          "createdStartDate", iconOperationParams.getCreatedStartDate(), Types.TIMESTAMP);
    }

    if (iconOperationParams.hasCreatedEndDate()) {
      sql += hlp.whereAnd() + " c.created <= :createdEndDate ";

      parameterSource.addValue(
          "createdEndDate", iconOperationParams.getCreatedEndDate(), Types.TIMESTAMP);
    }

    if (iconOperationParams.hasKeywords()) {

      sql += hlp.whereAnd() + " keywords @> string_to_array(:keywords,',') ";

      parameterSource.addValue(
          "keywords",
          iconOperationParams.getKeywords().stream().collect(Collectors.joining(",", "", "")));
    }

    if (iconOperationParams.hasKeys()) {
      sql += hlp.whereAnd() + " c.key IN (:keys )";

      parameterSource.addValue("keys", iconOperationParams.getKeys());
    }

    return sql;
  }

  private String getPaginatedQuery(
      int page, int pageSize, String sql, MapSqlParameterSource mapSqlParameterSource) {

    sql = sql + " LIMIT :limit OFFSET :offset ";

    int offset = (page - 1) * pageSize;

    mapSqlParameterSource.addValue("limit", pageSize);
    mapSqlParameterSource.addValue("offset", offset);

    return sql;
  }
}
