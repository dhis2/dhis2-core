// generatePeriods config object: { boolean offset, boolean filterFuturePeriods, boolean reversePeriods }

function PeriodType()
{
    var monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
		'July', 'August', 'September', 'October', 'November', 'December'],

		format_yyyymmdd = function(date) {
			var y = date.getFullYear(),
				m = new String(date.getMonth() + 1),
				d = new String(date.getDate());
			m = m.length < 2 ? '0' + m : m;
			d = d.length < 2 ? '0' + d : d;
			return y + '-' + m + '-' + d;
		},

		filterFuturePeriods = function( periods ) {
			var array = [],
				now = new Date();

			for ( var i = 0; i < periods.length; i++ )
			{
				if ( new Date( periods[i]['startDate'] ) <= now )
				{
					array.push(periods[i]);
				}
			}

			return array;
		};

    var periodTypes = [];
    periodTypes['Daily'] = new DailyPeriodType( format_yyyymmdd, filterFuturePeriods );
    periodTypes['Weekly'] = new WeeklyPeriodType( format_yyyymmdd, filterFuturePeriods );
    periodTypes['Monthly'] = new MonthlyPeriodType( format_yyyymmdd, monthNames, filterFuturePeriods );
    periodTypes['BiMonthly'] = new BiMonthlyPeriodType( format_yyyymmdd, monthNames, filterFuturePeriods );
    periodTypes['Quarterly'] = new QuarterlyPeriodType( format_yyyymmdd, monthNames, filterFuturePeriods );
    periodTypes['SixMonthly'] = new SixMonthlyPeriodType( monthNames, filterFuturePeriods );
    periodTypes['SixMonthlyApril'] = new SixMonthlyAprilPeriodType( monthNames, filterFuturePeriods );
    periodTypes['Yearly'] = new YearlyPeriodType( format_yyyymmdd, filterFuturePeriods );
    periodTypes['FinancialOct'] = new FinancialOctoberPeriodType( format_yyyymmdd, monthNames, filterFuturePeriods );
    periodTypes['FinancialJuly'] = new FinancialJulyPeriodType( format_yyyymmdd, monthNames, filterFuturePeriods );
    periodTypes['FinancialApril'] = new FinancialAprilPeriodType( format_yyyymmdd, monthNames, filterFuturePeriods );

    this.get = function( key )
    {
        return periodTypes[key];
    };
}

function DailyPeriodType( format_yyyymmdd, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset;
			date = new Date( '01 Jan ' + year );

        while ( date.getFullYear() === year )
        {
            var period = {};
            period['startDate'] = format_yyyymmdd( date );
            period['endDate'] = period['startDate'];
            period['name'] = period['startDate'];
            //period['id'] = 'Daily_' + period['startDate'];
            period['iso'] = period['startDate'].replace( /-/g, '' );
            period['id'] = period['iso'];
            periods.push( period );
            date.setDate( date.getDate() + 1 );
        }

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods.reverse() : periods;

        return periods;
    };
}

function WeeklyPeriodType( format_yyyymmdd, fnFilter )
{
    this.generatePeriods = function( config )
    {
		var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '01 Jan ' + year ),
			day = date.getDay(),
			week = 1;

		if ( day <= 4 )
		{
			date.setDate( date.getDate() - ( day - 1 ) );
		}
		else
		{
			date.setDate( date.getDate() + ( 8 - day ) );
		}

		while ( date.getFullYear() <= year )
		{
			var period = {};
			period['startDate'] = format_yyyymmdd( date );
			//period['id'] = 'Weekly_' + period['startDate'];
			period['iso'] = year + 'W' + week;
            period['id'] = period['iso'];
			date.setDate( date.getDate() + 6 );
			period['endDate'] = format_yyyymmdd( date );
			period['name'] = 'W' + week + ' - ' + period['startDate'] + ' - ' + period['endDate'];
			periods.push( period );
			date.setDate( date.getDate() + 1 );
			week++;
		}

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods.reverse() : periods;

        return periods;
    };
}

