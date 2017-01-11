package org.hisp.dhis.dxf2.common;

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

import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class OrganisationUnitUtils
{
    public static Map<String, OrganisationUnit> getOrganisationUnitMap( Collection<OrganisationUnit> organisationUnits )
    {
        Map<String, OrganisationUnit> organisationUnitMap = new HashMap<>();

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            if ( organisationUnit.getUid() != null )
            {
                organisationUnitMap.put( organisationUnit.getUid(), organisationUnit );
            }

            if ( organisationUnit.getCode() != null )
            {
                organisationUnitMap.put( organisationUnit.getCode(), organisationUnit );
            }

            if ( organisationUnit.getName() != null )
            {
                organisationUnitMap.put( organisationUnit.getName(), organisationUnit );
            }

            if ( organisationUnit.getShortName() != null )
            {
                organisationUnitMap.put( organisationUnit.getShortName(), organisationUnit );
            }
        }

        return organisationUnitMap;
    }

    public static void updateParents( Collection<OrganisationUnit> organisationUnits )
    {
        updateParents( organisationUnits, getOrganisationUnitMap( organisationUnits ) );
    }

    public static void updateParents( Collection<OrganisationUnit> organisationUnits, Map<String, OrganisationUnit> organisationUnitMap )
    {
        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            OrganisationUnit parent = organisationUnit.getParent();

            if ( parent != null )
            {
                if ( parent.getUid() != null )
                {
                    parent = organisationUnitMap.get( parent.getUid() );
                }
                else if ( parent.getCode() != null )
                {
                    parent = organisationUnitMap.get( parent.getCode() );
                }
                else if ( parent.getName() != null )
                {
                    parent = organisationUnitMap.get( parent.getName() );
                }
                else if ( parent.getShortName() != null )
                {
                    parent = organisationUnitMap.get( parent.getShortName() );
                }
            }

            if ( parent != null )
            {
                organisationUnit.setParent( parent );
            }
        }
    }
}
