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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class Order {

  public static Order asc(String property) {
    return new Order(property, Direction.ASCENDING);
  }

  public static Order iasc(String property) {
    return new Order(property, Direction.ASCENDING).ignoreCase();
  }

  public static Order desc(String property) {
    return new Order(property, Direction.DESCENDING);
  }

  public static Order idesc(String property) {
    return new Order(property, Direction.DESCENDING).ignoreCase();
  }

  private static Order of(String direction, String property) {
    return switch (direction) {
      case "asc" -> Order.asc(property);
      case "iasc" -> Order.iasc(property);
      case "desc" -> Order.desc(property);
      case "idesc" -> Order.idesc(property);
      default -> Order.asc(property);
    };
  }

  /**
   * Converts the specified string orders (e.g. <code>name:asc</code>) to order objects.
   *
   * @param orders the order strings that should be converted.
   * @return the converted order.
   */
  @Nonnull
  public static List<Order> parse(@CheckForNull Collection<String> orders) {
    if (orders == null) return List.of();

    Set<String> properties = new HashSet<>();
    List<Order> res = new ArrayList<>();
    for (String order : orders) {
      String[] parts = order.split(":");
      if (parts.length < 1) continue;
      String direction = "asc";
      if (parts.length == 2) direction = parts[1].toLowerCase();
      String property = parts[0];
      if (!properties.contains(property)) {
        properties.add(property);
        res.add(of(direction, property));
      }
    }
    return res;
  }

  private final String property;
  private final Direction direction;
  private final boolean ignoreCase;

  public Order(@Nonnull String property, @Nonnull Direction direction) {
    this(property, direction, false);
  }

  public Order ignoreCase() {
    return new Order(property, direction, true);
  }

  public boolean isAscending() {
    return Direction.ASCENDING == direction;
  }
}
