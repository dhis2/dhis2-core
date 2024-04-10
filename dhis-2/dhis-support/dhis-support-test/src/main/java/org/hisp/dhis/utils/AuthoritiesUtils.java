package org.hisp.dhis.utils;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.security.Authorities;

public class AuthoritiesUtils {

  private AuthoritiesUtils() {}

  /**
   * Util method to transform Authorities[] to String[]
   *
   * @param authorities - authorities
   * @return String[] of transformed authorities
   */
  public static String[] toStringArray(Authorities... authorities) {
    List<String> authorityStrings = new ArrayList<>();
    for (Authorities auth : authorities) {
      authorityStrings.add(auth.toString());
    }
    return authorityStrings.toArray(new String[0]);
  }
}
