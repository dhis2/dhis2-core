/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.user.hibernate;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.RowMapper;

/**
 * Maps a {@code uid, code, username, firstname, surname, name} projection to a transient,
 * session-less {@link User}. Used by {@link HibernateUserRoleStore#getUserSummaries} and {@link
 * HibernateUserGroupStore#getUserSummaries} to avoid loading full {@link User} entities via
 * Hibernate just to serialize a handful of fields. The returned instances are never attached to a
 * Hibernate session and must not be passed to any session operation.
 */
class UserSummaryRowMapper implements RowMapper<User> {
  static final UserSummaryRowMapper INSTANCE = new UserSummaryRowMapper();

  static final String SELECT_COLUMNS = "u.uid, u.code, u.username, u.firstname, u.surname, u.name";

  @Override
  public User mapRow(ResultSet rs, int rowNum) throws SQLException {
    User user = new User();
    user.setUid(rs.getString("uid"));
    user.setCode(rs.getString("code"));
    user.setUsername(rs.getString("username"));
    user.setFirstName(rs.getString("firstname"));
    user.setSurname(rs.getString("surname"));
    user.setName(rs.getString("name"));
    return user;
  }
}
