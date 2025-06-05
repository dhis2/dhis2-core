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
package org.hisp.dhis.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpMethod;
import org.hisp.dhis.test.config.IntegrationTestBaseConfig;
import org.hisp.dhis.test.junit.SpringIntegrationTest;
import org.hisp.dhis.user.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Main base class for all Spring based integration tests. The Spring context is configured to
 * contain the Spring components to test DHIS2 services and repositories.
 *
 * <p>Concrete test classes should extend {@code
 * org.hisp.dhis.test.integration.PostgresIntegrationTestBase}.
 *
 * <p>Read through
 *
 * <ul>
 *   <li>{@link org.hisp.dhis.test}
 *   <li>{@link ContextConfiguration}
 *   <li>{@link ActiveProfiles}
 * </ul>
 *
 * if you are unsure how to get started and certainly before you create another base test class!
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ContextConfiguration(
    classes = {
      IntegrationTestBaseConfig.class,
    })
@SpringIntegrationTest
public abstract class IntegrationTestBase extends TestBase {

  @Getter private User adminUser;
  public @PersistenceContext EntityManager entityManager;

  protected final void injectAdminIntoSecurityContext() {
    injectSecurityContextUser(getAdminUser());
  }

  public abstract HttpClientAdapter.HttpResponse performBinary(
      @Nonnull HttpMethod method,
      @Nonnull String url,
      @Nonnull List<HttpClientAdapter.Header> headers,
      String contentType,
      byte[] content);
}
