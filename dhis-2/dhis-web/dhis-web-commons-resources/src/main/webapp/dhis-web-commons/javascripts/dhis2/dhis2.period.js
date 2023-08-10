"use strict";

/*
 * Copyright (c) 2004-2014, University of Oslo
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

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */

dhis2.util.namespace( 'dhis2.period' );

dhis2.period.DEFAULT_DATE_FORMAT = "yyyy-mm-dd";

/**
 * A date picker class that allows for creating both simple date pickers, and ranged date pickers.
 *
 * There is probably no reason to use this directly, since on startup, a global variable have been made available:
 *  - dhis2.period.picker   DatePicker object created with system calendar and system date format
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @see <a href="http://keith-wood.name/datepick.html">http://keith-wood.name/datepick.html</a>
 */
dhis2.period.DatePicker = function(calendar, format) {
  if ( typeof calendar === 'undefined' ) {
    if ( typeof dhis2.period.calendar !== 'undefined' ) {
      calendar = dhis2.period.calendar;
    } else {
      throw new Error( 'calendar parameter is required' );
    }
  }

  format = format || dhis2.period.DEFAULT_DATE_FORMAT;

  if ( !(calendar instanceof $.calendars.baseCalendar) ) {
    throw new Error( 'calendar must be instance of $.calendars.baseCalendar' )
  }

  $.extend( this, {
    calendar: calendar,
    format: format,
    defaults: {
      calendar: calendar,
      dateFormat: format,
      showAnim: '',
      maxDate: calendar.today(),
      yearRange: 'c-100:c+100',
      altFormat: 'yyyy-mm-dd'
    }
  } );
};

/**
 * Creates a date picker.
 *
 * @param {jQuery|String|Object} el Element to select on, can be any kind of jQuery selector, or a jqEl
 * @param {boolean} [fromIso] Convert field from ISO 8601 to local calendar
 * @param {boolean} [defaultIsToday] set default date to today
 * @param {Object} [options] Additional options, will be merged with the defaults
 */
dhis2.period.DatePicker.prototype.createInstance = function(el, fromIso, defaultIsToday, options) {
  var self = this;
  var $el = $( el );

  if ( fromIso ) {
    var iso8601 = $.calendars.instance( 'gregorian' );
    var isoDate = iso8601.parseDate( this.format, $el.val() );
    var cDateIsoDate = this.calendar.fromJD( isoDate.toJD() );
    $el.val( this.calendar.formatDate( this.format, cDateIsoDate ) );
  }

  if ( !$el.val() && defaultIsToday ) {
    $el.val( dhis2.period.calendar.formatDate( this.format, dhis2.period.calendar.today() ) );
  }

  var isoFieldId = $el.attr( 'id' );
  $el.attr( 'id', isoFieldId + '-dp' );

  $el.before( $( '<input type="hidden"/>' )
    .attr( {
      id: isoFieldId
    } ) );

  if ( options ) {
    options.altField = '#' + isoFieldId;
  }

  $el.calendarsPicker( $.extend( {}, this.defaults, options ) );
};

/**
 * Formats and sets the formatted date value.
 *
 * @param {String} fieldId field id
 */
dhis2.period.DatePicker.prototype.updateDate = function(fieldId) {
  var $isoField = $( fieldId );
  var $el = $( fieldId + '-dp' );

  if ( $el.length > 0 ) {
    var value = $isoField.val();
    value = value.indexOf('T') ? value.split('T')[0] : value;
    var date = this.calendar.parseDate( 'yyyy-mm-dd', value );
    var localDate = this.calendar.formatDate( this.format, date );

    $el.val( localDate );
  }
};

/**
 * Creates a ranged date picker, keeping two fields in sync.
 *
 * @param {jQuery|String|Object} fromEl From element to select on, can be any kind of jQuery selector, or a jqEl
 * @param {jQuery|String|Object} toEl To element to select on, can be any kind of jQuery selector, or a jqEl
 * @param {Boolean} fromIso Convert fields from ISO 8601 to local calendar
 * @param {Object} options Additional options, will be merged with the defaults
 */
dhis2.period.DatePicker.prototype.createRangedInstance = function(fromEl, toEl, fromIso, options) {
  var mergedOptions = $.extend( {}, this.defaults, options || {} );

  var $fromEl = $( fromEl );
  var $toEl = $( toEl );

  if ( fromIso ) {
    var iso8601 = $.calendars.instance( 'gregorian' );
    var from = iso8601.parseDate( this.format, $fromEl.val() );
    var to = iso8601.parseDate( this.format, $toEl.val() );

    var cDateFrom = this.calendar.fromJD( from.toJD() );
    var cDateTo = this.calendar.fromJD( to.toJD() );

    $fromEl.val( this.calendar.formatDate( this.format, cDateFrom ) );
    $toEl.val( this.calendar.formatDate( this.format, cDateTo ) );
  }

  if ( !$fromEl.val() ) {
    $fromEl.val( dhis2.period.calendar.formatDate( this.format, dhis2.period.calendar.today() ) );
  }

  if ( !$toEl.val() ) {
    $toEl.val( dhis2.period.calendar.formatDate( this.format, dhis2.period.calendar.today() ) );
  }

  mergedOptions.onSelect = function(dates) {
    if ( this.id === $fromEl.attr( 'id' ) ) {
      $toEl.calendarsPicker( "option", "minDate", dates[0] || null );
    }
    else if ( this.id === $toEl.attr( 'id' ) ) {
      $fromEl.calendarsPicker( "option", "maxDate", dates[0] || null );
    }
  };

  $fromEl.calendarsPicker( mergedOptions );

  $toEl.calendarsPicker( $.extend( {}, mergedOptions, {
    maxDate: null
  } ) );

  $fromEl.calendarsPicker( "setDate", $fromEl.calendarsPicker( "getDate" ) );
  $toEl.calendarsPicker( "setDate", $toEl.calendarsPicker( "getDate" ) );
};

