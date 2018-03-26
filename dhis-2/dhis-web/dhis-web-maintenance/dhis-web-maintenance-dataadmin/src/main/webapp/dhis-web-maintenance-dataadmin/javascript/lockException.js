jQuery(document).ready(function () {
    jQuery("body").bind("oust.selected", ouChanged);
    jQuery("#dataSets").bind("change", dataSetChanged);
    jQuery("#periods").bind("change", periodChanged);
});

function ouChanged( e ) {
    // arguments is only "array-like", so it doesnt have splice
    var args = Array.prototype.slice.call(arguments);
    var selectedOus = args.splice(1, args.length);
    
    jQuery("#organisationUnitId").val(selectedOus.join(','));

    jQuery.getJSON('getDataSets.action?id=' + selectedOus.join(','), function ( data ) {
        jQuery("#dataSets").children().remove();

        if ( data.dataSets.length == 0 ) {
            resetDataSets();
        } else {
            for ( var n in data.dataSets ) {
				jQuery("#dataSets").append("<option value='" +  data.dataSets[n].id + "' periodType='" + data.dataSets[n].periodType + "' allowFuturePeriods='" + data.dataSets[n].allowFuturePeriods + "'>" + data.dataSets[n].name + "</option>");
            }

            jQuery("#dataSets").removeAttr("disabled");
        }

        jQuery("#dataSets").trigger("change");
    });
}

function dataSetChanged( e ) {
    var dataSetId = jQuery("#dataSets option:selected").val();
    jQuery("#periods").children().remove();
	currentPeriodOffset = 0;

    if ( !isNaN(dataSetId) ) {
       displayPeriods();
	   enable('periods');
	   enable('prevPeriod');
	   enable('nextPeriod');
    } else {
        resetPeriods();
        jQuery("#periods").trigger("change");
    }
}

function periodChanged( e ) {
    if ( jQuery("#periods").attr("disabled") ) {
        jQuery("#submit").attr("disabled", true);
    } else {
        jQuery("#submit").removeAttr("disabled");
    }
}

function resetDataSets() {
    jQuery("#dataSets").append("<option>-- Please select an organisation unit with a dataset --</option>").attr("disabled", true);
	disable('prevPeriod');
	disable('nextPeriod');
}

function resetPeriods() {
    jQuery("#periods").append("<option>-- Please select a dataset --</option>").attr("disabled", true);
	disable('prevPeriod');
	disable('nextPeriod');
}

/**
 * Handles the onClick event for the next period button.
 */
var currentPeriodOffset = 0;

function nextPeriodsSelected()
{
	if( currentPeriodOffset < 0 )
	{
		currentPeriodOffset++;
		displayPeriods();
	}
}

/**
 * Handles the onClick event for the previous period button.
 */
function previousPeriodsSelected()
{
	currentPeriodOffset--;
    displayPeriods();
}

/**
 * Generates the period select list options.
 */
function displayPeriods()
{
    var periodType = $( '#dataSets option:selected' ).attr( "periodType" )
    var allowFuturePeriods = $( '#dataSets option:selected' ).attr( "allowFuturePeriods" );
    var periods = dhis2.period.generator.generateReversedPeriods(periodType,currentPeriodOffset);

    if ( allowFuturePeriods == "false" )
    {
        periods = dhis2.period.generator.filterFuturePeriods( periods );
    }

    clearListById( 'periods' );

    for( var i in periods )
    {
        jQuery("#periods").append("<option value='" + periods[i].iso + "' >" + periods[i].name  + "</option>" );
    }
}
