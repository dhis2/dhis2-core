/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.security.oidc;

import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.AUTHORIZATION_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_ID;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_SECRET;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.DISPLAY_ALIAS;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ENABLE_LOGOUT;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ENABLE_PKCE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.END_SESSION_ENDPOINT;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.EXTERNAL_CLIENT_PREFIX;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.EXTRA_REQUEST_PARAMETERS;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.ISSUER_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.JWK_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.LOGIN_IMAGE;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.LOGIN_IMAGE_PADDING;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.MAPPING_CLAIM;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.PROVIDER_ID;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.REDIRECT_URL;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.SCOPES;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.TOKEN_URI;
import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.USERINFO_URI;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;
import org.hisp.dhis.security.oidc.provider.GenericOidcProviderBuilder;

/**
 * Parses the DHIS.conf file for valid generic OIDC provider configurations. See the DHIS2 manual
 * for how to configure a OIDC provider correctly.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public final class GenericOidcProviderConfigParser {
  public static final String OIDC_PROVIDER_PREFIX = "oidc.provider.";

  /**
   * OIDC provider config properties lines starting with these client names will be ignored by this
   * parser, these clients/providers have their own respective provider classes and config parsers.
   */
  private static final Set<String> RESERVED_PROVIDER_IDS =
      ImmutableSet.of("azure", "google", "wso2");

  private static final ImmutableMap<String, Boolean> KEY_REQUIRED_MAP;

  static {
    ImmutableMap.Builder<String, Boolean> builder = ImmutableMap.builder();

    builder.put(CLIENT_ID, Boolean.TRUE);
    builder.put(CLIENT_SECRET, Boolean.TRUE);
    builder.put(AUTHORIZATION_URI, Boolean.TRUE);
    builder.put(TOKEN_URI, Boolean.TRUE);
    builder.put(USERINFO_URI, Boolean.TRUE);
    builder.put(JWK_URI, Boolean.TRUE);

    builder.put(REDIRECT_URL, Boolean.FALSE);
    builder.put(ENABLE_LOGOUT, Boolean.FALSE);
    builder.put(ENABLE_PKCE, Boolean.FALSE);
    builder.put(DISPLAY_ALIAS, Boolean.FALSE);
    builder.put(MAPPING_CLAIM, Boolean.FALSE);
    builder.put(END_SESSION_ENDPOINT, Boolean.FALSE);
    builder.put(SCOPES, Boolean.FALSE);
    builder.put(LOGIN_IMAGE, Boolean.FALSE);
    builder.put(LOGIN_IMAGE_PADDING, Boolean.FALSE);
    builder.put(EXTRA_REQUEST_PARAMETERS, Boolean.FALSE);
    builder.put(ISSUER_URI, Boolean.FALSE);

    KEY_REQUIRED_MAP = builder.build();
  }

  private static final Set<String> VALID_KEY_NAMES = KEY_REQUIRED_MAP.keySet();

  public static final Predicate<String> IS_EXTERNAL_CLIENT =
      s -> s.contains(EXTERNAL_CLIENT_PREFIX);

  /**
   * Parses the DhisConfigurationProvider for valid OIDC providers.
   *
   * @param properties The config
   * @return A list of DhisOidcClientRegistrations
   */
  public static List<DhisOidcClientRegistration> parse(Properties properties) {
    Objects.requireNonNull(properties);

    List<DhisOidcClientRegistration> allProviderConfigs = new ArrayList<>();

    Map<String, Set<String>> keysByProvider = extractKeysGroupByProvider(properties);
    for (Map.Entry<String, Set<String>> entry : keysByProvider.entrySet()) {
      String providerName = entry.getKey();
      Set<String> providerKeys = entry.getValue();

      // Don't parse the reserved OIDC provider names, they have separate
      // config parser classes. e.g. GoogleProvider, AzureProvider...
      if (RESERVED_PROVIDER_IDS.contains(providerName)) {
        continue;
      }

      // Get external client configs key/values, before validating the
      // rest of the configuration.
      Map<String, Map<String, String>> externalClientConfigs =
          getAllExternalClients(properties, providerName, providerKeys);

      // Remove external client keys, we don't want to validate them.
      providerKeys.stream()
          .filter(IS_EXTERNAL_CLIENT)
          .collect(Collectors.toSet())
          .forEach(providerKeys::remove);

      // Validate config key names
      if (!validateKeyNames(providerName, providerKeys)) {
        continue;
      }

      Map<String, String> providerConfig = new HashMap<>();
      providerConfig.put(PROVIDER_ID, providerName);

      // Put the property values into our "providerConfig" map with
      // the full keys.
      for (String key : providerKeys) {
        String fullKey = OIDC_PROVIDER_PREFIX + providerName + "." + key;
        String configValue = properties.getProperty(fullKey);

        providerConfig.put(key, configValue);
      }

      // Validate we have all the required configuration properties.
      if (!validateConfig(providerConfig)) {
        continue;
      }

      allProviderConfigs.add(
          GenericOidcProviderBuilder.build(providerConfig, externalClientConfigs));
    }

    return allProviderConfigs;
  }

  /**
   * Groups all keys into a map with provider name as key and a set of corresponding keys as value.
   *
   * @param properties All config properties
   * @return A map with provider name as key and all its keys as a set of keys.
   */
  public static Map<String, Set<String>> extractKeysGroupByProvider(Properties properties) {
    Objects.requireNonNull(properties);

    // Get/collect all properties that start with the OIDC_PROVIDER_PREFIX.
    Predicate<String> predicate = e -> e.startsWith(OIDC_PROVIDER_PREFIX);

    Set<String> allKeys =
        properties.keySet().stream().map(Object::toString).collect(Collectors.toSet());

    return filterSplitGroupAndJoin(allKeys, predicate, 2);
  }

  /**
   * Groups all keys in a provider group that starts with "ext_client.X", into a new map with "X" as
   * the key, and a map of all its corresponding key/values as value.
   *
   * @param properties Main config properties object
   * @param providerName Provider name
   * @param providerKeys List of all keys for that provider
   * @return a Map of set of keys for each external client
   */
  public static Map<String, Map<String, String>> getAllExternalClients(
      Properties properties, String providerName, Set<String> providerKeys) {
    Objects.requireNonNull(properties);
    Objects.requireNonNull(providerName);
    Objects.requireNonNull(providerKeys);

    Map<String, Map<String, String>> allClientConfigs = new HashMap<>();

    Map<String, Set<String>> allClientsKeys =
        filterSplitGroupAndJoin(providerKeys, IS_EXTERNAL_CLIENT, 1);
    for (Map.Entry<String, Set<String>> entry : allClientsKeys.entrySet()) {
      String clientName = entry.getKey();
      Set<String> clientKeys = entry.getValue();

      Map<String, String> keyValues = new HashMap<>();

      allClientConfigs.put(clientName, keyValues);

      for (String clientKey : clientKeys) {
        String fullKey =
            OIDC_PROVIDER_PREFIX
                + providerName
                + "."
                + EXTERNAL_CLIENT_PREFIX
                + "."
                + clientName
                + "."
                + clientKey;

        keyValues.put(clientKey, (String) properties.get(fullKey));
      }
    }

    return allClientConfigs;
  }

  /**
   * Filter set on keys on predicate, then split on . , then group array on key index, then join
   * remaining part of arrays into a set of keys
   *
   * @param keys Keys to use
   * @param predicate Predicate to filter by
   * @param keyIndex Index to group arrays by
   * @return A map where key is the keyIndex part of the array, and value is a set of all joined
   *     arrays after keyIndex
   */
  public static Map<String, Set<String>> filterSplitGroupAndJoin(
      Set<String> keys, Predicate<String> predicate, int keyIndex) {
    Objects.requireNonNull(keys);
    Objects.requireNonNull(predicate);

    return keys.stream()
        .filter(predicate)
        .map(x -> x.split("\\."))
        .collect(groupAndJoinOnArrayIndex(keyIndex));
  }

  /**
   * Groups an array on input index, join the rest of the arrays into a set of keys.
   *
   * @param keyIndex What position/index in the array should be the key
   * @return a Collector
   */
  private static Collector<String[], ?, Map<String, Set<String>>> groupAndJoinOnArrayIndex(
      int keyIndex) {
    Function<String[], String> mappingFunction =
        a -> String.join(".", Arrays.copyOfRange(a, keyIndex + 1, a.length));

    return Collectors.groupingBy(
        a -> a[keyIndex], Collectors.mapping(mappingFunction, Collectors.toSet()));
  }

  /**
   * Validates that all key names are valid.
   *
   * @param providerName Name of provider
   * @param configKeys Set of config keys to validate
   * @return valid or not valid
   */
  private static boolean validateKeyNames(String providerName, Set<String> configKeys) {
    Objects.requireNonNull(providerName);
    Objects.requireNonNull(configKeys);

    Sets.SetView<String> differences = Sets.difference(configKeys, VALID_KEY_NAMES);

    if (!differences.isEmpty()) {
      checkAndLogInvalidKeyNames(providerName, differences);

      log.error(
          String.format(
              "OpenID Connect (OIDC) configuration for provider: '%s' contains one or more invalid properties. "
                  + "Failed to configure the provider successfully! "
                  + "See previous errors for more information on what property that triggered this error!",
              providerName));

      return false;
    }

    return true;
  }

  /**
   * Checks if there is any keys with invalid names, it then tries to be nice and find a possible
   * typo by calculating the Levenshtein distance, and finally logs the error to the logger.
   *
   * @param providerName Provider name
   * @param nonValidKeys Set of non valid key names
   */
  private static void checkAndLogInvalidKeyNames(
      String providerName, Sets.SetView<String> nonValidKeys) {
    Objects.requireNonNull(providerName);
    Objects.requireNonNull(nonValidKeys);

    int maxLevenshteinDistance = 3;

    for (String wrongKeyName : nonValidKeys) {
      Pair<String, Integer> wrongKeyAndMinDist = Pair.of("", maxLevenshteinDistance);

      wrongKeyAndMinDist = getLevenshteinDistances(wrongKeyName, wrongKeyAndMinDist);

      String msg =
          "OpenID Connect (OIDC) configuration for provider: '%s' contains an invalid property: '%s'";

      if (wrongKeyAndMinDist.getRight() < maxLevenshteinDistance) {
        msg += ", did you mean: '%s' ?";
        log.error(String.format(msg, providerName, wrongKeyName, wrongKeyAndMinDist.getLeft()));
      } else {
        log.error(String.format(msg, providerName, wrongKeyName));
      }
    }
  }

  /**
   * Loops through all valid key names and compare against the input non valid key name and
   * calculates the Levenshtein distances between them. If the distance is less than the max
   * distance set in the keyAndMaxDist it updates the keyAndMaxDist with that key (aka. the most
   * similar)
   *
   * @param wrongKeyName The non valid key name to check against the valid ones.
   * @param keyAndMaxDist A pair of a maximum interesting Levenshtein distance and a place holder
   *     for the valid key name.
   * @return keyAndMinDist with possibly valid key name if there was a match with a valid key with
   *     less than the maximum Levenshtein distance.
   */
  private static Pair<String, Integer> getLevenshteinDistances(
      String wrongKeyName, Pair<String, Integer> keyAndMaxDist) {
    Objects.requireNonNull(wrongKeyName);
    Objects.requireNonNull(keyAndMaxDist);

    for (String validKeyName : VALID_KEY_NAMES) {
      int distance = StringUtils.getLevenshteinDistance(wrongKeyName, validKeyName);
      if (distance < keyAndMaxDist.getRight()) {
        keyAndMaxDist = Pair.of(validKeyName, distance);
      }
    }

    return keyAndMaxDist;
  }

  /**
   * Validates that all required properties are present in the final providerConfig map and that
   * uris values are valid.
   *
   * @param providerConfig Map of key value config properties for a provider
   * @return true or false
   */
  private static boolean validateConfig(Map<String, String> providerConfig) {
    Objects.requireNonNull(providerConfig);

    String providerId = providerConfig.get(PROVIDER_ID);

    for (Map.Entry<String, Boolean> entry : KEY_REQUIRED_MAP.entrySet()) {
      String key = entry.getKey();
      boolean isRequired = entry.getValue();

      String value = providerConfig.get(key);

      if (isRequired && Strings.isNullOrEmpty(value)) {
        log.error(
            String.format(
                "OpenId Connect (OIDC) configuration for provider: '%s' is missing a required property: '%s'. "
                    + "Failed to configure the provider successfully!",
                providerId, key));

        return false;
      }

      UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
      if (value != null && key.endsWith("uri") && !urlValidator.isValid(value)) {
        log.error(
            String.format(
                "OpenId Connect (OIDC) configuration for provider: '%s' has a URI property: '%s', "
                    + "with a malformed value: '%s'. Failed to configure the provider successfully!",
                providerId, key, value));

        return false;
      }
    }

    return true;
  }
}
