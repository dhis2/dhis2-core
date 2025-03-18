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
package org.hisp.dhis.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to mark endpoint methods with their maturity level. If types are annotated all
 * endpoints of the type inherit the level unless their method has an overriding annotation.
 *
 * <p>Can also be used to mark endpoint parameters with their maturity level. In parameter objects
 * the corresponding fields are annotated.
 *
 * @author Jan Bernitt
 */
@Target({
  ElementType.METHOD,
  ElementType.TYPE,
  ElementType.PARAMETER,
  ElementType.FIELD,
  ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Maturity {

  enum Classification {
    /**
     * The API is stable and rarely subject to change. Change management occurs. Once an API is
     * declared stable it does not change to another classification.
     */
    STABLE,
    /**
     * The API is not stable yet and often subject to change. No change management occurs. Usually
     * it takes APIs 1-2 releases before they transition to stable.
     */
    BETA,
    /**
     * The API is an experiment or subject to change due to factors outside the API implementation
     * itself. It is subject to change or removal without any change management. An alpha API might
     * transition to beta and stable at some point but might also never be suited to do so.
     */
    ALPHA
  }

  Classification value();

  /**
   * The API is an experiment or subject to change due to factors outside the API implementation
   * itself. It is subject to change or removal without any change management. An alpha API might
   * transition to beta and stable at some point but might also never be suited to do so.
   */
  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @Maturity(Classification.ALPHA)
  @interface Alpha {

    /**
     * @return An explanation as to why the annotated element was classified as alpha
     */
    String reason() default "";
  }

  /**
   * The API is not stable yet and often subject to change. No change management occurs. Usually it
   * takes APIs 1-2 releases before they transition to stable.
   */
  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @Maturity(Classification.BETA)
  @interface Beta {}

  /**
   * The API is stable and rarely subject to change. Change management occurs. Once an API is
   * declared stable it does not change to another classification.
   */
  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @Maturity(Classification.STABLE)
  @interface Stable {}
}
