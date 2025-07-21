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
