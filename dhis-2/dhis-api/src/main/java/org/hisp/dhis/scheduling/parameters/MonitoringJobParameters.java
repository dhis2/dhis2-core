package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.validation.ValidationRuleGroup;

import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class MonitoringJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 5L;

    private JobId jobId;

    @Property
    private List<Period> periods;

    @Property
    private List<OrganisationUnit> organisationUnits;

    @Property
    private List<ValidationRuleGroup> validationRuleGroups;

    // Optional parameters
    @Property
    private boolean sendNotifications;

    @Property
    private boolean persistResults;

    public MonitoringJobParameters()
    {}

    public MonitoringJobParameters( JobId jobId, List<Period> periods, List<OrganisationUnit> organisationUnits, List<ValidationRuleGroup> validationRuleGroups, boolean sendNotifications, boolean persistResults )
    {
        this.jobId = jobId;
        this.periods = periods;
        this.organisationUnits = organisationUnits;
        this.validationRuleGroups = validationRuleGroups;
        this.sendNotifications = sendNotifications;
        this.persistResults = persistResults;
    }

    public JobId getJobId()
    {
        return jobId;
    }

    public void setJobId( JobId jobId )
    {
        this.jobId = jobId;
    }

    public List<Period> getPeriods()
    {
        return periods;
    }

    public void setPeriods( List<Period> periods )
    {
        this.periods = periods;
    }

    public List<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( List<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    public List<ValidationRuleGroup> getValidationRuleGroups()
    {
        return validationRuleGroups;
    }

    public void setValidationRuleGroups( List<ValidationRuleGroup> validationRuleGroups )
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
}
