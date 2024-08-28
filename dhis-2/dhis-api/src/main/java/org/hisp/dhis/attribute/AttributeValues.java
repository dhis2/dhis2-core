package org.hisp.dhis.attribute;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Abstraction of a mutable set of {@link Attribute} values.
 *
 * <p>This should prevent usages to get dependent on the exact representation of attribute values as
 * well as prevent issues with managing the state such as the backing collection being
 * uninitialised/null.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public interface AttributeValues {

  /**
   * @return a fresh mutable empty collection of attribute values
   */
  @Nonnull
  static AttributeValues empty() {
    //FIXME(JB)
    return null;
  }

  @Nonnull
  static AttributeValues of(@Nonnull Map<String, String> values) {
    //FIXME(JB)
    return null;
  }

  boolean isEmpty();

  int size();

  @CheckForNull
  String get( String uid);

  void add(@Nonnull String uid, @Nonnull String value);

  void remove(@Nonnull String uid);

  void forEach(BiConsumer<String,String> action);

  @Nonnull
  Stream<Map.Entry<String, String>> stream();
}
