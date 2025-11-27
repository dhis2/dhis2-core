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
package org.hisp.dhis.analytics.tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;

/** A fluent builder for constructing metadata maps. */
class MetadataBuilder {

  private final Map<String, Object> metadata;

  private MetadataBuilder() {
    this.metadata = new HashMap<>();
  }

  /**
   * Creates a new MetadataBuilder instance.
   *
   * @return a new builder instance.
   */
  static MetadataBuilder builder() {
    return new MetadataBuilder();
  }

  /**
   * Adds a metadata entry.
   *
   * @param key the metadata key.
   * @param value the metadata value.
   * @return this builder for chaining.
   */
  MetadataBuilder put(AnalyticsMetaDataKey key, Object value) {
    metadata.put(key.getKey(), value);
    return this;
  }

  /**
   * Conditionally adds a metadata entry. The value is only computed and added if the condition is
   * true.
   *
   * @param key the metadata key.
   * @param valueSupplier supplies the value (only called if condition is true).
   * @param condition the condition that must be true for the entry to be added.
   * @return this builder for chaining.
   */
  MetadataBuilder putIf(
      AnalyticsMetaDataKey key, Supplier<Object> valueSupplier, BooleanSupplier condition) {
    if (condition.getAsBoolean()) {
      metadata.put(key.getKey(), valueSupplier.get());
    }
    return this;
  }

  /**
   * Builds and returns the metadata map.
   *
   * @return a view of the metadata map.
   */
  Map<String, Object> build() {
    return metadata;
  }
}
