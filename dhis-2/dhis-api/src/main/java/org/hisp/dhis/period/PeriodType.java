package org.hisp.dhis.period;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.calendar.DateInterval;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.calendar.DateUnitPeriodTypeParser;
import org.hisp.dhis.calendar.DateUnitType;
import org.hisp.dhis.calendar.PeriodTypeParser;
import org.hisp.dhis.calendar.impl.Iso8601Calendar;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObjectUtils;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The superclass of all PeriodTypes.
 *
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "periodType", namespace = DxfNamespaces.DXF_2_0 )
public abstract class PeriodType
    implements Serializable
{
    // Cache for period lookup, uses calendar.name() + periodType.getName() + date.getTime() as key
    private static Cache<Period> PERIOD_CACHE = new SimpleCacheBuilder<Period>()
        .forRegion( "periodCache" )
        .expireAfterAccess( 12, TimeUnit.HOURS )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 30000 )
        .build();

    private String getCacheKey( Date date )
    {
        return getCalendar().name() + getName() + date.getTime();
    }

    private String getCacheKey( org.hisp.dhis.calendar.Calendar calendar, Date date )
    {
        return calendar.name() + getName() + date.getTime();
    }

    /**
     * Invalidates the period cache.
     * <p/>
     * Used in testing when there are multiple database loads
     * and the same periods may be assigned different database ids.
     */
    public static void invalidatePeriodCache()
    {
        PERIOD_CACHE.invalidateAll();
    }

    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 2402122626196305083L;

    private static CalendarService calendarService;

    public static void setCalendarService( CalendarService calendarService )
    {
        PeriodType.calendarService = calendarService;
    }

    public static CalendarService getCalendarService()
    {
        return calendarService;
    }

    public static org.hisp.dhis.calendar.Calendar getCalendar()
    {
        if ( calendarService != null )
        {
            return calendarService.getSystemCalendar();
        }

        return Iso8601Calendar.getInstance();
    }

    protected PeriodTypeParser dateUnitFormat = new DateUnitPeriodTypeParser();

    // -------------------------------------------------------------------------
    // Available PeriodTypes
    // -------------------------------------------------------------------------

    /**
     * All period types enumerated in descending order according to frequency.
     */
    public static final List<PeriodType> PERIOD_TYPES = new ArrayList<PeriodType>()
    {
        {
            add( new DailyPeriodType() );
            add( new WeeklyPeriodType() );
            add( new WeeklyWednesdayPeriodType() );
            add( new WeeklyThursdayPeriodType() );
            add( new WeeklySaturdayPeriodType() );
            add( new WeeklySundayPeriodType() );
            add( new BiWeeklyPeriodType() );
            add( new MonthlyPeriodType() );
            add( new BiMonthlyPeriodType() );
            add( new QuarterlyPeriodType() );
            add( new SixMonthlyPeriodType() );
            add( new SixMonthlyAprilPeriodType() );
            add( new YearlyPeriodType() );
            add( new FinancialAprilPeriodType() );
            add( new FinancialJulyPeriodType() );
            add( new FinancialOctoberPeriodType() );
        }
    };

    public static final Map<String, DayOfWeek> MAP_WEEK_TYPE = ImmutableMap.of(
        WeeklySundayPeriodType.NAME, DayOfWeek.SUNDAY,
        WeeklyWednesdayPeriodType.NAME, DayOfWeek.WEDNESDAY,
        WeeklyThursdayPeriodType.NAME, DayOfWeek.THURSDAY,
        WeeklySaturdayPeriodType.NAME, DayOfWeek.SATURDAY,
        WeeklyPeriodType.NAME, DayOfWeek.MONDAY );

    private static final Map<String, PeriodType> PERIOD_TYPE_MAP = new HashMap<String, PeriodType>()
    {
        {
            for ( PeriodType periodType : PERIOD_TYPES )
            {
                put( periodType.getName(), periodType );
            }
        }
    };

    /**
     * Returns an immutable list of all available PeriodTypes in their natural order.
     *
     * @return all available PeriodTypes in their natural order.
     */
    public static List<PeriodType> getAvailablePeriodTypes()
    {
        return new ArrayList<>( PERIOD_TYPES );
    }

    /**
     * Returns a PeriodType with a given name.
     *
     * @param name the name of the PeriodType to return.
     * @return the PeriodType with the given name or null if no such PeriodType
     * exists.
     */
    public static PeriodType getPeriodTypeByName( String name )
    {
        return PERIOD_TYPE_MAP.get( name );
    }

    public static PeriodType getByNameIgnoreCase( String name )
    {
        for ( PeriodType periodType : getAvailablePeriodTypes() )
        {
            if ( name != null && periodType.getName().toLowerCase().trim().equals( name.toLowerCase().trim() ) )
            {
                return periodType;
            }
        }

        return null;
    }

    /**
     * Get period type according to natural order order.
     *
     * @param index the index of the period type with base 1
     * @return period type according to index order or null if no match
     * TODO: Consider manual ordering, since relying on natural order might create problems if new periods are introduced.
     */
    public static PeriodType getByIndex( int index )
    {
        index -= 1;

        if ( index < 0 || index > PERIOD_TYPES.size() - 1 )
        {
            return null;
        }

        return PERIOD_TYPES.get( index );
    }
    
    /**
     * Returns a list of periods for each of the available period types defined by
     * {@link PeriodType#PERIOD_TYPES} in matching order relative to the given period.
     * 
     * @param period the period.
     * @param calendar the calendar.
     * @return a list of periods.
     */
    public static List<Period> getPeriodTypePeriods( Period period, org.hisp.dhis.calendar.Calendar calendar )
    {
        List<Period> periods = new ArrayList<>();
        
        PeriodType periodType = period.getPeriodType();
        
        for ( PeriodType type : PeriodType.PERIOD_TYPES )
        {
            if ( periodType.getFrequencyOrder() < type.getFrequencyOrder() || periodType.equals( type ) )
            {
                periods.add( IdentifiableObjectUtils.getPeriodByPeriodType( period, type, calendar ) );
            }
            else
            {
                periods.add( null );
            }
        }

        return periods;
    }

    // -------------------------------------------------------------------------
    // Persistable
    // -------------------------------------------------------------------------

    private int id;

    public void setId( int id )
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    /**
     * Returns a unique name for the PeriodType.
     *
     * @return a unique name for the PeriodType. E.g. "Monthly".
     */
    public abstract String getName();

    /**
     * Creates a valid Period based on the current date. E.g. if today is
     * January 5. 2007, a monthly PeriodType should return January 2007.
     *
     * @return a valid Period based on the current date.
     */
    public Period createPeriod()
    {
        return createPeriod( createCalendarInstance() );
    }

    /**
     * Creates a valid Period based on the given date. E.g. the given date is
     * February 10. 2007, a monthly PeriodType should return February 2007.
     *
     * @param date the date which is contained by the created period.
     * @return the valid Period based on the given date
     */
    public Period createPeriod( final Date date )
    {
        return PERIOD_CACHE.get( getCacheKey( date ), s -> createPeriod( createCalendarInstance( date ) ) ).orElse( null );
    }

    public Period createPeriod( Calendar cal )
    {
        org.hisp.dhis.calendar.Calendar calendar = getCalendar();

        return createPeriod( calendar.fromIso( DateTimeUnit.fromJdkCalendar( cal ) ), calendar );
    }

    /**
     * Creates a valid Period based on the given date. E.g. the given date is
     * February 10. 2007, a monthly PeriodType should return February 2007. This
     * method is intended for use in situations where a huge number of of periods
     * will be generated and its desirable to re-use the calendar.
     *
     * @param date     the date which is contained by the created period.
     * @param calendar the calendar implementation to use.
     * @return the valid Period based on the given date
     */
    public Period createPeriod( final Date date, final org.hisp.dhis.calendar.Calendar calendar )
    {
        return PERIOD_CACHE.get( getCacheKey( calendar, date ), p -> createPeriod( calendar.fromIso( DateTimeUnit.fromJdkDate( date ) ), calendar ) ).orElse( null );
    }

    public Period toIsoPeriod( DateTimeUnit start, DateTimeUnit end )
    {
        return toIsoPeriod( start, end, getCalendar() );
    }

    protected Period toIsoPeriod( DateTimeUnit start, DateTimeUnit end, org.hisp.dhis.calendar.Calendar calendar )
    {
        DateTimeUnit from = calendar.toIso( start );
        DateTimeUnit to = calendar.toIso( end );

        return new Period( this, from.toJdkDate(), to.toJdkDate(), getIsoDate( start, calendar ) );
    }

    public Period toIsoPeriod( DateTimeUnit dateTimeUnit )
    {
        return toIsoPeriod( dateTimeUnit, dateTimeUnit );
    }

    public abstract String getIso8601Duration();

    public abstract Period createPeriod( DateTimeUnit dateTimeUnit, org.hisp.dhis.calendar.Calendar calendar );

    /**
     * Returns a comparable value for the frequency length of this PeriodType.
     * Shortest is 0.
     *
     * @return the frequency order.
     */
    public abstract int getFrequencyOrder();

    /**
     * Returns a new date rewinded from now.
     *
     * @return the Date.
     */
    public abstract Date getRewindedDate( Date date, Integer rewindedPeriods );

    // -------------------------------------------------------------------------
    // Calendar support
    // -------------------------------------------------------------------------

    /**
     * Returns an instance of a Calendar without any time of day, with the
     * current date.
     *
     * @return an instance of a Calendar without any time of day.
     */
    public static Calendar createCalendarInstance()
    {
        org.hisp.dhis.calendar.Calendar cal = getCalendar();

        return cal.toIso( cal.today() ).toJdkCalendar();
    }

    /**
     * Returns an instance of a Calendar without any time of day, with the given
     * date.
     *
     * @param date the date of the Calendar.
     * @return an instance of a Calendar without any time of day.
     */
    public static Calendar createCalendarInstance( Date date )
    {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime( date );

        clearTimeOfDay( calendar );

        return calendar;
    }

    /**
     * Returns an instance of a DateUnit.
     *
     * @param date date of calendar in local calendar
     * @return an instance of a Calendar without any time of day.
     */
    public static DateTimeUnit createLocalDateUnitInstance( Date date )
    {
        return createLocalDateUnitInstance( date, getCalendar() );
    }

    /**
     * Returns an instance of a DateUnit.
     *
     * @param date date of calendar in local calendar
     * @return an instance of a Calendar without any time of day.
     */
    public static DateTimeUnit createLocalDateUnitInstance( Date date, org.hisp.dhis.calendar.Calendar calendar )
    {
        return calendar.fromIso( date );
    }

    /**
     * Clears the time of day in a Calendar instance.
     *
     * @param calendar the Calendar to fix.
     */
    public static void clearTimeOfDay( Calendar calendar )
    {
        calendar.set( Calendar.MILLISECOND, 0 );
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MINUTE, 0 );
        calendar.set( Calendar.HOUR_OF_DAY, 0 );
    }

    /**
     * Returns a PeriodType corresponding to the provided string The test is
     * quite rudimentary, testing for string format rather than invalid periods.
     * Currently only recognizes the basic subset of common period types.
     *
     * @param isoPeriod String formatted period (2011, 201101, 2011W34, 2011Q1
     *                  etc
     * @return the PeriodType or null if unrecognized
     */
    public static PeriodType getPeriodTypeFromIsoString( String isoPeriod )
    {
        DateUnitType dateUnitType = DateUnitType.find( isoPeriod );
        return dateUnitType != null ? PERIOD_TYPE_MAP.get( dateUnitType.getName() ) : null;
    }

    /**
     * Returns a period based on the given date string in ISO format. Returns
     * null if the date string cannot be parsed to a period.
     *
     * @param isoPeriod the date string in ISO format.
     * @return a period.
     */
    public static Period getPeriodFromIsoString( String isoPeriod )
    {
        if ( isoPeriod != null )
        {
            PeriodType periodType = getPeriodTypeFromIsoString( isoPeriod );

            try
            {
                return periodType != null ? periodType.createPeriod( isoPeriod ) : null;
            }
            catch ( Exception ex )
            {
                // Do nothing and return null
            }
        }

        return null;
    }

    /**
     * Returns a list of periods based on the given date string in ISO format.
     *
     * @param isoPeriods the date strings in ISO format.
     * @return a period.
     */
    public static List<Period> getPeriodsFromIsoStrings( List<String> isoPeriods )
    {
        List<Period> periods = new ArrayList<>();

        for ( String isoPeriod : isoPeriods )
        {
            Period period = getPeriodFromIsoString( isoPeriod );

            if ( period != null )
            {
                periods.add( period );
            }
        }

        return periods;
    }

    /**
     * Return the potential number of periods of the given period type which is
     * spanned by this period.
     *
     * @param type the period type.
     * @return the potential number of periods of the given period type spanned
     * by this period.
     */
    public int getPeriodSpan( PeriodType type )
    {
        double no = (double) this.getFrequencyOrder() / type.getFrequencyOrder();

        return (int) Math.floor( no );
    }

    // -------------------------------------------------------------------------
    // ISO format methods
    // -------------------------------------------------------------------------

    /**
     * @param dateInterval DateInterval to create period from
     * @return the period.
     */
    public Period createPeriod( DateInterval dateInterval )
    {
        if ( dateInterval == null || dateInterval.getFrom() == null || dateInterval.getTo() == null )
        {
            return null;
        }

        org.hisp.dhis.calendar.Calendar cal = getCalendar();

        final DateTimeUnit from = cal.toIso( dateInterval.getFrom() );
        final DateTimeUnit to = cal.toIso( dateInterval.getTo() );

        return new Period( this, from.toJdkDate(), to.toJdkDate(), getIsoDate( from ) );
    }

    /**
     * Returns an iso8601 formatted string representation of the period
     *
     * @param period Period
     * @return the period as string
     */
    public String getIsoDate( Period period )
    {
        return getIsoDate( createLocalDateUnitInstance( period.getStartDate() ) );
    }

    /**
     * Returns an iso8601 formatted string representation of the dataUnit
     *
     * @param dateTimeUnit Period
     * @return the period as string
     */
    public String getIsoDate( DateTimeUnit dateTimeUnit )
    {
        return getIsoDate( dateTimeUnit, getCalendar() );
    }

    /**
     * Returns an iso8601 formatted string representation of the dataUnit
     *
     * @param dateTimeUnit Period
     * @return the period as string
     */
    public abstract String getIsoDate( DateTimeUnit dateTimeUnit, org.hisp.dhis.calendar.Calendar calendar );

    /**
     * Generates a period based on the given iso8601 formatted string.
     *
     * @param isoDate the iso8601 string.
     * @return the period.
     */
    public Period createPeriod( String isoDate )
    {
        return createPeriod( dateUnitFormat.parse( isoDate ) );
    }

    /**
     * Returns the iso8601 format as a string for this period type.
     *
     * @return the iso8601 format.
     */
    public abstract String getIsoFormat();

    // -------------------------------------------------------------------------
    // CalendarPeriodType
    // -------------------------------------------------------------------------

    /**
     * Returns a Period which is the next of the given Period. Only valid
     * Periods are returned. If the given Period is of different PeriodType than
     * the executing PeriodType, or the given Period is invalid, the returned
     * Period might overlap the given Period.
     *
     * @param period the Period to base the next Period on.
     * @return a Period which is the next of the given Period.
     */
    public final Period getNextPeriod( Period period )
    {
        return getNextPeriod( period, getCalendar() );
    }

    /**
     * Returns the next period determined by the given number of periods.
     *
     * @param period  the Period to base the next Period on.
     * @param periods the number of periods into the future.
     * @return the next period.
     */
    public Period getNextPeriod( Period period, int periods )
    {
        Period nextPeriod = period;

        if ( periods > 0 )
        {
            org.hisp.dhis.calendar.Calendar calendar = getCalendar();

            for ( int i = 0; i < periods; i++ )
            {
                nextPeriod = getNextPeriod( nextPeriod, calendar );
            }
        }

        return nextPeriod;
    }

    /**
     * Returns a Period which is the next of the given Period. Only valid
     * Periods are returned. If the given Period is of different PeriodType than
     * the executing PeriodType, or the given Period is invalid, the returned
     * Period might overlap the given Period.
     *
     * @param period   the Period to base the next Period on.
     * @param calendar the Calendar to use.
     * @return a Period which is the next of the given Period.
     */
    public Period getNextPeriod( Period period, org.hisp.dhis.calendar.Calendar calendar )
    {
        DateTimeUnit dateWithOffset = getDateWithOffset( createLocalDateUnitInstance( period.getStartDate(), calendar ),
            1, calendar );

        return createPeriod( dateWithOffset, calendar );
    }
    
    /**
     * Returns a Period which is the previous of the given Period. Only valid
     * Periods are returned. If the given Period is of different PeriodType than
     * the executing PeriodType, or the given Period is invalid, the returned
     * Period might overlap the given Period.
     *
     * @param period the Period to base the previous Period on.
     * @return a Period which is the previous of the given Period.
     */
    public final Period getPreviousPeriod( Period period )
    {
        return getPreviousPeriod( period, getCalendar() );
    }

    /**
     * Returns the previous period determined by the given number of periods.
     *
     * @param period  the Period to base the previous Period on.
     * @param periods the number of periods into the past.
     * @return the previous period.
     */
    public Period getPreviousPeriod( Period period, int periods )
    {
        Period previousPeriod = period;

        if ( periods > 0 )
        {
            org.hisp.dhis.calendar.Calendar calendar = getCalendar();

            for ( int i = 0; i < periods; i++ )
            {
                previousPeriod = getPreviousPeriod( previousPeriod, calendar );
            }
        }

        return previousPeriod;
    }

    /**
     * Returns a Period which is the previous of the given Period. Only valid
     * Periods are returned. If the given Period is of different PeriodType than
     * the executing PeriodType, or the given Period is invalid, the returned
     * Period might overlap the given Period.
     *
     * @param period the Period to base the previous Period on.
     * @return a Period which is the previous of the given Period.
     */
    public Period getPreviousPeriod( Period period, org.hisp.dhis.calendar.Calendar calendar )
    {
        DateTimeUnit dateWithOffset = getDateWithOffset( createLocalDateUnitInstance( period.getStartDate(), calendar ),
            -1, calendar );
        
        return createPeriod( dateWithOffset, calendar );
    }

    /**
     * Returns the period at the same time of year going back a number of years.
     *
     * @param period    the Period to base the previous Period on.
     * @param yearCount how many years to go back.
     * @return the past year period.
     */
    public Period getPreviousYearsPeriod( Period period, int yearCount )
    {
        Calendar calendar = PeriodType.createCalendarInstance( period.getStartDate() );

        calendar.set( Calendar.YEAR, calendar.get( Calendar.YEAR ) - yearCount );

        return createPeriod( calendar );
    }
    
    /**
     * Offsets the input date with the provided number of periods within the current period type.
     * If the offset number is positive, the date is offset into later periods. When the offset is
     * negative, the date is offset into earlier periods.
     * @param dateTimeUnit for where to start the offset.
     * result in the original date to be returned.
     * @return a new date object that has been offset from the original date passed into the function.
     */
    protected abstract DateTimeUnit getDateWithOffset( DateTimeUnit dateTimeUnit, int offset, org.hisp.dhis.calendar.Calendar calendar );
    
    /**
    * Offsets the input date with the provided number of periods within the current period type.
     * If the offset number is positive, the date is offset into later periods. When the offset is
     * negative, the date is offset into earlier periods.
     * @param date for where to start the offset.
     * result in the original date to be returned.
     * @return a new date object that has been offset from the original date passed into the function.
     */
    public Date getDateWithOffset( Date date, int offset )
    {
        org.hisp.dhis.calendar.Calendar calendar = getCalendar();
        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date, calendar );
        return getDateWithOffset( dateTimeUnit, offset, calendar ).toJdkDate();
    }

    /**
     * Returns true if the period spans more than one calendar year.
     * @return true if the period spans more than one calendar year.
     */
    public boolean spansMultipleCalendarYears()
    {
        return false;
    }

    /**
     * Returns true if the supplied name equals the name of this period type.
     * @param periodTypeName the period type name.
     *
     * @return true if the supplied name equals the name of the period type.
     */
    public boolean equalsName( String periodTypeName )
    {
        return this.getName().equals( periodTypeName );
    }
    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null )
        {
            return false;
        }

        if ( !(o instanceof PeriodType) )
        {
            return false;
        }

        final PeriodType other = (PeriodType) o;

        return getName().equals( other.getName() );
    }

    @Override
    public String toString()
    {
        return "[" + getName() + "]";
    }
}
