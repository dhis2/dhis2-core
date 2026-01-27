/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A subset of a BCP47 conform locale that only supports language, region and script.
 *
 * @param language 2-3 lower case characters, e.g. {@code en}
 * @param region 2 upper case characters or 3 digits, e.g. {@code EN} (or null if undefined)
 * @param script upper case letter followed by 3 lower case letters, e.g. {@code Latn} (or null if
 *     undefined)
 * @author Jan Bernitt
 * @since 2.43
 */
public record Locale(
    @Nonnull String language, @CheckForNull String region, @CheckForNull String script)
    implements Serializable {

  public static final Locale //
      ENGLISH = of(java.util.Locale.ENGLISH),
      FRENCH = of(java.util.Locale.FRENCH),
      US = of(java.util.Locale.US);

  /**
   * Parses and creates a locale from a BCP47 compatible or Java-format string.
   *
   * @param locale BCP47 or Java format (if variant is present it is intentionally "misunderstood"
   *     as script for backwards compatability)
   * @return the parsed value
   * @throws IllegalArgumentException in case the input is not a valid locale string
   */
  @Nonnull
  public static Locale of(@Nonnull String locale) throws IllegalArgumentException {
    int len = locale.length();
    // is it just ll or lll
    if (len == 2 || len == 3) return new Locale(locale, null, null);
    // is it ll-LL or ll_LL?
    if (len == 5 && isDash(locale.charAt(2)))
      return new Locale(locale.substring(0, 2), locale.substring(3, 5), null);
    String[] parts = locale.split("-|_#|_"); // order matters!
    if (parts.length < 2 || parts.length > 3)
      throw new IllegalArgumentException("Invalid locale: " + locale);
    if (parts.length == 2) return new Locale(parts[0], parts[1]);
    if (isScript(parts[1])) return new Locale(parts[0], parts[2], parts[1]);
    return new Locale(parts[0], parts[1], parts[2]);
  }

  @JsonCreator
  @CheckForNull
  public static Locale ofNullable(@CheckForNull String locale) throws IllegalArgumentException {
    return locale == null ? null : of(locale);
  }

  @Nonnull
  public static Locale of(@Nonnull java.util.Locale locale) {
    String lang = locale.getLanguage();
    String region = locale.getCountry();
    String script = locale.getScript();
    if (region.isEmpty()) region = null;
    if (script.isEmpty()) script = null;
    return new Locale(lang, region, script);
  }

  @CheckForNull
  public static Locale ofNullable(@CheckForNull java.util.Locale locale) {
    return locale == null ? null : of(locale);
  }

  public static Locale getDefault() {
    return of(java.util.Locale.getDefault());
  }

  public Locale(@Nonnull String language, @CheckForNull String region) {
    this(language, region, null);
  }

  public Locale(@Nonnull String language) {
    this(language, null, null);
  }

  public Locale {
    if (!isLanguageCode(language))
      throw new IllegalArgumentException("Invalid language code: " + language);
    if (!isRegionCode(region)) throw new IllegalArgumentException("Invalid region code: " + region);
    if (!isScript(script)) throw new IllegalArgumentException("Invalid script: " + script);
    if (script != null && region == null)
      throw new IllegalArgumentException("Script must be used with region");
    // map outdated codes
    language =
        switch (language) {
          case "iw" -> "he";
          case "ji" -> "yi";
          case "in" -> "id";
          default -> language;
        };
  }

  private static boolean isLanguageCode(String str) {
    if (str == null) return false;
    return str.length() >= 2
        && str.length() <= 3
        && str.chars().allMatch(Locale::isLowerCaseLetter);
  }

  private static boolean isRegionCode(String str) {
    if (str == null) return true;
    if (str.length() == 2) return str.chars().allMatch(Locale::isUpperCaseLetter);
    if (str.length() == 3) return str.chars().allMatch(Locale::isDigit);
    return false;
  }

  private static boolean isScript(String str) {
    if (str == null) return true;
    return str.length() == 4
        && isUpperCaseLetter(str.charAt(0))
        && str.chars().skip(1).allMatch(Locale::isLowerCaseLetter);
  }

  private static boolean isLowerCaseLetter(int c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isUpperCaseLetter(int c) {
    return c >= 'A' && c <= 'Z';
  }

  private static boolean isDigit(int c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isDash(int c) {
    return c == '-' || c == '_';
  }

  public String toLanguageTag() {
    if (region == null && script == null) return language;
    if (script == null) return language + "-" + region;
    return language + "-" + script + "-" + region;
  }

  @Override
  public String toString() {
    if (region == null && script == null) return language;
    if (script == null) return language + "_" + region;
    return language + "_" + region + "_" + script;
  }

  @Nonnull
  public java.util.Locale toJavaLocale() {
    if (region == null && script == null) return new java.util.Locale(language);
    if (script == null) return new java.util.Locale(language, region);
    return new java.util.Locale.Builder()
        .setLanguage(language)
        .setRegion(region)
        .setScript(script)
        .build();
  }

  /*
  Display stuff (just cached resolve of the Java util code)
   */

  private static final Map<String, String> DISPLAY_NAMES = new ConcurrentHashMap<>();
  private static final Map<String, String> DISPLAY_LANGUAGES = new ConcurrentHashMap<>();
  private static final Map<String, String> DISPLAY_REGIONS = new ConcurrentHashMap<>();

  public String getDisplayName(Locale in) {
    return DISPLAY_NAMES.computeIfAbsent(
        this + " in " + in, k -> toJavaLocale().getDisplayName(in.toJavaLocale()));
  }

  public String getDisplayName() {
    return DISPLAY_NAMES.computeIfAbsent(toString(), k -> toJavaLocale().getDisplayName());
  }

  public String getDisplayLanguage() {
    return DISPLAY_LANGUAGES.computeIfAbsent(toString(), k -> toJavaLocale().getDisplayLanguage());
  }

  public String getDisplayRegion() {
    return DISPLAY_REGIONS.computeIfAbsent(toString(), k -> toJavaLocale().getDisplayCountry());
  }
}
