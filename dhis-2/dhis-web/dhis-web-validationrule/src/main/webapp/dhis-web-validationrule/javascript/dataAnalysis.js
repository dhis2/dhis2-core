var selectedOrganisationUnit = null;

function organisationUnitSelected( organisationUnits )
{
    selectedOrganisationUnit = organisationUnits[0];
}

function validateRunAnalyseData()
{
    if ( analyseDataInvalid() )
    {
        $( '#startButton').attr( 'disabled', true );

        $.getJSON( "validateRunAnalysis.action", 
        {
            fromDate : getFieldValue( 'fromDate' ),
            toDate : getFieldValue( 'toDate' )
        }, 
        function( json )
        {
            if ( json.response == "success" )
            {
                analyseData();
            }
            else
            {
                setHeaderDelayMessage( json.message );
                $( '#startButton').removeAttr( 'disabled' );
            }
        } );
    }
}

function analyseDataInvalid()
{
    if ( $( "#fromDate" ).val().length == 0 )
    {
        setHeaderDelayMessage( i18n_specify_a_start_date );
        return false;
    }

    if ( $( "#toDate" ).val().length == 0 )
    {
        setHeaderDelayMessage( i18n_specify_an_ending_date );
        return false;
    }

    var dataSets = document.getElementById( "dataSets" );

    if ( dataSets.options.length == 0 )
    {
        setHeaderDelayMessage( i18n_specify_dataset );
        return false;
    }

    return true;
}

function analyseData()
{
    setHeaderWaitMessage( i18n_analysing_please_wait );

    var url = "getAnalysis.action" + "?key=" + $( "#key" ).val() + "&toDate=" + $( "#toDate" ).val() + "&fromDate="
            + $( "#fromDate" ).val() + "&" + getParamString( "dataSets", "dataSets" );

    if ( byId( "standardDeviation" ) != null )
    {
        url += "&standardDeviation=" + $( "#standardDeviation" ).val();
    }

    $.get( url, function( data )
    {
    	hideHeaderMessage();
        $( "div#analysisInput" ).hide();
        $( "div#analysisResult" ).show();
        $( "div#analysisResult" ).html( data );

        $( "#startButton" ).removeAttr( 'disabled' );
    } );
}

function displayAnalysisInput()
{
    $( 'div#analysisInput' ).show();
    $( 'div#analysisResult' ).empty().hide();
}

function exportAnalysisResult( type )
{
    var url = 'exportAnalysisResult.action?type=' + type;
    window.location.href = url;
}

function markFollowup( valueId )
{
    var dataElementId = $( '#value-' + valueId + '-de' ).val();
    var categoryOptionComboId = $( '#value-' + valueId + '-coc' ).val();
    var periodId = $( '#value-' + valueId + '-pe' ).val();
    var sourceId = $( '#value-' + valueId + '-ou' ).val();
    
    $.ajax( {
      url: 'markForFollowup.action',
      data: { dataElementId:dataElementId, periodId:periodId, sourceId:sourceId, categoryOptionComboId:categoryOptionComboId },
      type: 'POST',
      dataType: 'json',
      success: function( json )
      {
        var $image = $( '#value-' + valueId + '-followUp' );

        if ( json.message == "marked" )
        {
            $image.attr( "src", "../images/marked.png" );
            $image.attr( "title", i18n_unmark_value_for_followup );
        }
        else if ( json.message == "unmarked" )
        {
            $image.attr( "src", "../images/unmarked.png" );
            $image.attr( "title", i18n_mark_value_for_followup );
        }
      }
    } );
}

function getFollowupAnalysis()
{
    setHeaderWaitMessage( i18n_analysing_please_wait );

    var url = "getFollowup.action";

    $.get( url, function( data )
    {
        hideHeaderMessage();
        $( "div#analysisResult" ).show();
        $( "div#analysisResult" ).html( data );
    } );
}
