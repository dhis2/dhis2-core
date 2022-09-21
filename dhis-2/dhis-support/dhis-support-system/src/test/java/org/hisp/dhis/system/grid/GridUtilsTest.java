/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.system.grid;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class GridUtilsTest
{
    @Test
    public void testFromHtml()
        throws Exception
    {
        String html = IOUtils.toString( new ClassPathResource( "customform.html" ).getInputStream(),
            StandardCharsets.UTF_8 );

        List<Grid> grids = GridUtils.fromHtml( html, "TitleA", null, null, null );

        assertNotNull( grids );
        assertEquals( 6, grids.size() );
        assertEquals( "TitleA", grids.get( 0 ).getTitle() );
    }

    @Test
    public void testGetGridIndexByDimensionItem()
    {
        Period period1 = PeriodType.getPeriodFromIsoString( "202010" );
        period1.setUid( CodeGenerator.generateUid() );

        Period period2 = PeriodType.getPeriodFromIsoString( "202011" );
        period2.setUid( CodeGenerator.generateUid() );

        Period period3 = PeriodType.getPeriodFromIsoString( "202012" );
        period3.setUid( CodeGenerator.generateUid() );

        List<DimensionalItemObject> periods = Lists.newArrayList( period1, period2, period3 );

        List<Object> row = new ArrayList<>( 3 );
        row.add( CodeGenerator.generateUid() ); // dimension
        row.add( period2.getIsoDate() ); // period
        row.add( 10.22D ); // value
        assertEquals( 1, GridUtils.getGridIndexByDimensionItem( row, periods, 2 ) );

        List<Object> row2 = new ArrayList<>( 3 );
        row2.add( CodeGenerator.generateUid() ); // dimension
        row2.add( "201901" ); // period
        row2.add( 10.22D ); // value
        assertEquals( 2, GridUtils.getGridIndexByDimensionItem( row2, periods, 2 ) );

    }

}
