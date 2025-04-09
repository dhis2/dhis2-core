package org.hisp.dhis.analytics.event.data.programindicator.ctefactory.coalesce;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.ValueType;

@RequiredArgsConstructor
public enum ValueCoalescePolicy {

    NUMBER("0"),
    BOOLEAN("false"),
    TEXT("''"),
    DATE(null);

    private final String defaultSqlLiteral;

    /** Renders either <code>alias.value</code> or <code>coalesce(alias.value,&nbsp;default)</code>. */
    public String render(String alias) {
        if (defaultSqlLiteral == null) {
            return alias + ".value";
        }
        return "coalesce(" + alias + ".value, " + defaultSqlLiteral + ")";
    }

    /** Maps DHIS2 {@link ValueType} → policy. */
    public static ValueCoalescePolicy from(ValueType vt) {
        return switch (vt) {
            case INTEGER, NUMBER, INTEGER_POSITIVE, INTEGER_NEGATIVE,
                 INTEGER_ZERO_OR_POSITIVE, PERCENTAGE, UNIT_INTERVAL -> NUMBER;
            case BOOLEAN -> BOOLEAN;
            case DATE, DATETIME -> DATE;
            default -> TEXT;
        };
    }
}
