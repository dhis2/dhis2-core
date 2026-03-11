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
package org.hisp.dhis.config;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Extension of {@link JpaTransactionManager} that sets the {@code app.dhis2_user} PostgreSQL
 * session variable at the start of every non-read-only transaction. This variable is consumed by
 * the {@code dhis2_db_audit_fn} trigger function, enabling DB-level audit recording that is
 * atomically consistent with the originating transaction.
 *
 * <p>The variable is set with {@code is_local=true} (transaction-scoped), so it is automatically
 * cleared on commit or rollback — safe for use with connection pools such as HikariCP.
 *
 * <p>Audit failure (e.g. the variable cannot be set) is logged as a warning but never propagates —
 * business transactions must not be aborted by audit bookkeeping.
 */
@Slf4j
public class DbAuditTransactionManager extends JpaTransactionManager {

  private final DataSource dataSource;
  private final Supplier<String> usernameSupplier;

  public DbAuditTransactionManager(
      EntityManagerFactory entityManagerFactory,
      DataSource dataSource,
      Supplier<String> usernameSupplier) {
    super(entityManagerFactory);
    this.dataSource = dataSource;
    this.usernameSupplier = usernameSupplier;
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    // Spring binds the connection to the transaction first.
    super.doBegin(transaction, definition);

    if (definition.isReadOnly()) {
      return;
    }

    String username = usernameSupplier.get();
    Connection conn = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement ps =
        conn.prepareStatement("SELECT set_config('app.dhis2_user', ?, true)")) {
      ps.setString(1, username);
      ps.execute();
    } catch (SQLException e) {
      log.warn("Could not set app.dhis2_user session variable for DB audit", e);
      // Never throw — audit failure must not abort business transactions.
    }
  }
}
