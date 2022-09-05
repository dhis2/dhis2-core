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

import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.test.setup.MetadataSetup.AttributeSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.DataElementSetup;
import org.hisp.dhis.test.setup.MetadataSetup.Objects;
import org.hisp.dhis.test.setup.MetadataSetup.OrganisationUnitSetup;
import org.hisp.dhis.test.setup.MetadataSetup.PeriodSetup;

/**
 * Given the required collections of existing {@link Objects} the helper allows
 * to create {@link DataValue}s using the names of the referenced objects.
 *
 * @author Jan Bernitt
 */
public interface TestValueGenerator
{
    Objects<AttributeSetup> getAttributes();

    Objects<DataElementSetup> getDataElements();

    Objects<PeriodSetup> getPeriods();

    Objects<OrganisationUnitSetup> getOrganisationUnits();

    Objects<CategoryOptionComboSetup> getCategoryOptionCombos();

    default DataValue newDateValue( String de, String pe, String ou, String coc, String value )
    {
        return newDateValue( de, pe, ou, coc, null, value );
    }

    default DataValue newDateValue( String de, String pe, String ou, String coc, String aoc, String value )
    {
        return new DataValue(
            getDataElements().get( de ).getObject(),
            getPeriods().get( pe ).getObject(),
            getOrganisationUnits().get( ou ).getObject(),
            getCategoryOptionCombos().get( coc ).getObject(),
            aoc == null ? null : getCategoryOptionCombos().get( aoc ).getObject(),
            value );
    }
}
