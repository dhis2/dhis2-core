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

import org.hisp.dhis.common.OpenApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pure sleep controller for demonstrating OSIV connection holding via HikariCP logs.
 *
 * <p>This endpoint does NO database work. If OSIV causes connections to be acquired early (e.g., in
 * CspFilter), you'll see active connection count increase in HikariCP pool stats even though this
 * endpoint doesn't touch the database.
 *
 * <p>Usage:
 *
 * <pre>
 * # Start slow requests and watch HikariCP pool stats
 * curl -u admin:district "http://localhost:8080/api/debug/osiv/sleep?sleepMs=60000"
 *
 * # HikariCP logs every 5s (with demo config):
 * # Pool stats (total=2, active=N, idle=M, waiting=0)
 * </pre>
 *
 * @see <a href="https://vladmihalcea.com/the-open-session-in-view-anti-pattern/">OSIV
 *     Anti-pattern</a>
 */
@RestController
@RequestMapping("/api/debug/osiv")
@OpenApi.Document(classifiers = {"team:platform", "purpose:support"})
public class OsivSleepController {

  /**
   * Pure sleep - does NO database work.
   *
   * <p>Watch HikariCP pool stats to see if connections are held by OSIV even though this endpoint
   * doesn't access the database.
   *
   * @param sleepMs how long to sleep in milliseconds (default 10000)
   * @return message indicating completion
   */
  @GetMapping("/sleep")
  public String sleep(@RequestParam(defaultValue = "10000") long sleepMs)
      throws InterruptedException {
    Thread.sleep(sleepMs);
    return "Slept for " + sleepMs + "ms with no DB access. Check HikariCP pool stats.";
  }
}
