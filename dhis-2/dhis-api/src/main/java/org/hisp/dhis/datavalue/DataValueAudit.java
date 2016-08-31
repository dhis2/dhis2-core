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

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Date;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */

public class DataValueAudit
{
    private int id;

    private String value;

    private String modifiedBy;

    private Date timestamp;

    private DataElement dataElement;

    private Period period;

    private OrganisationUnit organisationUnit;

    private DataElementCategoryOptionCombo categoryOptionCombo;
    
    private DataElementCategoryOptionCombo attributeOptionCombo;

    private AuditType auditType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataValueAudit()
    {
    }

    public DataValueAudit( DataValue dataValue, String value, String modifiedBy, Date timestamp, AuditType auditType )
    {
        this.dataElement = dataValue.getDataElement();
        this.period = dataValue.getPeriod();
        this.organisationUnit = dataValue.getSource();
        this.categoryOptionCombo = dataValue.getCategoryOptionCombo();
        this.attributeOptionCombo = dataValue.getAttributeOptionCombo();

        this.value = value;
        this.modifiedBy = modifiedBy;
        this.timestamp = timestamp;
        this.auditType = auditType;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getModifiedBy()
    {
        return modifiedBy;
    }

    public void setModifiedBy( String modifiedBy )
    {
        this.modifiedBy = modifiedBy;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( Date timestamp )
    {
        this.timestamp = timestamp;
    }

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    public DataElementCategoryOptionCombo getCategoryOptionCombo()
    {
        return categoryOptionCombo;
    }

    public void setCategoryOptionCombo( DataElementCategoryOptionCombo categoryOptionCombo )
    {
        this.categoryOptionCombo = categoryOptionCombo;
    }

    public DataElementCategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( DataElementCategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    public AuditType getAuditType()
    {
        return auditType;
    }

    public void setAuditType( AuditType auditType )
    {
        this.auditType = auditType;
    }
}
