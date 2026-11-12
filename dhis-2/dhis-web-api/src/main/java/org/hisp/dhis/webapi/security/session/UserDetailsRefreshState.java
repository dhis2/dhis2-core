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

import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;

/**
 * The refresh state of the {@link UserDetails} principal of a session: the token of the last change
 * the principal was refreshed for (see {@link UserDetailsChangeTracker}) and the time in
 * milliseconds it was last rebuilt or checked.
 *
 * <p>While the session impersonates another user, the state of the impersonating user at the start
 * of the impersonation is kept as well, so that its changes are noticed during the impersonation
 * and refreshed after it.
 *
 * <p>The whole state is kept in one session attribute, so that token and time are always written
 * together. It is immutable, and it is {@link Serializable} because Redis sessions use JDK
 * serialization.
 *
 * @param changeToken the token of the last change applied to the principal, or null when none
 * @param refreshedAt the time in milliseconds the principal was last rebuilt or checked
 * @param impersonatorState the state of the impersonating user while the session impersonates
 *     another user, otherwise null
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public record UserDetailsRefreshState(
    @CheckForNull String changeToken,
    long refreshedAt,
    @CheckForNull UserDetailsRefreshState impersonatorState)
    implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private static final String SESSION_ATTRIBUTE = "DHIS2_USER_DETAILS_REFRESH_STATE";

  public UserDetailsRefreshState(@CheckForNull String changeToken, long refreshedAt) {
    this(changeToken, refreshedAt, null);
  }

  /**
   * Reads the state of the session. A session without a state has applied no change, and its
   * principal was built when the session was created.
   *
   * @throws IllegalStateException when the session was invalidated
   */
  @Nonnull
  public static UserDetailsRefreshState of(@Nonnull HttpSession session) {
    if (session.getAttribute(SESSION_ATTRIBUTE) instanceof UserDetailsRefreshState state) {
      return state;
    }
    return new UserDetailsRefreshState(null, session.getCreationTime());
  }

  /**
   * Writes this state to the session.
   *
   * @throws IllegalStateException when the session was invalidated
   */
  public void saveTo(@Nonnull HttpSession session) {
    session.setAttribute(SESSION_ATTRIBUTE, this);
  }

  /** Returns true when the current change token of the principal was not applied yet. */
  public boolean isChangedBy(@CheckForNull String currentToken) {
    return currentToken != null && !currentToken.equals(changeToken);
  }

  /**
   * Returns true when the refresh interval has passed since the principal was last rebuilt or
   * checked. A zero or negative interval is never due.
   */
  public boolean isRefreshDue(@Nonnull Duration interval, long now) {
    if (interval.isZero() || interval.isNegative()) {
      return false;
    }
    return now - refreshedAt >= interval.toMillis();
  }

  /**
   * Returns true when the current change token of the impersonating user was not applied when the
   * impersonation started. Without a saved state no change was applied.
   */
  public boolean isImpersonatorChangedBy(@CheckForNull String currentToken) {
    return impersonatorState == null
        ? currentToken != null
        : impersonatorState.isChangedBy(currentToken);
  }

  /** Returns the state after the principal was rebuilt for the given change token. */
  @Nonnull
  public UserDetailsRefreshState refreshed(@CheckForNull String token, long now) {
    return new UserDetailsRefreshState(token, now, impersonatorState);
  }

  /** Returns the state after the principal was checked without being rebuilt. */
  @Nonnull
  public UserDetailsRefreshState checked(long now) {
    return new UserDetailsRefreshState(changeToken, now, impersonatorState);
  }

  /**
   * Returns the state of a session that starts to impersonate a user. The impersonating user's
   * state is kept for its exit. Switching directly to another user keeps the state of the original
   * impersonating user.
   *
   * @param targetToken the change token of the impersonated user, read before it was loaded
   * @param now the current time in milliseconds
   */
  @Nonnull
  public UserDetailsRefreshState impersonating(@CheckForNull String targetToken, long now) {
    UserDetailsRefreshState impersonator =
        impersonatorState != null
            ? impersonatorState
            : new UserDetailsRefreshState(changeToken, refreshedAt);
    return new UserDetailsRefreshState(targetToken, now, impersonator);
  }

  /**
   * Returns the state of a session that ends an impersonation: the state of the impersonating user.
   * Without a saved state none of its changes is applied, and its refresh interval is due.
   */
  @Nonnull
  public UserDetailsRefreshState afterImpersonation() {
    return impersonatorState != null ? impersonatorState : new UserDetailsRefreshState(null, 0L);
  }
}
