package org.hisp.dhis.orgunitmerge;

import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;

public interface OrgUnitMergeService
{
    void merge( Set<OrganisationUnit> sources, OrganisationUnit target );
}
