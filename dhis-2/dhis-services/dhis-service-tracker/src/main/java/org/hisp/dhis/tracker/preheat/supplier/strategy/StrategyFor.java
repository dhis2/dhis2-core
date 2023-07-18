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
package org.hisp.dhis.tracker.preheat.supplier.strategy;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hisp.dhis.tracker.preheat.mappers.PreheatMapper;

/**
 * Annotation for {@link ClassBasedSupplierStrategy} classes that specifies the Tracker domain
 * object the annotated strategy has to process
 *
 * @author Luciano Fiandesio
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface StrategyFor {
  Class<?> value();

  Class<? extends PreheatMapper> mapper();

  /** Whether the object used in this Strategy can be cached */
  boolean cache() default false;

  /**
   * The time-to-live for the type of object being cached The value is in **minutes**. Defaults to 5
   * minutes
   */
  int ttl() default 5;

  /**
   * The maximum number of entries hold by the cache. Defaults to 5. The reason for the low default,
   * is that certain objects can contain a lot of references and quickly consume memory. For most
   * metadata, a capacity of 5 is not necessarily small either. We should always specify capacity
   * for each strategy, on not rely on the default.
   */
  long capacity() default 5;
}
