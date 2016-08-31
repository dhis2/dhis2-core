package org.hisp.dhis.dataapproval;

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

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;

/**
 * Records the approval of DataSet values for a given OrganisationUnit and
 * Period.
 * 
 * @author Jim Grace
 */
public class DataApproval
    implements Serializable
{
    public static final String AUTH_APPROVE = "F_APPROVE_DATA";
    public static final String AUTH_APPROVE_LOWER_LEVELS = "F_APPROVE_DATA_LOWER_LEVELS";
    public static final String AUTH_ACCEPT_LOWER_LEVELS = "F_ACCEPT_DATA_LOWER_LEVELS";
    public static final String AUTH_VIEW_UNAPPROVED_DATA = "F_VIEW_UNAPPROVED_DATA";

    private static final long serialVersionUID = -4034531921928532366L;

    /**
     * Identifies the data approval instance (required).
     */
    private int id;

    /**
     * The approval level for which this approval is defined.
     */
    private DataApprovalLevel dataApprovalLevel;

    /**
     * The DataSet for the values being approved (required).
     */
    private DataSet dataSet;

    /**
     * The Period of the DataSet values being approved (required).
     */
    private Period period;

    /**
     * The OrganisationUnit of the DataSet values being approved (required).
     */
    private OrganisationUnit organisationUnit;
    
    /**
     * The attribute category option combo being approved (optional).
     */
    private DataElementCategoryOptionCombo attributeOptionCombo;

    /**
     * Whether the approval has been accepted (optional, usually by another
     * user.)
     */
    private boolean accepted;

    /**
     * The Date (including time) when the DataSet values were approved
     * (required).
     */
    private Date created;

    /**
     * The User who approved the DataSet values (required).
     */
    private User creator;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataApproval()
    {
    }

    public DataApproval( DataApprovalLevel dataApprovalLevel, DataSet dataSet,
                         Period period, OrganisationUnit organisationUnit,
                         DataElementCategoryOptionCombo attributeOptionCombo,
                         boolean accepted, Date created, User creator )
    {
        this.dataApprovalLevel = dataApprovalLevel;
        this.dataSet = dataSet;
        this.period = period;
        this.organisationUnit = organisationUnit;
        this.attributeOptionCombo = attributeOptionCombo;
        this.accepted = accepted;
        this.created = created;
        this.creator = creator;
    }

    public DataApproval( DataApproval da )
    {
        this.dataApprovalLevel = da.dataApprovalLevel;
        this.dataSet = da.dataSet;
        this.period = da.period;
        this.organisationUnit = da.organisationUnit;
        this.attributeOptionCombo = da.attributeOptionCombo;
        this.accepted = da.accepted;
        this.created = da.created;
        this.creator = da.creator;
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

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataApprovalLevel getDataApprovalLevel()
    {
        return dataApprovalLevel;
    }

    public void setDataApprovalLevel( DataApprovalLevel dataApprovalLevel )
    {
        this.dataApprovalLevel = dataApprovalLevel;
    }

    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
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

    public DataElementCategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( DataElementCategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public void setAccepted( boolean accepted )
    {
        this.accepted = accepted;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    public User getCreator()
    {
        return creator;
    }

    public void setCreator( User creator )
    {
        this.creator = creator;
    }

    // ----------------------------------------------------------------------
    // hashCode, equals, toString
    // ----------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;

        int result = 1;

        result = prime * result + ((dataApprovalLevel == null) ? 0 : dataApprovalLevel.hashCode());
        result = prime * result + ((dataSet == null) ? 0 : dataSet.hashCode());
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((organisationUnit == null) ? 0 : organisationUnit.hashCode());
        result = prime * result + ((attributeOptionCombo == null) ? 0 : attributeOptionCombo.hashCode());

        return result;
    }

    @Override
    public String toString()
    {
        return "DataApproval{" +
                "id=" + id +
                ", dataApprovalLevel=" + ( dataApprovalLevel == null ? "(null)" : dataApprovalLevel.getLevel() ) +
                ", dataSet='" + ( dataSet == null ? "(null)" : dataSet.getName() ) + "'" +
                ", period=" + ( period == null ? "(null)" : period.getName() ) +
                ", organisationUnit='" + ( organisationUnit == null ? "(null)" : organisationUnit.getName() ) + "'" +
                ", attributeOptionCombo='" + ( attributeOptionCombo == null ? "(null)" : attributeOptionCombo.getName() ) + "'" +
                ", accepted=" + accepted +
                ", created=" + created +
                ", creator=" + ( creator == null ? "(null)" : creator.getName() ) +
                '}';
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null || !( object instanceof DataApproval ) )
        {
            return false;
        }

        DataApproval that = (DataApproval) object;

        if ( dataApprovalLevel != null )
        {
            if ( !dataApprovalLevel.equals( that.getDataApprovalLevel() ) )
            {
                return false;
            }
        }
        else if ( that.getDataApprovalLevel() != null )
        {
            return false;
        }

        if ( dataSet != null )
        {
            if ( !dataSet.equals( that.getDataSet() ) )
            {
                return false;
            }
        }
        else if ( that.getDataSet() != null )
        {
            return false;
        }

        if ( period != null )
        {
            if ( !period.equals( that.getPeriod() ) )
            {
                return false;
            }
        }
        else if ( that.getPeriod() != null )
        {
            return false;
        }

        if ( organisationUnit != null )
        {
            if ( !organisationUnit.equals( that.getOrganisationUnit() ) )
            {
                return false;
            }
        }
        else if ( that.getOrganisationUnit() != null )
        {
            return false;
        }

        if ( attributeOptionCombo != null )
        {
            if ( !attributeOptionCombo.equals( that.getAttributeOptionCombo() ) )
            {
                return false;
            }
        }
        else if ( that.getAttributeOptionCombo() != null )
        {
            return false;
        }

        return true;
    }
}
