package org.hisp.dhis.trackedentity;

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

import java.util.Date;

/**
 * @author Chau Thu Tran
 * 
 * @version TrackedEntityAudit.java 8:56:01 AM Sep 26, 2012 $
 */
public class TrackedEntityAudit
{
    public static final String MODULE_ENTITY_INSTANCE_DASHBOARD = "tracked_entity_instance_dashboard";

    private int id;

    private TrackedEntityInstance entityInstance;

    private String visitor;

    private Date date;

    private String accessedModule;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityAudit()
    {

    }

    public TrackedEntityAudit( TrackedEntityInstance entityInstance, String visitor, Date date, String accessedModule )
    {
        this.entityInstance = entityInstance;
        this.visitor = visitor;
        this.date = date;
        this.accessedModule = accessedModule;
    }

    // TODO implement hashCode and equals

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public TrackedEntityInstance getEntityInstance()
    {
        return entityInstance;
    }

    public void setEntityInstance( TrackedEntityInstance entityInstance )
    {
        this.entityInstance = entityInstance;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate( Date date )
    {
        this.date = date;
    }

    public String getVisitor()
    {
        return visitor;
    }

    public void setVisitor( String visitor )
    {
        this.visitor = visitor;
    }

    public String getAccessedModule()
    {
        return accessedModule;
    }

    public void setAccessedModule( String accessedModule )
    {
        this.accessedModule = accessedModule;
    }
}
