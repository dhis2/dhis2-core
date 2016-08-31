
// -----------------------------------------------------------------------------
// Organisation unit to move
// -----------------------------------------------------------------------------

selection.setListenerFunction( organisationUnitToMoveSelected );

function organisationUnitToMoveSelected( orgUnitIds )
{
	hideHeaderMessage();

  if ( orgUnitIds.length == 1 )
  {
    var id = orgUnitIds[0];

    $.getJSON( '../dhis-web-commons-ajax-json/getOrganisationUnit.action', { id:id }, function( json )
    {
      $( '#organisationUnitToMoveId' ).val( json.organisationUnit.id );
      $( '#toMoveNameField' ).html( json.organisationUnit.name );
      $( '#confirmOrganisationUnitToMoveButton' ).removeAttr( 'disabled' );
    } );
  }
}

function organisationUnitToMoveConfirmed()
{
  var id = $( '#organisationUnitToMoveId' ).val();

	$.getJSON( 'validateOrganisationUnitToMove.action', { organisationUnitToMoveId:id }, function( json ) 
	{
		if ( json.response == 'success' )
		{
			$( '#confirmOrganisationUnitToMoveButton' ).attr( 'disabled', 'disabled' );
        
        	$( '#step1' ).css( 'background-color', '#ffffff' );
        	$( '#step2' ).css( 'background-color', '#ccffcc' );
        
        	selection.setListenerFunction( newParentSelected );
		}
		else
		{
			setHeaderMessage( json.message );
		}
	} );
}

// -----------------------------------------------------------------------------
// New parent organisation unit
// -----------------------------------------------------------------------------

function newParentSelected( orgUnitIds )
{
	hideHeaderMessage();

    if ( orgUnitIds.length == 1 )
    {
    	var id = orgUnitIds[0];
    	
    	$.getJSON( '../dhis-web-commons-ajax-json/getOrganisationUnit.action', { id:id }, function( json )
    	{
    		$( '#newParentOrganisationUnitId' ).val( json.organisationUnit.id );
    		$( '#newParentNameField' ).html( json.organisationUnit.name );
        	$( '#confirmNewParentOrganisationUnitButton' ).removeAttr( 'disabled' );
    	} );
    }
    else if ( orgUnitIds.length == 0 )
    {
        $( '#newParentOrganisationUnitId' ).val( '' );
        $( '#newParentNameField' ).html( '[' + not_selected_moved_to_root_position + ']' );
    }
}

function newParentOrganisationUnitConfirmed()
{
  var toMoveId = $( '#organisationUnitToMoveId' ).val();
  var newParentId = $( '#newParentOrganisationUnitId' ).val();

	$.getJSON( 'validateNewParentOrganisationUnit.action', {
		organisationUnitToMoveId:toMoveId, newParentOrganisationUnitId:newParentId }, function( json ) 
		{
			if ( json.response == 'success' )
		    {
		        $( '#confirmNewParentOrganisationUnitButton' ).attr( 'disabled', 'disabled' );
		        $( '#submitButton' ).removeAttr( 'disabled' );
		        
		        $( '#step2' ).css( 'background-color', '#ffffff' );
		        $( '#step3' ).css( 'background-color', '#ccffcc' );
		        
		        selection.setListenerFunction( null );
		    }
		    else
		    {
		    	setHeaderMessage( message );
		    }
	} );
}
