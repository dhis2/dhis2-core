package org.hisp.dhis.datavalue;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.deletion.DeletionHandler;

/**
 * @author Quang Nguyen
 * @version Apr 5, 2010 10:27:08 PM
 */

public class DataValueAuditDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataValueAuditService dataValueAuditService;

    public void setDataValueAuditService( DataValueAuditService dataValueAuditService )
    {
        this.dataValueAuditService = dataValueAuditService;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return DataValueAudit.class.getSimpleName();
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit unit )
    {
        dataValueAuditService.deleteDataValueAuditByOrganisationUnit( unit );
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        dataValueAuditService.deleteDataValueAuditByDataElement( dataElement );
    }

    @Override
    public void deletePeriod( Period period )
    {
        dataValueAuditService.deleteDataValueAuditByPeriod( period );
    }
    
    @Override
    public void deleteDataElementCategoryOptionCombo( DataElementCategoryOptionCombo categoryOptionCombo)
    {
        dataValueAuditService.deleteDataValueAuditByCategoryOptionCombo( categoryOptionCombo );
    }
}
