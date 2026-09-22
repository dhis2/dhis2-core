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
package org.hisp.dhis.webapi.security.session;

import java.util.Date;
import javax.annotation.CheckForNull;

/**
 * Provides the creation time of an HTTP session by id.
 *
 * <p>Two implementations exist, one per session backend:
 *
 * <ul>
 *   <li>In-memory (non-Redis): records creation times from {@code HttpSessionCreatedEvent} into a
 *       map and clears them on {@code HttpSessionDestroyedEvent}.
 *   <li>Redis-backed Spring Session: looks up the session via {@code
 *       RedisIndexedSessionRepository#findById(String)} and returns its creation time.
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface SessionCreationTimeProvider {

  /**
   * Returns the creation time of the session with the given id, or {@code null} when the session is
   * unknown or creation time is unavailable.
   *
   * @param sessionId the raw session id (not the hashed value exposed by the API)
   * @return creation time, or null
   */
  @CheckForNull
  Date getCreationTime(String sessionId);
}
