
function modifyMinMaxValues( remove )
{
	var datasetIds = "";
	var datasetField = byId( 'dataSetIds' ); 
	
	if ( $( "#dataSetIds :selected" ).length == 0 )
	{
		setHeaderDelayMessage( i18n_not_choose_dataset );
		return;
	}
	
	for ( var i = 0; i < datasetField.options.length; i++ )
	{
		if ( datasetField.options[ i ].selected )
		{
			datasetIds+= "dataSets=" + datasetField.options[ i ].value + "&";
		}
	}
	
	if ( remove )
	{
		setHeaderWaitMessage( i18n_removing_min_max_values );
	}
	else
	{
		setHeaderWaitMessage( i18n_generating_min_max_values );
	}
	
	disableButtons();
	
	$.ajax({
		   type: "POST",
		   url: "generateMinMaxValue.action",
		   data: datasetIds,
		   dataType: "xml",
		   success: function(xmlObject)
		   {
		   		enableButtons();
				xmlObject = xmlObject.getElementsByTagName( 'message' )[0];
				setHeaderDelayMessage(xmlObject.firstChild.nodeValue);
		   }
		});
}

function disableButtons()
{
	$( ":button" ).attr( "disabled", "disabled" );
}

function enableButtons()
{
	$( ":button" ).removeAttr( "disabled" );
}