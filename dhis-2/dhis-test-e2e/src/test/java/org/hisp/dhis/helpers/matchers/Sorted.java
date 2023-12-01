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
package org.hisp.dhis.helpers.matchers;

import com.google.common.collect.Ordering;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class Sorted extends TypeSafeDiagnosingMatcher<Iterable> {
  private final String orderType;

  private final Ordering ordering;

  public Sorted(Ordering ordering, String orderType) {
    this.ordering = ordering;
    this.orderType = orderType;
  }

  public static TypeSafeDiagnosingMatcher by(String orderType) {
    Ordering ordering = Ordering.natural().nullsLast();

    if (isInDescendingOrder(orderType)) {
      ordering = ordering.reverse().nullsLast();
    }

    return new Sorted(ordering, orderType);
  }

  private static boolean isInDescendingOrder(String orderType) {
    return orderType.equalsIgnoreCase("desc") || orderType.equalsIgnoreCase("descending");
  }

  @Override
  protected boolean matchesSafely(Iterable items, Description mismatchDescription) {
    return ordering.isOrdered(items);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an iterable in ").appendText(orderType).appendText(" order");
  }
}
