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
package org.hisp.dhis.test.integration;

import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.IntegrationTest;
import org.hisp.dhis.test.IntegrationTestBase;
import org.hisp.dhis.test.config.PostgresTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for all Spring based integration tests which use a Postgres DB running in a Docker
 * container. Extend this if you test Spring services or repositories.
 *
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@IntegrationTest
@ContextConfiguration(classes = {PostgresTestConfig.class})
public abstract class PostgresIntegrationTestBase extends IntegrationTestBase {

  @Autowired private DbmsManager dbmsManager;

  /**
   * Flushes then detaches all Hibernate-managed entities. The tracker importer writes via JDBC,
   * bypassing Hibernate, so entities loaded into the test's thread-bound session before an import
   * linger there with stale pre-import state. Call this after importing tracker data whenever the
   * test reads the import's outcome back through Hibernate. Only call it inside the test
   * transaction (test methods of {@code @Transactional} tests) -- in {@code @BeforeAll} there is no
   * transaction (the flush throws {@code TransactionRequiredException}) and no thread-bound session
   * to clear in the first place.
   */
  protected final void clearSession() {
    dbmsManager.clearSession();
  }
}
