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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;

import com.opensymphony.xwork2.ActionContext;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: DefaultTreeStateManager.java 5282 2008-05-28 10:41:06Z larshelg $
 */
public class DefaultTreeStateManager
    implements TreeStateManager
{
    private static final String SESSION_KEY_TREE_STATE = "dhis-ouwt-tree-state";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private boolean collapseClosesAllSubtrees;

    public void setCollapseClosesAllSubtrees( boolean collapseClosesAllSubtrees )
    {
        this.collapseClosesAllSubtrees = collapseClosesAllSubtrees;
    }

    // -------------------------------------------------------------------------
    // TreeStateManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void setSubtreeExpanded( OrganisationUnit unit )
    {
        getTreeState().add( unit.getId() );
    }

    @Override
    public Collection<OrganisationUnit> setSubtreeCollapsed( OrganisationUnit unit )
    {
        if ( collapseClosesAllSubtrees )
        {
            return closeAllSubtrees( unit );
        }
        else
        {
            getTreeState().remove( unit.getId() );

            Set<OrganisationUnit> collapsedUnits = new HashSet<>( 1 );
            collapsedUnits.add( unit );
            return collapsedUnits;
        }
    }

    @Override
    public boolean isSubtreeExpanded( OrganisationUnit unit )
    {
        return getTreeState().contains( unit.getId() );
    }

    @Override
    public void clearTreeState()
    {
        getTreeState().clear();
    }

    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------

    private Collection<OrganisationUnit> closeAllSubtrees( OrganisationUnit parentUnit )
    {
        Collection<OrganisationUnit> units = organisationUnitService.getOrganisationUnitWithChildren( parentUnit
            .getId() );

        Set<OrganisationUnit> collapsedUnits = new HashSet<>();

        Set<Integer> treeState = getTreeState();

        for ( OrganisationUnit unit : units )
        {
            if ( treeState.contains( unit.getId() ) )
            {
                treeState.remove( unit.getId() );
                collapsedUnits.add( unit );
            }
        }

        return collapsedUnits;
    }

    @SuppressWarnings( "unchecked" )
    private Set<Integer> getTreeState()
    {
        Set<Integer> treeState = (Set<Integer>) getSession().get( SESSION_KEY_TREE_STATE );

        if ( treeState == null )
        {
            treeState = new HashSet<>();

            getSession().put( SESSION_KEY_TREE_STATE, treeState );
        }

        return treeState;
    }

    protected Map<String, Object> getSession()
    {
        return ActionContext.getContext().getSession();
    }
}
