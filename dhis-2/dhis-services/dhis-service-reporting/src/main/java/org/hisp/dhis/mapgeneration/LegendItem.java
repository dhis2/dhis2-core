package org.hisp.dhis.mapgeneration;

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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

/**
 * A legend item is a graphical presentation of a interval. It serves as a
 * helper for the Legend class.
 * 
 * @author Kristin Simonsen <krissimo@ifi.uio.no>
 * @author Kjetil Andresen <kjetil.andrese@gmail.com>
 */
public class LegendItem
{
    private Interval interval;

    private static final int WIDTH = 22;
    private static final int HEIGHT = 20;
    private static final int LABEL_MARGIN = 6;

    public LegendItem( Interval interval )
    {
        this.interval = interval;
    }

    public void draw( Graphics2D g )
    {
        String label = String.format( "%.2f - %.2f (%d)", interval.getValueLow(), interval.getValueHigh(), interval
            .getMembers().size() );
        Stroke s = new BasicStroke( 1.0f );
        Rectangle rect = new Rectangle( 0, 0, WIDTH, HEIGHT );

        g.setColor( interval.getColor() );
        g.fill( rect );
        g.setPaint( Color.BLACK );
        g.setStroke( s );
        g.draw( rect );

        g.setColor( Color.BLACK );
        g.setFont( Legend.PLAIN_FONT );
        g.drawString( label, WIDTH + LABEL_MARGIN, HEIGHT - 5 );
    }

    public int getHeight()
    {
        return HEIGHT;
    }

    public Interval getInterval()
    {
        return interval;
    }

    public void setInterval( Interval interval )
    {
        this.interval = interval;
    }

    @Override
    public String toString()
    {
        return interval != null ? interval.toString() : "[No interval]";
    }
}
