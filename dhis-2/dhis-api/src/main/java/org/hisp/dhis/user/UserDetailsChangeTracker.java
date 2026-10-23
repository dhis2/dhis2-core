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

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Keeps track of the users whose {@link UserDetails} changed, so that sessions holding an older
 * snapshot of their principal refresh it on their next request instead of being logged out.
 *
 * <p>Every change is recorded as a new random token per user. A session remembers the token of the
 * last change it has applied and refreshes its principal when the current token differs. Tokens are
 * kept for the session timeout: a session that is still alive makes a request within that time
 * after the change, so it always sees the token.
 *
 * <p>Changes must be recorded after the transaction that made them has committed. A refresh started
 * after reading a token then always sees the changed data.
 *
 * <p>Two implementations exist, one per session backend: an in-memory map when sessions are kept in
 * memory, and Redis when sessions are kept in Redis, so that all nodes of a cluster see the change.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface UserDetailsChangeTracker {

  /**
   * Records that the {@link UserDetails} of the given users changed.
   *
   * @param usernames the usernames of the users
   */
  void markChanged(@Nonnull Collection<String> usernames);

  /**
   * Returns the token of the latest change of the user, or null if the user did not change within
   * the session timeout.
   *
   * @param username the username of the user
   * @return the change token, or null
   */
  @CheckForNull
  String getChangeToken(@Nonnull String username);
}
