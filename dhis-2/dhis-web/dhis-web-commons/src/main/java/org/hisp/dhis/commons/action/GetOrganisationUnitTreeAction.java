package org.hisp.dhis.commons.action;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.version.Version;
import org.hisp.dhis.version.VersionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class GetOrganisationUnitTreeAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private VersionService versionService;

    public void setVersionService( VersionService versionService )
    {
        this.versionService = versionService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private List<OrganisationUnit> organisationUnits = new ArrayList<>();

    public List<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    private List<OrganisationUnit> rootOrganisationUnits = new ArrayList<>();

    public List<OrganisationUnit> getRootOrganisationUnits()
    {
        return rootOrganisationUnits;
    }

    private String version;

    public String getVersion()
    {
        return version;
    }

    private String username;

    public String getUsername()
    {
        return username;
    }

    private boolean versionOnly;

    public void setVersionOnly( Boolean versionOnly )
    {
        this.versionOnly = versionOnly;
    }

    public Boolean getVersionOnly()
    {
        return versionOnly;
    }

    private String parentId;

    public void setParentId( String parentId )
    {
        this.parentId = parentId;
    }

    private String leafId;

    public void setLeafId( String leafId )
    {
        this.leafId = leafId;
    }

    private String byName;

    public void setByName( String byName )
    {
        this.byName = byName;
    }

    private boolean realRoot;

    public boolean isRealRoot()
    {
        return realRoot;
    }

    private Integer offlineLevel;

    public void setOfflineLevel( Integer offlineLevel )
    {
        this.offlineLevel = offlineLevel;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        version = getVersionString();

        username = currentUserService.getCurrentUsername();

        User user = currentUserService.getCurrentUser();

        if ( user != null && user.hasOrganisationUnit() )
        {
            rootOrganisationUnits = new ArrayList<>( user.getOrganisationUnits() );
        }
        else if ( currentUserService.currentUserIsSuper() || user == null )
        {
            rootOrganisationUnits = new ArrayList<>( organisationUnitService.getRootOrganisationUnits() );
        }

        if ( byName != null )
        {
            List<OrganisationUnit> organisationUnitByName = organisationUnitService.getOrganisationUnitByName( byName );

            if ( !organisationUnitByName.isEmpty() )
            {
                OrganisationUnit child = organisationUnitByName.get( 0 );
                organisationUnits.add( child );
                OrganisationUnit parent = child.getParent();

                if ( parent != null )
                {
                    do
                    {
                        organisationUnits.add( parent );
                        organisationUnits.addAll( parent.getChildren() );
                    }
                    while ( (parent = parent.getParent()) != null );
                }

                return "partial";
            }
        }

        if ( leafId != null )
        {
            OrganisationUnit leaf = organisationUnitService.getOrganisationUnit( leafId );

            if ( leaf != null )
            {
                organisationUnits.add( leaf );
                organisationUnits.addAll( leaf.getChildren() );

                for ( OrganisationUnit organisationUnit : leaf.getAncestors() )
                {
                    organisationUnits.add( organisationUnit );
                    organisationUnits.addAll( organisationUnit.getChildren() );
                }
            }

            return "partial";
        }

        if ( parentId != null )
        {
            OrganisationUnit parent = organisationUnitService.getOrganisationUnit( parentId );

            if ( parent != null )
            {
                organisationUnits.addAll( parent.getChildren() );
            }

            return "partial";
        }

        if ( !versionOnly && !rootOrganisationUnits.isEmpty() )
        {
            Integer offlineLevels = getOfflineOrganisationUnitLevels();

            for ( OrganisationUnit unit : rootOrganisationUnits )
            {
                organisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( unit.getId(), offlineLevels ) );
            }
        }

        Collection<?> intersection = org.apache.commons.collections.CollectionUtils.intersection(
            organisationUnitService.getRootOrganisationUnits(), rootOrganisationUnits );

        if ( intersection.size() > 0 )
        {
            realRoot = true;
        }

        Collections.sort( rootOrganisationUnits );

        return SUCCESS;
    }

    private String getVersionString()
    {
        Version orgUnitVersion = versionService.getVersionByKey( VersionService.ORGANISATIONUNIT_VERSION );

        if ( orgUnitVersion == null )
        {
            String uuid = UUID.randomUUID().toString();
            orgUnitVersion = new Version();
            orgUnitVersion.setKey( VersionService.ORGANISATIONUNIT_VERSION );
            orgUnitVersion.setValue( uuid );
            versionService.addVersion( orgUnitVersion );
        }

        return orgUnitVersion.getValue();
    }

    /**
     * Returns the number of org unit levels to cache offline based on the given
     * org unit level argument, next the org unit level from the user org unit,
     * next the level from the configuration.
     */
    private Integer getOfflineOrganisationUnitLevels()
    {
        List<OrganisationUnitLevel> orgUnitLevels = organisationUnitService.getOrganisationUnitLevels();

        if ( orgUnitLevels == null || orgUnitLevels.isEmpty() )
        {
            return null;
        }

        Integer level = offlineLevel != null ? offlineLevel : organisationUnitService.getOfflineOrganisationUnitLevels();

        return level == 1 ? 2 : level;
    }
}
