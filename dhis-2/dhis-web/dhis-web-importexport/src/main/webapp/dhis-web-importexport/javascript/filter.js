
jQuery( function() {
    tableSorter( "filterList" );
    
    dhis2.contextmenu.makeContextMenu({
        menuId: 'contextMenu',
        menuItemActiveClass: 'contextMenuItemActive',
        listId: 'filterTableBody'
      });
} );

function submitFilterForm( command )
{
    $( "input[name='command']" ).val( command );
    $( "#formFilter" ).submit();
}

function showFilterDetails( context )
{
	var filter = $.getJSON( "../api/metaDataFilters/" + context.uid, function( json ) {
		setInnerHTML( 'nameField', json.name );
		setInnerHTML( 'idField', json.id );
		showDetails();
	} );
}

function exportFilterButton( context )
{
	var filter = $.getJSON( "../api/metaDataFilters/" + context.uid, function( json ) {
		$( "#exportJson" ).attr( "value", json.jsonFilter );
        jQuery( "#exportDialog" ).dialog( {
            title: i18n_export,
            modal: true
        } );
	} );
}

function exportFilteredMetaData()
{
    var exportJson = {};
    exportJson.exportDependencies = $( "#exportDependencies" ).is( ":checked" ).toString();
    exportJson.jsonFilter = $( "#exportJson" ).val();

    $( "#exportJsonValue" ).val( JSON.stringify( exportJson ) );

    document.getElementById( 'exportForm' ).action = getURL();
    $( "#exportForm" ).submit();
    $( "#exportDialog" ).dialog( "close" );
}

function getURL()
{
    var url = "../api/filteredMetaData";
    var format = $( "#format" ).val();
    var compression = $( "#compression" ).val();
    url += "." + format;

    if(compression == "zip")
    {
        url += ".zip";
    }
    else if(compression == "gz")
    {
        url += ".gz";
    }

    return url;
}

function editFilterButton( context )
{
    window.location.href = 'updateFilterExportForm.action?id=' + context.id;
}

function removeFilterButton( context )
{	
	$.ajax( {
		url: '../api/metaDataFilters/' + context.uid,
		type: 'delete',
		success: function() {
			window.location.href = 'dxf2FilteredMetaDataExport.action'
		}
	} );
}