/**
 * A period generator that uses a specified calendar chronology to generate DHIS 2 periods.
 *
 * There is probably no reason to use this directly, since on startup, two global variables have been made available:
 *  - dhis2.period.calendar   The currently selected system calendar
 *  - dhis2.period.generator  An instance of this class using the system calendar
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 */
dhis2.period.PeriodGenerator = function(calendar, format) {
  calendar = calendar || dhis2.period.calendar;
  format = format || dhis2.period.DEFAULT_DATE_FORMAT;

  if ( !(calendar instanceof $.calendars.baseCalendar) ) {
    throw new Error( 'calendar must be instance of $.calendars.baseCalendar' )
  }

  $.extend( this, {
    calendar: calendar,
    format: format
  } );

  this.registerGenerator( dhis2.period.DailyGenerator );
  this.registerGenerator( dhis2.period.WeeklyGenerator );
  this.registerGenerator( dhis2.period.WeeklyWednesdayGenerator );
  this.registerGenerator( dhis2.period.WeeklyThursdayGenerator );
  this.registerGenerator( dhis2.period.WeeklySaturdayGenerator );
  this.registerGenerator( dhis2.period.WeeklySundayGenerator );
  this.registerGenerator( dhis2.period.BiWeeklyGenerator );
  this.registerGenerator( dhis2.period.MonthlyGenerator );
  this.registerGenerator( dhis2.period.BiMonthlyGenerator );
  this.registerGenerator( dhis2.period.QuarterlyGenerator );
  this.registerGenerator( dhis2.period.SixMonthlyGenerator );
  this.registerGenerator( dhis2.period.SixMonthlyAprilGenerator );
  this.registerGenerator( dhis2.period.SixMonthlyNovemberGenerator );
  this.registerGenerator( dhis2.period.YearlyGenerator );
  this.registerGenerator( dhis2.period.FinancialAprilGenerator );
  this.registerGenerator( dhis2.period.FinancialJulyGenerator );
  this.registerGenerator( dhis2.period.FinancialOctoberGenerator );
  this.registerGenerator( dhis2.period.FinancialNovemberGenerator );
};

/**
 * Registers a new generator, must be of instance of dhis2.period.BaseGenerator
 *
 * @param {*} klass Class to register, will be checked for type
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.PeriodGenerator.prototype.registerGenerator = function(klass) {
  this.generators = this.generators || {};
  var o = new klass( this.calendar, this.format );

  if ( !(o instanceof dhis2.period.BaseGenerator) ) {
    throw new Error( 'Failed to register new generator class, must be instance of dhis2.period.BaseGenerator.' );
  }

  this.generators[o.name] = o;
};

/**
 * @returns {Array} All available period generators
 */
dhis2.period.PeriodGenerator.prototype.getAll = function() {
  return this.generators;
};

/**
 * @returns {Object} The calendar chronology used for this period generator
 */
dhis2.period.PeriodGenerator.prototype.getCalendar = function() {
  return this.calendar;
};

/**
 * @returns {Object} The date format used for this period generator
 */
dhis2.period.PeriodGenerator.prototype.getDateFormat = function() {
  return this.format;
};

/**
 * @param {String} generator Generator to find
 * @returns {*} Wanted generator if it exists
 */
dhis2.period.PeriodGenerator.prototype.get = function(generator) {
  return this.generators[generator];
};

/**
 * @param {String} generator Generator to use (String)
 * @param {int} offset Offset for generatePeriods
 * @returns {Array} Generated periods as array
 */
dhis2.period.PeriodGenerator.prototype.generatePeriods = function(generator, offset) {
  return this.generators[generator].generatePeriods( offset );
};

/**
 * @param {String} generator Generator to use (String)
 * @param {int} offset Offset for generatePeriods
 * @returns {Array} Generated periods as array
 */
dhis2.period.PeriodGenerator.prototype.generateReversedPeriods = function(generator, offset) {
  return this.reverse( this.generators[generator].generatePeriods( offset ) );
};

/**
 * @param {String} generator Generator to use (String)
 * @param {Array} periods the periods to filter.
 * @param {int} n number of open periods in the future.
 * @param {boolean} checkStartDate
 * @param {boolean} checkEndDate
 * @returns {Array} Generated periods as array
 */
dhis2.period.PeriodGenerator.prototype.filterOpenPeriods = function(generator, periods, n, checkStartDate, checkEndDate) {
  var max = this.generators[generator].datePlusPeriods( this.calendar.today(), n );
  var array = [];
  var today = this.calendar.today();
  var startDate = checkStartDate ? this.calendar.parseDate( 'yyyy-mm-dd', checkStartDate.split( " " )[0] ) : null;
  var endDate = checkEndDate ? this.calendar.parseDate( 'yyyy-mm-dd', checkEndDate.split( " " )[0] ) : null;

  $.each( periods, function() {
    if ( checkStartDate || checkEndDate ) {
      if ( !( ( checkStartDate && this['_startDate'].compareTo( startDate ) < 0  ) || ( checkEndDate && this['_endDate'].compareTo( endDate ) > 0 ) ) ) {
        if ( this['_endDate'].compareTo( max ) < 0 ) {
          array.push( this );
        }
      }
    }
    else if ( this['_endDate'].compareTo( max ) < 0 ) {
      array.push( this );
    }
  } );
  return array;
};

