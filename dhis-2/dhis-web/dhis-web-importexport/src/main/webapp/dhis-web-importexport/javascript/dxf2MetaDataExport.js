$( document ).ready( function() {
  $.ajax( {
    url: '../api/schemas.json',
    dataType: 'json'
  } ).done( function(data) {
    var metadata = [];

    $.each( data.schemas, function(idx) {
      if ( this.metadata ) {
        var o = {
          name: this.displayName,
          id: this.plural
        }

        metadata.push( o );
      }
    } );

    metadata.sort( function(a, b) {
      if ( a.name < b.name )
        return -1;
      if ( a.name > b.name )
        return 1;
      return 0;
    } );

    $.each( metadata, function(idx) {
      var type = $( '<div />' ).css( {
        width: '200px',
        float: 'left'
      } );

      var checkbox = $( '<input type="checkbox"/>' ).attr( 'id', this.id ).attr( 'value', true );
      var labelFor = $( '<label />' ).attr( 'for', this.id ).text( this.name );

      type.append( checkbox ).append( labelFor );
      $( '#metadata' ).append( type );
    } );

    selectAll();
  } )
} );

function selectAll() {
  $( "#exportForm" ).find( "input:checkbox" ).attr( "checked", true );
}

function selectNone() {
  $( "#exportForm" ).find( "input:checkbox" ).attr( "checked", false );
}

function exportMetaData() {
  if ( $( '#dataSets' ).is( ':checked' ) ) {
    $( '#sections' ).attr( 'checked', true );
  }
  else {
    $( '#sections' ).removeAttr( 'checked' );
  }

  if ( $( '#categories' ).is( ':checked' ) ) {
    $( '#categoryCombos' ).attr( 'checked', true );
    $( '#categoryOptionCombos' ).attr( 'checked', true );
    $( '#categoryOptions' ).attr( 'checked', true );
  }
  else {
    $( '#categoryCombos' ).removeAttr( 'checked' );
    $( '#categoryOptionCombos' ).removeAttr( 'checked' );
    $( '#categoryOptions' ).removeAttr( 'checked' );
  }

  if ( $( '#mapLegendSets' ).is( ':checked' ) ) {
    $( '#mapLegends' ).attr( 'checked', true );
  }
  else {
    $( '#mapLegends' ).removeAttr( 'checked' );
  }

  if ( $( '#maps' ).is( ':checked' ) ) {
    $( '#mapViews' ).attr( 'checked', true );
  }
  else {
    $( '#mapViews' ).removeAttr( 'checked' );
  }

  var url = "../api/25/metadata";
  var format = $( "#format" ).val();
  var compression = $( "#compression" ).val();
  var sharing = $( "#sharing" ).val() === "true";

  url += "." + format;

  if ( compression == "zip" ) {
    url += ".zip";
  }
  else if ( compression == "gz" ) {
    url += ".gz";
  }

  var selected = {};

  $( "#exportForm" ).find( 'input:checked' ).each( function(idx) {
    selected[this.id] = this.value;
  } );

  url += "?assumeTrue=false&" + $.param( selected );

  if ( !sharing ) {
    url += "&fields=:owner,!user,!publicAccess,!userGroupAccesses";
  }

  log( "url" + url );
  window.location = url;
}
