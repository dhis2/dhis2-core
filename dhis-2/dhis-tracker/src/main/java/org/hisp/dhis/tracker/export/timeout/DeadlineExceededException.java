/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.timeout;

import java.time.Duration;
import org.springframework.dao.DataAccessException;

/**
 * Thrown when a tracker export request has used up its {@code tracker.export.timeout} budget. Maps
 * to HTTP 504 Gateway Timeout.
 *
 * <p>Extends {@link DataAccessException} so it can be returned from {@code JdbcTemplate}'s
 * exception translation, which is where a query cancelled by the deadline surfaces.
 *
 * <p>The message names the budget and nothing else. The caller knows the request they sent, and
 * what made it slow is usually something it does not name, such as ordering by a non-indexed
 * attribute. Nothing is logged either: the access log already records the 504, and the request is
 * an idempotent GET, so diagnosis is to reproduce it rather than to read anything captured here.
 */
public class DeadlineExceededException extends DataAccessException {

  public DeadlineExceededException(Duration budget) {
    super(message(budget));
  }

  public DeadlineExceededException(Duration budget, Throwable cause) {
    super(message(budget), cause);
  }

  private static String message(Duration budget) {
    return "Tracker export exceeded its time budget of %ss".formatted(budget.toSeconds());
  }
}
