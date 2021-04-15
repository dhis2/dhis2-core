package org.hisp.dhis.analytics.data;

import org.hisp.dhis.analytics.HealthCheckService;
import org.springframework.stereotype.Service;

@Service( "org.hisp.dhis.analytics.HealthCheckService" )
public class DefaultHealthCheckService implements HealthCheckService {
    @Override
    public String alive() {
        return "OK :-)";
    }
}
