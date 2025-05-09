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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import jakarta.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Cache;
import org.springframework.stereotype.Service;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
public class QueryCacheManager {
  private final Map<String, Set<String>> regionNameMap = new ConcurrentHashMap<>();

  private final HashFunction sessionIdHasher = Hashing.sha256();

  public String getQueryCacheRegionName(Class<?> klass, Query query) {
    String queryString = query.unwrap(org.hibernate.query.Query.class).getQueryString();
    return generateRegionName(klass, queryString);
  }

  public String generateRegionName(Class<?> klass, String queryString) {
    String queryStringHash =
        sessionIdHasher
            .newHasher()
            .putString(queryString, StandardCharsets.UTF_8)
            .hash()
            .toString();
    String regionName = klass.getName() + "_" + queryStringHash;

    Set<String> allQueriesOnKlass =
        regionNameMap.computeIfAbsent(klass.getName(), s -> new HashSet<>());
    allQueriesOnKlass.add(queryStringHash);

    return regionName;
  }

  public void evictQueryCache(Cache cache, Class<?> klass) {
    Set<String> hashes = regionNameMap.getOrDefault(klass.getName(), Collections.emptySet());

    for (String regionNameHash : hashes) {
      String key = klass.getName() + "_" + regionNameHash;

      cache.evictQueryRegion(key);
    }
  }
}
