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
package org.hisp.dhis.webapi.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hisp.dhis.common.OpenApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hibernate-based controller for demonstrating OSIV connection holding.
 *
 * <p>This controller tags the actual Hibernate session's connection (the one OSIV holds) so it's
 * visible in pg_stat_activity.
 *
 * <p>Usage:
 *
 * <pre>
 * # Terminal 1: Start slow requests
 * curl -u admin:district "http://localhost:8080/api/debug/hibernate/sleep?sleepMs=60000&requestId=SLOW1"
 *
 * # Terminal 2: Check pg_stat_activity
 * SELECT pid, application_name, state FROM pg_stat_activity WHERE application_name LIKE 'demo-%';
 * </pre>
 *
 * @see <a href="https://vladmihalcea.com/the-open-session-in-view-anti-pattern/">OSIV
 *     Anti-pattern</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/debug/hibernate")
@OpenApi.Document(classifiers = {"team:platform", "purpose:support"})
public class OsivHibernateController {

  @PersistenceContext private EntityManager entityManager;

  /**
   * Sleep while holding and tagging the Hibernate session's connection.
   *
   * <p>This uses the actual connection that OSIV holds (via Hibernate Session.doWork), tags it with
   * the requestId, then sleeps. The connection will be visible in pg_stat_activity.
   *
   * @param sleepMs how long to sleep in milliseconds (default 10000)
   * @param requestId identifier to tag the connection with (visible in pg_stat_activity)
   * @return message indicating completion
   */
  @GetMapping("/sleep")
  public String sleep(
      @RequestParam(defaultValue = "10000") long sleepMs,
      @RequestParam(defaultValue = "unnamed") String requestId)
      throws InterruptedException {

    Session session = entityManager.unwrap(Session.class);

    // Tag the OSIV-held connection so it's visible in pg_stat_activity
    session.doWork(
        connection -> {
          try (var stmt = connection.createStatement()) {
            stmt.execute("SET application_name = 'demo-" + requestId + "'");
          }
        });

    log.info("Tagged connection as 'demo-{}', sleeping for {}ms", requestId, sleepMs);

    try {
      Thread.sleep(sleepMs);
    } finally {
      // Reset application_name before connection returns to pool
      session.doWork(
          connection -> {
            try (var stmt = connection.createStatement()) {
              stmt.execute("SET application_name = 'PostgreSQL JDBC Driver'");
            }
          });
    }

    return "Slept for " + sleepMs + "ms. Connection 'demo-" + requestId + "' was held by OSIV.";
  }
}
