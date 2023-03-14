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
package org.hisp.dhis.common;

import java.util.List;

import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AnalyticalObjectStore<T extends AnalyticalObject>
    extends IdentifiableObjectStore<T>
{
    List<T> getAnalyticalObjects( ExpressionDimensionItem expressionDimensionItem );

    List<T> getAnalyticalObjects( Indicator indicator );

    List<T> getAnalyticalObjects( DataElement dataElement );

    List<T> getAnalyticalObjectsByDataDimension( DataElement dataElement );

    List<T> getAnalyticalObjectsByDataDimension( TrackedEntityAttribute attribute );

    List<T> getAnalyticalObjects( DataSet dataSet );

    List<T> getAnalyticalObjects( ProgramIndicator programIndicator );

    List<T> getAnalyticalObjects( Period period );

    List<T> getAnalyticalObjects( OrganisationUnit organisationUnit );

    List<T> getAnalyticalObjects( OrganisationUnitGroup organisationUnitGroup );

    List<T> getAnalyticalObjects( OrganisationUnitGroupSet organisationUnitGroupSet );

    List<T> getAnalyticalObjects( CategoryOptionGroup categoryOptionGroup );

    List<T> getAnalyticalObjects( LegendSet legendSet );

    long countAnalyticalObjects( Indicator indicator );

    long countAnalyticalObjects( DataElement dataElement );

    long countAnalyticalObjects( DataSet dataSet );

    long countAnalyticalObjects( ProgramIndicator programIndicator );

    long countAnalyticalObjects( Period period );

    long countAnalyticalObjects( OrganisationUnit organisationUnit );

    long countAnalyticalObjects( CategoryOptionGroup categoryOptionGroup );
}
