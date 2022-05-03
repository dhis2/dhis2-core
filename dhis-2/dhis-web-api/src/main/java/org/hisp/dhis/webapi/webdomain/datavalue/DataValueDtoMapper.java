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
package org.hisp.dhis.webapi.webdomain.datavalue;

import static org.hisp.dhis.commons.collection.CollectionUtils.mapToSet;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.minmax.MinMaxDataElement;

/**
 * Class which provides methods for mapping between domain objcets and DTOs.
 *
 * @author Lars Helge Overland
 */
public class DataValueDtoMapper
{
    /**
     * Converts a {@link DataValueAudit} object to a {@link DataValueAuditDto}
     * object.
     *
     * @param audit the {@link DataValueAudit}.
     * @return a {@link DataValueAuditDto}.
     */
    public static DataValueAuditDto toDto( DataValueAudit audit )
    {
        return new DataValueAuditDto()
            .setDataElement( audit.getDataElement().getUid() )
            .setPeriod( audit.getPeriod().getIsoDate() )
            .setOrgUnit( audit.getOrganisationUnit().getUid() )
            .setCategoryOptionCombo( audit.getCategoryOptionCombo().getUid() )
            .setAttributeOptionCombo( audit.getAttributeOptionCombo().getUid() )
            .setValue( audit.getValue() )
            .setModifiedBy( audit.getModifiedBy() )
            .setCreated( audit.getCreated() )
            .setAuditType( audit.getAuditType() );
    }

    /**
     * Converts a {@link DataValue} object to a {@link DataValueDto} object.
     *
     * @param value the {@link DataValue}.
     * @return a {@link DataValueDto}.
     */
    public static DataValueDto toDto( DataValue value )
    {
        return new DataValueDto()
            .setDataElement( value.getDataElement().getUid() )
            .setPeriod( value.getPeriod().getIsoDate() )
            .setOrgUnit( value.getSource().getUid() )
            .setCategoryOptionCombo( value.getCategoryOptionCombo().getUid() )
            .setAttribute( toDto( value.getAttributeOptionCombo() ) )
            .setValue( value.getValue() )
            .setComment( value.getComment() )
            .setFollowUp( value.isFollowup() );
    }

    /**
     * Converts an attribute {@link CategoryOptionCombo} object to a
     * {@link DataValueCategoryDto} object.
     *
     * @param attribute the attribute {@link CategoryOptionCombo}.
     * @return a {@link DataValueCategoryDto}.
     */
    public static DataValueCategoryDto toDto( CategoryOptionCombo attribute )
    {
        return new DataValueCategoryDto()
            .setCombo( attribute.getCategoryCombo().getUid() )
            .setOptions( mapToSet( attribute.getCategoryOptions(), CategoryOption::getUid ) );
    }

    /**
     * Converts a {@link MinMaxDataElement} object to a {@link MinMaxValueDto}
     * object.
     *
     * @param attribute the {@link MinMaxDataElement}.
     * @return a {@link MinMaxValueDto}.
     */
    public static MinMaxValueDto toDto( MinMaxDataElement value )
    {
        return new MinMaxValueDto()
            .setDataElement( value.getDataElement().getUid() )
            .setOrgUnit( value.getSource().getUid() )
            .setCategoryOptionCombo( value.getOptionCombo().getUid() )
            .setMinValue( value.getMin() )
            .setMaxValue( value.getMax() );
    }
}
