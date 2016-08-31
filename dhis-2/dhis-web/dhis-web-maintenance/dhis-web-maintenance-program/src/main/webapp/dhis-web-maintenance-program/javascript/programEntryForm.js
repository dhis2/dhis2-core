$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function defineForm( context ){
	window.location.href='viewProgramEntryForm.action?programId=' + context.id;
}

function removeProgramEntryForm( context )
{
	var result = window.confirm( i18n_confirm_delete + "\n\n" + context.name );
    
    if ( result )
    {
		jQuery.postJSON("removeProgramEntryForm.action", {id:context.id}
		,function(json) {});
	}
}
