jQuery( document ).ready( function()
{
	tableSorter( 'listTable' );
} );

function removeUserGroup( userGroupId, userGroupName )
{
	removeItem( userGroupId, userGroupName, i18n_confirm_delete, "removeUserGroup.action" );
}