/**
 * Adds the given number of periods to a date.
 *
 * @param {String} generator Generator to use (String)
 * @param {Date} d the starting date.
 * @param {int} n number of periods to add.
 * @returns {Date} the date, n periods later.
 */
dhis2.period.PeriodGenerator.prototype.datePlusPeriods = function(generator, d, n) {
  return this.generators[generator].datePlusPeriods( d, n );
}

/**
 * Convenience method to get Daily generator
 */
dhis2.period.PeriodGenerator.prototype.daily = function(offset) {
  return this.get( 'Daily' ).generatePeriods( offset );
};

/**
 * Convenience method to get Weekly generator
 */
dhis2.period.PeriodGenerator.prototype.weekly = function(offset) {
  return this.get( 'Weekly' ).generatePeriods( offset );
};

/**
 * Convenience method to get Weekly Wednesday generator
 */
dhis2.period.PeriodGenerator.prototype.weeklyWednesday = function(offset) {
  return this.get( 'WeeklyWednesday' ).generatePeriods( offset );
};

/**
 * Convenience method to get Weekly Thursday generator
 */
dhis2.period.PeriodGenerator.prototype.weeklyThursday = function(offset) {
  return this.get( 'WeeklyThursday' ).generatePeriods( offset );
};

/**
 * Convenience method to get Weekly Saturday generator
 */
dhis2.period.PeriodGenerator.prototype.weeklySaturday = function(offset) {
  return this.get( 'WeeklySaturday' ).generatePeriods( offset );
};

/**
 * Convenience method to get Weekly Sunday generator
 */
dhis2.period.PeriodGenerator.prototype.weeklySaturday = function(offset) {
  return this.get( 'WeeklySunday' ).generatePeriods( offset );
};

/**
 * Convenience method to get BiWeekly generator
 */
dhis2.period.PeriodGenerator.prototype.BiWeekly = function(offset) {
    return this.get( 'BiWeekly' ).generatePeriods( offset );
};

/**
 * Convenience method to get Monthly generator
 */
dhis2.period.PeriodGenerator.prototype.monthly = function(offset) {
  return this.get( 'Monthly' ).generatePeriods( offset );
};

/**
 * Convenience method to get BiMonthly generator
 */
dhis2.period.PeriodGenerator.prototype.biMonthly = function(offset) {
  return this.get( 'BiMonthly' ).generatePeriods( offset );
};

/**
 * Convenience method to get Quarterly generator
 */
dhis2.period.PeriodGenerator.prototype.quarterly = function(offset) {
  return this.get( 'Quarterly' ).generatePeriods( offset );
};

/**
 * Convenience method to get SixMonthly generator
 */
dhis2.period.PeriodGenerator.prototype.sixMonthly = function(offset) {
  return this.get( 'SixMonthly' ).generatePeriods( offset );
};

/**
 * Convenience method to get SixMonthlyApril generator
 */
dhis2.period.PeriodGenerator.prototype.sixMonthlyApril = function(offset) {
  return this.get( 'SixMonthlyApril' ).generatePeriods( offset );
};

/**
 * Convenience method to get Yearly generator
 */
dhis2.period.PeriodGenerator.prototype.yearly = function(offset) {
  return this.get( 'Yearly' ).generatePeriods( offset );
};

/**
 * Convenience method to get FinancialOct generator
 */
dhis2.period.PeriodGenerator.prototype.financialOct = function(offset) {
  return this.get( 'FinancialOct' ).generatePeriods( offset );
};

/**
 * Convenience method to get FinancialJuly generator
 */
dhis2.period.PeriodGenerator.prototype.financialJuly = function(offset) {
  return this.get( 'FinancialJuly' ).generatePeriods( offset );
};

/**
 * Convenience method to get FinancialApril generator
 */
dhis2.period.PeriodGenerator.prototype.financialApril = function(offset) {
  return this.get( 'FinancialApril' ).generatePeriods( offset );
};

/**
 * Convenience method to get FinancialNov generator
 */
dhis2.period.PeriodGenerator.prototype.financialNov = function(offset) {
  return this.get( 'FinancialNov' ).generatePeriods( offset );
};

/**
 * Out-of-place reversal of a list of periods
 *
 * @param {Array} periods Periods to reverse
 * @returns {Array} Reversed array
 */
dhis2.period.PeriodGenerator.prototype.reverse = function(periods) {
  return periods.slice( 0 ).reverse();
};

/**
 * Out-of-place filtering of current + future periods
 *
 * @param {Array} periods Periods to filter
 * @return {Array} Filtered periods array
 */
dhis2.period.PeriodGenerator.prototype.filterFuturePeriods = function(periods) {
  var array = [];
  var today = this.calendar.today();

  $.each( periods, function() {
    if ( this['_endDate'].compareTo( today ) < 0 ) {
      array.push( this );
    }
  } );

  return array;
};

/**
 * Out-of-place filtering of future periods
 *
 * @param {Array} periods Periods to filter
 * @return {Array} Filtered periods array
 */
dhis2.period.PeriodGenerator.prototype.filterFuturePeriodsExceptCurrent = function(periods) {
  var array = [];
  var today = this.calendar.today();

  $.each( periods, function() {
    if ( this['_startDate'].compareTo( today ) <= 0 ) {
      array.push( this );
    }
  } );

  return array;
};

