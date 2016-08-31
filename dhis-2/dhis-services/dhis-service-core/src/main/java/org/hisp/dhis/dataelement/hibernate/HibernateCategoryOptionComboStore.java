package org.hisp.dhis.dataelement.hibernate;

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

import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.CategoryOptionComboStore;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dbms.DbmsManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class HibernateCategoryOptionComboStore
    extends HibernateIdentifiableObjectStore<DataElementCategoryOptionCombo>
    implements CategoryOptionComboStore
{
    @Autowired
    private DbmsManager dbmsManager;

    @Override
    public DataElementCategoryOptionCombo getCategoryOptionCombo( DataElementCategoryCombo categoryCombo, Set<DataElementCategoryOption> categoryOptions )
    {
        String hql = "from DataElementCategoryOptionCombo co where co.categoryCombo = :categoryCombo";
        
        for ( DataElementCategoryOption option : categoryOptions )
        {
            hql += " and :option" + option.getId() + " in elements (co.categoryOptions)";
        }
        
        Query query = getQuery( hql );
        
        query.setEntity( "categoryCombo", categoryCombo );
        
        for ( DataElementCategoryOption option : categoryOptions )
        {
            query.setEntity( "option" + option.getId(), option );
        }
        
        return (DataElementCategoryOptionCombo) query.uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateNames()
    {
        List<DataElementCategoryOptionCombo> categoryOptionCombos = getQuery( "from DataElementCategoryOptionCombo co where co.name is null" ).list();
        int counter = 0;
        
        Session session = getSession();
        
        for ( DataElementCategoryOptionCombo coc : categoryOptionCombos )
        {
            session.update( coc );
            
            if ( ( counter % 400 ) == 0 )
            {
                dbmsManager.clearSession();
            }
        }
    }
}
