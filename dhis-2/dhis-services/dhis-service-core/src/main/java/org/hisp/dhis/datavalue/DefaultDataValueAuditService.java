/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.datavalue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@Service( "org.hisp.dhis.datavalue.DataValueAuditService" )
public class DefaultDataValueAuditService
    implements DataValueAuditService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final DataValueAuditStore dataValueAuditStore;

    private final DataValueStore dataValueStore;

    private final CategoryOptionComboStore categoryOptionComboStore;

    public DefaultDataValueAuditService( DataValueAuditStore dataValueAuditStore, DataValueStore dataValueStore,
        CategoryOptionComboStore categoryOptionComboStore )
    {
        checkNotNull( dataValueAuditStore );
        checkNotNull( dataValueStore );
        checkNotNull( categoryOptionComboStore );

        this.dataValueAuditStore = dataValueAuditStore;
        this.dataValueStore = dataValueStore;
        this.categoryOptionComboStore = categoryOptionComboStore;
    }

    // -------------------------------------------------------------------------
    // DataValueAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void addDataValueAudit( DataValueAudit dataValueAudit )
    {
        dataValueAuditStore.addDataValueAudit( dataValueAudit );
    }

    @Override
    @Transactional
    public void deleteDataValueAudits( OrganisationUnit organisationUnit )
    {
        dataValueAuditStore.deleteDataValueAudits( organisationUnit );
    }

    @Override
    @Transactional
    public void deleteDataValueAudits( DataElement dataElement )
    {
        dataValueAuditStore.deleteDataValueAudits( dataElement );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValueAudit> getDataValueAudits( DataValue dataValue )
    {
        return dataValueAuditStore.getDataValueAudits( dataValue );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValueAudit> getDataValueAudits( DataElement dataElement, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo )
    {
        List<DataValueAudit> audits = new ArrayList<>();

        CategoryOptionCombo defaultCoc = categoryOptionComboStore
            .getByName( CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );

        DataValue dataValue = dataValueStore.getDataValue( dataElement, period, organisationUnit,
            categoryOptionCombo, defaultCoc );

        if ( dataValue != null )
        {
            DataValueAudit dataValueAudit = new DataValueAudit( dataValue, dataValue.getValue(),
                dataValue.getStoredBy(), AuditType.UPDATE );
            audits.add( dataValueAudit );
        }

        audits.addAll( dataValueAuditStore.getDataValueAudits( List.of( dataElement ), List.of( period ),
            List.of( organisationUnit ), categoryOptionCombo, attributeOptionCombo, null ) );

        if ( !audits.isEmpty() )
        {
            audits.get( audits.size() - 1 ).setAuditType( AuditType.CREATE );
        }

        return audits;
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods,
        List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        return dataValueAuditStore.getDataValueAudits( dataElements, periods, organisationUnits, categoryOptionCombo,
            attributeOptionCombo, auditType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods,
        List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType, int first, int max )
    {
        return dataValueAuditStore.getDataValueAudits( dataElements, periods, organisationUnits, categoryOptionCombo,
            attributeOptionCombo, auditType, first, max );
    }

    @Override
    @Transactional( readOnly = true )
    public int countDataValueAudits( List<DataElement> dataElements, List<Period> periods,
        List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, AuditType auditType )
    {
        return dataValueAuditStore.countDataValueAudits( dataElements, periods, organisationUnits, categoryOptionCombo,
            attributeOptionCombo, auditType );
    }
}
