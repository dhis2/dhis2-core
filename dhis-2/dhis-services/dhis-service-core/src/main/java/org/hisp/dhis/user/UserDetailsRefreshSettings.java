/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.user;

import static org.hisp.dhis.external.conf.ConfigurationKey.SYSTEM_SESSION_TIMEOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL;

import java.time.Duration;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;

/**
 * Reads the configuration of the refresh of session principals, see {@link
 * UserDetailsChangeTracker}, and logs a warning for every value that switches a part of it off or
 * that cannot be used.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public final class UserDetailsRefreshSettings {

  private UserDetailsRefreshSettings() {}

  /**
   * Returns how long a change of a user is remembered: the session timeout. A session timeout that
   * is not a positive number of seconds gives zero, which means that changes are remembered without
   * expiry. Change tokens must live at least as long as the sessions that check them, and such a
   * timeout does not expire sessions in time, or not at all.
   *
   * @param config the configuration
   * @return the time to live of a change token, zero for no expiry
   */
  @Nonnull
  public static Duration changeTokenTimeToLive(@Nonnull DhisConfigurationProvider config) {
    String value = config.getProperty(SYSTEM_SESSION_TIMEOUT);
    Integer seconds = parseSeconds(value);
    if (seconds == null || seconds <= 0) {
      log.warn(
          "{} is '{}', not a positive number of seconds: changes of users are remembered for the"
              + " session refresh without expiry",
          SYSTEM_SESSION_TIMEOUT.getKey(),
          value);
      return Duration.ZERO;
    }
    return Duration.ofSeconds(seconds);
  }

  /**
   * Returns the interval after which session principals are rebuilt also without a recorded change.
   * Zero turns that timed rebuild off. A value that is not a number of seconds of zero or more
   * gives the default interval.
   *
   * @param config the configuration
   * @return the refresh interval, zero when the timed rebuild is off
   */
  @Nonnull
  public static Duration refreshInterval(@Nonnull DhisConfigurationProvider config) {
    String value = config.getProperty(SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL);
    Integer seconds = parseSeconds(value);
    if (seconds == null || seconds < 0) {
      String defaultValue = SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL.getDefaultValue();
      log.warn(
          "{} is '{}', not a number of seconds of 0 or more: using the default of {} seconds",
          SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL.getKey(),
          value,
          defaultValue);
      return Duration.ofSeconds(Integer.parseInt(defaultValue));
    }
    if (seconds == 0) {
      log.warn(
          "{} is 0: the timed rebuild of session principals is off, so changes that are not"
              + " recorded, for example the deletion of a user group or user role, reach logged-in"
              + " users only when they log in again",
          SYSTEM_SESSION_USER_DETAILS_REFRESH_INTERVAL.getKey());
    }
    return Duration.ofSeconds(seconds);
  }

  @CheckForNull
  private static Integer parseSeconds(@CheckForNull String value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
