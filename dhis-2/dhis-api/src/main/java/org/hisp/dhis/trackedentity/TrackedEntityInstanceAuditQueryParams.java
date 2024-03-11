package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.AuditType;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 *
 */
public class TrackedEntityInstanceAuditQueryParams
{
    /**
     * Tracked entity instances to fetch audits for
     */
    private Set<String> trackedEntityInstances = new HashSet<>();
    
    /**
     * Users to fetch audits for
     */
    private Set<String> users = new HashSet<>();
    
    /**
     * AuditType to fetch for
     */
    private AuditType auditType;

    /**
     * Starting date.
     */
    private Date startDate = null;

    /**
     * Ending date.
     */
    private Date endDate = null;
    
    /**
     * Tracked entity instance audit count start
     */
    private int first;
    
    /**
     * Tracked entity instance audit count end
     */
    private int max;    

    /**
     * Traked entity instance audit skip paging or not
     */
    private boolean skipPaging;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityInstanceAuditQueryParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasTrackedEntityInstances()
    {
        return trackedEntityInstances != null && !trackedEntityInstances.isEmpty();
    }
    
    public boolean hasUsers()
    {
        return users != null && !users.isEmpty();
    }
    
    public boolean hasAuditType()
    {
        return auditType != null;
    }

    public boolean hasStartDate()
    {
        return startDate != null;
    }

    public boolean hasEndDate()
    {
        return endDate != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Set<String> getTrackedEntityInstances()
    {
        return trackedEntityInstances;
    }

    public void setTrackedEntityInstances( Set<String> trackedEntityInstances )
    {
        this.trackedEntityInstances = trackedEntityInstances;
    }    

    public Set<String> getUsers()
    {
        return users;
    }

    public void setUsers( Set<String> users )
    {
        this.users = users;
    }    

    public AuditType getAuditType()
    {
        return auditType;
    }

    public void setAuditType( AuditType auditType )
    {
        this.auditType = auditType;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    public int getFirst()
    {
        return first;
    }

    public void setFirst( int first )
    {
        this.first = first;
    }

    public int getMax()
    {
        return max;
    }

    public void setMax( int max )
    {
        this.max = max;
    }    

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public void setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
    }
}
