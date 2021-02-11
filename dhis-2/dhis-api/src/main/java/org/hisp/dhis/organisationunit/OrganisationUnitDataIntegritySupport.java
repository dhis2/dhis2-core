package org.hisp.dhis.organisationunit;

import java.util.List;
import java.util.Set;

/**
 * A number of data integrity tests are solely related to
 * {@link OrganisationUnit}s. They are contained in this interface to avoid
 * duplication.
 */
public interface OrganisationUnitDataIntegritySupport
{

    /**
     * Gets all organisation units which are related to each other in a cyclic
     * reference.
     */
    Set<OrganisationUnit> getOrganisationUnitsWithCyclicReferences();

    /**
     * Gets all organisation units with no parents or children.
     */
    List<OrganisationUnit> getOrphanedOrganisationUnits();

    /**
     * Returns all OrganisationUnits which are not a member of any
     * OrganisationUnitGroups.
     *
     * @return all OrganisationUnits which are not a member of any
     *         OrganisationUnitGroups.
     */
    List<OrganisationUnit> getOrganisationUnitsWithoutGroups();
}
