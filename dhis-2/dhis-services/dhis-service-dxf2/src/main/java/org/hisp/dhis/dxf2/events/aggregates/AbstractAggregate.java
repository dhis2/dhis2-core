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
package org.hisp.dhis.dxf2.events.aggregates;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
public abstract class AbstractAggregate
{
    /**
     * Executes the Supplier asynchronously using the thread pool from the
     * provided {@see Executor}
     *
     * @param condition A condition that, if true, executes the Supplier, if
     *        false, returns an empty Multimap
     * @param supplier The Supplier to execute
     * @param executor an Executor instance
     *
     * @return A CompletableFuture with the result of the Supplier
     */
    <T> CompletableFuture<Multimap<String, T>> conditionalAsyncFetch( boolean condition,
        Supplier<Multimap<String, T>> supplier, Executor executor )
    {
        return (condition ? supplyAsync( supplier, executor ) : supplyAsync( ArrayListMultimap::create, executor ));
    }

    /**
     * Executes the Supplier asynchronously using the thread pool from the
     * provided {@see Executor}
     *
     * @param supplier The Supplier to execute
     *
     * @return A CompletableFuture with the result of the Supplier
     */
    <T> CompletableFuture<Multimap<String, T>> asyncFetch( Supplier<Multimap<String, T>> supplier, Executor executor )
    {
        return supplyAsync( supplier, executor );
    }

}
