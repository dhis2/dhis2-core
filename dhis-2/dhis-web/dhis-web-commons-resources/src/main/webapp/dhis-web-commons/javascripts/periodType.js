function PeriodType()
{
    var dateFormat = 'yyyy-MM-dd';

    var periodTypes = [];
    periodTypes['Daily'] = new DailyPeriodType( dateFormat );
    periodTypes['Weekly'] = new WeeklyPeriodType( dateFormat );
    periodTypes['Monthly'] = new MonthlyPeriodType( dateFormat );
    periodTypes['BiMonthly'] = new BiMonthlyPeriodType( dateFormat );
    periodTypes['Quarterly'] = new QuarterlyPeriodType( dateFormat );
    periodTypes['SixMonthly'] = new SixMonthlyPeriodType( dateFormat );
    periodTypes['SixMonthlyApril'] = new SixMonthlyAprilPeriodType( dateFormat );
    periodTypes['Yearly'] = new YearlyPeriodType( dateFormat );
    periodTypes['FinancialOct'] = new FinancialOctoberPeriodType( dateFormat );
    periodTypes['FinancialJuly'] = new FinancialJulyPeriodType( dateFormat );
    periodTypes['FinancialApril'] = new FinancialAprilPeriodType( dateFormat );

    this.get = function( key )
    {
        return periodTypes[key];
    };

    this.reverse = function( array )
    {
        var reversed = [];
        var i = 0;

        for ( var j = array.length - 1; j >= 0; j-- )
        {
            reversed[i++] = array[j];
        }

        return reversed;
    };

    this.filterFuturePeriods = function( periods )
    {
        var array = [];
        var i = 0;

        var now = new Date().getTime();

        for ( var j = 0; j < periods.length; j++ )
        {
            if ( $.date( periods[j]['endDate'], dateFormat ).date().getTime() <= now )
            {
                array[i++] = periods[j];
            }
        }

        return array;
    };
    
    this.filterFuturePeriodsExceptCurrent = function( periods )
    {
        var array = [];
        var i = 0;

        var now = new Date().getTime();

        for ( var j = 0; j < periods.length; j++ )
        {
            if ( $.date( periods[j]['startDate'], dateFormat ).date().getTime() <= now )
            {
                array[i++] = periods[j];
            }
        }

        return array;
    };
}

function DailyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-01-01', dateFormat );
        var i = 0;

        while ( startDate.date().getFullYear() <= year )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = startDate.format( dateFormat );
            period['name'] = startDate.format( dateFormat );
            period['id'] = 'Daily_' + period['startDate'];
            period['iso'] = startDate.format( 'yyyyMMdd' );
            periods[i] = period;

            startDate.adjust( 'D', +1 );
            i++;
        }

        return periods;
    };
}

function WeeklyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-01-01', dateFormat );
        var day = startDate.date().getDay();
        var i = 0;

        if ( day == 0 ) // Sunday (0), forward to Monday
        {
            startDate.adjust( 'D', +1 );
        }
        else if ( day <= 4 ) // Monday - Thursday, rewind to Monday
        {
            startDate.adjust( 'D', ( ( day - 1 ) * -1 ) );
        }
        else
        // Friday - Saturday, forward to Monday
        {
            startDate.adjust( 'D', ( 8 - day ) );
        }
        
        var endDate = startDate.clone().adjust( 'D', +6 );

        // Include all weeks where Thursday falls in same year
        
        while ( startDate.clone().adjust( 'D', 3 ).date().getFullYear() <= year )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] = 'W' + ( i + 1 ) + ' - ' + startDate.format( dateFormat ) + " - " + endDate.format( dateFormat );
            period['id'] = 'Weekly_' + period['startDate'];
            period['iso'] = year + 'W' + ( i + 1 );
            periods[i] = period;

            startDate.adjust( 'D', +7 );
            endDate = startDate.clone().adjust( 'D', +6 );
            i++;
        }

        return periods;
    };
}

function MonthlyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-01-01', dateFormat );
        var endDate = startDate.clone().adjust( 'M', +1 ).adjust( 'D', -1 );
        var i = 0;

        while ( startDate.date().getFullYear() == year )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] = monthNames[i] + ' ' + year;
            period['id'] = 'Monthly_' + period['startDate'];
            period['iso'] = startDate.format( 'yyyyMM' );
            periods[i] = period;

            startDate.adjust( 'M', +1 );
            endDate = startDate.clone().adjust( 'M', +1 ).adjust( 'D', -1 );
            i++;
        }

        return periods;
    };
}

function BiMonthlyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-01-01', dateFormat );
        var endDate = startDate.clone().adjust( 'M', +2 ).adjust( 'D', -1 );
        var i = 0;
        var j = 0;

        while ( startDate.date().getFullYear() == year )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] = monthNames[i] + ' - ' + monthNames[i + 1] + ' ' + year;
            period['id'] = 'BiMonthly_' + period['startDate'];
            period['iso'] = startDate.format( 'yyyyMM' ) + 'B';
            periods[j] = period;

            startDate.adjust( 'M', +2 );
            endDate = startDate.clone().adjust( 'M', +2 ).adjust( 'D', -1 );
            i += 2;
            j++;
        }

        return periods;
    };
}

function QuarterlyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-01-01', dateFormat );
        var endDate = startDate.clone().adjust( 'M', +3 ).adjust( 'D', -1 );
        var i = 0;
        var j = 0;

        while ( startDate.date().getFullYear() == year )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] = monthNames[i] + ' - ' + monthNames[i + 2] + ' ' + year;
            period['id'] = 'Quarterly_' + period['startDate'];
            period['iso'] = year + 'Q' + ( j + 1 );
            periods[j] = period;

            startDate.adjust( 'M', +3 );
            endDate = startDate.clone().adjust( 'M', +3 ).adjust( 'D', -1 );
            i += 3;
            j++;
        }

        return periods;
    };
}

function SixMonthlyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;

        var period = [];
        period['startDate'] = year + '-01-01';
        period['endDate'] = year + '-06-30';
        period['name'] = monthNames[0] + ' - ' + monthNames[5] + ' ' + year;
        period['id'] = 'SixMonthly_' + period['startDate'];
        period['iso'] = year + 'S1';
        periods[0] = period;

        period = [];
        period['startDate'] = year + '-07-01';
        period['endDate'] = year + '-12-31';
        period['name'] = monthNames[6] + ' - ' + monthNames[11] + ' ' + year;
        period['id'] = 'SixMonthly_' + period['startDate'];
        period['iso'] = year + 'S2';
        periods[1] = period;

        return periods;
    };
}

function SixMonthlyAprilPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;

        var period = [];
        period['startDate'] = year + '-04-01';
        period['endDate'] = year + '-09-30';
        period['name'] = monthNames[3] + ' - ' + monthNames[8] + ' ' + year;
        period['id'] = 'SixMonthlyApril_' + period['startDate'];
        period['iso'] = year + 'AprilS1';
        periods[0] = period;

        period = [];
        period['startDate'] = year + '-10-01';
        period['endDate'] = ( year + 1 ) + '-03-31';
        period['name'] = monthNames[9] + ' ' + year + ' - ' + monthNames[2] + ' ' + ( year + 1 );
        period['id'] = 'SixMonthlyApril_' + period['startDate'];
        period['iso'] = year + 'AprilS2';
        periods[1] = period;

        return periods;
    };
}

function YearlyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-01-01', dateFormat ).adjust( 'Y', -5 );
        var endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );

        for ( var i = 0; i < 11; i++ )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] = startDate.date().getFullYear();
            period['id'] = 'Yearly_' + period['startDate'];
            period['iso'] = startDate.date().getFullYear();
            periods[i] = period;

            startDate.adjust( 'Y', +1 );
            endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );
        }

        return periods;
    };
}

function FinancialOctoberPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-10-01', dateFormat ).adjust( 'Y', -5 );
        var endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );

        for ( var i = 0; i < 11; i++ )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] =  monthNames[9] + ' ' +  startDate.date().getFullYear() + '-' + monthNames[8] + ' ' + (startDate.date().getFullYear() +1 );
            period['id'] = 'FinancialOct_' + period['startDate'];
            period['iso'] = startDate.date().getFullYear() + 'Oct';
            periods[i] = period;

            startDate.adjust( 'Y', +1 );
            endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );
        }

        return periods;
    };
}

function FinancialJulyPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-07-01', dateFormat ).adjust( 'Y', -5 );
        var endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );

        for ( var i = 0; i < 11; i++ )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] =  monthNames[6] + ' ' +  startDate.date().getFullYear() + '-' + monthNames[5] + ' ' + (startDate.date().getFullYear() +1 );
            period['id'] = 'FinancialJuly_' + period['startDate'];
            period['iso'] = startDate.date().getFullYear() + 'July';
            periods[i] = period;

            startDate.adjust( 'Y', +1 );
            endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );
        }

        return periods;
    };
}

function FinancialAprilPeriodType( dateFormat )
{
    this.generatePeriods = function( offset )
    {
        var periods = [];
        var year = new Date().getFullYear() + offset;
        var startDate = $.date( year + '-04-01', dateFormat ).adjust( 'Y', -5 );
        var endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );

        for ( var i = 0; i < 11; i++ )
        {
            var period = [];
            period['startDate'] = startDate.format( dateFormat );
            period['endDate'] = endDate.format( dateFormat );
            period['name'] =  monthNames[3] + ' ' +  startDate.date().getFullYear() + '-' + monthNames[2] + ' ' + (startDate.date().getFullYear() +1 );
            period['id'] = 'FinancialApril_' + period['startDate'];
            period['iso'] = startDate.date().getFullYear() + 'April';
            periods[i] = period;

            startDate.adjust( 'Y', +1 );
            endDate = startDate.clone().adjust( 'Y', +1 ).adjust( 'D', -1 );
        }

        return periods;
    };
}
