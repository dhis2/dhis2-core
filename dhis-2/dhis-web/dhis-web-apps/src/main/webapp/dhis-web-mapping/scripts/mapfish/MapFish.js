(function() {
	window.mapfish = {

        /**
         * Property: _scriptName
         * {String} Relative path of this script.
         */
        _scriptName: "MapFish.js",

        /**
         * Function: _getScriptLocation
         * Return the path to this script.
         *
         * Returns:
         * Path to this script
         */
        _getScriptLocation: function () {
            // Workaround for Firefox bug:
            // https://bugzilla.mozilla.org/show_bug.cgi?id=351282
            if (window.gMfLocation) {
                return window.gMfLocation;
            }

            var scriptLocation = "";
            var scriptName = mapfish._scriptName;

            var scripts = document.getElementsByTagName('script');
            for (var i = 0; i < scripts.length; i++) {
                var src = scripts[i].getAttribute('src');
                if (src) {
                    var index = src.lastIndexOf(scriptName);
                    // is it found, at the end of the URL?
                    if ((index > -1) && (index + scriptName.length == src.length)) {
                        scriptLocation = src.slice(0, -scriptName.length);
                        break;
                    }
                }
            }
            return scriptLocation;
         }
    };

	var jsfiles = new Array(
		"core/Color.js",
		"core/GeoStat.js",
		"core/GeoStat/Boundary.js",
		"core/GeoStat/Thematic1.js",
		"core/GeoStat/Thematic2.js",
		"core/GeoStat/Facility.js",
		"core/GeoStat/Symbol.js",
		"core/Util.js"
		//"widgets/geostat/Boundary.js",
		//"widgets/geostat/Thematic1.js",
		//"widgets/geostat/Thematic2.js",
		//"widgets/geostat/Facility.js",
		//"widgets/geostat/Symbol.js"
	);

	var allScriptTags = "";
	var host = mapfish._getScriptLocation();

	for (var i = 0; i < jsfiles.length; i++) {
		if (/MSIE/.test(navigator.userAgent) || /Safari/.test(navigator.userAgent)) {
			var currentScriptTag = "<script src='" + host + jsfiles[i] + "'></script>";
			allScriptTags += currentScriptTag;
		} else {
			var s = document.createElement("script");
			s.src = host + jsfiles[i];
			var h = document.getElementsByTagName("head").length ?
					   document.getElementsByTagName("head")[0] :
					   document.body;
			h.appendChild(s);
		}
	}
	if (allScriptTags) {
		//document.write(allScriptTags);
	}

})();
