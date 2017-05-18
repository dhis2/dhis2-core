package org.hisp.dhis.dataapproval;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;

import java.io.Serializable;
import java.util.Date;

/**
 * Records the approval of DataSet values for a given OrganisationUnit and
 * Period.
 *
 * @author Jim Grace
 */
public class DataApprovalAudit
    implements Serializable
{

    private static final long serialVersionUID = 4209187342531545619L;

    /**
     * Identifies the data approval audit record (required).
     */
    private int id;

    /**
     * The approval level for which this approval is defined.
     */
    private DataApprovalLevel level;

    /**
     * The workflow for the values being approved (required).
     */
    private DataApprovalWorkflow workflow;

    /**
     * The Period of the approval (required).
     */
    private Period period;

    /**
     * The OrganisationUnit of the approval (required).
     */
    private OrganisationUnit organisationUnit;

    /**
     * The attribute category option combo being approved (optional).
     */
    private DataElementCategoryOptionCombo attributeOptionCombo;

    /**
     * Type of data approval action done.
     */
    private DataApprovalAction action;

    /**
     * The Date (including time) when this approval was made (required).
     */
    private Date created;

    /**
     * The User who made this approval (required).
     */
    private User creator;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataApprovalAudit()
    {
    }

    public DataApprovalAudit( DataApproval da, DataApprovalAction action )
    {
        this.id = da.getId();
        this.level = da.getDataApprovalLevel();
        this.workflow = da.getWorkflow();
        this.period = da.getPeriod();
        this.organisationUnit = da.getOrganisationUnit();
        this.attributeOptionCombo = da.getAttributeOptionCombo();
        this.action = action;
        this.created = da.getCreated();
        this.creator = da.getCreator();
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataApprovalLevel getLevel()
    {
        return level;
    }

    public void setLevel( DataApprovalLevel level )
    {
        this.level = level;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataApprovalWorkflow getWorkflow()
    {
        return workflow;
    }

    public void setWorkflow( DataApprovalWorkflow workflow )
    {
        this.workflow = workflow;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( DataElementCategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataApprovalAction getAction()
    {
        return action;
    }

    public void setAction( DataApprovalAction action )
    {
        this.action = action;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
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

        result = prime * result + id;
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((workflow == null) ? 0 : workflow.hashCode());
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((organisationUnit == null) ? 0 : organisationUnit.hashCode());
        result = prime * result + ((attributeOptionCombo == null) ? 0 : attributeOptionCombo.hashCode());
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((creator == null) ? 0 : creator.hashCode());

        return result;
    }

    @Override
    public String toString()
    {
        return "DataApproval{" +
            "id=" + id +
            ", level=" + (level == null ? "(null)" : level.getLevel()) +
            ", workflow='" + (workflow == null ? "(null)" : workflow.getName()) + "'" +
            ", period=" + (period == null ? "(null)" : period.getName()) +
            ", organisationUnit='" + (organisationUnit == null ? "(null)" : organisationUnit.getName()) + "'" +
            ", attributeOptionCombo='" + (attributeOptionCombo == null ? "(null)" : attributeOptionCombo.getName()) + "'" +
            ", action=" + action +
            ", created=" + created +
            ", creator=" + (creator == null ? "(null)" : creator.getName()) +
            '}';
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null || !(object instanceof DataApprovalAudit ) )
        {
            return false;
        }

        DataApprovalAudit that = (DataApprovalAudit) object;

        if ( id != that.id )
        {
            return false;
        }
        else if ( level != null )
        {
            if ( !level.equals( that.getLevel() ) )
            {
                return false;
            }
        }
        else if ( that.getLevel() != null )
        {
            return false;
        }

        if ( workflow != null )
        {
            if ( !workflow.equals( that.getWorkflow() ) )
            {
                return false;
            }
        }
        else if ( that.getWorkflow() != null )
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

        if ( action != null )
        {
            if ( !action.equals( that.getAction() ) )
            {
                return false;
            }
        }
        else if ( that.getAction() != null )
        {
            return false;
        }

        if ( created != null )
        {
            if ( !created.equals( that.getCreated() ) )
            {
                return false;
            }
        }
        else if ( that.getCreated() != null )
        {
            return false;
        }

        if ( creator != null )
        {
            if ( !creator.equals( that.getCreator() ) )
            {
                return false;
            }
        }
        else if ( that.getCreator() != null )
        {
            return false;
        }

        return true;
    }
}
