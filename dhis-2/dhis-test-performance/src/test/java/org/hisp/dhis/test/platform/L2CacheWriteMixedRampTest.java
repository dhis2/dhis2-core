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
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.percent;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;

/**
 * Write-mixed concurrency ramp for the L2 cache baseline (l2-cache-truth Phase 3).
 *
 * <p>Same hot-metadata read chain as {@link L2CacheReadHeavyRampTest}, but a configurable share of
 * workflow iterations (default {@code writePercent=5}) additionally issues a metadata write: a JSON
 * Patch on a random data element's description. Each write invalidates the DataElement entry under
 * the region write lock AND puts into {@code default-update-timestamps-region} (the query-cache
 * write tax, 26.9k puts per Phase 2 suite), invalidating every cached query on the dataelement
 * table while readers hammer the same regions. This is the READ_WRITE lock-convoy pressure
 * scenario.
 *
 * <p>Concurrent patches of the same UID can race in the metadata import; HTTP 409 is accepted on
 * the write so the run keeps measuring instead of failing (the write rate is what matters, not
 * per-write success).
 *
 * <pre>{@code
 * DHIS2_IMAGE=dhis2/core-l2truth:local \
 * DHIS_CONF_FILE=dhis-l2cache-on.conf \
 * SIMULATION_CLASS=org.hisp.dhis.test.platform.L2CacheWriteMixedRampTest \
 * ./run-simulation.sh
 * }</pre>
 *
 * <p>See {@link L2CacheRampSimulation} for the ramp model and available properties.
 *
 * @author Morten Svanæs
 */
public class L2CacheWriteMixedRampTest extends L2CacheRampSimulation {

  public L2CacheWriteMixedRampTest() {
    install(L2CacheWriteMixedRampTest::readsWithWrites);
  }

  private static ChainBuilder readsWithWrites(String p) {
    ChainBuilder write =
        exec(
            http(p + " PATCH dataElement")
                .patch("/api/dataElements/#{deUid}")
                .header("Content-Type", "application/json-patch+json")
                .body(
                    StringBody(
                        "[{\"op\":\"replace\",\"path\":\"/description\","
                            + "\"value\":\"l2-cache-truth perf #{randomUuid()}\"}]"))
                .check(status().in(200, 409)));

    return exec(reads(p)).randomSwitch().on(percent(WRITE_PERCENT).then(write));
  }
}
