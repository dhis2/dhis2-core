package org.hisp.dhis.orgunitdistribution;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;

public class OrgUnitDistributionParams
{
    private List<OrganisationUnit> orgUnits = new ArrayList<>();

    private List<OrganisationUnitGroupSet> orgUnitGroupSets = new ArrayList<>();

    public List<OrganisationUnit> getOrgUnits()
    {
        return orgUnits;
    }

    public int getOrgUnitLevel()
    {
        return !orgUnits.isEmpty() ? orgUnits.get( 0 ).getLevel() : 1; //TODO implement properly
    }

    public OrgUnitDistributionParams setOrgUnits( List<OrganisationUnit> orgUnits )
    {
        this.orgUnits = orgUnits;
        return this;
    }

    public List<OrganisationUnitGroupSet> getOrgUnitGroupSets()
    {
        return orgUnitGroupSets;
    }

    public OrgUnitDistributionParams setOrgUnitGroupSets( List<OrganisationUnitGroupSet> orgUnitGroupSets )
    {
        this.orgUnitGroupSets = orgUnitGroupSets;
        return this;
    }
}
