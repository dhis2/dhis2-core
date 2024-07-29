package org.hisp.dhis.test.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DbActiveProfilesResolverTest {
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Inherited
  @ActiveProfiles("A")
  @interface ProfileA {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Inherited
  @ActiveProfiles("B")
  @interface ProfileB {}

  @ActiveProfiles(resolver = DbActiveProfilesResolver.class)
  static class TestBase {}

  @ProfileA
  @ProfileB
  static class ExampleTest extends TestBase {}

  @Test
  void shouldResolveBothActiveProfiles() {}
}