dhis2.period.PeriodGenerator.prototype.getPeriodForTheDate = function(date, type) {
  var dateArray = date.split( '-' );
  var year = parseInt( dateArray [0] ), month = parseInt( dateArray [1] ), day = parseInt( dateArray [2] );
  var yearOffset = year - (new Date()).getFullYear();
  var offset = 0, eventDate, allPeriods, periodWithDate = null;

  if ( type === 'FinancialNov' ) {
	  offset = month <= 10 ? 4 : 5;
  } else if ( type === 'FinancialOct' ) {
    offset = month <= 9 ? 4 : 5;
  } else if ( type === 'FinancialJuly' ) {
    offset = month <= 6 ? 4 : 5;
  } else if ( type === 'FinancialApril' ) {
    offset = month <= 3 ? 4 : 5;
  } else if ( type === 'Yearly' ) {
    offset = -5;
  } else if ( type === 'SixMonthlyApril' ) {
    offset = month < 4 ? -1 : 0;
  } else if ( type === 'SixMonthlyNov' ) {
    offset = month < 11 ? -1 : 0;
  } else if ( (type === 'SixMonthly') || (type === 'Quarterly') || (type === 'BiMonthly') || (type === 'Monthly')
    || (type === 'Weekly') || (type === 'WeeklyWednesday') || (type === 'WeeklyThursday') || (type === 'WeeklySaturday') || (type === 'WeeklySunday')
    || (type === 'Daily') ) {
    offset = 0;
  } else {
    return periodWithDate;
  }

  allPeriods = this.generatePeriods( type, yearOffset + offset );

  if ( allPeriods ) {
    for ( var i = 0, sd, ed; i < allPeriods.length; i++ ) {
      sd = moment( allPeriods[i]._startDate.toString(), "YYYY-MM-DD" );
      ed = moment( allPeriods[i]._endDate.toString(), "YYYY-MM-DD" );
      eventDate = moment( date, "YYYY-MM-DD" );
      if ( !(eventDate.isBefore( sd ) || eventDate.isAfter( ed )) ) {
        periodWithDate = {startDate: sd.format( "YYYY-MM-DD" ), endDate: ed.format( "YYYY-MM-DD" )};
        break;
      }
    }
  }

  return periodWithDate;
};

/**
 * Base class for generator classes, should not be instantiated directly.
 *
 * @param {String} name Name of generator
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 */
dhis2.period.BaseGenerator = function(name, calendar, format) {
  if ( !(calendar instanceof $.calendars.baseCalendar) ) {
    throw new Error( 'calendar must be instance of $.calendars.baseCalendar' )
  }

  format = format || dhis2.period.DEFAULT_DATE_FORMAT;

  $.extend( this, {
    name: name,
    calendar: calendar,
    format: format
  } );
};

$.extend( dhis2.period.BaseGenerator.prototype, {
  /**
   * Generate periods from a year offset (offset from current year)
   *
   * @param {int=} offset Year to generate from, offset from current year (default 0)
   * @return {Array} Generated periods using selected offset
   * @access public
   */
  generatePeriods: function(offset) {
    offset = offset || 0;
    return this.$generate( offset );
  },
  /**
   * @param {int} offset Year to generate from, offset from current year (default 0)
   *
   * @return {Array} Generated periods using selected offset
   * @abstract
   * @access protected
   */
  $generate: function(offset) {
    throw new Error( '$generate method not implemented on ' + this.name + ' generator.' );
  },
  /**
   * Adds the given number of periods to a date.
   *
   * @param {Date} d the starting date.
   * @param {int} n number of periods to add.
   * @returns {Date} the date, n periods later.
   */
  datePlusPeriods: function( d, n) {
    n = n || 0;
    return this.$datePlusPeriods( d, n );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Daily periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.DailyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'Daily', calendar, format );
};

dhis2.period.DailyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.DailyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = this.calendar.newDate( year, 1, 1 );

    for ( var day = 1; day <= this.calendar.daysInYear( year ); day++ ) {
      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = startDate.formatDate( this.format );
      period['name'] = startDate.formatDate( this.format );
      period['id'] = 'Daily_' + period['startDate'];
      period['iso'] = startDate.formatDate( "yyyymmdd" );

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( startDate );

      periods.push( period );

      startDate.add( 1, 'd' );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'd' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Weekly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.WeeklyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'Weekly', calendar, format );
};

dhis2.period.WeeklyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.WeeklyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = dhis2.period.getStartDateOfYear( this.calendar, year, 1 );
    var nextYearStartDate = Date.parse( dhis2.period.getStartDateOfYear( this.calendar, year + 1, 1 ) );

    // no reliable way to figure out number of weeks in a year (can differ in different calendars)
    // goes up to 200, but break when week is back to 1
    for ( var week = 1; week < 200; week++ ) {

      // not very elegant, but seems to be best way to get week end, adds a week, then minus 1 day
      var endDate = this.calendar.newDate( startDate ).add( 1, 'w' ).add( -1, 'd' );

      var isNextYearWeek = Date.parse( startDate ) <= nextYearStartDate && nextYearStartDate <= Date.parse( endDate );

      if ( startDate.weekOfYear() === 1 && week > 50 || isNextYearWeek ) {
        break;
      }

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = 'Week ' + week + ' - ' + period['startDate'] + ' - ' + period['endDate'];
      period['id'] = 'Weekly_' + period['startDate'];
      period['iso'] = year + 'W' + week;

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );

      startDate.add( 1, 'w' );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'w' );
  }
} );


/**
 * Calculate the first date of an EPI year base on ISO standard  ( first week always contains 4th Jan )
 * and week types ( week start on Monday or Sunday, etc. )
 * @param calendar The calendar under consideration
 * @param year The year to calculate first date
 * @param startDayOfWeek The week type ( Sunday = 0, Monday = 1, etc. )
 * @returns {Date} The first date of the given year
 */
