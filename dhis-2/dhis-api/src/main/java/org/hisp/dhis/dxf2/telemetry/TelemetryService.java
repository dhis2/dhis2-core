package org.hisp.dhis.dxf2.telemetry;

public interface TelemetryService {
    public static final String DEFAULT_TELEMETRY_URL = "https://telemetry.dhis2.org/v1";
    
    TelemetryData getTelemetryData();
    void pushTelemetryReport();
}
