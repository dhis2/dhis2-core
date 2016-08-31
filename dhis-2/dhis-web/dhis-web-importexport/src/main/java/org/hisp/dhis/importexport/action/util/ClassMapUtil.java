package org.hisp.dhis.importexport.action.util;

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

import static org.hisp.dhis.common.Objects.CONSTANT;
import static org.hisp.dhis.common.Objects.DATAELEMENT;
import static org.hisp.dhis.common.Objects.DATAELEMENTGROUP;
import static org.hisp.dhis.common.Objects.DATAELEMENTGROUPSET;
import static org.hisp.dhis.common.Objects.DATASET;
import static org.hisp.dhis.common.Objects.DATAVALUE;
import static org.hisp.dhis.common.Objects.INDICATOR;
import static org.hisp.dhis.common.Objects.INDICATORGROUP;
import static org.hisp.dhis.common.Objects.INDICATORGROUPSET;
import static org.hisp.dhis.common.Objects.INDICATORTYPE;
import static org.hisp.dhis.common.Objects.ORGANISATIONUNIT;
import static org.hisp.dhis.common.Objects.ORGANISATIONUNITGROUP;
import static org.hisp.dhis.common.Objects.ORGANISATIONUNITGROUPSET;
import static org.hisp.dhis.common.Objects.ORGANISATIONUNITLEVEL;
import static org.hisp.dhis.common.Objects.REPORTTABLE;
import static org.hisp.dhis.common.Objects.VALIDATIONRULE;
import static org.hisp.dhis.common.Objects.valueOf;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.validation.ValidationRule;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class ClassMapUtil
{
    private static Map<Objects, Class<?>> classMap;
    
    static
    {
        classMap = new HashMap<>();
        
        classMap.put( CONSTANT, Constant.class );
        classMap.put( DATAELEMENT, DataElement.class );
        classMap.put( DATAELEMENTGROUP, DataElementGroup.class );
        classMap.put( DATAELEMENTGROUPSET, DataElementGroupSet.class );
        classMap.put( INDICATORTYPE, IndicatorType.class );
        classMap.put( INDICATOR, Indicator.class );
        classMap.put( INDICATORGROUP, IndicatorGroup.class );
        classMap.put( INDICATORGROUPSET, IndicatorGroupSet.class );
        classMap.put( DATASET, DataSet.class );
        classMap.put( ORGANISATIONUNIT, OrganisationUnit.class );
        classMap.put( ORGANISATIONUNITGROUP, OrganisationUnitGroup.class );
        classMap.put( ORGANISATIONUNITGROUPSET, OrganisationUnitGroupSet.class );
        classMap.put( ORGANISATIONUNITLEVEL, OrganisationUnitLevel.class );
        classMap.put( VALIDATIONRULE, ValidationRule.class );
        classMap.put( REPORTTABLE, ReportTable.class );
        classMap.put( DATAVALUE, DataValue.class );
    }
    
    public static Class<?> getClass( String type )
    {        
        try
        {            
            return classMap.get( valueOf( type ) );
        }
        catch ( IllegalArgumentException ex )
        {
            return null;
        }
    }
}
