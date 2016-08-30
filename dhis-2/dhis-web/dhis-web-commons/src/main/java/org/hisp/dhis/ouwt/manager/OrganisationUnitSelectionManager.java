package org.hisp.dhis.ouwt.manager;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Collection;

import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * The web tree is used for data input and data capture.
 * 
 * @author Torgeir Lorange Ostby
 */
public interface OrganisationUnitSelectionManager
{
    String ID = OrganisationUnitSelectionManager.class.getName();

    /**
     * Sets a single root of the selection tree. Any selected organisation units
     * will be unselected. The OrganisationUnit doesn't have to be fetched
     * within the current transaction.
     * 
     * @param unit
     *            The root OrganisationUnit to set.
     * @throws IllegalArgumentException
     *             if the argument is null
     */
    void setRootOrganisationUnits( Collection<OrganisationUnit> units );

    /**
     * Sets the roots of the selection tree by specifying the roots' parent. Any
     * selected organisation units will be unselected. The OrganisationUnit
     * doesn't have to be fetched within the current transaction.
     * 
     * @param units
     *            The root OrganisationUnit parent to set.
     * @throws IllegalArgumentException
     *             if the argument is null
     */
    void setRootOrganisationUnitsParent( OrganisationUnit unit );

    /**
     * Returns the roots of the selection tree. The OrganisationUnit is fetched
     * within the current transaction.
     * 
     * @return the root OrganisationUnits
     */
    Collection<OrganisationUnit> getRootOrganisationUnits();

    /**
     * Returns the roots' parent of the selection tree. The OrganisationUnit is
     * fetched within the current transaction.
     * 
     * @return the root OrganisationUnits parent
     */
    OrganisationUnit getRootOrganisationUnitsParent();

    /**
     * Resets the selection tree to use the actual root of the OrganisationUnit
     * tree.
     */
    void resetRootOrganisationUnits();

    /**
     * Sets the selected OrganisationUnits. The OrganisationUnits don't have to
     * be fetched within the current transaction.
     * 
     * @param units
     *            the selected OrganisationUnits to set
     * @throws IllegalArgumentException
     *             if the argument is null
     */
    void setSelectedOrganisationUnits( Collection<OrganisationUnit> units );

    /**
     * Returns the selected OrganisationUnits. The returned OrganisationUnits
     * are always in the subtree of the selected root. The OrganisationUnits are
     * fetched within the current transaction.
     * 
     * @return the selected OrganisationUnits or an empty collection if no unit
     *         is selected
     */
    Collection<OrganisationUnit> getSelectedOrganisationUnits();

    /**
     * Clears the selection and makes getSelectedOrganisationUnit() return null.
     */
    void clearSelectedOrganisationUnits();

    /**
     * Convenience method for getting one selected OrganisationUnit. If multiple
     * OrganisationUnits are selected, this method returns one of them.
     * 
     * @return a selected OrganisationUnit or null if no OrganisationUnit is
     *         selected
     */
    OrganisationUnit getSelectedOrganisationUnit();

    /**
     * Convenience method for setting one selected OrganisationUnit.
     * 
     * @param unit
     *            the OrganisationUnit to set
     * @throws IllegalArgumentException
     *             if the argument is null
     */
    void setSelectedOrganisationUnit( OrganisationUnit unit );
}
