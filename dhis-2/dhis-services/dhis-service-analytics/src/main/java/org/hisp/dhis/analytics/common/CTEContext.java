package org.hisp.dhis.analytics.common;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CTEContext {
    private final Map<String, String> cteDefinitions = new LinkedHashMap<>();
    private final Map<String, String> columnMappings = new HashMap<>();

    public void addCTE(String cteName, String cteDefinition) {
        cteDefinitions.put(cteName, cteDefinition);
    }

    public void addColumnMapping(String originalColumn, String cteReference) {
        columnMappings.put(originalColumn, cteReference);
    }

    public String getCTEDefinition() {
        if (cteDefinitions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("WITH ");
        boolean first = true;
        for (Map.Entry<String, String> entry : cteDefinitions.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey())
                    .append(" AS (")
                    .append(entry.getValue())
                    .append(")");
            first = false;
        }
        return sb.toString();
    }

    public Set<String> getCTENames() {
        return cteDefinitions.keySet();
    }

    public String getColumnMapping(String columnId) {
        return columnMappings.getOrDefault(columnId, columnId);
    }
}
