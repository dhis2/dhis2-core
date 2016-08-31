
// internal 'plugins' that touches jquery core

;(function( $, window, document, undefined ) {

    // iterate over and trim every item of an array
    $.trimArray = function( arr ) {
        if( !$.isArray(arr) ) {
            throw new Error('requires an array as argument')
        }

        return $.map(arr, $.trim);
    }

})(jQuery, window, document);

jQuery.extend( {
	
	postJSON: function( url, data, success ) {
		return $.ajax( { url:url, data:data, success:success, type:'post', dataType:'json', contentType:'application/x-www-form-urlencoded;charset=utf-8' } );
	},

	postUTF8: function( url, data, success ) {
		return $.ajax( { url:url, data:data, success:success, type:'post', contentType:'application/x-www-form-urlencoded;charset=utf-8' } );
	},
	
	loadNoCache: function( elementId, url, data ) {
		return $.ajax( { url:url, data:data, type:'get', dataType:'html', success:function( data ) {
			$( '#' + elementId ).html( data );
		} } );
	},
	
	toggleCss: function( elementId, property, value1, value2 ) {
		var id = '#' + elementId;
		var curValue = $( id ).css( property );
		if ( curValue == value1 ) {
			$( id ).css( property, value2 );
		}
		else {
			$( id ).css( property, value1 );
		} 
	}
} );