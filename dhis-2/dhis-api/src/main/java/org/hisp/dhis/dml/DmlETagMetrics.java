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
package org.hisp.dhis.dml;

/**
 * Metric name and tag constants for the DML observer and ETag caching subsystem. All metrics
 * related to DML observation, parsing, event bridging, and conditional ETag cache behaviour are
 * centralised here so they can be discovered in one place.
 *
 * @author Morten Svanæs
 */
public final class DmlETagMetrics {

  private DmlETagMetrics() {}

  // ---------------------------------------------------------------------------
  // DML Observer metrics (DmlObserverListener)
  // ---------------------------------------------------------------------------

  /** Per-statement counter with tag {@link #TAG_RESULT}. */
  public static final String DML_OBSERVER_STATEMENTS = "dhis2_dml_observer_statements_total";

  /** Batches successfully published on commit/close. */
  public static final String DML_OBSERVER_BATCHES_PUBLISHED =
      "dhis2_dml_observer_batches_published_total";

  /** Batches discarded on rollback. */
  public static final String DML_OBSERVER_BATCHES_DISCARDED =
      "dhis2_dml_observer_batches_discarded_total";

  /** Counter for stale-batch sweep runs. */
  public static final String DML_OBSERVER_STALE_SWEEPS = "dhis2_dml_observer_stale_sweeps_total";

  /** Distribution summary of events per published batch. */
  public static final String DML_OBSERVER_BATCH_SIZE = "dhis2_dml_observer_batch_size";

  // ---------------------------------------------------------------------------
  // ETag version bump metrics (DmlObserverListener)
  // ---------------------------------------------------------------------------

  /** Per-event counter with tag {@link #TAG_STATUS}. */
  public static final String ETAG_BRIDGE_EVENTS = "dhis2_etag_bridge_events_total";

  /** Per-entity-type version bump counter with tag {@link #TAG_ENTITY_TYPE}. */
  public static final String ETAG_VERSION_BUMPS = "dhis2_etag_version_bumps_total";

  // ---------------------------------------------------------------------------
  // ETag Cache metrics (ConditionalETagInterceptor)
  // ---------------------------------------------------------------------------

  /** Per-request counter with tag {@link #TAG_RESULT}. */
  public static final String ETAG_CACHE_REQUESTS = "dhis2_etag_cache_requests_total";

  /** Per-request counter with tag {@link #TAG_ENDPOINT_TYPE}. */
  public static final String ETAG_CACHE_ENDPOINT_TYPE = "dhis2_etag_cache_endpoint_type_total";

  // ---------------------------------------------------------------------------
  // Tag keys
  // ---------------------------------------------------------------------------

  public static final String TAG_STATUS = "status";
  public static final String TAG_RESULT = "result";
  public static final String TAG_ENTITY_TYPE = "entity_type";
  public static final String TAG_ENDPOINT_TYPE = "endpoint_type";

  // ---------------------------------------------------------------------------
  // Tag values – DML Observer statements
  // ---------------------------------------------------------------------------

  public static final String RESULT_OBSERVED = "observed";
  public static final String RESULT_SKIPPED_NON_DML = "skipped_non_dml";
  public static final String RESULT_SKIPPED_EXCLUDED = "skipped_excluded";

  // ---------------------------------------------------------------------------
  // Tag values – ETag Bridge events
  // ---------------------------------------------------------------------------

  public static final String STATUS_PROCESSED = "processed";
  public static final String STATUS_SKIPPED_UNTRACKED = "skipped_untracked";
  public static final String STATUS_SKIPPED_NULL = "skipped_null";

  // ---------------------------------------------------------------------------
  // Tag values – ETag Cache requests
  // ---------------------------------------------------------------------------

  public static final String RESULT_HIT = "hit";
  public static final String RESULT_MISS = "miss";
  public static final String RESULT_SKIP = "skip";
  public static final String ENDPOINT_COMPOSITE = "composite";
  public static final String ENDPOINT_METADATA = "metadata";
  public static final String ENDPOINT_NAMED_KEY = "named_key";
}
