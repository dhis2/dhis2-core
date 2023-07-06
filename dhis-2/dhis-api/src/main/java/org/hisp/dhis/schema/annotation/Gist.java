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
package org.hisp.dhis.schema.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hisp.dhis.schema.Property;

/**
 * {@link Gist} can be used to annotate getters to preselect whether or not the annotated property
 * is {@link #included()} in a gist item and for collection properties which {@link
 * #transformation()} should be used by default.
 *
 * <p>This can be used to fine tune gist appearance.
 *
 * <p>The annotation data becomes the {@link org.hisp.dhis.schema.GistPreferences} in {@link
 * Property}.
 *
 * @author Jan Bernitt
 */
@Documented
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Gist {
  /**
   *
   *
   * <ol>
   *   <li>When true the annotated field is always included.
   *   <li>When false the annotated field is only included when requested explicitly
   *   <li>When auto the annotated field is included based on schema and logic
   * </ol>
   *
   * @return whether or not the annotated property is included in the set of "all" fields.
   */
  Include included() default Include.AUTO;

  /**
   * @return the type used in case the user has not specified the type explicitly.
   */
  Transform transformation() default Transform.AUTO;

  enum Include {
    FALSE,
    TRUE,
    AUTO
  }

  /**
   * The {@link Transform} controls what transformation is used to transform a list or complex value
   * into a simple value that can be included in a gist.
   *
   * <p>The intention is to provide guidance and protection so that request provide useful
   * information units while avoiding overly large responses that are slow and resource intensive.
   *
   * @author Jan Bernitt
   */
  enum Transform {
    /**
     * No actual type but used to represent the choice that the actual value should be determined by
     * program logic considering other relevant metadata.
     */
    AUTO,

    /** Used to indicate that the property does not need or use a projection. */
    NONE,

    /** Emptiness of a collection (no item exists) */
    IS_EMPTY,

    /** Non-emptiness of a collection (does exist at least one item) */
    IS_NOT_EMPTY,

    /** Size of a collection */
    SIZE,

    /** Has the collection a member X (X supplied as argument) */
    MEMBER,

    /** Lacks the collection a member X (X supplied as argument) */
    NOT_MEMBER,

    /** Collection shown as a list of item UIDs. */
    IDS,

    /**
     * Identical to {@link #IDS} except that each entry is still represented as object with a single
     * property {@code id}. This is mostly for easier transition from existing APIs that usually
     * return this type of ID lists.
     *
     * <pre>
     * { "id": UID }
     * </pre>
     *
     * (instead of plain UID)
     */
    ID_OBJECTS,

    /**
     * Without argument same as {@link #IDS}, argument can be used to extract any other {@link
     * String} field.
     */
    PLUCK,

    /**
     * Provides a non-persistent property based on one or more persisted ones. For example {@code
     * name~from(firstName,surname)}.
     */
    FROM;

    public static Transform parse(String transform) {
      int startOfArgument = transform.indexOf('(');
      String name =
          (startOfArgument < 0 ? transform : transform.substring(0, startOfArgument))
              .replace("-", "")
              .replace("+", "");
      for (Transform p : Transform.values()) {
        if (p.name().replace("_", "").equalsIgnoreCase(name)) {
          return p;
        }
      }
      return AUTO;
    }
  }
}
