/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.report;

import lombok.*;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This immutable object collects all the relevant information created during a
 * Tracker Import process.
 *
 * The TrackerImportReport is created at the beginning of a Tracker Import
 * session and returned to the caller.
 *
 * @author Luciano Fiandesio
 */
@Getter
@Builder( access = AccessLevel.PROTECTED )
@AllArgsConstructor( access = AccessLevel.PROTECTED )
@NoArgsConstructor( access = AccessLevel.PROTECTED )
public class TrackerImportReport
{

    /**
     * The global status of the Import operation.
     *
     * - OK - no errors ( including validation errors )
     *
     * - WARNING - at least one Warning was collected during the Import
     *
     * - ERROR - at least on Error was collected during the Import
     */
    @JsonProperty
    TrackerStatus status;

    /**
     * A list of all validation errors occurred during the validation process
     */
    @JsonProperty
    TrackerValidationReport validationReport;

    /**
     * A final summary broken down by operation (Insert, Update, Delete,
     * Ignored) showing how many entities where processed
     */
    @JsonProperty
    TrackerStats stats;

    /**
     * A report object containing the elapsed time for each Import stage
     */
    @JsonProperty
    TrackerTimingsStats timingsStats;

    /**
     * A report containing the outcome of the commit stage (e.g. how many
     * entities were persisted)
     */
    @JsonProperty
    TrackerBundleReport bundleReport;

    /**
     * A message to attach to the report. This message is designed to be used
     * only if a catastrophic error occurs and the Import Process has to stop
     * abruptly.
     */
    @JsonProperty
    String message;
}
