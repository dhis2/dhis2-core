/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.fieldfiltering.better;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

// TODO(ivo) make sure toString is useful in case of error
public final class FieldsPredicate implements Predicate<String> {
  private boolean includesAll = false;
  private final Set<String> includes;
  private final Set<String> excludes;
  private final Map<String, FieldsPredicate> children;

  /** Create FieldsPredicate which do not include any field by default. */
  public FieldsPredicate() {
    this.includes = new HashSet<>();
    this.excludes = new HashSet<>();
    this.children = new HashMap<>();
  }

  public Set<String> getIncludes() {
    return includes;
  }

  public void includeAll() {
    this.includesAll = true;
  }

  public void include(String field) {
    this.includes.add(field);
  }

  public void exclude(String field) {
    this.excludes.add(field);
  }

  public Map<String, FieldsPredicate> getChildren() {
    return children;
  }

  @Override
  public boolean test(String field) {
    if (includesAll) {
      return !excludes.contains(field);
    }

    // TODO productionize: obviously only do this once, FieldsPredicate API will likely change a bit
    // as right now its open for mutation
    includes.removeAll(excludes);
    return includes.contains(field);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (FieldsPredicate) obj;
    return Objects.equals(this.includes, that.includes)
        && Objects.equals(this.children, that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includes, children);
  }

  @Override
  public String toString() {
    return "FieldsPredicate[" + "includes=" + includes + ", " + "children=" + children + ']';
  }
}
