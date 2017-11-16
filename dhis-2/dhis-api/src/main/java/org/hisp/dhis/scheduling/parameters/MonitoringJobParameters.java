package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.databind.JsonNode;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.io.IOException;
import java.util.List;

import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

/**
 * @author Henning Håkonsen
 * @author Stian Sandvold
 */
public class MonitoringJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -1683853240301569669L;

    @Property
    private List<RelativePeriodEnum> relativePeriods;

    @Property
    private List<String> validationRuleGroups;

    // Optional parameters

    @Property( required = FALSE )
    private boolean sendNotifications;

    @Property( required = FALSE )
    private boolean persistResults;

    public MonitoringJobParameters()
    {
    }

    public MonitoringJobParameters( List<RelativePeriodEnum> relativePeriods, List<String> validationRuleGroups,
        boolean sendNotifications, boolean persistResults )
    {
        this.relativePeriods = relativePeriods;
        this.validationRuleGroups = validationRuleGroups;
        this.sendNotifications = sendNotifications;
        this.persistResults = persistResults;
    }

    public List<RelativePeriodEnum> getRelativePeriods()
    {
        return relativePeriods;
    }

    public void setRelativePeriods( List<RelativePeriodEnum> relativePeriods )
    {
        this.relativePeriods = relativePeriods;
    }

    public List<String> getValidationRuleGroups()
    {
        return validationRuleGroups;
    }

    public void setValidationRuleGroups( List<String> validationRuleGroups )
    {
        this.validationRuleGroups = validationRuleGroups;
    }

    public boolean isSendNotifications()
    {
        return sendNotifications;
    }

    public void setSendNotifications( boolean sendNotifications )
    {
        this.sendNotifications = sendNotifications;
    }

    public boolean isPersistResults()
    {
        return persistResults;
    }

    public void setPersistResults( boolean persistResults )
    {
        this.persistResults = persistResults;
    }

    @Override
    public JobParameters mapParameters( JsonNode parameters )
        throws IOException
    {
        return null;
    }
}
