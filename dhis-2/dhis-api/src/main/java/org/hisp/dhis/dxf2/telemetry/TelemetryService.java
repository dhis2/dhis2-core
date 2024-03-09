package org.hisp.dhis.dxf2.telemetry;

public interface TelemetryService {
    TelemetryData getTelemetryData();
    void pushTelemetryData();
}
