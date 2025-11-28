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
package org.hisp.dhis.hibernate;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.MDC;

/**
 * Hibernate StatementInspector that injects the X-Request-ID from MDC as a SQL comment. This allows
 * correlating SQL queries in PostgreSQL logs with application request logs.
 *
 * <p>Example output in PostgreSQL logs:
 *
 * <pre>
 * /&#42; request_id=1732468920347 &#42;/ SELECT * FROM trackedentity WHERE ...
 * </pre>
 */
public class RequestIdSqlCommentInspector implements StatementInspector {
  private static final String X_REQUEST_ID_KEY = "xRequestID";

  @Override
  public String inspect(String sql) {
    String requestId = MDC.get(X_REQUEST_ID_KEY);
    if (requestId != null && !requestId.isEmpty()) {
      return "/* request_id=" + requestId + " */ " + sql;
    }
    return sql;
  }
}
