package org.hisp.dhis.analysis;

public class FollowupParams
{
    private int dataElementId;

    private int periodId;

    private int organisationUnitId;

    private int categoryOptionComboId;

    private int attributeOptionComboId;

    private boolean followup;

    public FollowupParams()
    {
    }

    public FollowupParams( int dataElementId, int periodId, int organisationUnitId, int categoryOptionComboId, int attributeOptionComboId, boolean followup )
    {
        this.dataElementId = dataElementId;
        this.periodId = periodId;
        this.organisationUnitId = organisationUnitId;
        this.categoryOptionComboId = categoryOptionComboId;
        this.attributeOptionComboId = attributeOptionComboId;
        this.followup = followup;
    }

    public int getDataElementId()
    {
        return dataElementId;
    }

    public void setDataElementId( int dataElementId )
    {
        this.dataElementId = dataElementId;
    }

    public int getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId( int periodId )
    {
        this.periodId = periodId;
    }

    public int getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    public void setOrganisationUnitId( int organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    public int getCategoryOptionComboId()
    {
        return categoryOptionComboId;
    }

    public void setCategoryOptionComboId( int categoryOptionComboId )
    {
        this.categoryOptionComboId = categoryOptionComboId;
    }

    public int getAttributeOptionComboId()
    {
        return attributeOptionComboId;
    }

    public void setAttributeOptionComboId( int attributeOptionComboId )
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
