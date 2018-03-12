package org.hisp.dhis.analysis;

public class FollowupParams
{
    private String dataElementId;

    private String periodId;

    private String organisationUnitId;

    private String categoryOptionComboId;

    private String attributeOptionComboId;

    private boolean followup;

    public FollowupParams()
    {
    }

    public FollowupParams( String dataElementId, String periodId, String organisationUnitId, String categoryOptionComboId, String attributeOptionComboId, boolean followup )
    {
        this.dataElementId = dataElementId;
        this.periodId = periodId;
        this.organisationUnitId = organisationUnitId;
        this.categoryOptionComboId = categoryOptionComboId;
        this.attributeOptionComboId = attributeOptionComboId;
        this.followup = followup;
    }

    public String getDataElementId()
    {
        return dataElementId;
    }

    public void setDataElementId( String dataElementId )
    {
        this.dataElementId = dataElementId;
    }

    public String getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId( String periodId )
    {
        this.periodId = periodId;
    }

    public String getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    public String getCategoryOptionComboId()
    {
        return categoryOptionComboId;
    }

    public void setCategoryOptionComboId( String categoryOptionComboId )
    {
        this.categoryOptionComboId = categoryOptionComboId;
    }

    public String getAttributeOptionComboId()
    {
        return attributeOptionComboId;
    }

    public void setAttributeOptionComboId( String attributeOptionComboId )
    {
        this.attributeOptionComboId = attributeOptionComboId;
    }

    public boolean isFollowup()
    {
        return followup;
    }

    public void setFollowup( boolean followup )
    {
        this.followup = followup;
    }
}
