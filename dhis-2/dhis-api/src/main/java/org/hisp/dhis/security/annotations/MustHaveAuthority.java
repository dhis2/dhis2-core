package org.hisp.dhis.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hisp.dhis.security.Authorities;
import org.springframework.security.access.prepost.PreAuthorize;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole(T(org.hisp.dhis.security.Authorities).F_INDICATOR_MERGE.toString())")
//    "hasRole(authority())")
public @interface MustHaveAuthority {
  Authorities authority();

  String IS_ORGANIZATION_OWNER = "principal.userObject.isOwnerOf(#organization.id)";
  //  default Authorities getAuth() throws NoSuchMethodException {
  //    getClass().getMethod("adder", int.class).getAnnotation(Number.class);
  //    return Authorities.F_EDIT_EXPIRED;
  //  }
}
