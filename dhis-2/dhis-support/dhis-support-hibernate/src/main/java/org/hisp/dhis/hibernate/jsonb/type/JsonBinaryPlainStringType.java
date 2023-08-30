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
package org.hisp.dhis.hibernate.jsonb.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.postgresql.util.PGobject;

/**
 * User defined type to handle dynamic json structures to be stored in jsonb. Always deserializes
 * into java Strings.
 *
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class JsonBinaryPlainStringType extends JsonBinaryType {
  @Override
  public Class<?> returnedClass() {
    return String.class;
  }

  @Override
  public Object nullSafeGet(
      ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    final Object result = rs.getObject(names[0]);

    if (!rs.wasNull()) {
      String content = null;

      if (result instanceof String) {
        content = (String) result;
      } else if (result instanceof PGobject) {
        content = ((PGobject) result).getValue();
      }

      // Other types currently ignored

      if (content != null) {
        return content.toString();
      }
    }

    return null;
  }

  @Override
  public void nullSafeSet(
      PreparedStatement ps, Object value, int idx, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    if (value == null) {
      ps.setObject(idx, null);
      return;
    }

    PGobject pg = new PGobject();
    pg.setType("jsonb");
    pg.setValue(value.toString());

    ps.setObject(idx, pg);
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    return value == null ? null : value.toString();
  }

  @Override
  public void setParameterValues(Properties parameters) {}
}
