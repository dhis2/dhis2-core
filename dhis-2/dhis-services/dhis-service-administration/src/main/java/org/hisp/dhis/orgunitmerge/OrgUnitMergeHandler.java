package org.hisp.dhis.orgunitmerge;

import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;

@FunctionalInterface
public interface OrgUnitMergeHandler
{
    void apply( Set<OrganisationUnit> sources, OrganisationUnit target );
}
