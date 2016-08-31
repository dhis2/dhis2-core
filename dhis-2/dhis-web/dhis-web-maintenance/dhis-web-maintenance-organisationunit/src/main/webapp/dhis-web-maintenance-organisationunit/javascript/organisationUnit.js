// -----------------------------------------------------------------------------
// Organisation unit selection listener
// -----------------------------------------------------------------------------

$( document ).ready( function() {
	selection.setOfflineLevel( 1 );
	selection.setAutoSelectRoot( false );
	selection.setRootUnselectAllowed( true );
	selection.setListenerFunction( organisationUnitSelected, true );
});

function organisationUnitSelected( orgUnitIds ) {
	window.location.href = 'organisationUnit.action';
}

// -----------------------------------------------------------------------------
// Export to PDF
// -----------------------------------------------------------------------------

function exportPDF( type )
{
	var params = "type=" + type;
	
	exportPdfByType( type, params );
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateOrganisationUnitForm( context ) {
  location.href = 'showUpdateOrganisationUnitForm.action?id=' + context.id;
}

function showOrganisationUnitDetails( context ) {
    jQuery.post( '../dhis-web-commons-ajax-json/getOrganisationUnit.action',
		{ id: context.uid }, function ( json ) {
		setInnerHTML( 'nameField', json.organisationUnit.name );
		setInnerHTML( 'shortNameField', json.organisationUnit.shortName );
		setInnerHTML( 'descriptionField', json.organisationUnit.description );
		setInnerHTML( 'openingDateField', json.organisationUnit.openingDate );
		setInnerHTML( 'idField', json.organisationUnit.uid );
		var userName = json.organisationUnit.user.name  != "" ? json.organisationUnit.user.name : '[' + none + ']';
		setInnerHTML( 'createdByField', userName );

		var orgUnitCode = json.organisationUnit.code;
		setInnerHTML( 'codeField', orgUnitCode ? orgUnitCode : '[' + none + ']' );

		var closedDate = json.organisationUnit.closedDate;
		setInnerHTML( 'closedDateField', closedDate ? closedDate : '[' + none + ']' );

		var commentValue = json.organisationUnit.comment;
		setInnerHTML( 'commentField', commentValue ? commentValue.replace( /\n/g, '<br>' ) : '[' + none + ']' );

		var active = json.organisationUnit.active;
		setInnerHTML( 'activeField', active == 'true' ? yes : no );

		var url = json.organisationUnit.url;
		setInnerHTML( 'urlField', url ? '<a href="' + url + '">' + url + '</a>' : '[' + none + ']' );

		var lastUpdated = json.organisationUnit.lastUpdated;
		setInnerHTML( 'lastUpdatedField', lastUpdated ? lastUpdated : '[' + none + ']' );

		showDetails();
	});
}

// -----------------------------------------------------------------------------
// Remove organisation unit
// -----------------------------------------------------------------------------

function removeOrganisationUnit( context )
{
    removeItem( context.id, context.name, confirm_to_delete_org_unit, 'removeOrganisationUnit.action', subtree.refreshTree );
}
