package org.hisp.dhis.mapgeneration;

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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.i18n.I18nFormat;

/**
 * This class can be used to render a set of legends onto one image.
 * 
 * @author Kristin Simonsen <krissimo@ifi.uio.no>
 * @author Kjetil Andresen <kjetil.andrese@gmail.com>
 */
public class LegendSet
{
    private List<Legend> legends;

    private Color backgroundColor = null;

    public static final int LEGEND_WIDTH = 132;
    public static final int LEGEND_MARGIN_LEFT = 3;
    public static final int LEGEND_MARGIN_BOTTOM = 20;
    
    public static final int LEGEND_TOTAL_WIDTH = LEGEND_WIDTH + LEGEND_MARGIN_LEFT;

    public LegendSet()
    {
        legends = new ArrayList<>();
    }

    public LegendSet( InternalMapLayer mapLayer )
    {
        legends = new ArrayList<>();
        addMapLayer( mapLayer );
    }

    public LegendSet( List<InternalMapLayer> mapLayers )
    {
        legends = new ArrayList<>();
        addMapLayers( mapLayers );
    }

    /**
     * Render the legends contained in this set onto a image. The width of the
     * image returned may vary, depending on how many columns of legends that is
     * added. The image height can be decided by the user, but if the biggest
     * legend is higher than imageMaxHeight, the height will automatically be
     * set to the height of this legend.
     * 
     * @param imageMaxHeight
     * @return
     */
    public BufferedImage render( I18nFormat format )
    {
        int imageWidth = LEGEND_TOTAL_WIDTH;
        int imageHeight = calculateImageHeight();
        BufferedImage image = new BufferedImage( imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB );
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        graphics.translate( LEGEND_MARGIN_LEFT, 0 );

        // Draw legends
        for ( Legend legend : legends )
        {
            legend.draw( graphics, format );
            graphics.translate( 0, LEGEND_MARGIN_BOTTOM );
        }

        return image;
    }

    public void addLegend( Legend legend )
    {
        legends.add( legend );
    }

    public void addLegends( List<Legend> legends )
    {
        for ( Legend legend : legends )
        {
            addLegend( legend );
        }
    }

    public void addMapLayer( InternalMapLayer mapLayer )
    {
        legends.add( new Legend( mapLayer ) );
    }

    public void addMapLayers( List<InternalMapLayer> mapLayers )
    {
        for ( InternalMapLayer mapLayer : mapLayers )
        {
            addMapLayer( mapLayer );
        }
    }

    public List<Legend> getLegends()
    {
        return legends;
    }

    public Color getBackground()
    {
        return backgroundColor;
    }

    public void setBackground( Color c )
    {
        backgroundColor = c;
    }

    private int calculateImageHeight()
    {
        int imageHeight = 0;

        for ( Legend legend : legends )
        {
            imageHeight += legend.getHeight() + LEGEND_MARGIN_BOTTOM;
        }
        
        return imageHeight;
    }
    
    @Override
    public String toString()
    {
        return legends != null ? legends.toString() : "[No legends]";
    }
}
