package org.hisp.dhis.minmax;

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

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.Collection;
import java.util.List;

/**
 * @author Kristian Nordal
 * @version $Id: MinMaxDataElementStore.java 5012 2008-04-24 21:14:40Z larshelg $
 */
public interface MinMaxDataElementStore
    extends GenericStore<MinMaxDataElement>
{
    String ID = MinMaxDataElementStore.class.getName();

    MinMaxDataElement get( OrganisationUnit source, DataElement dataElement, DataElementCategoryOptionCombo optionCombo );
    
    List<MinMaxDataElement> get( OrganisationUnit source, DataElement dataElement );  

    List<MinMaxDataElement> get( OrganisationUnit source, Collection<DataElement> dataElements );

    List<MinMaxDataElement> query( MinMaxDataElementQueryParams query );

    int countMinMaxDataElements( MinMaxDataElementQueryParams query );

    void delete( OrganisationUnit organisationUnit );
    
    void delete( DataElement dataElement );
    
    void delete( DataElementCategoryOptionCombo optionCombo );
    
    void delete( Collection<DataElement> dataElements, OrganisationUnit parent );
}