dhis2.period.getStartDateOfYear = function( calendar, year, startDayOfWeek )
{
    var day4OfYear = calendar.newDate( year, 1, 4 );

    var startDate = day4OfYear;

    var dayDiff = day4OfYear.dayOfWeek() - startDayOfWeek;

    if ( dayDiff > 0 )
    {
        startDate.add( 0 - dayDiff, 'd' );
    }
    else if ( dayDiff < 0 )
    {
        startDate.add( 0 - dayDiff - 7, 'd' );
    }

    return startDate;
}

/**
 * Implementation of dhis2.period.BaseGenerator that generates Weekly Wednesday periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.WeeklyWednesdayGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'WeeklyWednesday', calendar, format );
};

dhis2.period.WeeklyWednesdayGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.WeeklyWednesdayGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = dhis2.period.getStartDateOfYear( this.calendar, year, 3 );
    var nextYearStartDate = Date.parse( dhis2.period.getStartDateOfYear( this.calendar, year + 1, 1 ) );

    // no reliable way to figure out number of weeks in a year (can differ in different calendars)
    // goes up to 200, but break when week is back to 1
    for ( var week = 1; week < 200; week++ ) {

      // not very elegant, but seems to be best way to get week end, adds a week, then minus 1 day
      var endDate = this.calendar.newDate( startDate ).add( 1, 'w' ).add( -1, 'd' );

      var isNextYearWeek = Date.parse( startDate ) <= nextYearStartDate && nextYearStartDate <= Date.parse( endDate );

      if ( startDate.weekOfYear() === 1 && week > 50 || isNextYearWeek ) {
        break;
      }

      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = 'Week ' + week + ' - ' + period['startDate'] + ' - ' + period['endDate'];
      period['id'] = 'WeeklyWednesday_' + period['startDate'];
      period['iso'] = year + 'WedW' + week;

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );

      startDate.add( 1, 'w' );

    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'w' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Weekly Thursday periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.WeeklyThursdayGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'WeeklyThursday', calendar, format );
};

dhis2.period.WeeklyThursdayGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.WeeklyThursdayGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = dhis2.period.getStartDateOfYear( this.calendar, year, 4 );
    var nextYearStartDate = Date.parse( dhis2.period.getStartDateOfYear( this.calendar, year + 1, 1 ) );

    // no reliable way to figure out number of weeks in a year (can differ in different calendars)
    // goes up to 200, but break when week is back to 1
    for ( var week = 1; week < 200; week++ ) {

      // not very elegant, but seems to be best way to get week end, adds a week, then minus 1 day
      var endDate = this.calendar.newDate( startDate ).add( 1, 'w' ).add( -1, 'd' );

      var isNextYearWeek = Date.parse( startDate ) <= nextYearStartDate && nextYearStartDate <= Date.parse( endDate );

      if ( startDate.weekOfYear() === 1 && week > 50 || isNextYearWeek ) {
        break;
      }

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = 'Week ' + week + ' - ' + period['startDate'] + ' - ' + period['endDate'];
      period['id'] = 'WeeklyThursday_' + period['startDate'];
      period['iso'] = year + 'ThuW' + week;

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );

      startDate.add( 1, 'w' );

    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'w' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Weekly Saturday periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.WeeklySaturdayGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'WeeklySaturday', calendar, format );
};

dhis2.period.WeeklySaturdayGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.WeeklySaturdayGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = dhis2.period.getStartDateOfYear( this.calendar, year, 6 );
    var nextYearStartDate = Date.parse( dhis2.period.getStartDateOfYear( this.calendar, year + 1, 1 ) );

    // no reliable way to figure out number of weeks in a year (can differ in different calendars)
    // goes up to 200, but break when week is back to 1
    for ( var week = 1; week < 200; week++ ) {

      // not very elegant, but seems to be best way to get week end, adds a week, then minus 1 day
      var endDate = this.calendar.newDate( startDate ).add( 1, 'w' ).add( -1, 'd' );

      var isNextYearWeek = Date.parse( startDate ) <= nextYearStartDate && nextYearStartDate <= Date.parse( endDate );

      if ( startDate.weekOfYear() === 1 && week > 50 || isNextYearWeek ) {
        break;
      }

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = 'Week ' + week + ' - ' + period['startDate'] + ' - ' + period['endDate'];
      period['id'] = 'WeeklySaturday_' + period['startDate'];
      period['iso'] = year + 'SatW' + week;

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );

      startDate.add( 1, 'w' );

    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'w' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Weekly Sunday periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.WeeklySundayGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'WeeklySunday', calendar, format );
};

dhis2.period.WeeklySundayGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.WeeklySundayGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = dhis2.period.getStartDateOfYear( this.calendar, year, 7 );
    var nextYearStartDate = Date.parse( dhis2.period.getStartDateOfYear( this.calendar, year + 1, 1 ) );

    // no reliable way to figure out number of weeks in a year (can differ in different calendars)
    // goes up to 200, but break when week is back to 1
    for ( var week = 1; week < 200; week++ ) {

      // not very elegant, but seems to be best way to get week end, adds a week, then minus 1 day
      var endDate = this.calendar.newDate( startDate ).add( 1, 'w' ).add( -1, 'd' );

      var isNextYearWeek = Date.parse( startDate ) <= nextYearStartDate && nextYearStartDate <= Date.parse( endDate );

      if ( startDate.weekOfYear() === 1 && week > 50 || isNextYearWeek ) {
        break;
      }

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = 'Week ' + week + ' - ' + period['startDate'] + ' - ' + period['endDate'];
      period['id'] = 'WeeklySunday_' + period['startDate'];
      period['iso'] = year + 'SunW' + week;

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );

      startDate.add( 1, 'w' );

      if ( startDate.weekOfYear() === 1 && week > 50 ) {
        break;
      }
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'w' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates BiWeekly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.BiWeeklyGenerator = function(calendar, format) {
    dhis2.period.BaseGenerator.call( this, 'BiWeekly', calendar, format );
};

dhis2.period.BiWeeklyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.BiWeeklyGenerator.prototype, {
    $generate: function(offset) {
        var year = offset + this.calendar.today().year();
        var periods = [];

        var startDate = dhis2.period.getStartDateOfYear( this.calendar, year, 1 );

        // no reliable way to figure out number of weeks in a year (can differ in different calendars)
        // goes up to 200, but break when week is back to 1
        for ( var biWeek = 1; biWeek < 200; biWeek++ ) {
            var period = {};
            period['startDate'] = startDate.formatDate( this.format );

            // add 2 weeks and remove 1 day
            var endDate = this.calendar.newDate( startDate ).add( 2, 'w' ).add( -1, 'd' );

            period['endDate'] = endDate.formatDate( this.format );
            period['name'] = 'Bi-Week ' + biWeek + ' - ' + period['startDate'] + ' - ' + period['endDate'];
            period['id'] = 'Bi-week' + period['startDate'];
            period['iso'] = year + 'BiW' + biWeek;

            period['_startDate'] = this.calendar.newDate( startDate );
            period['_endDate'] = this.calendar.newDate( endDate );

            periods.push( period );

            startDate.add( 2, 'w' );

            if ( ( startDate.weekOfYear() === 1 || startDate.weekOfYear() === 2 ) && biWeek > 25 ) {
                break;
            }
        }

        return periods;
    },
    $datePlusPeriods: function(d, n) {
        return d.add( n, 'w' );
    }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Monthly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.MonthlyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'Monthly', calendar, format );
};

dhis2.period.MonthlyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.MonthlyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    for ( var month = 1; month <= getHMISMonthsInYear( this.calendar, year ); month++ ) {
      var startDate = this.calendar.newDate( year, month, 1 );
      var endDate = this.calendar.newDate( startDate ).set( startDate.daysInMonth( month ), 'd' );

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' ' + year;
      period['id'] = 'Monthly_' + period['startDate'];
      period['iso'] = startDate.formatDate( "yyyymm" );

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'm' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates BiMonthly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.BiMonthlyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'BiMonthly', calendar, format );
};

dhis2.period.BiMonthlyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.BiMonthlyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    for ( var month = 1, idx = 1; month <= this.calendar.monthsInYear( year ); month += 2, idx++ ) {
      var startDate = this.calendar.newDate( year, month, 1 );
      var endDate = this.calendar.newDate( startDate ).set( month + 1, 'm' );
      endDate.set( endDate.daysInMonth( month + 1 ), 'd' );

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' - ' + getMonthTranslation( endDate.formatDate( "MM" ) ) + ' ' + year;
      period['id'] = 'BiMonthly_' + period['startDate'];
      period['iso'] = startDate.formatDate( "yyyy" ) + '0' + idx + 'B';

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return this.calendar.today().add( n * 2, 'm' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Quarterly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.QuarterlyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'Quarterly', calendar, format );
};

dhis2.period.QuarterlyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.QuarterlyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    for ( var month = 1, idx = 1; month <= getHMISMonthsInYear( this.calendar, year ); month += 3, idx++ ) {
      var startDate = this.calendar.newDate( year, month, 1 );
      var endDate = this.calendar.newDate( startDate ).set( month + 2, 'm' );
      endDate.set( endDate.daysInMonth( month + 2 ), 'd' );

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' - ' + getMonthTranslation( endDate.formatDate( "MM" ) ) + ' ' + year;
      period['id'] = 'Quarterly_' + period['startDate'];
      period['iso'] = startDate.formatDate( "yyyy" ) + 'Q' + idx;

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return this.calendar.today().add( n * 3, 'm' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates SixMonthly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.SixMonthlyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'SixMonthly', calendar, format );
};

dhis2.period.SixMonthlyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.SixMonthlyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = this.calendar.newDate( year, 1, 1 );
    var endDate = this.calendar.newDate( startDate ).set( 6, 'm' );
    endDate.set( endDate.daysInMonth( 6 ), 'd' );

    var period = {};
    period['startDate'] = startDate.formatDate( this.format );
    period['endDate'] = endDate.formatDate( this.format );
    period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' - ' + getMonthTranslation( endDate.formatDate( "MM" ) ) + ' ' + year;
    period['id'] = 'SixMonthly_' + period['startDate'];
    period['iso'] = startDate.formatDate( "yyyy" ) + 'S1';

    period['_startDate'] = this.calendar.newDate( startDate );
    period['_endDate'] = this.calendar.newDate( endDate );

    periods.push( period );

    startDate = this.calendar.newDate( year, 7, 1 );
    endDate = this.calendar.newDate( startDate ).set( this.calendar.monthsInYear( year ), 'm' );
    endDate.set( endDate.daysInMonth( 12 ), 'd' );

    period = {};
    period['startDate'] = startDate.formatDate( this.format );
    period['endDate'] = endDate.formatDate( this.format );
    period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' - ' + getMonthTranslation( endDate.formatDate( "MM" ) ) + ' ' + year;
    period['id'] = 'SixMonthly_' + period['startDate'];
    period['iso'] = startDate.formatDate( "yyyy" ) + 'S2';

    period['_startDate'] = this.calendar.newDate( startDate );
    period['_endDate'] = this.calendar.newDate( endDate );

    periods.push( period );

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return this.calendar.today().add( n * 6, 'm' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates SixMonthlyApril periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.SixMonthlyAprilGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'SixMonthlyApril', calendar, format );
};

dhis2.period.SixMonthlyAprilGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.SixMonthlyAprilGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = this.calendar.newDate( year, 4, 1 );
    var endDate = this.calendar.newDate( startDate ).set( 9, 'm' );
    endDate.set( endDate.daysInMonth( 9 ), 'd' );

    var period = {};
    period['startDate'] = startDate.formatDate( this.format );
    period['endDate'] = endDate.formatDate( this.format );
    period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' - ' + getMonthTranslation( endDate.formatDate( "MM" ) ) + ' ' + year;
    period['id'] = 'SixMonthlyApril_' + period['startDate'];
    period['iso'] = startDate.formatDate( "yyyy" ) + 'AprilS1';

    period['_startDate'] = this.calendar.newDate( startDate );
    period['_endDate'] = this.calendar.newDate( endDate );

    periods.push( period );

    startDate = this.calendar.newDate( year, 10, 1 );
    endDate = this.calendar.newDate( startDate ).set( startDate.year() + 1, 'y' ).set( 3, 'm' );
    endDate.set( endDate.daysInMonth( endDate.month() ), 'd' );

    period = {};
    period['startDate'] = startDate.formatDate( this.format );
    period['endDate'] = endDate.formatDate( this.format );
    period['name'] = startDate.formatDate( "MM yyyy" ) + ' - ' + endDate.formatDate( 'MM yyyy' );
    period['id'] = 'SixMonthlyApril_' + period['startDate'];
    period['iso'] = startDate.formatDate( "yyyy" ) + 'AprilS2';

    period['_startDate'] = this.calendar.newDate( startDate );
    period['_endDate'] = this.calendar.newDate( endDate );

    periods.push( period );

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return this.calendar.today().add( n * 6, 'm' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates SixMonthlyNovember periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.SixMonthlyNovemberGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'SixMonthlyNov', calendar, format );
};

dhis2.period.SixMonthlyNovemberGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.SixMonthlyNovemberGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = this.calendar.newDate( year, 11, 1 );
    var endDate = this.calendar.newDate( startDate ).set( year + 1, 'y').set( 4, 'm' );
    endDate.set( endDate.daysInMonth( 4 ), 'd' );

    var period = {};
    period['startDate'] = startDate.formatDate( this.format );
    period['endDate'] = endDate.formatDate( this.format );
    period['name'] = getMonthTranslation( startDate.formatDate( "MM yyyy" ) ) + ' - ' + getMonthTranslation( endDate.formatDate( "MM yyyy" ) );// + ' ' + year;
    period['id'] = 'SixMonthlyNov_' + period['startDate'];
    period['iso'] = endDate.formatDate( "yyyy" ) + 'NovS1';

    period['_startDate'] = this.calendar.newDate( startDate );
    period['_endDate'] = this.calendar.newDate( endDate );

    periods.push( period );

    startDate = this.calendar.newDate( year + 1, 5, 1 );
    endDate = this.calendar.newDate( startDate ).set( startDate.year(), 'y' ).set( 10, 'm' );
    endDate.set( endDate.daysInMonth( endDate.month() ), 'd' );

    period = {};
    period['startDate'] = startDate.formatDate( this.format );
    period['endDate'] = endDate.formatDate( this.format );
    period['name'] = startDate.formatDate( "MM yyyy" ) + ' - ' + endDate.formatDate( 'MM yyyy' );
    period['id'] = 'SixMonthlyNov_' + period['startDate'];
    period['iso'] = startDate.formatDate( "yyyy" ) + 'NovS2';

    period['_startDate'] = this.calendar.newDate( startDate );
    period['_endDate'] = this.calendar.newDate( endDate );

    periods.push( period );

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return this.calendar.today().add( n * 6, 'm' );
  }
} );

/**
 * Implementation of dhis2.period.BaseGenerator that generates Yearly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.YearlyGenerator = function(calendar, format) {
  dhis2.period.BaseGenerator.call( this, 'Yearly', calendar, format );
};

dhis2.period.YearlyGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.YearlyGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    // generate 11 years, thisYear +/- 5 years
    for ( var i = -5; i < 6; i++ ) {
      var startDate = this.calendar.newDate( year + i, 1, 1 );
      var endDate = this.calendar.newDate( startDate ).set( this.calendar.monthsInYear( year + i ), 'm' );
      endDate.set( endDate.daysInMonth( endDate.month() ), 'd' );

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = startDate.formatDate( "yyyy" );
      period['id'] = 'Yearly_' + period['startDate'];
      period['iso'] = startDate.formatDate( "yyyy" );

      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'y' );
  }
} );

/**
 * Base class for financial monthly offset generator classes, should not be instantiated directly.
 *
 * @param {String} name Name of generator
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @param {int} monthOffset Month offset to use as base for generating financial periods, 1 - 12
 * @param {String} monthShortName Short name to use for generated period names
 * @constructor
 * @augments dhis2.period.BaseGenerator
 * @see dhis2.period.BaseGenerator
 */
