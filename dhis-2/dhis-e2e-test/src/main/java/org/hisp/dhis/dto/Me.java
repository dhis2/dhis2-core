package org.hisp.dhis.dto;



import java.util.List;

public class Me
{
    private List<UserGroup> userGroups;

    private List<OrgUnit> teiSearchOrganisationUnits;

    private List<OrgUnit> organisationUnits;

    private List<String> authorities;

    public List<UserGroup> getUserGroups()
    {
        return userGroups;
    }

    public void setUserGroups( List<UserGroup> userGroups )
    {
        this.userGroups = userGroups;
    }

    public List<OrgUnit> getTeiSearchOrganisationUnits()
    {
        return teiSearchOrganisationUnits;
    }

    public void setTeiSearchOrganisationUnits( List<OrgUnit> teiSearchOrganisationUnits )
    {
        this.teiSearchOrganisationUnits = teiSearchOrganisationUnits;
    }

    public List<OrgUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( List<OrgUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    public List<String> getAuthorities()
    {
        return authorities;
    }

    public void setAuthorities( List<String> authorities )
    {
        this.authorities = authorities;
    }
}
