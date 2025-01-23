package org.hisp.dhis.webapi.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class that defines constants for Prometheus metrics types.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrometheusMetricsConstants
{
    public static final String GAUGE = "gauge";
    public static final String COUNTER = "counter";
    public static final String SUMMARY = "summary";
    public static final String HISTOGRAM = "histogram";
    public static final String UNTYPED = "untyped";
}
