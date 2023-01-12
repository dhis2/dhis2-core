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
package org.hisp.dhis.visualization;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.visualization.AxisType.RANGE;
import static org.hisp.dhis.visualization.CompatibilityGuard.keepAxesReadingCompatibility;
import static org.hisp.dhis.visualization.CompatibilityGuard.keepLegendReadingCompatibility;

import java.util.List;

import org.hisp.dhis.common.Font;
import org.hisp.dhis.common.FontStyle;
import org.junit.jupiter.api.Test;

/**
 * Unit tests to check the conversions related to {@link CompatibilityGuard}.
 *
 * @author maikel arabori
 */
class CompatibilityGuardTest
{
    @Test
    void testKeepLegendReadingCompatibility()
    {
        // Given
        Visualization visualization = mockVisualizationWithLegend();

        // When
        keepLegendReadingCompatibility( visualization );

        // Then
        assertThat( visualization.getFontStyle().getLegend(),
            is( equalTo( visualization.getSeriesKey().getLabel().getFontStyle() ) ) );
        assertThat( visualization.getFontStyle().getLegend().getFont(),
            is( equalTo( visualization.getSeriesKey().getLabel().getFontStyle().getFont() ) ) );
    }

    @Test
    void testKeepAxesReadingCompatibility()
    {
        // Given
        Visualization visualization = mockVisualizationWithAxes();

        // When
        keepAxesReadingCompatibility( visualization );

        // Then
        // # Fist axis assertions
        assertThat( visualization.getFontStyle().getSeriesAxisLabel(),
            is( equalTo( visualization.getAxes().get( 0 ).getLabel().getFontStyle() ) ) );
        assertThat( visualization.getFontStyle().getVerticalAxisTitle(),
            is( equalTo( visualization.getAxes().get( 0 ).getTitle().getFontStyle() ) ) );
        // Ranges
        assertThat( visualization.getRangeAxisDecimals(),
            is( equalTo( visualization.getAxes().get( 0 ).getDecimals() ) ) );
        assertThat( visualization.getRangeAxisMaxValue(),
            is( equalTo( visualization.getAxes().get( 0 ).getMaxValue().doubleValue() ) ) );
        assertThat( visualization.getRangeAxisMinValue(),
            is( equalTo( visualization.getAxes().get( 0 ).getMinValue().doubleValue() ) ) );
        assertThat( visualization.getRangeAxisSteps(), is( equalTo( visualization.getAxes().get( 0 ).getSteps() ) ) );
        // Target line
        assertThat( visualization.getFontStyle().getTargetLineLabel(),
            is( equalTo( visualization.getAxes().get( 0 ).getTargetLine().getTitle().getFontStyle() ) ) );
        assertThat( visualization.getTargetLineLabel(),
            is( equalTo( visualization.getAxes().get( 0 ).getTargetLine().getTitle().getText() ) ) );
        assertThat( visualization.getTargetLineValue(),
            is( equalTo( visualization.getAxes().get( 0 ).getTargetLine().getValue().doubleValue() ) ) );
        // Base line
        assertThat( visualization.getFontStyle().getBaseLineLabel(),
            is( equalTo( visualization.getAxes().get( 0 ).getBaseLine().getTitle().getFontStyle() ) ) );
        assertThat( visualization.getBaseLineLabel(),
            is( equalTo( visualization.getAxes().get( 0 ).getBaseLine().getTitle().getText() ) ) );
        assertThat( visualization.getBaseLineValue(),
            is( equalTo( visualization.getAxes().get( 0 ).getBaseLine().getValue().doubleValue() ) ) );
        // # Second axis assertions
        assertThat( visualization.getFontStyle().getHorizontalAxisTitle(),
            is( equalTo( visualization.getAxes().get( 1 ).getTitle().getFontStyle() ) ) );
        assertThat( visualization.getDomainAxisLabel(),
            is( equalTo( visualization.getAxes().get( 1 ).getTitle().getText() ) ) );
    }

    private Visualization mockVisualizationWithLegend()
    {
        SeriesKey legend = new SeriesKey();
        StyledObject label = new StyledObject();
        FontStyle fontStyle = new FontStyle();
        fontStyle.setFont( Font.ARIAL );
        label.setFontStyle( fontStyle );
        legend.setLabel( label );
        Visualization visualization = new Visualization();
        visualization.setSeriesKey( legend );
        return visualization;
    }

    private Visualization mockVisualizationWithAxes()
    {
        StyledObject title = new StyledObject();
        title.setText( "Some title" );
        StyledObject label = new StyledObject();
        label.setText( "Some label" );
        Line baseLine = new Line();
        baseLine.setTitle( title );
        baseLine.setValue( 20.0 );
        Line targetLine = new Line();
        targetLine.setTitle( title );
        targetLine.setValue( 40.0 );
        AxisV2 firstAxis = new AxisV2();
        firstAxis.setMaxValue( 1.0 );
        firstAxis.setDecimals( 2 );
        firstAxis.setIndex( 0 );
        firstAxis.setMinValue( 3.0 );
        firstAxis.setSteps( 4 );
        firstAxis.setBaseLine( baseLine );
        firstAxis.setLabel( label );
        firstAxis.setTargetLine( targetLine );
        firstAxis.setTitle( title );
        firstAxis.setType( RANGE );
        AxisV2 secondAxis = new AxisV2();
        secondAxis.setLabel( label );
        secondAxis.setTitle( title );
        List<AxisV2> axes = newArrayList( firstAxis, secondAxis );
        Visualization visualization = new Visualization();
        visualization.setAxes( axes );
        return visualization;
    }
}
