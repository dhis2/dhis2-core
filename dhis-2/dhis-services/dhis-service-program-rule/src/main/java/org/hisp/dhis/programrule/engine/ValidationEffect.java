package org.hisp.dhis.programrule.engine;

public record ValidationEffect(String ruleId,
                               String data,
                               String field,
                               String content,
                               ValidationActionType type) {
}
