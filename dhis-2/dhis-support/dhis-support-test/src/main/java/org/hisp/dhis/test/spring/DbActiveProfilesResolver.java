package org.hisp.dhis.test.spring;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.context.support.DefaultActiveProfilesResolver;

@Slf4j
public class DbActiveProfilesResolver implements ActiveProfilesResolver {

  private static final DefaultActiveProfilesResolver defaultActiveProfilesResolver =
      new DefaultActiveProfilesResolver();

  @Override
  public String[] resolve(Class<?> testClass) {
    //     TODO could this simply delegate to the default resolver, try to find our DB annotations
    // and add the db profile to whatever the default resolver does?
    String[] profiles = defaultActiveProfilesResolver.resolve(testClass);
    System.out.println(testClass + " resolved profiles " + Arrays.toString(profiles));
    return new String[0];
  }
}
