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
        DataValueAuditQueryParams params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataValue.getDataElement() ) )
            .setPeriods( List.of( dataValue.getPeriod() ) )
            .setOrgUnits( List.of( dataValue.getSource() ) )
            .setCategoryOptionCombo( dataValue.getCategoryOptionCombo() )
            .setAttributeOptionCombo( dataValue.getAttributeOptionCombo() );

        return dataValueAuditStore.getDataValueAudits( params );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValueAudit> getDataValueAudits( DataElement dataElement, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo )
    {
        List<DataValueAudit> dataValueAudits = new ArrayList<>();
        List<DataValueAudit> audits = new ArrayList<>();

        CategoryOptionCombo defaultCoc = categoryOptionComboStore
            .getByName( CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );

        DataValue dataValue = dataValueStore.getDataValue( dataElement, period, organisationUnit,
            categoryOptionCombo, defaultCoc );

        // if for whatever reason the DV is null, we just return a empty list
        if ( dataValue == null )
        {
            return audits;
        }

        DataValueAudit currentDataValueAudit = new DataValueAudit( dataValue, dataValue.getValue(),
            dataValue.getStoredBy(), AuditType.UPDATE );

        // inject current data value into audit list
        dataValueAudits.add( currentDataValueAudit );

        DataValueAuditQueryParams params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElement ) )
            .setPeriods( List.of( period ) )
            .setOrgUnits( List.of( organisationUnit ) )
            .setCategoryOptionCombo( categoryOptionCombo )
            .setAttributeOptionCombo( attributeOptionCombo );

        dataValueAudits.addAll( dataValueAuditStore.getDataValueAudits( params ) );

        for ( int i = 0; i < dataValueAudits.size(); i++ )
        {
            DataValueAudit dva = dataValueAudits.get( i );
            DataValueAudit dataValueAudit = DataValueAudit.from( dva );
            audits.add( dataValueAudit );

            if ( i < dataValueAudits.size() - 1 )
            {
                dataValueAudit.setCreated( dataValueAudits.get( i + 1 ).getCreated() );
                dataValueAudit.setModifiedBy( dataValueAudits.get( i + 1 ).getModifiedBy() );
            }
            else if ( i == dataValueAudits.size() - 1 )
            {
                dataValueAudit.setAuditType( AuditType.CREATE );
                dataValueAudit.setCreated( dataValue.getCreated() );
                dataValueAudit.setModifiedBy( dataValue.getStoredBy() );
            }
        }

        return audits;
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataValueAudit> getDataValueAudits( DataValueAuditQueryParams params )
    {
        return dataValueAuditStore.getDataValueAudits( params );
    }

    @Override
    @Transactional( readOnly = true )
    public int countDataValueAudits( DataValueAuditQueryParams params )
    {
        return dataValueAuditStore.countDataValueAudits( params );
    }
}
