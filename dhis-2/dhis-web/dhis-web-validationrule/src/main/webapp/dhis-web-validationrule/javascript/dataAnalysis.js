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
    var attributeOptionComboId = $( '#value-' + valueId + '-aoc' ).val();
    var periodId = $( '#value-' + valueId + '-pe' ).val();
    var sourceId = $( '#value-' + valueId + '-ou' ).val();
    
    $.ajax( {
      url: 'markForFollowup.action',
      data: { dataElementId:dataElementId, periodId:periodId, sourceId:sourceId, categoryOptionComboId:categoryOptionComboId, attributeOptionComboId:attributeOptionComboId },
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

    var dataSetId = $("#selectedDataSetId").val();

    var url = "getFollowup.action";

    if ( dataSetId )
    {
        url += "?dataSetId=" + dataSetId;
    }

    $.ajax({
        type: 'GET',
        url: url,
        success: function( data ){
            hideHeaderMessage();
            $( "div#analysisResult" ).show();
            $( "div#analysisResult" ).html( data );
        }
    });
}

function viewComment( index )
{
    var comment = $("#value-"+ index + "-comment").val();
    $("#commentTextArea").val(comment);

    $("#commentContainer").dialog({
        autoOpen: true,
        modal: false,
        width: 200,
        height: 200,
        resizable: true,
        title: "Comment"
    });
}

function fetchDataSets()
{
    var ouId = selectionTreeSelection.getSelected();

    $.ajax({
        type: 'GET',
        url: '../api/organisationUnits/' + ouId,
        data: {
            fields: 'id,dataSets[id,name],children[id,dataSets[id,name]]'
        },
        success: function( ou ){
            var dataSets = [];
            if ( ou.dataSets )
            {
                ou.dataSets.forEach(function( item ) {
                    dataSets.push(item);
                });

                initDataSetList( dataSets );
            }
        }
    });
};

function initDataSetList( dataSets )
{
    dataSets.sort( function( a, b )
    {
        return a.name > b.name ? 1 : a.name < b.name ? -1 : 0;
    } );

    var dataSetId = $('#selectedDataSetId').val();
    $("#selectedDataSetId").removeAttr("disabled");
    clearListById('selectedDataSetId');
    addOptionById('selectedDataSetId', '', '[ ' + i18n_select_all + ' ]');

    var dataSetValid = false;

    $.each(dataSets, function ( idx, item ) {
        if( item ) {
            addOptionById('selectedDataSetId', item.id, item.name);

            if( dataSetId == item.id ) {
                dataSetValid = true;
            }
        }
    });
}