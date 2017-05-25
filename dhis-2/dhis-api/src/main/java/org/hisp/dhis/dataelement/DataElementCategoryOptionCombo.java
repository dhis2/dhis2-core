package org.hisp.dhis.dataelement;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Aselefew
 */
@JacksonXmlRootElement( localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementCategoryOptionCombo
    extends BaseDimensionalItemObject implements MetadataObject
{
    public static final String DEFAULT_NAME = "default";

    public static final String DEFAULT_TOSTRING = "(default)";

    /**
     * The category combo.
     */
    private DataElementCategoryCombo categoryCombo;

    /**
     * The category options.
     */
    private Set<DataElementCategoryOption> categoryOptions = new HashSet<>();

    /**
     * Indicates whether to ignore data approval.
     */
    private boolean ignoreApproval;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementCategoryOptionCombo()
    {
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;

        int result = 1;

        result = prime * result + ((categoryCombo == null) ? 0 : categoryCombo.hashCode());
        result = prime * result + ((categoryOptions == null) ? 0 : categoryOptions.hashCode());

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !(object instanceof DataElementCategoryOptionCombo) )
        {
            return false;
        }

        final DataElementCategoryOptionCombo other = (DataElementCategoryOptionCombo) object;

        if ( categoryCombo == null )
        {
            if ( other.categoryCombo != null )
            {
                return false;
            }
        }
        else if ( !categoryCombo.equals( other.categoryCombo ) )
        {
            return false;
        }

        if ( categoryOptions == null )
        {
            if ( other.categoryOptions != null )
            {
                return false;
            }
        }
        else if ( !categoryOptions.equals( other.categoryOptions ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + getId() + "\", " +
            "\"uid\":\"" + getUid() + "\", " +
            "\"code\":\"" + getCode() + "\", " +
            "\"categoryCombo\":" + categoryCombo + ", " +
            "\"categoryOptions\":" + categoryOptions +
            "}";
    }

    // -------------------------------------------------------------------------
    // hashCode and equals based on identifiable object
    // -------------------------------------------------------------------------

    public int hashCodeIdentifiableObject()
    {
        return super.hashCode();
    }

    public boolean equalsIdentifiableObject( Object object )
    {
        return super.equals( object );
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptions.add( dataElementCategoryOption );
        dataElementCategoryOption.getCategoryOptionCombos().add( this );
    }

    public void removeDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptions.remove( dataElementCategoryOption );
        dataElementCategoryOption.getCategoryOptionCombos().remove( this );
    }

    public void removeAllCategoryOptions()
    {
        categoryOptions.clear();
    }

    public boolean isDefault()
    {
        return categoryCombo != null && DEFAULT_NAME.equals( categoryCombo.getName() );
    }

    /**
     * Gets a range of valid dates for this (attribute) cateogry option combo.
     * <p>
     * The earliest valid date is the latest start date (if any) from all the
     * category options associated with this option combo.
     * <p>
     * The latest valid date is the earliest end date (if any) from all the
     * category options associated with this option combo.
     *
     * @return valid date range for this (attribute) category option combo.
     */
    public DateRange getDateRange()
    {
        Date latestStartDate = null;
        Date earliestEndDate = null;

        for ( DataElementCategoryOption option : getCategoryOptions() )
        {
            if ( option.getStartDate() != null && (latestStartDate == null || option.getStartDate().compareTo( latestStartDate ) > 0) )
            {
                latestStartDate = option.getStartDate();
            }

            if ( option.getEndDate() != null && (earliestEndDate == null || option.getStartDate().compareTo( earliestEndDate ) < 0) )
            {
                earliestEndDate = option.getEndDate();
            }
        }

        return new DateRange( latestStartDate, earliestEndDate );
    }

    /**
     * Gets a set of valid organisation units (subtrees) for this (attribute)
     * category option combo, if any.
     * <p>
     * The set of valid organisation units (if any) is the intersection of the
     * sets of valid organisation untis for all the category options associated
     * with this option combo.
     * <p>
     * Note: returns null if there are no organisation unit restrictions (no
     * associated option combos have any organisation unit restrictions), but
     * returns an empty set if associated option combos have organisation unit
     * restrictions and their intersection is empty.
     *
     * @return valid organisation units for this (attribute) category option
     * combo.
     */
    public Set<OrganisationUnit> getOrganisationUnits()
    {
        Set<OrganisationUnit> orgUnits = null;

        for ( DataElementCategoryOption option : getCategoryOptions() )
        {
            if ( !CollectionUtils.isEmpty( option.getOrganisationUnits() ) )
            {
                if ( orgUnits == null )
                {
                    orgUnits = option.getOrganisationUnits();
                }
                else
                {
                    orgUnits = new HashSet<>( orgUnits );
                    orgUnits.retainAll( option.getOrganisationUnits() );
                }
            }
        }

        return orgUnits;
    }

    public Date getLatestStartDate()
    {
        Date latestStartDate = null;

        for ( DataElementCategoryOption co : getCategoryOptions() )
        {
            if ( co.getStartDate() != null )
            {
                latestStartDate = (latestStartDate == null || latestStartDate.before( co.getStartDate() ) ?
                    co.getStartDate() : latestStartDate);
            }
        }

        return latestStartDate;
    }

    public Date getEarliestEndDate()
    {
        Date earliestEndDate = null;

        for ( DataElementCategoryOption co : getCategoryOptions() )
        {
            if ( co.getEndDate() != null )
            {
                earliestEndDate = (earliestEndDate == null || earliestEndDate.after( co.getEndDate() ) ?
                    co.getStartDate() : earliestEndDate);
            }
        }

        return earliestEndDate;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        if ( name != null )
        {
            return name;
        }

        StringBuilder builder = new StringBuilder();

        if ( categoryCombo == null || categoryCombo.getCategories().isEmpty() )
        {
            return uid;
        }

        List<DataElementCategory> categories = categoryCombo.getCategories();

        for ( DataElementCategory category : categories )
        {
            List<DataElementCategoryOption> options = category.getCategoryOptions();

            optionLoop:
            for ( DataElementCategoryOption option : categoryOptions )
            {
                if ( options.contains( option ) )
                {
                    builder.append( option.getDisplayName() ).append( ", " );

                    continue optionLoop;
                }
            }
        }

        builder.delete( Math.max( builder.length() - 2, 0 ), builder.length() );

        return StringUtils.substring( builder.toString(), 0, 255 );
    }

    @Override
    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public String getShortName()
    {
        return getName();
    }

    @Override
    public void setShortName( String shortName )
    {
        // Not supported
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementCategoryOption> getCategoryOptions()
    {
        return categoryOptions;
    }

    public void setCategoryOptions( Set<DataElementCategoryOption> categoryOptions )
    {
        this.categoryOptions = categoryOptions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isIgnoreApproval()
    {
        return ignoreApproval;
    }

    public void setIgnoreApproval( boolean ignoreApproval )
    {
        this.ignoreApproval = ignoreApproval;
    }
}
