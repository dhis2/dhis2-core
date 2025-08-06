/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.util;

import javax.annotation.Nullable;

public class ExceptionUtils {

  /**
   * Some exceptions (e.g. Database) can have deeply-nested root causes and may have a very vague
   * top-level message, which may not be very helpful to log/return. This method checks if a more
   * detailed, user-friendly message is available and returns it if found.
   *
   * <p>For example, instead of returning: <b><i>org.hibernate.exception.GenericJDBCException: could
   * not execute statement</i></b> , potentially returning: <b><i>"PSQLException: ERROR: cannot
   * execute UPDATE in a read-only transaction"</i></b>.
   *
   * @param ex exception to check
   * @return detailed message or original exception message
   */
  @Nullable
  public static String getHelpfulMessage(Exception ex) {
    Throwable cause = ex.getCause();

    if (cause != null) {
      Throwable rootCause = cause.getCause();
      if (rootCause != null) {
        return rootCause.getMessage();
      }
    }
    return ex.getMessage();
  }
}
