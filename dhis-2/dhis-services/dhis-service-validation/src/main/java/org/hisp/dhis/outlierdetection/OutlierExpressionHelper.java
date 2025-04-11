package org.hisp.dhis.outlierdetection;

public enum OutlierExpressionHelper
{
    // Enum constants
    NUMERIC_PATTERN("^(-)?[0-9]+(\\.[0-9]+)?$");
    private String key;

    OutlierExpressionHelper(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
