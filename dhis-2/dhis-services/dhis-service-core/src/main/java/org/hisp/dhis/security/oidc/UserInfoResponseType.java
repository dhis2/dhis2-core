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
package org.hisp.dhis.security.oidc;

import javax.annotation.CheckForNull;

/**
 * Selects how DHIS2 should consume the OIDC userinfo response for a given provider.
 *
 * <ul>
 *   <li>{@link #JSON} — Spring Security's default: userinfo endpoint returns {@code
 *       application/json}.
 *   <li>{@link #JWT} — userinfo endpoint returns a signed JWT ({@code application/jwt}); used by
 *       MOSIP eSignet and similar IdPs.
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public enum UserInfoResponseType {
  JSON,
  JWT;

  /**
   * @param value config string from {@code dhis.conf}; case-insensitive
   * @return matching enum, defaulting to {@link #JSON} when {@code value} is {@code null} or blank
   * @throws IllegalArgumentException for unknown values
   */
  public static UserInfoResponseType fromConfig(@CheckForNull String value) {
    if (value == null || value.isBlank()) {
      return JSON;
    }
    return UserInfoResponseType.valueOf(value.trim().toUpperCase());
  }
}
