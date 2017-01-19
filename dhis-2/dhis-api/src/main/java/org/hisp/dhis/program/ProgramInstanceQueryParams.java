package org.hisp.dhis.program;

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

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ProgramInstanceQueryParams
{
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * Last updated for PI.
     */
    private Date lastUpdated;

    /**
     * Organisation units for which instances in the response were registered at.
     * Is related to the specified OrganisationUnitMode.
     */
    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    /**
     * Selection mode for the specified organisation units.
     */
    private OrganisationUnitSelectionMode organisationUnitMode;

    /**
     * Program for which instances in the response must be enrolled in.
     */
    private Program program;

    /**
     * Status of the tracked entity instance in the given program.
     */
    private ProgramStatus programStatus;

    /**
     * Indicates whether tracked entity instance is marked for follow up for the
     * specified program.
     */
    private Boolean followUp;

    /**
     * Start date for enrollment in the given program.
     */
    private Date programStartDate;

    /**
     * End date for enrollment in the given program.
     */
    private Date programEndDate;

    /**
     * Tracked entity of the instances in the response.
     */
    private TrackedEntity trackedEntity;

    /**
     * Tracked entity instance.
     */
    private TrackedEntityInstance trackedEntityInstance;

    /**
     * Page number.
     */
    private Integer page;

    /**
     * Page size.
     */
    private Integer pageSize;

    /**
     * Indicates whether to include the total number of pages in the paging response.
     */
    private boolean totalPages;

    /**
     * Indicates whether paging should be skipped.
     */
    private boolean skipPaging;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramInstanceQueryParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Adds an organisation unit to the parameters.
     */
    public void addOrganisationUnit( OrganisationUnit unit )
    {
        this.organisationUnits.add( unit );
    }

    /**
     * Indicates whether this params specifies last updated.
     */
    public boolean hasLastUpdated()
    {
        return lastUpdated != null;
    }

    /**
     * Indicates whether this params specifies any organisation units.
     */
    public boolean hasOrganisationUnits()
    {
        return organisationUnits != null && !organisationUnits.isEmpty();
    }

    /**
     * Indicates whether this params specifies a program.
     */
    public boolean hasProgram()
    {
        return program != null;
    }

    /**
     * Indicates whether this params specifies a program status.
     */
    public boolean hasProgramStatus()
    {
        return programStatus != null;
    }

    /**
     * Indicates whether this params specifies follow up for the given program.
     * Follow up can be specified as true or false.
     */
    public boolean hasFollowUp()
    {
        return followUp != null;
    }

    /**
     * Indicates whether this params specifies a program start date.
     */
    public boolean hasProgramStartDate()
    {
        return programStartDate != null;
    }

    /**
     * Indicates whether this params specifies a program end date.
     */
    public boolean hasProgramEndDate()
    {
        return programEndDate != null;
    }

    /**
     * Indicates whether this params specifies a tracked entity.
     */
    public boolean hasTrackedEntity()
    {
        return trackedEntity != null;
    }

    /**
     * Indicates whether this params specifies a tracked entity instance.
     */
    public boolean hasTrackedEntityInstance()
    {
        return trackedEntityInstance != null;
    }

    /**
     * Indicates whether this params is of the given organisation unit mode.
     */
    public boolean isOrganisationUnitMode( OrganisationUnitSelectionMode mode )
    {
        return organisationUnitMode != null && organisationUnitMode.equals( mode );
    }

    /**
     * Indicates whether paging is enabled.
     */
    public boolean isPaging()
    {
        return page != null || pageSize != null;
    }

    /**
     * Returns the page number, falls back to default value of 1 if not specified.
     */
    public int getPageWithDefault()
    {
        return page != null && page > 0 ? page : DEFAULT_PAGE;
    }

    /**
     * Returns the page size, falls back to default value of 50 if not specified.
     */
    public int getPageSizeWithDefault()
    {
        return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Returns the offset based on the page number and page size.
     */
    public int getOffset()
    {
        return (getPageWithDefault() - 1) * getPageSizeWithDefault();
    }

    /**
     * Sets paging properties to default values.
     */
    public void setDefaultPaging()
    {
        this.page = DEFAULT_PAGE;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.skipPaging = false;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public ProgramInstanceQueryParams setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public ProgramInstanceQueryParams setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
        return this;
    }

    public OrganisationUnitSelectionMode getOrganisationUnitMode()
    {
        return organisationUnitMode;
    }

    public ProgramInstanceQueryParams setOrganisationUnitMode( OrganisationUnitSelectionMode organisationUnitMode )
    {
        this.organisationUnitMode = organisationUnitMode;
        return this;
    }

    public Program getProgram()
    {
        return program;
    }

    public ProgramInstanceQueryParams setProgram( Program program )
    {
        this.program = program;
        return this;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public ProgramInstanceQueryParams setProgramStatus( ProgramStatus programStatus )
    {
        this.programStatus = programStatus;
        return this;
    }

    public Boolean getFollowUp()
    {
        return followUp;
    }

    public ProgramInstanceQueryParams setFollowUp( Boolean followUp )
    {
        this.followUp = followUp;
        return this;
    }

    public Date getProgramStartDate()
    {
        return programStartDate;
    }

    public ProgramInstanceQueryParams setProgramStartDate( Date programStartDate )
    {
        this.programStartDate = programStartDate;
        return this;
    }

    public Date getProgramEndDate()
    {
        return programEndDate;
    }

    public ProgramInstanceQueryParams setProgramEndDate( Date programEndDate )
    {
        this.programEndDate = programEndDate;
        return this;
    }

    public TrackedEntity getTrackedEntity()
    {
        return trackedEntity;
    }

    public ProgramInstanceQueryParams setTrackedEntity( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity;
        return this;
    }

    public TrackedEntityInstance getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public ProgramInstanceQueryParams setTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
        return this;
    }

    public Integer getPage()
    {
        return page;
    }

    public ProgramInstanceQueryParams setPage( Integer page )
    {
        this.page = page;
        return this;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public ProgramInstanceQueryParams setPageSize( Integer pageSize )
    {
        this.pageSize = pageSize;
        return this;
    }

    public boolean isTotalPages()
    {
        return totalPages;
    }

    public ProgramInstanceQueryParams setTotalPages( boolean totalPages )
    {
        this.totalPages = totalPages;
        return this;
    }

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public ProgramInstanceQueryParams setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
        return this;
    }
}
