package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.util.Date;
import java.util.List;

import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

/**
 * @author Henning Håkonsen
 */
public class DataValidationJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 4611088348113126038L;

    @Property
    private Date startDate;

    @Property
    private Date endDate;

    @Property
    private List<String> validationRuleGroupUids;

    @Property
    private String parentOrgUnitUid;

    @Property (required = FALSE)
    private boolean sendNotifications = false;

    @Property (required = FALSE)
    private boolean persistResults = false;

    DataValidationJobParameters()
    {
    }

    DataValidationJobParameters( Date startDate, Date endDate, List<String> validationRuleGroupUids, String parentOrgUnitUid, boolean sendNotifications, boolean persistResults )
    {
        this.startDate = startDate;
        this.endDate = endDate;
        this.validationRuleGroupUids = validationRuleGroupUids;
        this.parentOrgUnitUid = parentOrgUnitUid;
        this.sendNotifications = sendNotifications;
        this.persistResults = persistResults;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    public List<String> getValidationRuleGroupUids()
    {
        return validationRuleGroupUids;
    }

    public void setValidationRuleGroupUids( List<String> validationRuleGroupUids )
    {
        this.validationRuleGroupUids = validationRuleGroupUids;
    }

    public String getParentOrgUnitUid()
    {
        return parentOrgUnitUid;
    }

    public void setParentOrgUnitUid( String parentOrgUnitUid )
    {
        this.parentOrgUnitUid = parentOrgUnitUid;
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
