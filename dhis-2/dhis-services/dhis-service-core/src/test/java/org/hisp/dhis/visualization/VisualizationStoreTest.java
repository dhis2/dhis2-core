/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.Font;
import org.hisp.dhis.common.FontStyle;
import org.hisp.dhis.common.TextAlign;
import org.hisp.dhis.period.RelativePeriods;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class VisualizationStoreTest
    extends DhisSpringTest
{
    @Autowired
    private VisualizationStore subject;

    @Test
    public void testSaveGet()
    {
        FontStyle visualizationTitle = new FontStyle();
        visualizationTitle.setFont( Font.VERDANA );
        visualizationTitle.setFontSize( 16 );
        visualizationTitle.setBold( true );
        visualizationTitle.setItalic( false );
        visualizationTitle.setUnderline( false );
        visualizationTitle.setTextColor( "#3a3a3a" );
        visualizationTitle.setTextAlign( TextAlign.LEFT );

        FontStyle horizontalAxisTitle = new FontStyle();
        horizontalAxisTitle.setFont( Font.ARIAL );
        horizontalAxisTitle.setFontSize( 14 );
        horizontalAxisTitle.setBold( false );
        horizontalAxisTitle.setItalic( true );
        horizontalAxisTitle.setUnderline( false );
        horizontalAxisTitle.setTextColor( "#2b2b2b" );
        horizontalAxisTitle.setTextAlign( TextAlign.CENTER );

        FontStyle seriesAxisLabel = new FontStyle();
        seriesAxisLabel.setFont( Font.ARIAL );
        seriesAxisLabel.setFontSize( 12 );
        seriesAxisLabel.setBold( false );
        seriesAxisLabel.setItalic( false );
        seriesAxisLabel.setUnderline( true );
        seriesAxisLabel.setTextColor( "#cdcdcd" );
        seriesAxisLabel.setTextAlign( TextAlign.RIGHT );

        FontStyle targetLineLabel = new FontStyle();
        targetLineLabel.setFont( Font.VERDANA );
        targetLineLabel.setFontSize( 10 );
        targetLineLabel.setBold( true );
        targetLineLabel.setItalic( false );
        targetLineLabel.setUnderline( false );
        targetLineLabel.setTextColor( "#dddddd" );
        targetLineLabel.setTextAlign( TextAlign.CENTER );

        VisualizationFontStyle fontStyle = new VisualizationFontStyle();
        fontStyle.setVisualizationTitle( visualizationTitle );
        fontStyle.setHorizontalAxisTitle( horizontalAxisTitle );
        fontStyle.setSeriesAxisLabel( seriesAxisLabel );
        fontStyle.setTargetLineLabel( targetLineLabel );

        RelativePeriods relativePeriods = new RelativePeriods()
            .setLast30Days( true );

        Visualization vA = createVisualization( 'A' );
        vA.setFontStyle( fontStyle );
        vA.setRelatives( relativePeriods );

        Visualization vB = createVisualization( 'B' );

        subject.save( vA );
        subject.save( vB );

        vA = subject.getByUid( vA.getUid() );
        vB = subject.getByUid( vB.getUid() );

        assertNotNull( vA );
        assertNotNull( vA.getFontStyle() );
        assertNotNull( vA.getFontStyle().getVisualizationTitle() );
        assertEquals( Font.VERDANA, vA.getFontStyle().getVisualizationTitle().getFont() );
        assertEquals( Integer.valueOf( 16 ), vA.getFontStyle().getVisualizationTitle().getFontSize() );
        assertNotNull( vA.getFontStyle().getHorizontalAxisTitle() );
        assertEquals( Font.ARIAL, vA.getFontStyle().getHorizontalAxisTitle().getFont() );
        assertTrue( vA.getFontStyle().getHorizontalAxisTitle().getItalic() );
        assertNotNull( vA.getFontStyle().getSeriesAxisLabel() );
        assertEquals( Font.ARIAL, vA.getFontStyle().getSeriesAxisLabel().getFont() );
        assertTrue( vA.getFontStyle().getSeriesAxisLabel().getUnderline() );
        assertNotNull( vA.getFontStyle().getTargetLineLabel() );
        assertEquals( Font.VERDANA, vA.getFontStyle().getTargetLineLabel().getFont() );
        assertTrue( vA.getFontStyle().getTargetLineLabel().getBold() );

        assertNotNull( vA.getRelatives() );
        assertTrue( vA.getRelatives().isLast30Days() );

        assertNotNull( vB );
        assertNull( vB.getFontStyle() );
    }
}
