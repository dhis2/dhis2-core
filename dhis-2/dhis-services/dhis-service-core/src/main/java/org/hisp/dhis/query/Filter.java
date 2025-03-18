/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.query;

import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hisp.dhis.query.operators.InOperator;
import org.hisp.dhis.query.operators.Operator;
import org.hisp.dhis.query.planner.PropertyPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Accessors(chain = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Filter {
  /**
   * Path to property you want to restrict only, one first-level properties are currently supported.
   */
  private final String path;

  /** Operator for restriction. */
  private final Operator<?> operator;

  /**
   * Indicates that the {@link #path} is an attribute UID. This also means the {@link Filter} is an
   * in-memory filter.
   */
  private final boolean attribute;

  /** Query Path used in persistent part of a query. */
  @Setter private PropertyPath propertyPath;

  public Filter(String path, Operator<?> operator) {
    this(path, operator, false);
  }

  public Filter asAttribute() {
    return new Filter(path, operator, true);
  }

  @Override
  public String toString() {
    return "[" + path + ", op: " + operator + "]";
  }

  /**
   * @return true, when the condition cannot match any rows, e.g. an in-operator with an empty
   *     collection to test against
   */
  public boolean isAlwaysFalse() {
    if (operator instanceof InOperator<?> in) return in.getArgs().isEmpty();
    return false;
  }

  /**
   * @return true, if this restriction is not a single condition yet but resolved to a complex
   *     expression at the engine level.
   */
  public boolean isVirtual() {
    return isIdentifiable() || isQuery() || isMentions();
  }

  /**
   * @return true, if this restriction matches id (eq), code (eq) and name (ilike) columns
   */
  public boolean isQuery() {
    return "$query".equals(path);
  }

  /**
   * @return true, if this restriction matches on the identifiable set of paths or false if the
   *     {@link #path} is a specific value
   */
  public boolean isIdentifiable() {
    return "identifiable".equals(path);
  }

  public boolean isMentions() {
    return "mentions".equals(path) && operator instanceof InOperator;
  }

  public Stream<String> aliases() {
    return propertyPath == null ? Stream.empty() : Stream.of(propertyPath.getAlias());
  }
}