dhis2.period.FinancialBaseGenerator = function(name, calendar, format, monthOffset, monthShortName) {
  dhis2.period.BaseGenerator.call( this, name, calendar, format );

  $.extend( this, {
    monthOffset: monthOffset,
    monthShortName: monthShortName
  } );
};

dhis2.period.FinancialBaseGenerator.prototype = Object.create( dhis2.period.BaseGenerator.prototype );

$.extend( dhis2.period.FinancialBaseGenerator.prototype, {
  $generate: function(offset) {
    var year = offset + this.calendar.today().year();
    var periods = [];

    var startDate = this.calendar.newDate( year - 5, this.monthOffset, 1 );

    // generate 11 years, thisYear +/- 5 years
    for ( var i = 1; i < 12; i++ ) {
      var endDate = this.calendar.newDate( startDate ).add( 1, 'y' ).add( -1, 'd' );

      var period = {};
      period['startDate'] = startDate.formatDate( this.format );
      period['endDate'] = endDate.formatDate( this.format );
      period['name'] = getMonthTranslation( startDate.formatDate( "MM" ) ) + ' ' + startDate.year() + ' - ' + getMonthTranslation( endDate.formatDate( "MM" ) ) + ' ' + endDate.year();
      period['id'] = 'Financial' + this.monthShortName + '_' + period['startDate'];

      var isoDate = this.name === 'FinancialNov' ? endDate.formatDate( "yyyy" ) : startDate.formatDate( "yyyy" );

      period['iso'] = isoDate + this.monthShortName;
      period['_startDate'] = this.calendar.newDate( startDate );
      period['_endDate'] = this.calendar.newDate( endDate );

      periods.push( period );
      startDate.add( 1, 'y' );
    }

    return periods;
  },
  $datePlusPeriods: function(d, n) {
    return d.add( n, 'y' );
  }
} );

