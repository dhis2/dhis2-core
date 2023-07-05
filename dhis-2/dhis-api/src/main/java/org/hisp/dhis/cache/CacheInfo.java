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
package org.hisp.dhis.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * REST API data structures for local cache information.
 *
 * @author Jan Bernitt
 */
@Getter
@AllArgsConstructor
public final class CacheInfo {

  @JsonProperty private final CacheCapInfo cap;

  @JsonProperty private final CacheBurdenInfo burden;

  @JsonProperty private final CacheGroupInfo total;

  @JsonProperty private final List<CacheGroupInfo> regions;

  @Getter
  @Setter
  @RequiredArgsConstructor
  public static final class CacheGroupInfo {
    @JsonProperty private final String name;

    @JsonProperty private final int entries;

    @JsonProperty private final long hits;

    @JsonProperty private final long misses;

    @JsonProperty private final long size;

    @JsonProperty private final double burden;

    @JsonProperty private int highBurdenEntries;

    @JsonProperty
    public String getSizeHumanReadable() {
      return humanReadableSize(size);
    }

    @JsonProperty
    public long getAverageEntrySize() {
      return entries == 0 ? 0L : size / entries;
    }

    @JsonProperty
    public String getAverageEntrySizeHumanReadable() {
      return humanReadableSize(getAverageEntrySize());
    }

    @JsonProperty
    public float getHitsMissesRatio() {
      return misses == 0 ? Float.MAX_VALUE : hits / (float) misses;
    }
  }

  @Getter
  @RequiredArgsConstructor
  public static final class CacheCapInfo {
    @JsonProperty private final int capPercentage;

    @JsonProperty private final int softCapPercentage;

    @JsonProperty private final int hardCapPercentage;
  }

  @Getter
  @RequiredArgsConstructor
  public static final class CacheBurdenInfo {

    @JsonProperty private final int entries;

    @JsonProperty private final long size;

    @JsonProperty private final double threshold;

    @JsonProperty
    public String getSizeHumanReadable() {
      return humanReadableSize(size);
    }

    @JsonProperty
    public long getAverageEntrySize() {
      return entries == 0 ? 0L : size / entries;
    }

    @JsonProperty
    public String getAverageEntrySizeHumanReadable() {
      return humanReadableSize(getAverageEntrySize());
    }
  }

  /**
   * Returns a human readable form of the passed size in bytes. As this is used in the context of
   * estimated sizes the returned human readable form is also giving a approximation.
   *
   * @param size number of bytes
   * @return a human readable form of the passed size in bytes.
   */
  public static String humanReadableSize(long size) {
    if (size == 0L) {
      return "0";
    }
    if (size > 1024L * 1024L) {
      return String.format("~%.1fMB", (size / 1024L / 1024d));
    }
    if (size > 1024L) {
      return String.format("~%.1fkB", (size / 1024d));
    }
    return "< 1kB";
  }
}