function MonthlyPeriodType( format_yyyymmdd, monthNames, fnFilter )
{
	var format_iso = function(date) {
		var y = date.getFullYear(),
			m = new String(date.getMonth() + 1);
		m = m.length < 2 ? '0' + m : m;
		return y + m;
	};

    this.generatePeriods = function( config )
    {
		var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '31 Dec ' + year );

		while ( date.getFullYear() === year )
		{
			var period = {};
			period['endDate'] = format_yyyymmdd( date );
			date.setDate( 1 );
			period['startDate'] = format_yyyymmdd( date );
			period['name'] = monthNames[date.getMonth()] + ' ' + date.getFullYear();
			//period['id'] = 'Monthly_' + period['startDate'];
			period['iso'] = format_iso( date );
            period['id'] = period['iso'];
			periods.push( period );
			date.setDate( 0 );
		}

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // Months are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

function BiMonthlyPeriodType( format_yyyymmdd, monthNames, fnFilter )
{
	var format_iso = function( date ) {
		var y = date.getFullYear(),
			m = new String(date.getMonth() + 1);
		m = m.length < 2 ? '0' + m : m;
		return y + m + 'B';
	};

    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '31 Dec ' + year );

        while ( date.getFullYear() === year )
        {
            var period = {};
            period['endDate'] = format_yyyymmdd( date );
            date.setDate( 0 );
            date.setDate( 1 );
			period['startDate'] = format_yyyymmdd( date );
            period['name'] = monthNames[date.getMonth()] + ' - ' + monthNames[date.getMonth() + 1] + ' ' + date.getFullYear();
            //period['id'] = 'BiMonthly_' + period['startDate'];
            period['iso'] = format_iso( date );
            period['id'] = period['iso'];
            periods.push(period);
            date.setDate( 0 );
        }

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // Bi-months are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

function QuarterlyPeriodType( format_yyyymmdd, monthNames, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '31 Dec ' + year ),
			quarter = 4;

        while ( date.getFullYear() === year )
        {
            var period = {};
            period['endDate'] = format_yyyymmdd( date );
            date.setDate( 0 );
            date.setDate( 0 );
            date.setDate( 1 );
			period['startDate'] = format_yyyymmdd( date );
            period['name'] = monthNames[date.getMonth()] + ' - ' + monthNames[date.getMonth() + 2] + ' ' + date.getFullYear();
            //period['id'] = 'Quarterly_' + period['startDate'];
            period['iso'] = year + 'Q' + quarter;
            period['id'] = period['iso'];
            periods.push(period);
            date.setDate( 0 );
            quarter--;
        }

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // Quarters are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

function SixMonthlyPeriodType( monthNames, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
            offset = parseInt(config.offset),
            isFilter = config.filterFuturePeriods,
            isReverse = config.reversePeriods,
            year = new Date().getFullYear() + offset;

        var period = {};
        period['startDate'] = year + '-01-01';
        period['endDate'] = year + '-06-30';
        period['name'] = monthNames[0] + ' - ' + monthNames[5] + ' ' + year;
        //period['id'] = 'SixMonthly_' + period['startDate'];
        period['iso'] = year + 'S1';
        period['id'] = period['iso'];
        periods.push(period);

        period = {};
        period['startDate'] = year + '-07-01';
        period['endDate'] = year + '-12-31';
        period['name'] = monthNames[6] + ' - ' + monthNames[11] + ' ' + year;
        //period['id'] = 'SixMonthly_' + period['startDate'];
        period['iso'] = year + 'S2';
        period['id'] = period['iso'];
        periods.push(period);

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods.reverse() : periods;

        return periods;
    };
}

