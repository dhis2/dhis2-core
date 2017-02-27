var startDate;
var endDate;
var validationRuleGroupId;
var sendNotifications;
var organisationUnitId;

function organisationUnitSelected( ids )
{
    organisationUnitId = ids[0];
}

function toIso(date) {
  var iso8601 = $.calendars.instance( 'gregorian' );
  var localDate = dhis2.period.calendar.parseDate( dhis2.period.format, date );
  var isoDate = iso8601.fromJD( localDate.toJD() );

  return iso8601.formatDate( "yyyy-mm-dd", isoDate );
};

function validateRunValidation()
{
  startDate = toIso( $( '#startDate' ).val() );
  endDate = toIso( $( '#endDate' ).val() );

  validationRuleGroupId = $( '#validationRuleGroupId' ).val();
  sendNotifications = $( '#sendNotifications' ).is( ':checked' );

  $.getJSON( 'validateRunValidation.action',
    {
      startDate: startDate,
      endDate: endDate
    },
    function(json) {
      if ( json.response == 'success' ) {
        $( '#validateButton' ).attr( 'disabled', true );

        setHeaderWaitMessage( i18n_analysing_please_wait );

        $.get( 'runValidationAction.action',
          {
            organisationUnitId: organisationUnitId,
            startDate: startDate, endDate: endDate,
            validationRuleGroupId: validationRuleGroupId,
            sendNotifications: sendNotifications
          },
          function(data) {
            hideHeaderMessage();
            $( 'div#analysisInput' ).hide();
            $( 'div#analysisResult' ).show();
            $( 'div#analysisResult' ).html( data );
            setTableStyles();

            $( '#validateButton' ).removeAttr( 'disabled' );
          } );
      }
      else if ( json.response == 'input' ) {
        setHeaderDelayMessage( json.message );
      }
    } );

  return false;
}

function displayAnalysisInput()
{
    $( 'div#analysisInput' ).show();
    $( 'div#analysisResult' ).empty().hide();
}

function displayValidationDetailsDialog()
{
	$( '#validationResultDetailsDiv' ).dialog( {
	    modal: true,
	   	title: 'Validation details',
	   	width: 550,
	   	height: 500
	} );
}

function viewValidationResultDetails( validationRuleId, sourceId, periodId )
{
	$( '#validationResultDetailsDiv' ).load( 'viewValidationResultDetails.action', 
	{
		validationRuleId: validationRuleId, 
		sourceId: sourceId, 
		periodId: periodId
	},
	displayValidationDetailsDialog 
	);
}

function exportValidationResult( type )
{
    var url = 'exportValidationResult.action?type=' + type + 
    	"&organisationUnitId=" + $( "#organisationUnitId" ).val();
    	
    window.location.href = url;
}
