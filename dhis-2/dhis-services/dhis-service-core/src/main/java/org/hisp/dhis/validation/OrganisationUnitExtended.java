package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Holds information for each organisation unit that is needed during a
 * validation run (either interactive or a scheduled run).
 * 
 * It is important that they should be copied from Hibernate lazy collections
 * before the multithreaded part of the run starts, otherwise the threads may
 * not be able to access these values.
 * 
 * @author Jim Grace
 */
public class OrganisationUnitExtended
{
    private OrganisationUnit source;

    private boolean toBeValidated;

    private Collection<OrganisationUnit> children;

    private int level;

    public OrganisationUnitExtended( OrganisationUnit source, boolean toBeValidated )
    {
        this.source = source;
        this.toBeValidated = toBeValidated;
        children = new HashSet<>( source.getChildren() );
        level = source.getLevel();
    }

    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE ).append( "\n  name", source.getName() )
            .append( "\n  children[", children.size() + "]" ).append( "\n  level", level ).toString();
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------

    public OrganisationUnit getSource()
    {
        return source;
    }

    public boolean getToBeValidated()
    {
        return toBeValidated;
    }

    public Collection<OrganisationUnit> getChildren()
    {
        return children;
    }

    public int getLevel()
    {
        return level;
    }
}
