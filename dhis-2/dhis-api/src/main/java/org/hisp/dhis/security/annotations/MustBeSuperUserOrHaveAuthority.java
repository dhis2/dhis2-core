package org.hisp.dhis.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hisp.dhis.security.Authorities;
import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize(
    "hasRole(T(org.hisp.dhis.security.Authorities).ALL.toString()) or hasRole(T(org.hisp.dhis.security.annotations.MustBeSuperUserOrHaveAuthority).authority().toString())")
public @interface MustBeSuperUserOrHaveAuthority {
  Authorities authority();
}
