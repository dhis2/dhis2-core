/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.cache;

import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.common.event.CacheInvalidationEvent;

/**
 * The {@link CacheProvider} has a factory method for each {@link Cache} use case in DHIS2.
 *
 * <p>The {@link Cache} value type is kept generic to not bind this interface to numerous domain
 * types.
 *
 * <p>Names of the factory methods follow the pattern: {@code create{region}Cache}.
 *
 * @author Jan Bernitt
 */
public interface CacheProvider {

  <V> Cache<V> createAnalyticsCache();

  <V> Cache<V> createOutliersCache();

  <V> Cache<V> createDefaultObjectCache();

  <V> Cache<V> createIsDataApprovedCache();

  <V> Cache<V> createAllConstantsCache();

  <V> Cache<V> createInUserOrgUnitHierarchyCache();

  <V> Cache<V> createInUserSearchOrgUnitHierarchyCache();

  <V> Cache<V> createUserFailedLoginAttemptCache(V defaultValue);

  <V> Cache<V> createDisable2FAFailedAttemptCache(V defaultValue);

  <V> Cache<V> createUserAccountRecoverAttemptCache(V defaultValue);

  <V> Cache<V> createProgramOwnerCache();

  <V> Cache<V> createProgramTempOwnerCache();

  <V> Cache<V> createCurrentUserGroupInfoCache();

  <V> Cache<V> createAttrOptionComboIdCache();

  <V> Cache<V> createGoogleAccessTokenCache();

  <V> Cache<V> createDataItemsPaginationCache();

  <V> Cache<V> createMetadataAttributesCache();

  <V> Cache<V> createCanDataWriteCocCache();

  <V> Cache<V> createAnalyticsSqlCache();

  <V> Cache<V> createPropertyTransformerCache();

  <V> Cache<V> createUserGroupNameCache();

  <V> Cache<V> createUserDisplayNameCache();

  void handleApplicationCachesCleared(ApplicationCacheClearedEvent event);

  void handleCacheInvalidationEvent(CacheInvalidationEvent event);

  <V> Cache<V> createProgramOrgUnitAssociationCache();

  <V> Cache<V> createCatOptOrgUnitAssociationCache();

  <V> Cache<V> createDataSetOrgUnitAssociationCache();

  <V> Cache<V> createApiKeyCache();

  <V> Cache<V> createTeAttributesCache();

  <V> Cache<V> createProgramTeAttributesCache();

  <V> Cache<V> createUserGroupUIDCache();

  <V> Cache<V> createSecurityCache();

  <V> Cache<V> createDataIntegritySummaryCache();

  <V> Cache<V> createDataIntegrityDetailsCache();

  <V> Cache<V> createQueryAliasCache();
}
