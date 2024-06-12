package org.hisp.dhis.programrule.engine;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record RuleEngineEffects(
        Map<String, List<ValidationEffect>> enrollmentValidationEffects,
        Map<String, List<ValidationEffect>> eventValidationEffects,
        Map<String, List<NotificationEffect>> enrollmentNotificationEffects,
        Map<String, List<NotificationEffect>> eventNotificationEffects) {

    public static RuleEngineEffects merge(RuleEngineEffects effects, RuleEngineEffects effects2) {
        return new RuleEngineEffects(null, null, null, null);
    }

    private static List<ValidationEffect> union(List<ValidationEffect> validationEffects, List<ValidationEffect> effects2){
        return Stream.of(validationEffects, effects2).flatMap(Collection::stream).toList();
    }
}
