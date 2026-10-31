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
package org.hisp.dhis.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service("org.hisp.dhis.configuration.ConfigurationService")
public class DefaultConfigurationService implements ConfigurationService {
  private static final String CORS_WHITELIST_CACHE_KEY = "CORS_WHITELIST";

  private final GenericStore<Configuration> configurationStore;

  private final Cache<CorsAllowlist> corsWhitelistCache;

  /**
   * The CORS allowlist as stored and its valid entries, parsed once per cache load.
   *
   * @param stored the entries as stored, returned by the API so invalid ones can be corrected
   * @param entries the valid entries used to decide which origins are trusted
   */
  private record CorsAllowlist(Set<String> stored, List<CorsAllowlistEntry> entries) {}

  public DefaultConfigurationService(
      @Qualifier("org.hisp.dhis.configuration.ConfigurationStore")
          GenericStore<Configuration> configurationStore,
      CacheProvider cacheProvider) {
    this.configurationStore = configurationStore;
    this.corsWhitelistCache = cacheProvider.createCorsWhitelistCache();
  }

  // -------------------------------------------------------------------------
  // ConfigurationService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void setConfiguration(Configuration configuration) {
    if (configuration == null) {
      return;
    }
    if (configuration.getId() > 0) {
      configurationStore.update(configuration);
    } else {
      configurationStore.save(configuration);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Configuration getConfiguration() {
    Iterator<Configuration> iterator = configurationStore.getAll().iterator();

    return iterator.hasNext() ? iterator.next() : new Configuration();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getCorsWhitelist() {
    return getCorsAllowlist().stored();
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getCorsAllowlistOrigins() {
    return getCorsAllowlist().entries().stream().map(CorsAllowlistEntry::getOrigin).toList();
  }

  @Override
  @Transactional
  public void setCorsWhitelist(Set<String> corsWhitelist) throws ConflictException {
    if (corsWhitelist == null) {
      throw new IllegalArgumentException("corsWhitelist must not be null");
    }
    Set<String> origins = new LinkedHashSet<>();
    for (String value : corsWhitelist) {
      CorsAllowlistEntry entry;
      try {
        entry = CorsAllowlistEntry.parse(value);
      } catch (IllegalArgumentException ex) {
        throw new ConflictException(ErrorCode.E1136, value, ex.getMessage());
      }
      if (entry.isSubdomainWildcard()) {
        log.warn(
            "CORS allowlist entry `{}` trusts every subdomain; list exact origins where possible",
            entry.getOrigin());
      }
      origins.add(entry.getOrigin());
    }
    Configuration configuration = getConfiguration();
    configuration.setCorsWhitelist(origins);
    setConfiguration(configuration);
    corsWhitelistCache.invalidate(CORS_WHITELIST_CACHE_KEY);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isCorsWhitelisted(String origin) {
    for (CorsAllowlistEntry entry : getCorsAllowlist().entries()) {
      if (entry.matches(origin)) {
        return true;
      }
    }
    return false;
  }

  private CorsAllowlist getCorsAllowlist() {
    return corsWhitelistCache.get(CORS_WHITELIST_CACHE_KEY, key -> loadCorsAllowlist());
  }

  private CorsAllowlist loadCorsAllowlist() {
    Set<String> stored = Set.copyOf(getConfiguration().getCorsWhitelist());
    List<CorsAllowlistEntry> entries = new ArrayList<>(stored.size());
    for (String value : stored) {
      try {
        entries.add(CorsAllowlistEntry.parse(value));
      } catch (IllegalArgumentException ex) {
        log.warn(
            "Ignoring invalid CORS allowlist entry `{}`: {}. Origins it was meant to trust are"
                + " not trusted until it is corrected via /api/configuration/corsAllowlist",
            value,
            ex.getMessage());
      }
    }
    return new CorsAllowlist(stored, List.copyOf(entries));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isUserInFeedbackRecipientUserGroup(UserDetails user) {
    UserGroup feedbackRecipients = getConfiguration().getFeedbackRecipients();
    if (feedbackRecipients == null) return false;
    return feedbackRecipients.getMembers().stream()
        .anyMatch(member -> member.getUid().equals(user.getUid()));
  }
}
