/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.programrule.engine;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record RuleEngineEffects(
    Map<String, List<ValidationEffect>> enrollmentValidationEffects,
    Map<String, List<ValidationEffect>> eventValidationEffects,
    Map<String, List<NotificationEffect>> enrollmentNotificationEffects,
    Map<String, List<NotificationEffect>> eventNotificationEffects) {

  public static RuleEngineEffects empty() {
    return new RuleEngineEffects(Map.of(), Map.of(), Map.of(), Map.of());
  }

  public static RuleEngineEffects merge(RuleEngineEffects effects, RuleEngineEffects effects2) {
    Map<String, List<ValidationEffect>> enrollmentValidationEffects =
        merge(effects.enrollmentValidationEffects, effects2.enrollmentValidationEffects);
    Map<String, List<ValidationEffect>> eventValidationEffects =
        merge(effects.eventValidationEffects, effects2.eventValidationEffects);
    Map<String, List<NotificationEffect>> enrollmentNotificationEffects =
        merge(effects.enrollmentNotificationEffects, effects2.enrollmentNotificationEffects);
    Map<String, List<NotificationEffect>> eventNotificationEffects =
        merge(effects.eventNotificationEffects, effects2.eventNotificationEffects);

    return new RuleEngineEffects(
        enrollmentValidationEffects,
        eventValidationEffects,
        enrollmentNotificationEffects,
        eventNotificationEffects);
  }

  private static <T> Map<String, List<T>> merge(
      Map<String, List<T>> effects, Map<String, List<T>> effects2) {
    return Stream.of(effects.entrySet(), effects2.entrySet())
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
