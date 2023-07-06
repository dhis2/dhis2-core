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

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.icon.CustomIcon;
import org.hisp.dhis.icon.CustomIconStore;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository("org.hisp.dhis.icon.CustomIconStore")
@RequiredArgsConstructor
public class JdbcCustomIconStore implements CustomIconStore {
  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<CustomIcon> customIconRowMapper =
      (rs, rowNum) -> {
        CustomIcon customIcon = new CustomIcon();

        customIcon.setKey(rs.getString("iconkey"));
        customIcon.setDescription(rs.getString("icondescription"));
        customIcon.setKeywords((String[]) rs.getArray("keywords").getArray());
        customIcon.setFileResourceUid(rs.getString("fileresourceuid"));
        customIcon.setCreatedByUserUid(rs.getString("useruid"));

        return customIcon;
      };

  @Override
  public CustomIcon getIconByKey(String key) {
    final String sql =
        """
            select c.key as iconkey, c.description as icondescription, c.keywords as keywords, f.uid as fileresourceuid, u.uid as useruid
            from customicon c join fileresource f on f.fileresourceid = c.fileresourceid
            join userinfo u on u.userinfoid = c.createdby
            where key = ?
            """;

    List<CustomIcon> customIcons = jdbcTemplate.query(sql, customIconRowMapper, key);

    return customIcons.isEmpty() ? null : customIcons.get(0);
  }

  @Override
  public List<CustomIcon> getIconsByKeywords(String[] keywords) {
    final String sql =
        """
            select c.key as iconkey, c.description as icondescription, c.keywords as keywords, f.uid as fileresourceuid, u.uid as useruid
            from customicon c join fileresource f on f.fileresourceid = c.fileresourceid
            join userinfo u on u.userinfoid = c.createdby
            where keywords @> string_to_array(?,',')
            """;

    return jdbcTemplate.query(sql, customIconRowMapper, String.join(",", keywords));
  }

  @Override
  public List<CustomIcon> getAllIcons() {
    final String sql =
        """
            select c.key as iconkey, c.description as icondescription, c.keywords as keywords, f.uid as fileresourceuid, u.uid as useruid
            from customicon c join fileresource f on f.fileresourceid = c.fileresourceid
            join userinfo u on u.userinfoid = c.createdby
            """;

    return jdbcTemplate.query(sql, customIconRowMapper);
  }

  @Override
  public Set<String> getKeywords() {
    return Set.copyOf(
        jdbcTemplate.queryForList(
            "select distinct unnest(keywords) from customicon", String.class));
  }

  @Override
  public void save(CustomIcon customIcon, FileResource fileResource, User createdByUser) {
    jdbcTemplate.update(
        "INSERT INTO customicon (key, description, keywords, fileresourceid, createdby) VALUES (?, ?, ?, ?, ?)",
        customIcon.getKey(),
        customIcon.getDescription(),
        customIcon.getKeywords(),
        fileResource.getId(),
        createdByUser.getId());
  }

  @Override
  public void delete(String customIconKey) {
    jdbcTemplate.update("delete from customicon where key = ?", customIconKey);
  }

  @Override
  public void update(CustomIcon customIcon) {
    jdbcTemplate.update(
        "update customicon set description = ?, keywords = ? where key = ?",
        customIcon.getDescription(),
        customIcon.getKeywords(),
        customIcon.getKey());
  }
}
