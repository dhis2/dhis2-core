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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import org.hibernate.exception.JDBCConnectionException;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * Pins the exception chains a failure to obtain a database connection arrives in, so an upgrade
 * that changes how Hibernate or Spring translate it fails here instead of silently returning 500.
 */
class CrudControllerAdviceTest {

  private final CrudControllerAdvice advice = new CrudControllerAdvice();

  @Test
  void shouldReturnServiceUnavailableWhenHikariPoolTimedOut() {
    // Hikari leaves the SQL state null when nothing failed underneath, only the wait timed out
    SQLTransientConnectionException poolTimeout =
        new SQLTransientConnectionException(
            "HikariPool-1 - Connection is not available, request timed out after 2000ms", null, 0);

    WebMessage message =
        advice.cannotCreateTransactionException(
            new CannotCreateTransactionException(
                "Could not open JPA EntityManager for transaction",
                new JDBCConnectionException("Unable to acquire JDBC Connection", poolTimeout)));

    assertEquals(503, message.getHttpStatusCode());
  }

  @Test
  void shouldReturnServiceUnavailableWhenDatabaseHasTooManyClients() {
    // PostgreSQL 53300, class 53 "insufficient resources", which Hibernate does not classify as a
    // connection error by state. It only reaches the handler via the connection-creator fallback.
    SQLException tooManyClients =
        new SQLException("FATAL: sorry, too many clients already", "53300");

    WebMessage message =
        advice.cannotCreateTransactionException(
            new CannotCreateTransactionException(
                "Could not open JPA EntityManager for transaction",
                new JDBCConnectionException("Unable to acquire JDBC Connection", tooManyClients)));

    assertEquals(503, message.getHttpStatusCode());
  }

  @Test
  void shouldReturnInternalServerErrorWhenTheFailureWasNotInObtainingAConnection() {
    WebMessage message =
        advice.dataAccessResourceFailureException(
            new DataAccessResourceFailureException(
                "Could not copy into LOB stream", new SQLException("stream closed")));

    assertEquals(500, message.getHttpStatusCode());
  }
}
