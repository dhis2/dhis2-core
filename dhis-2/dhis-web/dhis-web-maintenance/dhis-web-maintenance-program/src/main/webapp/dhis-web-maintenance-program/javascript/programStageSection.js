$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive',
    listItemProps: ['id', 'uid', 'name', 'type', 'psid']
  });
});

function programStageSectionList( programStageId )
{
	window.location.href = "programStage.action?id=" + programId;
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showSectionDetails( context )
{
	jQuery.getJSON( 'getProgramStageSection.action', { id: context.id }, function ( json ) {
		setInnerHTML( 'nameField', json.programStageSection.name );	
		setInnerHTML( 'dataElementCountField', json.programStageSection.dataElementCount ); 
		setInnerHTML( 'idField', json.programStageSection.uid ); 
		showDetails();
	});
}

function removeSection( context )
{
	removeItem( context.id, context.name, i18n_confirm_delete, 'removeProgramStageSection.action' );
}

function showUpdateProgramStageSectionForm( context ) {
  location.href = 'showUpdateProgramStageSectionForm.action?id=' + context.id + '&programStageId=' + context.psid;
}
