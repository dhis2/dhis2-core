package org.hisp.dhis.oust.manager;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 * The selection tree is used for data output and analysis.
 * 
 * @author Torgeir Lorange Ostby
 */
public interface SelectionTreeManager
{
    String ID = SelectionTreeManager.class.getName();

    /**
     * Sets the roots of the selection tree by specifying the roots' parent. The
     * selected OrganisationUnits will be removed. The OrganisationUnit doesn't
     * have to be fetched within the current transaction.
     * 
     * @param units
     *            The root OrganisationUnit parent to set.
     * @throws IllegalArgumentException
     *             if the argument is null
     */
    void setRootOrganisationUnitsParent( OrganisationUnit unit );

    /**
     * Sets the root of the selection tree. The selected OrganisationUnits will
     * be removed. The OrganisationUnit doesn't have to be fetched within the
     * current transaction.
     * 
     * @param units
     *            The root OrganisationUnit to set.
     * @throws IllegalArgumentException
     *             if the argument is null
     */
    void setRootOrganisationUnits( Collection<OrganisationUnit> units );

    /**
     * Returns the root parent of the selection tree. The OrganisationUnit is
     * fetched within the current transaction.
     * 
     * @return the root OrganisationUnit parent
     */
    OrganisationUnit getRootOrganisationUnitsParent();

    /**
     * Returns the roots of the selection tree. The OrganisationUnits are
     * fetched within the current transaction.
     * 
     * @return the root OrganisationUnits
     */
    Collection<OrganisationUnit> getRootOrganisationUnits();
    
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
     * are always in the subtree of the selected root. 
     * 
     * @return the selected OrganisationUnits or an empty collection if no unit
     *         is selected
     */
    Collection<OrganisationUnit> getSelectedOrganisationUnits();
    
    /**
     * Convenience method for getting one selected OrganisationUnit. If multiple
     * OrganisationUnits are selected, this method returns one of them.
     * 
     * @return a selected OrganisationUnit or null if no OrganisationUnit is
     *         selected
     */
    OrganisationUnit getSelectedOrganisationUnit();

    /**
     * Returns the selected OrganisationUnits. The returned OrganisationUnits
     * are always in the subtree of the selected root. The OrganisationUnits
     * are associated with the current session.
     * 
     * @return the selected OrganisationUnits or an empty collection if no unit
     *         is selected
     */
    Collection<OrganisationUnit> getReloadedSelectedOrganisationUnits();

    /**
     * Convenience method for getting one selected OrganisationUnit. If multiple
     * OrganisationUnits are selected, this method returns one of them. The 
     * OrganisationUnits are associated with the current session.
     * 
     * @return a selected OrganisationUnit or null if no OrganisationUnit is
     *         selected
     */
    OrganisationUnit getReloadedSelectedOrganisationUnit();
    
    /**
     * Clears the selection and makes getSelectedOrganisationUnit() return null.
     */
    void clearSelectedOrganisationUnits();

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
