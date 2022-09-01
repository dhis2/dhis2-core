/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.test.setup;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategorySetup;
import org.hisp.dhis.test.setup.MetadataSetup.DataElementSetup;
import org.hisp.dhis.test.setup.MetadataSetup.Objects;
import org.hisp.dhis.test.setup.MetadataSetup.OrganisationUnitSetup;
import org.hisp.dhis.test.setup.MetadataSetup.PeriodSetup;

/**
 * Provides convenient access to the generated test objects.
 *
 * @author Jan Bernitt
 */
public interface TestObjectRegistry
{
    Objects<CategorySetup> getCategories();

    Objects<CategoryOptionSetup> getCategoryOptions();

    Objects<CategoryComboSetup> getCategoryCombos();

    Objects<CategoryOptionComboSetup> getCategoryOptionCombos();

    Objects<OrganisationUnitSetup> getOrganisationUnits();

    Objects<DataElementSetup> getDataElements();

    Objects<PeriodSetup> getPeriods();

    default Category getCategory( String name )
    {
        return getCategories().get( name ).getObject();
    }

    default CategoryOption getCategoryOption( String name )
    {
        return getCategoryOptions().get( name ).getObject();
    }

    default CategoryCombo getCategoryCombo( String name )
    {
        return getCategoryCombos().get( name ).getObject();
    }

    default CategoryOptionCombo getCategoryOptionCombo( String name )
    {
        return getCategoryOptionCombos().get( name ).getObject();
    }

    default OrganisationUnit getOrganisationUnit( String name )
    {
        return getOrganisationUnits().get( name ).getObject();
    }

    default DataElement getDataElement( String name )
    {
        return getDataElements().get( name ).getObject();
    }

    default Period getPeriod( String isoPeriod )
    {
        return getPeriods().get( isoPeriod ).getObject();
    }
}
