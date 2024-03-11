package org.hisp.dhis.program.hibernate;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStore;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityType;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

/**
 * @author Chau Thu Tran
 */
public class HibernateProgramStore
    extends HibernateIdentifiableObjectStore<Program>
    implements ProgramStore
{
    // -------------------------------------------------------------------------
    // Implemented methods
    // -------------------------------------------------------------------------

    @Override
    public List<Program> getByType( ProgramType type )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
               .addPredicate( root ->  builder.equal( root.get( "programType" ), type )));
    }

    @Override
    public List<Program> get( OrganisationUnit organisationUnit )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.join( "organisationUnits" ).get( "id" ), organisationUnit.getId() ) ) );
    }

    @Override
    public List<Program> getByTrackedEntityType( TrackedEntityType trackedEntityType )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "trackedEntityType" ), trackedEntityType ) ) );
    }

    @Override
    public List<Program> getByDataEntryForm( DataEntryForm dataEntryForm )
    {
        if ( dataEntryForm == null )
        {
            return Lists.newArrayList();
        }

        final String hql = "from Program p where p.dataEntryForm = :dataEntryForm";

        return getQuery( hql ).setParameter( "dataEntryForm", dataEntryForm ).list();
    }
}
