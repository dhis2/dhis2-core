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
package org.hisp.dhis.commons.util;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for concurrency operations.
 *
 * @author Lars Helge Overland
 */
@Slf4j
public class ConcurrentUtils
{
    /**
     * Blocks and waits for all Futures in the given collection to complete.
     *
     * @param futures the collection of Futures.
     */
    public static void waitForCompletion( Collection<Future<?>> futures )
    {
        for ( Future<?> future : futures )
        {
            try
            {
                future.get();
            }
            catch ( ExecutionException ex )
            {
                throw new RuntimeException( "Exception during execution", ex );
            }
            catch ( InterruptedException ex )
            {
                log.warn( "Thread interrupted", ex );

                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns a {@link Future} which is immediately completed and has its value
     * set to an empty string.
     *
     * @return a future which is immediately completed.
     */
    public static Future<?> getImmediateFuture()
    {
        return CompletableFuture.completedFuture( "" );
    }
}
