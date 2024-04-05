package org.hisp.dhis.security.annotations;

import org.hisp.dhis.security.Authorities;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

  public boolean hasAccess(Authorities authority) {
    System.out.println("has access: " + authority);
    return true;
  }
}
