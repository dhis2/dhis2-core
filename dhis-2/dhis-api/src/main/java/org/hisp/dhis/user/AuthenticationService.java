/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import javax.annotation.CheckForNull;
import org.hisp.dhis.feedback.NotFoundException;

/**
 * Service that handles user authentication related actions. For example, switching the thread
 * context in such a way that the thread effectively executing as a certain user.
 *
 * @author Jan Bernitt
 */
public interface AuthenticationService {
  /**
   * Internally "login" as the provided user in the current thread without providing credentials of
   * some sort. The created context authority will not have credentials.
   *
   * <p>A.k.a. "becoming" a certain user
   *
   * <p>When user ID parameter is undefined the current thread is unlinked from any user.
   *
   * @param userId as this user, maybe {@code null} to unlink the current thread from a user
   * @throws NotFoundException when no user with the provided ID exists
   */
  void obtainAuthentication(@CheckForNull String userId) throws NotFoundException;

  /**
   * "Logout" or clear the current thread context.
   *
   * <p>A.k.a. unbecoming a any specific user.
   */
  void clearAuthentication();
}
