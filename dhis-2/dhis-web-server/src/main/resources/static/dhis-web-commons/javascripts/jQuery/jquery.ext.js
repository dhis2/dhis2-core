jQuery.extend({
	postJSON: function( url, data, callback ) {
		return jQuery.post(url, data, callback, "json");
	}
});