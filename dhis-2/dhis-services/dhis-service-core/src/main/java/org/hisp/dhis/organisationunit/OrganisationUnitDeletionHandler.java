package org.hisp.dhis.organisationunit;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitDeletionHandler
    extends DeletionHandler
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;
    
    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return OrganisationUnit.class.getSimpleName();
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        for ( OrganisationUnit unit : dataSet.getSources() )
        {
            unit.getDataSets().remove( dataSet );
            idObjectManager.updateNoAcl( unit );
        }
    }

    @Override
    public void deleteUser( User user )
    {
        for ( OrganisationUnit unit : user.getOrganisationUnits() )
        {
            unit.getUsers().remove( user );
            idObjectManager.updateNoAcl( unit );
        }
    }

    @Override
    public void deleteOrganisationUnitGroup( OrganisationUnitGroup group )
    {
        for ( OrganisationUnit unit : group.getMembers() )
        {
            unit.getGroups().remove( group );
            idObjectManager.updateNoAcl( unit );
        }
    }

    @Override
    public String allowDeleteOrganisationUnit( OrganisationUnit unit )
    {
        return unit.getChildren().isEmpty() ? null : ERROR;
    }
}
