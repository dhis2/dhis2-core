
// Global variables:
arrayIds = new Array();

function displayAssociatedForm()
{
	var url = "showedAssociationsEditor.action";
	
	showLoader();
	
	jQuery( "#contentDiv" ).load( url, function(){
		hideLoader();
	});
}

function changeAssociatedStatus( orgunitId, dataSetId, assigned )
{
	var url = "definedAssociationEditor.action?";
	url += "orgUnitId=" + orgunitId ;
	url += "&dataSetId=" + dataSetId;
	url += "&assigned=" + !assigned;
	
	jQuery( "#div" + orgunitId + dataSetId ).load( url );
}

function assignAll( element, orgunitId )
{
	var url = "definedMultiAssociationsEditor.action?";

	lockScreen();
	
	for ( var i in arrayIds )
	{
		url += "dataSetIds=" + arrayIds[i] + "&";
		url += "statuses=" + $("#div" + orgunitId + arrayIds[i] + " input[type='hidden']").val();
		url += (i < arrayIds.length-1) ? "&" : "";
	}
	
	if ( url.length > 0 )
	{
		jQuery( "tr#tr" + orgunitId ).load( url,
		{
			'orgUnitId': orgunitId,
			'checked': element.checked
		},
		function(){
			unLockScreen();
		});
	}
}