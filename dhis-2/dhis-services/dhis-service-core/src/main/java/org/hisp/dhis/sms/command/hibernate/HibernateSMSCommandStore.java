package org.hisp.dhis.sms.command.hibernate;

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

import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.parse.ParserType;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

public class HibernateSMSCommandStore
    extends HibernateIdentifiableObjectStore<SMSCommand> implements SMSCommandStore
{
    @Override
    public List<SMSCommand> getJ2MESMSCommands()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
                .addPredicate( root -> builder.equal( root.get( "parserType" ), ParserType.J2ME_PARSER ) ) );
    }

    @Override
    public SMSCommand getSMSCommand( String commandName, ParserType parserType )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        List<SMSCommand> list = getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "parserType" ), parserType ) )
            .addPredicate( root -> JpaQueryUtils.stringPredicateIgnoreCase( builder, root.get( "name" ), commandName, JpaQueryUtils.StringSearchMode.ANYWHERE ) ) );

        if ( list != null && !list.isEmpty() )
        {
            return  list.get( 0 );
        }

        return null;
    }

    @Override
    public int countDataSetSmsCommands( DataSet dataSet )
    {
        Query<Long> query = getQuery( "select count(distinct c) from SMSCommand c where c.dataset=:dataSet", Long.class );
        query.setParameter( "dataSet", dataSet );
        // TODO rename dataset prop

        return  query.uniqueResult().intValue();
    }
}
