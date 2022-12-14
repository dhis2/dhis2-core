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
package org.hisp.dhis.tracker.report;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.Data;

import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This report keeps track of the elapsed time for each stage of the validation
 * process.
 *
 * @author Luciano Fiandesio
 */
@Data
public class TimingsStats
{
    public static final String PREHEAT_OPS = "preheat";

    public static final String PREPROCESS_OPS = "preprocess";

    public static final String COMMIT_OPS = "commit";

    public static final String VALIDATION_OPS = "validation";

    public static final String PROGRAMRULE_OPS = "programrule";

    public static final String VALIDATE_PROGRAMRULE_OPS = "programruleValidation";

    public static final String TOTAL_OPS = "totalImport";

    public static final String PREPARE_REQUEST_OPS = "prepareRequest";

    public static final String TOTAL_REQUEST_OPS = "totalRequest";

    @JsonProperty
    private Map<String, String> timers = new LinkedHashMap<>();

    private static final String DEFAULT_VALUE = "0.0 sec.";

    public String getPrepareRequest()
    {
        return timers.getOrDefault( PREPARE_REQUEST_OPS, DEFAULT_VALUE );
    }

    public String getValidation()
    {
        return timers.getOrDefault( VALIDATION_OPS, DEFAULT_VALUE );
    }

    public String getCommit()
    {
        return timers.getOrDefault( COMMIT_OPS, DEFAULT_VALUE );
    }

    public String getPreheat()
    {
        return timers.getOrDefault( PREHEAT_OPS, DEFAULT_VALUE );
    }

    public String getProgramRule()
    {
        return timers.getOrDefault( PROGRAMRULE_OPS, DEFAULT_VALUE );
    }

    public String getTotalImport()
    {
        return timers.getOrDefault( TOTAL_OPS, DEFAULT_VALUE );
    }

    public String getTotalRequest()
    {

        return timers.getOrDefault( TOTAL_REQUEST_OPS, DEFAULT_VALUE );
    }

    @JsonIgnore
    private Timer totalTimer;

    public TimingsStats()
    {
        totalTimer = new SystemTimer().start();
    }

    public void set( String timedOperation, String elapsed )
    {
        this.timers.put( timedOperation, elapsed );
    }

    /**
     * Executes the given Supplier and measure the elapsed time.
     *
     * @param timedOperation the operation name to place in the timers map
     * @param supplier ths Supplier to execute
     *
     * @return the result of the Supplier invocation
     */
    public <T> T exec( String timedOperation, Supplier<T> supplier )
    {
        Timer timer = new SystemTimer().start();

        T result = supplier.get();

        timer.stop();

        this.set( timedOperation, timer.toString() );

        return result;
    }

    /**
     * Executes the given operation.
     *
     * @param timedOperation the operation name to place in the timers map
     * @param runnable the operation to execute
     */
    public void execVoid( String timedOperation, Runnable runnable )
    {
        Timer timer = new SystemTimer().start();

        runnable.run();

        timer.stop();

        this.set( timedOperation, timer.toString() );
    }

    public TimingsStats stopTimer()
    {
        if ( totalTimer != null )
        {
            totalTimer.stop();
            this.timers.put( TOTAL_OPS, totalTimer.toString() );
        }
        return this;

    }

    public String get( String validationOps )
    {
        return this.timers.getOrDefault( validationOps, DEFAULT_VALUE );
    }
}
