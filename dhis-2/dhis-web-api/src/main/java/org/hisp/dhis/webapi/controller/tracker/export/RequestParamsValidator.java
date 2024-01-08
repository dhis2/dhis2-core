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
package org.hisp.dhis.webapi.controller.tracker.export;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;

/**
 * RequestParamUtils are functions used to parse and transform tracker request parameters. This
 * class is intended to only house functions without any dependencies on services or components.
 */
class RequestParamsValidator {
  private RequestParamsValidator() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Validate the {@code order} request parameter in tracker exporters. Allowed order values are
   * {@code supportedFieldNames} and UIDs which represent {@code uidMeaning}. Every field name or
   * UID can be specified at most once.
   */
  public static void validateOrderParams(
      List<OrderCriteria> order, Set<String> supportedFieldNames, String uidMeaning)
      throws BadRequestException {
    if (order == null || order.isEmpty()) {
      return;
    }

    Set<String> invalidOrderComponents =
        order.stream().map(OrderCriteria::getField).collect(Collectors.toSet());
    invalidOrderComponents.removeAll(supportedFieldNames);
    Set<String> uids =
        invalidOrderComponents.stream()
            .filter(CodeGenerator::isValidUid)
            .collect(Collectors.toSet());
    invalidOrderComponents.removeAll(uids);

    String errorSuffix =
        String.format(
            "Supported are %s UIDs and fields '%s'. All of which can at most be specified once.",
            uidMeaning, supportedFieldNames.stream().sorted().collect(Collectors.joining(", ")));
    if (!invalidOrderComponents.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "order parameter is invalid. Cannot order by '%s'. %s",
              String.join(", ", invalidOrderComponents), errorSuffix));
    }

    validateOrderParamsContainNoDuplicates(order, errorSuffix);
  }

  private static void validateOrderParamsContainNoDuplicates(
      List<OrderCriteria> order, String errorSuffix) throws BadRequestException {
    Set<String> duplicateOrderComponents =
        order.stream()
            .map(OrderCriteria::getField)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    if (!duplicateOrderComponents.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "order parameter is invalid. '%s' are repeated. %s",
              String.join(", ", duplicateOrderComponents), errorSuffix));
    }
  }

  /**
   * Validate the {@code order} request parameter in tracker exporters. Allowed order values are
   * {@code supportedFieldNames}. Every field name can be specified at most once. If the endpoint
   * supports field names and UIDs use {@link #validateOrderParams(List, Set, String)}.
   */
  public static void validateOrderParams(List<OrderCriteria> order, Set<String> supportedFieldNames)
      throws BadRequestException {
    if (order == null || order.isEmpty()) {
      return;
    }

    Set<String> invalidOrderComponents =
        order.stream().map(OrderCriteria::getField).collect(Collectors.toSet());
    invalidOrderComponents.removeAll(supportedFieldNames);

    String errorSuffix =
        String.format(
            "Supported fields are '%s'. All of which can at most be specified once.",
            supportedFieldNames.stream().sorted().collect(Collectors.joining(", ")));
    if (!invalidOrderComponents.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "order parameter is invalid. Cannot order by '%s'. %s",
              String.join(", ", invalidOrderComponents), errorSuffix));
    }

    validateOrderParamsContainNoDuplicates(order, errorSuffix);
  }
}
