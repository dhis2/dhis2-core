package org.hisp.dhis.query;

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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.system.util.ReflectionUtils;

import java.util.Date;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Order
{
    private Direction direction;

    private boolean ignoreCase;

    private Property property;

    public Order( Property property, Direction direction )
    {
        this.property = property;
        this.direction = direction;
    }

    public Order ignoreCase()
    {
        this.ignoreCase = true;
        return this;
    }

    public boolean isAscending()
    {
        return Direction.ASCENDING == direction;
    }

    public boolean isIgnoreCase()
    {
        return ignoreCase;
    }

    public Property getProperty()
    {
        return property;
    }

    public boolean isPersisted()
    {
        return property.isPersisted() && property.isSimple();
    }

    public boolean isNonPersisted()
    {
        return !property.isPersisted() && property.isSimple();
    }

    public int compare( Object lside, Object rside )
    {
        Object o1 = ReflectionUtils.invokeMethod( lside, property.getGetterMethod() );
        Object o2 = ReflectionUtils.invokeMethod( rside, property.getGetterMethod() );

        if ( o1 == null || o2 == null )
        {
            return 0;
        }

        if ( String.class.isInstance( o1 ) && String.class.isInstance( o2 ) )
        {
            String value1 = ignoreCase ? ((String) o1).toLowerCase() : (String) o1;
            String value2 = ignoreCase ? ((String) o2).toLowerCase() : (String) o2;

            return isAscending() ? value1.compareTo( value2 ) : value2.compareTo( value1 );
        }
        if ( Boolean.class.isInstance( o1 ) && Boolean.class.isInstance( o2 ) )
        {
            return isAscending() ? ((Boolean) o1).compareTo( (Boolean) o2 ) : ((Boolean) o2).compareTo( (Boolean) o1 );
        }
        else if ( Integer.class.isInstance( o1 ) && Integer.class.isInstance( o2 ) )
        {
            return isAscending() ? ((Integer) o1).compareTo( (Integer) o2 ) : ((Integer) o2).compareTo( (Integer) o1 );
        }
        else if ( Float.class.isInstance( o1 ) && Float.class.isInstance( o2 ) )
        {
            return isAscending() ? ((Float) o1).compareTo( (Float) o2 ) : ((Float) o2).compareTo( (Float) o1 );
        }
        else if ( Double.class.isInstance( o1 ) && Double.class.isInstance( o2 ) )
        {
            return isAscending() ? ((Double) o1).compareTo( (Double) o2 ) : ((Double) o2).compareTo( (Double) o1 );
        }
        else if ( Date.class.isInstance( o1 ) && Date.class.isInstance( o2 ) )
        {
            return isAscending() ? ((Date) o1).compareTo( (Date) o2 ) : ((Date) o2).compareTo( (Date) o1 );
        }
        else if ( Enum.class.isInstance( o1 ) && Enum.class.isInstance( o2 ) )
        {
            return isAscending() ? String.valueOf( o1 ).compareTo( String.valueOf( o2 ) ) : String.valueOf( o2 ).compareTo( String.valueOf( o1 ) );
        }

        return 0;
    }

    public static Order asc( Property property )
    {
        return new Order( property, Direction.ASCENDING );
    }

    public static Order iasc( Property property )
    {
        return new Order( property, Direction.ASCENDING ).ignoreCase();
    }

    public static Order desc( Property property )
    {
        return new Order( property, Direction.DESCENDING );
    }

    public static Order idesc( Property property )
    {
        return new Order( property, Direction.DESCENDING ).ignoreCase();
    }

    public static Order from( String direction, Property property )
    {
        switch ( direction )
        {
            case "asc":
                return Order.asc( property );
            case "iasc":
                return Order.iasc( property );
            case "desc":
                return Order.desc( property );
            case "idesc":
                return Order.idesc( property );
            default:
                return Order.asc( property );
        }
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( direction, ignoreCase, property );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        final Order other = (Order) obj;

        return Objects.equals( this.direction, other.direction )
            && Objects.equals( this.ignoreCase, other.ignoreCase )
            && Objects.equals( this.property, other.property );
    }


    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "direction", direction )
            .add( "ignoreCase", ignoreCase )
            .add( "property", property )
            .toString();
    }
}
