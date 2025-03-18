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
package org.hisp.dhis.system.util;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hisp.dhis.commons.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing stop watch functionality.
 *
 * @author Lars Helge Overland
 */
public class Clock extends StopWatch {
  private static final String SEPARATOR = ": ";

  private static final Logger defaultLog = LoggerFactory.getLogger(Clock.class);

  private Logger log;

  /** Constructor. */
  public Clock() {
    super();
  }

  /**
   * Create a new instance with a given logger.
   *
   * @param log the logger.
   * @return this {@link Clock}.
   */
  public Clock(Logger log) {
    super();
    this.log = log;
  }

  /**
   * Start the clock.
   *
   * @return this {@link Clock}.
   */
  public Clock startClock() {
    this.start();

    return this;
  }

  /**
   * Yields the elapsed time since the Clock was started as an HMS String.
   *
   * @return the elapsed time.
   */
  public String time() {
    super.split();

    return DurationFormatUtils.formatDurationHMS(super.getSplitTime());
  }

  /**
   * Logs the timestamp of the elapsed time with the given message.
   *
   * @param format the message format.
   * @param arguments the message arguments.
   * @return this {@link Clock}.
   */
  public Clock logTime(String format, Object... arguments) {
    super.split();

    String time = DurationFormatUtils.formatDurationHMS(super.getSplitTime());
    String msg = TextUtils.format(format, arguments) + SEPARATOR + time;

    if (log != null) {
      log.info(msg);
    } else {
      defaultLog.info(msg);
    }

    return this;
  }
}