/**
 * Implementation of dhis2.period.FinancialBaseGenerator that generates FinancialApril periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.FinancialBaseGenerator
 * @see dhis2.period.BaseGenerator
 * @see dhis2.period.FinancialBaseGenerator
 */
dhis2.period.FinancialAprilGenerator = function(calendar, format) {
  dhis2.period.FinancialBaseGenerator.call( this, 'FinancialApril', calendar, format, 4, 'April' );
};

dhis2.period.FinancialAprilGenerator.prototype = Object.create( dhis2.period.FinancialBaseGenerator.prototype );

/**
 * Implementation of dhis2.period.FinancialBaseGenerator that generates FinancialJuly periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.FinancialBaseGenerator
 * @see dhis2.period.BaseGenerator
 * @see dhis2.period.FinancialBaseGenerator
 */
dhis2.period.FinancialJulyGenerator = function(calendar, format) {
  dhis2.period.FinancialBaseGenerator.call( this, 'FinancialJuly', calendar, format, 7, 'July' );
};

dhis2.period.FinancialJulyGenerator.prototype = Object.create( dhis2.period.FinancialBaseGenerator.prototype );

/**
 * Implementation of dhis2.period.FinancialBaseGenerator that generates FinancialOctober periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.FinancialBaseGenerator
 * @see dhis2.period.BaseGenerator
 * @see dhis2.period.FinancialBaseGenerator
 */
