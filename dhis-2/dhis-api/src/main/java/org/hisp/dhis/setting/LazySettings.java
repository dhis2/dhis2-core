/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.setting;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.toUpperCase;
import static java.lang.Long.parseLong;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.i18n.locale.LocaleParsingUtils;
import org.hisp.dhis.jsontree.Json;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.translation.Translatable;

/**
 * {@link SystemSettings} or {@link UserSettings} represented by a set of keys and their values.
 *
 * <p>This only contain those settings that currently do have a defined value in the DB data.
 *
 * <p>Initially values are provided as raw {@link String} values.
 *
 * <p>When values are accessed as a specific type the conversion is applied to the {@link
 * #rawValues} and remembered as {@link #typedValues}.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@Slf4j
@ToString
@EqualsAndHashCode
@JsonSerialize(using = LazySettings.SettingsSerializer.class)
final class LazySettings implements SystemSettings, UserSettings {

  private static final Set<String> CONFIDENTIAL_KEYS = new HashSet<>();
  private static final Set<String> TRANSLATABLE_KEYS = new HashSet<>();

  private static final Map<String, String> KEY_BY_PROPERTY = new ConcurrentHashMap<>();

  private static final Map<String, Serializable> DEFAULTS_SYSTEM_SETTINGS =
      extractDefaults(SystemSettings.class);
  private static final Map<String, Serializable> DEFAULTS_USER_SETTINGS =
      extractDefaults(UserSettings.class);

  private static final LazySettings EMPTY_USER_SETTINGS =
      new LazySettings(UserSettings.class, new String[0], new String[0]);
  private static final LazySettings EMPTY_SYSTEM_SETTINGS =
      new LazySettings(SystemSettings.class, new String[0], new String[0]);

  static Set<String> keysWithDefaults(Class<? extends Settings> type) {
    if (type == SystemSettings.class) return Set.copyOf(DEFAULTS_SYSTEM_SETTINGS.keySet());
    if (type == UserSettings.class) return Set.copyOf(DEFAULTS_USER_SETTINGS.keySet());
    return Set.of();
  }

  static String keyOf(String property) {
    return KEY_BY_PROPERTY.get(property);
  }

  static boolean isConfidential(@Nonnull String key) {
    return CONFIDENTIAL_KEYS.contains(key);
  }

  static boolean isTranslatable(@Nonnull String key) {
    return TRANSLATABLE_KEYS.contains(key);
  }

  @Nonnull
  static LazySettings of(Class<? extends Settings> type, @Nonnull Map<String, String> settings) {
    return of(type, settings, (k, v) -> v);
  }

  @Nonnull
  static LazySettings of(
      Class<? extends Settings> type,
      @Nonnull Map<String, String> settings,
      @Nonnull BinaryOperator<String> decoder) {
    if (settings.isEmpty()) {
      if (type == UserSettings.class) return EMPTY_USER_SETTINGS;
      if (type == SystemSettings.class) return EMPTY_SYSTEM_SETTINGS;
    }
    // sorts alphabetically which is essential for the binary search lookup used
    TreeMap<String, String> from = new TreeMap<>();
    settings.forEach(
        (k, v) -> {
          if (v != null && !v.isEmpty()) from.put(k, v);
        });
    if (from.isEmpty()) {
      if (type == UserSettings.class) return EMPTY_USER_SETTINGS;
      if (type == SystemSettings.class) return EMPTY_SYSTEM_SETTINGS;
    }
    return of(type, from, decoder);
  }

  @Nonnull
  private static LazySettings of(
      Class<? extends Settings> type,
      @Nonnull TreeMap<String, String> settings,
      @Nonnull BinaryOperator<String> decoder) {
    String[] keys = new String[settings.size()];
    String[] values = new String[keys.length];
    int i = 0;
    for (Map.Entry<String, String> e : settings.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      keys[i] = key;
      values[i++] = isConfidential(key) ? decoder.apply(key, value) : value;
    }
    return new LazySettings(type, keys, values);
  }

  private final Class<? extends Settings> type;
  private final String[] keys;
  private final String[] rawValues;
  @ToString.Exclude @EqualsAndHashCode.Exclude private final Serializable[] typedValues;

  private LazySettings(Class<? extends Settings> type, String[] keys, String[] values) {
    this.type = type;
    this.keys = keys;
    this.rawValues = values;
    this.typedValues = new Serializable[values.length];
  }

  @Override
  public Set<String> keys() {
    return Set.of(keys);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <E extends Enum<?>> E asEnum(@Nonnull String key, @Nonnull E defaultValue) {
    Serializable value = orDefault(key, defaultValue);
    if (value != null && value.getClass() == defaultValue.getClass()) return (E) value;
    return (E) asParseValue(key, defaultValue, raw -> parseEnum(defaultValue.getClass(), raw));
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException ex) {
      return Enum.valueOf(type, value.toUpperCase());
    }
  }

  @Nonnull
  @Override
  public String asString(@Nonnull String key, @Nonnull String defaultValue) {
    if (orDefault(key, defaultValue) instanceof String value) return value;
    // Note: index will exist as otherwise we would have exited above
    String rawValue = rawValues[indexOf(key)];
    return rawValue == null ? "" : rawValue;
  }

  @Nonnull
  @Override
  public Date asDate(@Nonnull String key, @Nonnull Date defaultValue) {
    if (orDefault(key, defaultValue) instanceof Date value) return value;
    return asParseValue(key, defaultValue, LazySettings::parseDate);
  }

  @Nonnull
  @Override
  public Locale asLocale(@Nonnull String key, @Nonnull Locale defaultValue) {
    if (orDefault(key, defaultValue) instanceof Locale value) {
      return value;
    }

    return asParseValue(
        key,
        defaultValue,
        raw -> {
          try {
            // Use locale builder to parse locale from string
            // We might have locale in the format "en_US" or "en-US" or "en_US_POSIX"
            Locale.Builder builder = new Locale.Builder();
            String[] parts = raw.split("[_-]");
            if (parts.length >= 1) {
              builder.setLanguage(parts[0]);
            }
            if (parts.length >= 2) {
              builder.setRegion(parts[1]);
            }
            if (parts.length >= 3) {
              builder.setScript(parts[2]);
            }
            return builder.build();
          } catch (Exception ex) {}

          try {
            // Fallback to Apache legacy format (en_US, uz_UZ)
            return org.apache.commons.lang3.LocaleUtils.toLocale(raw);
          } catch (Exception ex) {}

          return defaultValue;
        });
  }

  @Override
  public int asInt(@Nonnull String key, int defaultValue) {
    if (orDefault(key, defaultValue) instanceof Integer value) return value;
    return asParseValue(key, defaultValue, Integer::valueOf);
  }

  @Override
  public double asDouble(@Nonnull String key, double defaultValue) {
    if (orDefault(key, defaultValue) instanceof Double value) return value;
    return asParseValue(key, defaultValue, Double::valueOf);
  }

  @Override
  public boolean asBoolean(@Nonnull String key, boolean defaultValue) {
    if (orDefault(key, defaultValue) instanceof Boolean value) return value;
    return asParseValue(key, defaultValue, Boolean::valueOf);
  }

  @Override
  public Map<String, String> toMap() {
    Map<String, String> map = new TreeMap<>();
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], rawValues[i]);
    }
    return map;
  }

  @Override
  public JsonMap<JsonMixed> toJson(boolean includeConfidential) {
    return toJson(includeConfidential, Set.of());
  }

  @Override
  public JsonMap<JsonMixed> toJson(boolean includeConfidential, @Nonnull Set<String> filter) {
    Set<String> includedKeys = new HashSet<>(filter);
    if (filter.isEmpty()) {
      includedKeys.addAll(keysWithDefaults(type));
      includedKeys.addAll(List.of(keys));
    }
    if (!includeConfidential) includedKeys.removeIf(LazySettings::isConfidential);
    return Json.object(obj -> includedKeys.forEach(key -> obj.addMember(key, asJson(key).node())))
        .asMap(JsonMixed.class);
  }

  private static final DateTimeFormatter ISO_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  @Nonnull
  private JsonValue asJson(String key) {
    Serializable defaultValue = getDefault(key);
    if (defaultValue instanceof String s) return Json.of(asString(key, s));
    if (defaultValue instanceof Date d)
      return Json.of(ISO_DATE_TIME.format(asDate(key, d).toInstant()));
    if (defaultValue instanceof Double d) return Json.of(asDouble(key, d));
    if (defaultValue instanceof Number n) return Json.of(asInt(key, n.intValue()));
    if (defaultValue instanceof Boolean b) return Json.of(asBoolean(key, b));
    if (defaultValue instanceof Locale l)
      return Json.of(LocaleParsingUtils.toUnderscoreFormat(asLocale(key, l)));
    if (defaultValue instanceof Enum<?> e) return Json.of(asEnum(key, e).toString());
    String value = asString(key, "");
    // auto-conversion based on regex when no default is known to tell the type
    if ("true".equals(value) || "false".equals(value)) return Json.of(parseBoolean(value));
    if (value.matches("^[-+]?[0-9]+$")) return Json.of(parseLong(value));
    if (value.matches("^[-+]?[0-9]?\\.[0-9]+$")) return Json.of(Double.parseDouble(value));
    return Json.of(value);
  }

  @Override
  public boolean isValid(String key, String value) {
    Serializable defaultValue = getDefault(key);
    if (value == null || value.isEmpty()) return true;
    if (defaultValue == null || defaultValue instanceof String) return true;
    if (defaultValue instanceof Boolean) return "true".equals(value) || "false".equals(value);
    try {
      // Note: The != null is just a dummy test the parse yielded anything
      if (defaultValue instanceof Double) return Double.valueOf(value) != null;
      if (defaultValue instanceof Number) return Integer.valueOf(value) != null;
      if (defaultValue instanceof Date) return parseDate(value) != null;
      if (defaultValue instanceof Locale) return LocaleUtils.toLocale(value) != null;
      if (defaultValue instanceof Enum<?>)
        return parseEnum(((Enum<?>) defaultValue).getDeclaringClass(), value) != null;
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private int indexOf(String key) {
    if (keys.length == 0) return -1;
    int i = Arrays.binarySearch(keys, key);
    if (i >= 0 || key.startsWith("key")) return i;
    // try with key prefix
    key = "key" + toUpperCase(key.charAt(0)) + key.substring(1);
    return Arrays.binarySearch(keys, key);
  }

  @CheckForNull
  private Serializable orDefault(String key, @Nonnull Serializable defaultValue) {
    int i = indexOf(key);
    return i < 0 ? defaultValue : typedValues[i];
  }

  @CheckForNull
  private Serializable getDefault(String key) {
    if (type == UserSettings.class) return DEFAULTS_USER_SETTINGS.get(key);
    if (type == SystemSettings.class) return DEFAULTS_SYSTEM_SETTINGS.get(key);
    return null;
  }

  @Nonnull
  private <T extends Serializable> T asParseValue(
      String key, @Nonnull T defaultValue, Function<String, T> parser) {
    int i = indexOf(key);
    String raw = rawValues[i];
    if (raw == null || raw.isEmpty()) {
      typedValues[i] = defaultValue; // remember the default
      return defaultValue;
    }
    T res = defaultValue;
    try {
      res = parser.apply(raw);
    } catch (Exception ex) {
      log.warn(
          "Setting {} has a raw value that cannot be parsed successfully as a {}; using default {}: {}",
          key,
          defaultValue.getClass().getSimpleName(),
          defaultValue,
          raw);
      // fall-through and use the default value
    }
    typedValues[i] = res;
    return res;
  }

  private static Date parseDate(String raw) {
    if (raw.isEmpty()) return new Date(0);
    if (raw.matches("^[0-9]+$")) return new Date(parseLong(raw));
    return Date.from(LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant());
  }

  private static Map<String, Serializable> extractDefaults(Class<? extends Settings> type) {
    Map<String, Serializable> defaults = new TreeMap<>();
    Method[] lastDefault = new Method[1];
    Object instance =
        Proxy.newProxyInstance(
            LazySettings.class.getClassLoader(),
            new Class[] {type},
            (proxy, method, args) -> {
              if (method.isDefault()) {
                lastDefault[0] = method;
                return getDefaultMethodHandle(method).bindTo(proxy).invokeWithArguments(args);
              }
              if (args == null) return null;
              if (args.length == 2) {
                String key = (String) args[0];
                String name = lastDefault[0].getName();
                String property = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                KEY_BY_PROPERTY.putIfAbsent(property, key);
                Serializable defaultValue = (Serializable) args[1];
                defaults.put(key, defaultValue);
                if (lastDefault[0].isAnnotationPresent(Confidential.class))
                  CONFIDENTIAL_KEYS.add(key);
                if (lastDefault[0].isAnnotationPresent(Translatable.class))
                  TRANSLATABLE_KEYS.add(key);
              }
              return args[1];
            });
    for (Method m : type.getDeclaredMethods()) {
      if (m.isDefault() && m.getParameterCount() == 0 && m.getName().startsWith("get")) {
        try {
          m.invoke(instance);
        } catch (Exception ex) {
          log.warn("Failed to extract setting default for method: %s".formatted(m.getName()), ex);
        }
      }
    }
    return Map.copyOf(defaults);
  }

  private static MethodHandle getDefaultMethodHandle(Method method) {
    try {
      Class<?> declaringClass = method.getDeclaringClass();
      return MethodHandles.lookup()
          .findSpecial(
              declaringClass,
              method.getName(),
              MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
              declaringClass);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  public static class SettingsSerializer extends JsonSerializer<Settings> {

    @Override
    public void serialize(Settings value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeRawValue(value.toJson(false).toJson());
      }
    }
  }
}
