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
 * Controller exempt from OSIV (Open Session in View) filter.
 *
 * <p>This endpoint demonstrates the correct behavior when OSIV is disabled: slow requests don't
 * hold database connections. Compare HikariCP pool stats between this endpoint and
 * /api/debug/osiv/sleep to see the difference.
 *
 * <p>Usage:
 *
 * <pre>
 * # Start slow requests WITHOUT holding connections
 * curl -u admin:district "http://localhost:8080/api/debug/noOsiv/sleep?sleepMs=60000"
 *
 * # HikariCP logs should show active=0 even during the sleep
 * </pre>
 *
 * @see OsivSleepController for the OSIV-enabled version
 */
@RestController
@RequestMapping("/api/debug/noOsiv")
@OpenApi.Document(classifiers = {"team:platform", "purpose:support"})
public class NoOsivController {

  /**
   * Pure sleep - does NO database work and is EXEMPT from OSIV.
   *
   * <p>Unlike /api/debug/osiv/sleep, this endpoint doesn't have the OSIV filter applied, so no
   * connection is acquired or held during the request.
   *
   * @param sleepMs how long to sleep in milliseconds (default 10000)
   * @return message indicating completion
   */
  @GetMapping("/sleep")
  public String sleep(@RequestParam(defaultValue = "10000") long sleepMs)
      throws InterruptedException {
    Thread.sleep(sleepMs);
    return "Slept for "
        + sleepMs
        + "ms with NO OSIV. No connection was held. Check HikariCP pool stats.";
  }
}
