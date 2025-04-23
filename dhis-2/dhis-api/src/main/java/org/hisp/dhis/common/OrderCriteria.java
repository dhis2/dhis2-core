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
package org.hisp.dhis.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

/**
 * This class is used as a container for order parameters and is deserialized from web requests
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Value
@AllArgsConstructor(staticName = "of")
public class OrderCriteria {
  String field;

  SortDirection direction;

  public static List<OrderCriteria> fromOrderString(String source) {
    return Optional.of(source)
        .filter(StringUtils::isNotBlank)
        .map(String::trim)
        .map(OrderCriteria::toOrderCriterias)
        .orElse(Collections.emptyList());
  }

  private static List<OrderCriteria> toOrderCriterias(String s) {
    return Arrays.stream(s.split(","))
        .filter(StringUtils::isNotBlank)
        .map(OrderCriteria::valueOf)
        .toList();
  }

  /**
   * Create an {@link OrderCriteria} from a string in the format of "field:direction". Valid
   * directions are defined by {@link SortDirection}.
   *
   * @throws IllegalArgumentException if the input is not in the correct format.
   */
  public static OrderCriteria valueOf(String input) {
    String[] props = input.split(":");
    if (props.length > 2) {
      throw new IllegalArgumentException(
          "Invalid order property: '"
              + input
              + "'. Valid formats are 'field:direction' or 'field'. Valid directions are 'asc' or 'desc'. Direction defaults to 'asc'.");
    }

    String field = props[0].trim();
    if (StringUtils.isEmpty(field)) {
      throw new IllegalArgumentException(
          "Missing field name in order property: '"
              + input
              + "'. Valid formats are 'field:direction' or 'field'. Valid directions are 'asc' or 'desc'. Direction defaults to 'asc'.");
    }

    SortDirection direction =
        props.length == 1 ? SortDirection.ASC : SortDirection.of(props[1].trim());
    return OrderCriteria.of(field, direction);
  }
}