dhis2.period.FinancialOctoberGenerator = function(calendar, format) {
  dhis2.period.FinancialBaseGenerator.call( this, 'FinancialOct', calendar, format, 10, 'Oct' );
};

dhis2.period.FinancialOctoberGenerator.prototype = Object.create( dhis2.period.FinancialBaseGenerator.prototype );

/**
 * Implementation of dhis2.period.FinancialBaseGenerator that generates FinancialNovember periods
 *
 * @param {$.calendars.baseCalendar} calendar Calendar to use, this must come from $.calendars.instance(chronology).
 * @param {String} format Date format to use for formatting, will default to ISO 8601
 * @constructor
 * @augments dhis2.period.FinancialBaseGenerator
 * @see dhis2.period.BaseGenerator
 * @see dhis2.period.FinancialBaseGenerator
 */
dhis2.period.FinancialNovemberGenerator = function(calendar, format) {
  dhis2.period.FinancialBaseGenerator.call( this, 'FinancialNov', calendar, format, 11, 'Nov' );
};

dhis2.period.FinancialNovemberGenerator.prototype = Object.create( dhis2.period.FinancialBaseGenerator.prototype );

/**
 * Convenience method to get DHIS2/HMIS months in a year
 */
function getHMISMonthsInYear(calendar, year) {

  var monthsInYear = calendar.monthsInYear( year );

  if ( $.calendars.calendars.ethiopian && calendar instanceof $.calendars.calendars.ethiopian ) {
    monthsInYear = monthsInYear - 1;
  }

  return monthsInYear;
}

/**
 * Convenience method to get month translations which are fetched by main.vm
 */
function getMonthTranslation(monthName) {
  //todo: convert into some kind of JSON. Potentially this JSON can be fetched from api/i18n/calendar

  switch ( monthName ) {
    case 'January':
      return typeof i18n_month_january !== 'undefined' ? i18n_month_january : monthName;
      break;
    case 'February':
      return typeof i18n_month_february !== 'undefined' ? i18n_month_february : monthName;
      break;
    case 'March':
      return typeof i18n_month_march !== 'undefined' ? i18n_month_march : monthName;
      break;
    case 'April':
      return typeof i18n_month_april !== 'undefined' ? i18n_month_april : monthName;
      break;
    case 'May':
      return typeof i18n_month_may !== 'undefined' ? i18n_month_may : monthName;
      break;
    case 'June':
      return typeof i18n_month_june !== 'undefined' ? i18n_month_june : monthName;
      break;
    case 'July':
      return typeof i18n_month_july !== 'undefined' ? i18n_month_july : monthName;
      break;
    case 'August':
      return typeof i18n_month_august !== 'undefined' ? i18n_month_august : monthName;
      break;
    case 'September':
      return typeof i18n_month_september !== 'undefined' ? i18n_month_september : monthName;
      break;
    case 'October':
      return typeof i18n_month_october !== 'undefined' ? i18n_month_october : monthName;
      break;
    case 'November':
      return typeof i18n_month_november !== 'undefined' ? i18n_month_november : monthName;
      break;
    case 'December':
      return typeof i18n_month_december !== 'undefined' ? i18n_month_december : monthName;
      break;
    default:
      return monthName;
  }
};
