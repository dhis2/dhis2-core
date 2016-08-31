
var currentPeriodOffset = 0;

//------------------------------------------------------------------------------
// Period
//------------------------------------------------------------------------------

function displayPeriods()
{
  var periodType = $( "#periodTypeId" ).val();
  var periods = dhis2.period.generator.generateReversedPeriods(periodType, currentPeriodOffset);

  $( "#periodId" ).removeAttr( "disabled" );
  clearListById( "periodId" );

  for ( i in periods )
  {
      addOptionById( "periodId", periods[i].iso, periods[i].name );
  }
}

function displayNextPeriods()
{
  if ( currentPeriodOffset < 0 ) // Cannot display future periods
  {
      currentPeriodOffset++;
      displayPeriods();
  }
}

function displayPreviousPeriods()
{
  currentPeriodOffset--;
  displayPeriods();
}

function displayCompleteness()
{
    var criteria = $( "input[name='criteria']:checked" ).val();
    var dataSetId = $( "#dataSetId" ).val();
    var periodList = byId( "periodId" );
    var periodId = periodList.options.length > 0 ? $( "#periodId" ).val() : null;

    if ( !completenessIsValid( periodId ) )
    {
        return false;
    }

    $( '#reportButton' ).attr( 'disabled', true );

    showLoader();

    var url = "getDataCompleteness.action" + "?periodId=" + periodId + "&criteria=" + criteria + "&dataSetId="
            + dataSetId + "&type=html&" + getDC();

    $( '[name="groupId"]' ).each( function()
    {
    	if ( $( this ).val() != '-1' )
    	{
    		url += "&groupId=" + $( this ).val();
    	}
    } );
    
    $( "#contentDiv" ).load( url, function()
    {
        hideLoader();
        setTableStyles();

        $( '#reportButton').removeAttr( 'disabled' );
    } );
}

function completenessIsValid( periodId )
{
    if ( !selectionTreeSelection.isSelected() )
    {
        setHeaderDelayMessage( i18n_please_select_org_unit );
        return false;
    }

    if ( periodId == null )
    {
        setHeaderDelayMessage( i18n_please_select_period );
        return false;
    }

    return true;
}

function getCompleteness( type )
{
    window.location.href = "getDataCompleteness.action?type=" + type + "&" + getDC();
}

function showAdvancedOptions()
{
	$( "#selectionTree" ).css( "height", "292px" );
	$( "#advancedOptionsLink" ).hide();
	$( "#advancedOptions" ).slideDown();
}
