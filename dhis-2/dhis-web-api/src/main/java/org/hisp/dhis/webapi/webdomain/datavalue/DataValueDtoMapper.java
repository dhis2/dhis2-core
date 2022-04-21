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

import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;

/**
 * Class which provides methods for mapping between domain objcets and DTO
 * objects.
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
        DataValueAuditDto dto = new DataValueAuditDto();
        dto.setDataElement( audit.getDataElement().getUid() );
        dto.setPeriod( audit.getPeriod().getIsoDate() );
        dto.setOrgUnit( audit.getOrganisationUnit().getUid() );
        dto.setCategoryOptionCombo( audit.getCategoryOptionCombo().getUid() );
        dto.setAttributeOptionCombo( audit.getAttributeOptionCombo().getUid() );
        dto.setValue( audit.getValue() );
        dto.setModifiedBy( audit.getModifiedBy() );
        dto.setCreated( audit.getCreated() );
        dto.setAuditType( audit.getAuditType() );
        return dto;
    }

    /**
     * Converts a {@link DataValue} object to a {@link DataValueDto} object.
     *
     * @param audit the {@link DataValue}.
     * @return a {@link DataValueDto}.
     */
    public static DataValueDto toDto( DataValue value )
    {
        DataValueDto dto = new DataValueDto();
        dto.setDataElement( value.getDataElement().getUid() );
        dto.setPeriod( value.getPeriod().getIsoDate() );
        dto.setOrgUnit( value.getSource().getUid() );
        dto.setCategoryOptionCombo( value.getCategoryOptionCombo().getUid() );
        dto.setAttribute( null );
        dto.setValue( value.getValue() );
        dto.setComment( value.getComment() );
        dto.setFollowUp( value.isFollowup() );
        return dto;
    }
}
