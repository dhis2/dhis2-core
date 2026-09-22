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

/**
 * Read-heavy concurrency ramp for the L2 cache baseline.
 *
 * <p>Hot-metadata reads only: {@code /api/me} (exercises User's 8 cached collections, among the
 * hottest regions measured), data elements (list, field-filtered and by-id), category combos with
 * the Category* regions, and organisation units (paged list + subtree with cached children
 * collections). Under cache ON every request funnels through {@code AbstractReadWriteAccess} region
 * read locks; putFromLoad (miss path) and query-cache puts take the region write lock.
 *
 * <pre>{@code
 * DHIS2_IMAGE=dhis2/core-l2truth:local \
 * DHIS_CONF_FILE=dhis-l2cache-on.conf \
 * SIMULATION_CLASS=org.hisp.dhis.test.platform.L2CacheReadHeavyRampTest \
 * ./run-simulation.sh
 * }</pre>
 *
 * <p>See {@link L2CacheRampSimulation} for the ramp model and available properties.
 *
 * @author Morten Svanæs
 */
public class L2CacheReadHeavyRampTest extends L2CacheRampSimulation {

  public L2CacheReadHeavyRampTest() {
    install(L2CacheRampSimulation::reads);
  }
}
