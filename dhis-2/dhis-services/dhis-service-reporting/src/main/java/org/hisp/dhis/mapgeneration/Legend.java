package org.hisp.dhis.mapgeneration;

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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.i18n.I18nFormat;

/**
 * A legend is a graphical presentation of data contained in a map layer. This
 * class works as helper for LegendSet when it comes to drawing the actual
 * legend using java graphics. A legend has a height, but the actual width is
 * not defined.
 * 
 * @author Kristin Simonsen <krissimo@ifi.uio.no>
 * @author Kjetil Andresen <kjetil.andrese@gmail.com>
 */
public class Legend
{
    public static final Font TITLE_FONT = new Font( "title", Font.BOLD, 12 );
    public static final Font PLAIN_FONT = new Font( "plain", Font.PLAIN, 11 );

    private InternalMapLayer mapLayer;

    private List<LegendItem> legendItems;

    private static final int HEADER_HEIGHT = 50;

    public Legend( InternalMapLayer mapLayer )
    {
        this.mapLayer = mapLayer;
        this.legendItems = new ArrayList<>();

        for ( Interval interval : mapLayer.getIntervalSet().getIntervals() )
        {
            addLegendItem( new LegendItem( interval ) );
        }
    }

    public void draw( Graphics2D g, I18nFormat format )
    {
        g.setColor( Color.BLACK );
        g.setFont( PLAIN_FONT );
        g.drawString( mapLayer.getName(), 0, 15 );
        g.drawString( format.formatPeriod( mapLayer.getPeriod() ) + "", 0, 35 );

        g.translate( 0, HEADER_HEIGHT );

        for ( LegendItem legendItem : legendItems )
        {
            legendItem.draw( g );
            g.translate( 0, legendItem.getHeight() );
        }
    }
    
    public int getHeight()
    {
        int height = 0;

        for ( LegendItem legendItem : legendItems )
        {
            height += legendItem.getHeight();
        }

        return HEADER_HEIGHT + height;
    }

    public List<LegendItem> getLegendItems()
    {
        return legendItems;
    }

    public void addLegendItem( LegendItem legendItem )
    {
        legendItems.add( legendItem );
    }

    @Override
    public String toString()
    {
        return legendItems != null ? legendItems.toString() : "[No legend items]";
    }
}
