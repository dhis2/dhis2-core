package org.hisp.dhis.setting;

import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonPrimitive;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@link Settings} are plain {@link String} key-value pairs that can be accessed as certain types
 * using of the {@code asX} helper methods.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public sealed interface Settings permits UserSettings, SystemSettings{

  /**
   * Note that this only entails the names that have a defined value in the database. Settings that
   * are not defined in data and which do fall back to their default value are not included. This
   * can be used to test if a default would take an effect in case logic needs to combine several
   * settings based on them being defined.
   *
   * @return names of the settings whose value is defined as data
   */
  Set<String> keys();

  /**
   * @return a map of the names and raw values
   */
  Map<String, String> toMap();

  /**
   * @return a JSON of all public properties that are non-null with their JSON value
   */
  JsonMap<? extends JsonPrimitive> toJson();

  <E extends Enum<E>> E asEnum(String key, @Nonnull E defaultValue);

  @Nonnull
  String asString(String key, @Nonnull String defaultValue);

  @Nonnull
  Date asDate(String key, @Nonnull Date defaultValue);

  @Nonnull
  Locale asLocale(String key, @Nonnull Locale defaultValue);

  int asInt(String key, int defaultValue);

  double asDouble(String key, double defaultValue);

  boolean asBoolean(String key, boolean defaultValue);
}
