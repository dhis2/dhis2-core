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
package org.hisp.dhis.webapi.security.csp;

/**
 * Thread-local holder for storing the Content-Security-Policy to be applied to the current request.
 *
 * <p>This allows handlers and interceptors to set a custom CSP policy that will be used by the
 * CspFilter when adding CSP headers to the response.
 *
 * @author DHIS2 Team
 */
public class CspPolicyHolder {
  private static final ThreadLocal<String> CSP_POLICY = new ThreadLocal<>();

  private CspPolicyHolder() {}

  /**
   * Set the CSP policy for the current request.
   *
   * @param policy the CSP policy string (e.g., "script-src 'none'; ")
   */
  public static void setCspPolicy(String policy) {
    CSP_POLICY.set(policy);
  }

  /**
   * Get the CSP policy for the current request, if one was set.
   *
   * @return the CSP policy string, or null if no custom policy was set
   */
  public static String getCspPolicy() {
    return CSP_POLICY.get();
  }

  /**
   * Clear the CSP policy for the current request. This should be called at the end of request
   * processing to avoid ThreadLocal leaks.
   */
  public static void clear() {
    CSP_POLICY.remove();
  }
}