function SixMonthlyAprilPeriodType( monthNames, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
            offset = parseInt(config.offset),
            isFilter = config.filterFuturePeriods,
            isReverse = config.reversePeriods,
            year = new Date().getFullYear() + offset;

        var period = {};
        period['startDate'] = year + '-04-01';
        period['endDate'] = year + '-09-30';
        period['name'] = monthNames[3] + ' - ' + monthNames[8] + ' ' + year;
        //period['id'] = 'SixMonthlyApril_' + period['startDate'];
        period['iso'] = year + 'AprilS1';
        period['id'] = period['iso'];
        periods.push(period);

        period = {};
        period['startDate'] = year + '-10-01';
        period['endDate'] = ( year + 1 ) + '-03-31';
        period['name'] = monthNames[9] + ' ' + year + ' - ' + monthNames[2] + ' ' + ( year + 1 );
        //period['id'] = 'SixMonthlyApril_' + period['startDate'];
        period['iso'] = year + 'AprilS2';
        period['id'] = period['iso'];
        periods.push(period);

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods.reverse() : periods;

        return periods;
    };
}

function YearlyPeriodType( format_yyyymmdd, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '31 Dec ' + year );

        while ( ( year - date.getFullYear() ) < 10 )
        {
            var period = {};
            period['endDate'] = format_yyyymmdd( date );
            date.setMonth( 0, 1 );
            period['startDate'] = format_yyyymmdd( date );
            period['name'] = date.getFullYear().toString();
            //period['id'] = 'Yearly_' + period['startDate'];
            period['iso'] = date.getFullYear().toString();
            period['id'] = period['iso'].toString();
            periods.push(period);
            date.setDate(0);
        }

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // Years are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

function FinancialOctoberPeriodType( format_yyyymmdd, monthNames, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '30 Sep ' + ( year + 1 ) );

        for ( var i = 0; i < 10; i++ )
        {
			var period = {};
			period['endDate'] = format_yyyymmdd( date );
			date.setYear( date.getFullYear() - 1 );
			date.setDate( date.getDate() + 1 );
			period['startDate'] = format_yyyymmdd( date );
			period['name'] = monthNames[9] + ' ' + date.getFullYear() + ' - ' + monthNames[8] + ' ' + ( date.getFullYear() + 1 );
			period['id'] = date.getFullYear() + 'Oct';
			periods.push( period );
			date.setDate( date.getDate() - 1 );
		}

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // FinancialOctober periods are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

function FinancialJulyPeriodType( format_yyyymmdd, monthNames, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '30 Jun ' + ( year + 1 ) );

        for ( var i = 0; i < 10; i++ )
        {
			var period = {};
			period['endDate'] = format_yyyymmdd( date );
			date.setYear( date.getFullYear() - 1 );
			date.setDate( date.getDate() + 1 );
			period['startDate'] = format_yyyymmdd( date );
			period['name'] = monthNames[6] + ' ' + date.getFullYear() + ' - ' + monthNames[5] + ' ' + ( date.getFullYear() + 1 );
			period['id'] = date.getFullYear() + 'July';
			periods.push( period );
			date.setDate( date.getDate() - 1 );
		}

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // FinancialJuly periods are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

function FinancialAprilPeriodType( format_yyyymmdd, monthNames, fnFilter )
{
    this.generatePeriods = function( config )
    {
        var periods = [],
			offset = parseInt(config.offset),
			isFilter = config.filterFuturePeriods,
			isReverse = config.reversePeriods,
			year = new Date().getFullYear() + offset,
			date = new Date( '31 Mar ' + ( year + 1 ) );

        for ( var i = 0; i < 10; i++ )
        {
			var period = {};
			period['endDate'] = format_yyyymmdd( date );
			date.setYear( date.getFullYear() - 1 );
			date.setDate( date.getDate() + 1 );
			period['startDate'] = format_yyyymmdd( date );
			period['name'] = monthNames[3] + ' ' + date.getFullYear() + ' - ' + monthNames[2] + ' ' + ( date.getFullYear() + 1 );
			period['id'] = date.getFullYear() + 'April';
			periods.push( period );
			date.setDate( date.getDate() - 1 );
		}

        periods = isFilter ? fnFilter( periods ) : periods;
        periods = isReverse ? periods : periods.reverse();
        // FinancialApril periods are collected backwards. If isReverse is true, then do nothing. Else reverse to correct order and return.

        return periods;
    };
}

