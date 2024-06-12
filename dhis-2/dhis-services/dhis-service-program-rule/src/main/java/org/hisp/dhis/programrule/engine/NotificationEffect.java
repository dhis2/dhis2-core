package org.hisp.dhis.programrule.engine;

public record NotificationEffect(String ruleId,
    String data,
    String field,
    String content,
    NotificationActionType type){}
