
jQuery.validator.addMethod("remoteWarning", function(value, element, param) {
    if ( this.optional(element) )
        return "dependency-mismatch";

    var previous = this.previousValue(element);
    if (!this.settings.messages[element.name] )
        this.settings.messages[element.name] = {};
    previous.originalMessage = this.settings.messages[element.name].remote;
    this.settings.messages[element.name].remote = previous.message;

    param = typeof param == "string" && {url:param} || param;

    if ( this.pending[element.name] ) {
        return "pending";
    }
    if ( previous.old === value ) {
        return previous.valid;
    }

    previous.old = value;
    var validator = this;
    this.startRequest(element);
    var data = {};
    data[element.name] = value;
    $.ajax($.extend(true, {
        url: param,
        mode: "abort",
        port: "validate" + element.name,
        dataType: "json",
        data: data,
        success: function(response) {
            validator.settings.messages[element.name].remote = previous.originalMessage;
            var valid = response.response === 'success';

            if ( valid ) {
                console.log("is valid")
                var submitted = validator.formSubmitted;
                validator.prepareElement(element);
                validator.formSubmitted = submitted;
                validator.successList.push(element);
                validator.showErrors();
            } else {
                var message = (previous.message = response.message || validator.defaultMessage( element, "remote" ));
                alert(message);

                var submitted = validator.formSubmitted;
                validator.prepareElement(element);
                validator.formSubmitted = submitted;
                validator.successList.push(element);
                validator.showErrors();

                /*
                var errors = {};
                var message = (previous.message = response.message || validator.defaultMessage( element, "remote" ));

                errors[element.name] = previous.message = $.isFunction(message) ? message(value) : message;
                validator.showErrors(errors);
                */
            }

            previous.valid = true;
            validator.stopRequest(element, true);
        }
    }, param));
    return "pending";
});

methods_en_GB = function() {
    jQuery.validator.addMethod("dateI", function(value, element, params) {
        var check = false;
        var re = /^\d{4}[\/-]\d{1,2}[\/-]\d{1,2}$/;
        if (re.test(value)) {
            var adata = value.split('-');
            var aaaa = parseInt(adata[0], 10);
            var mm = parseInt(adata[1], 10);
            var gg = parseInt(adata[2], 10);
            var xdata = new Date(aaaa, mm - 1, gg);
            if ((xdata.getFullYear() == aaaa) && (xdata.getMonth() == mm - 1)
                && (xdata.getDate() == gg))
                check = true;
            else
                check = false;
        } else
            check = false;
        return this.optional(element) || check;
    });
};

methods_vi_VN = function() {
    jQuery.validator.addMethod("dateI", function(value, element, params) {
        var check = false;
        var re = /^\d{1,2}[\/-]\d{1,2}[\/-]\d{4}$/;
        if (re.test(value)) {
            var adata = value.split('-');
            var gg = parseInt(adata[0], 10);
            var mm = parseInt(adata[1], 10);
            var aaaa = parseInt(adata[2], 10);
            var xdata = new Date(aaaa, mm - 1, gg);
            if ((xdata.getFullYear() == aaaa) && (xdata.getMonth() == mm - 1)
                && (xdata.getDate() == gg))
                check = true;
            else
                check = false;
        } else
            check = false;
        return this.optional(element) || check;
    });
};

 jQuery.validator.addMethod("conditionRemote", function(value, element) {
	var rule = this.settings.rules[element.name].conditionRemote;
	if (rule.condition && jQuery.isFunction(rule.condition) && !
	rule.condition.call(this,element)) return "dependency-mismatch";
	return jQuery.validator.methods.remote.apply(this, arguments);
}, jQuery.validator.messages.remote);

jQuery.validator.addMethod("submitRemote", function(value, element, param) {
	if ( this.optional(element) )
		return "dependency-mismatch";

	var previous = this.previousValue(element);

	if (!this.settings.messages[element.name] )
		this.settings.messages[element.name] = {};
	previous.originalMessage = this.settings.messages[element.name].submitRemote;
	this.settings.messages[element.name].submitRemote = previous.message;

	param = typeof param == "string" && {url:param} || param; 


		var validator = this;
		this.startRequest(element);
		var data = {};
		data[element.name] = value;
		$.ajax($.extend(true, {
			url: param,
			mode: "abort",
			port: "validate" + element.name,
			dataType: "json",
			data: data,
			success: function(response) {
				validator.settings.messages[element.name].remote = previous.originalMessage;
				var valid = response.response === 'success';
				if ( valid ) {
					var submitted = validator.formSubmitted;
					validator.prepareElement(element);
					validator.formSubmitted = submitted;
					validator.successList.push(element);
					validator.showErrors();
				} else {
					var errors = {};
					var message = (previous.message = response.message || validator.defaultMessage( element, "remote" ));
					errors[element.name] = $.isFunction(message) ? message(value) : message;
					validator.showErrors(errors);
				}
				previous.valid = valid;
				validator.stopRequest(element, valid);
			}
		}, param));
				
			
			return previous.valid;
});

// http://docs.jquery.com/Plugins/Validation/Methods/remote
		

(function() {

    function stripHtml(value) {
        // remove html tags and space chars
        return value.replace(/<.[^<>]*?>/g, ' ')
        .replace(/&nbsp;|&#160;/gi, ' ')
        // remove numbers and punctuation
        .replace(/[0-9.(),;:!?%#$'"_+=\/-]*/g, '');
    }
    jQuery.validator.addMethod("maxWords", function(value, element, params) {
        return this.optional(element)
        || stripHtml(value).match(/\b\w+\b/g).length < params;
    }, jQuery.validator.format("Please enter {0} words or less."));

    jQuery.validator.addMethod("minWords", function(value, element, params) {
        return this.optional(element)
        || stripHtml(value).match(/\b\w+\b/g).length >= params;
    }, jQuery.validator.format("Please enter at least {0} words."));

    jQuery.validator.addMethod("rangeWords", function(value, element, params) {
        return this.optional(element)
        || stripHtml(value).match(/\b\w+\b/g).length >= params[0]
        && value.match(/bw+b/g).length < params[1];
    }, jQuery.validator.format("Please enter between {0} and {1} words."));
})();

jQuery.validator.addMethod("letterswithbasicpunc", function(value, element) {
    return this.optional(element) || /^[a-z-.,()'\"\s]+$/i.test(value);
}, "Letters or punctuation only please");


jQuery.validator.addMethod("alphanumericwithbasicpuncspaces", function(value,
    element) {
    //return this.optional(element) || /^[\w-.,()\/'\"\s]+$/i.test(value);
	return this.optional(element) || !(/^[~`!@#^*+{}:;?|[]]$/i.test(value));
}, "Please Letters, numbers, spaces or some special chars like .,-,(,) only ");

jQuery.validator
    .addMethod("letterswithbasicspecialchars", function(value, element) {
        return this.optional(element)
        || /^[\w-.,()\/%'\"\s]+$/i.test(value);
    },
    "Please Letters, numbers, spaces or some special chars like .,-,%,(,) only ");
	
	jQuery.validator
    .addMethod("letterswithbasicspecialchars", function(value, element) {
        return this.optional(element)
        || /^[\w-.,()\/%'\"\s]+$/i.test(value);
    },
    "Please Letters, numbers, spaces or some special chars like .,-,%,(,) only ");

jQuery.validator
    .addMethod("unicodechars", function(value, element) {
        return this.optional(element)
        || /^([a-zA-Z_\u00A1-\uFFFF ])*$/i.test(value);
    },
    "Please unicode chars like .,-,%,(,) only ");

 jQuery.validator.addMethod("greaterDate",function(value, element, params) {
        
		if ($(params).val()){
			var closedDate = new Date(value);
			var openDate= new Date($(params).val());
			return closedDate > openDate;
		}
        return true;

    }, "");

jQuery.validator.addMethod("alphanumeric", function(value, element) {
    return this.optional(element) || /^[\w\s]+$/i.test(value);
}, "Letters, numbers, spaces or underscores only please");

jQuery.validator.addMethod("lettersdigitsonly", function(value, element) {
    return this.optional(element) || /^[a-z\d]+$/i.test(value);
}, "Letters and digits only please");

jQuery.validator.addMethod("lettersonly", function(value, element) {
    return this.optional(element) || /^[a-z]+$/i.test(value);
}, "Letters only please");

jQuery.validator.addMethod("decimals", function(value, element) {
	return this.optional(element) || /^-?[0-9]{0,3}(\.[0-9]{1,2})?$|^-?(999)(\.[0]{1,2})?$/i.test(value);
}, "Please enter digits only and 3 digits before and 2 after decimal point are allowed.");

jQuery.validator.addMethod("nowhitespace", function(value, element) {
    return this.optional(element) || /^\S+$/i.test(value);
}, "No white space please");

jQuery.validator.addMethod("nostartwhitespace", function(value, element) {
    return this.optional(element) || /^\S+/i.test(value);
}, "Can not start with whitespace");

jQuery.validator.addMethod("ziprange", function(value, element) {
    return this.optional(element) || /^90[2-5]\d\{2}-\d{4}$/.test(value);
}, "Your ZIP-code must be in the range 902xx-xxxx to 905-xx-xxxx");

jQuery.validator.addMethod("firstletteralphabet", function(value, element) {
    return this.optional(element) || /^[a-z]+$/i.test(value.charAt(0));
}, "The first character must be alphabetical");

jQuery.validator.addMethod("notequalto", function(value, element, param) {
    return value != $(param).val();
}, "Please enter a different value to above");

// param[0] : id of the element to compare
// param[1] : Name of the element to compare
jQuery.validator.addMethod("lessthanequal", function(value, element, params) {
    if ($(params[0]).val())
        return value <= $(params[0]).val();
    return true;
}, "");

// param[0] : id of the element to compare
// param[1] : Name of the element to compare
jQuery.validator.addMethod("greaterthanequal",
    function(value, element, params) {
        if ($(params[0]).val())
            return value >= $(params[0]).val();
        return true;

    }, "");

jQuery.validator.addMethod("unique", function(value, element, param) {
var flag = true;
jQuery("input."+param).each(function(){

    if( jQuery(this).attr("name") != jQuery(element).attr("name") )
    {
        var thisVal = jQuery(this).val();
        thisVal = jQuery.trim(thisVal);
        var values = value.toLowerCase();
        values = jQuery.trim(values);
        if( thisVal.toLowerCase() == values.toLowerCase() )
        {
            flag = false;
        }
    }

});
return flag;
}, "");

jQuery.validator.addMethod("password", function(value, element, param) {
    return this.optional(element) || /[A-Z]+/.test(value) && /\d+/.test(value);
});

jQuery.validator.addMethod("notOnlyDigits", function(value, element) {
    return this.optional(element) || !(/^\d+$/.test(value));
}, "Only Digits not allowed.");

/**
         * Return true, if the value is a valid date, also making this formal check
         * dd/mm/yyyy.
         *
         * @example jQuery.validator.methods.date("01/01/1900")
         * @result true
         *
         * @example jQuery.validator.methods.date("01/13/1990")
         * @result false
         *
         * @example jQuery.validator.methods.date("01.01.1900")
         * @result false
         *
         * @example <input name="pippo" class="{dateITA:true}" />
         * @desc Declares an optional input element whose value must be a valid date.
         *
         * @name jQuery.validator.methods.dateITA
         * @type Boolean
         * @cat Plugins/Validate/Methods
         */
jQuery.validator.addMethod("dateITA", function(value, element) {
    var check = false;
    var re = /^\d{1,2}\/\d{1,2}\/\d{4}$/
    if (re.test(value)) {
        var adata = value.split('/');
        var gg = parseInt(adata[0], 10);
        var mm = parseInt(adata[1], 10);
        var aaaa = parseInt(adata[2], 10);
        var xdata = new Date(aaaa, mm - 1, gg);
        if ((xdata.getFullYear() == aaaa) && (xdata.getMonth() == mm - 1)
            && (xdata.getDate() == gg))
            check = true;
        else
            check = false;
    } else
        check = false;
    return this.optional(element) || check;
}, "Please enter a correct date");

jQuery.validator.addMethod("time", function(value, element) {
    return this.optional(element)
    || /^(([0-1]?[0-9])|([2][0-3])):([0-5]?[0-9])(:([0-5]?[0-9]))?$/i.test(value);
}, "Please enter a valid time, between 00:00 and 23:59");


jQuery.validator.addMethod("phone", function(value, element) {
    return this.optional(element) || (/^(\+)?\d+$/.test(value));
}, "Please enter valid phone number");

// TODO check if value starts with <, otherwise don't try stripping anything
jQuery.validator.addMethod("strippedminlength",
    function(value, element, param) {
        return jQuery(value).text().length >= param;
    }, jQuery.validator.format("Please enter at least {0} characters"));

// same as email, but TLD is optional
jQuery.validator
    .addMethod(
        "email2",
        function(value, element, param) {
            return this.optional(element)
            || /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)*(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?$/i
            .test(value);
        }, jQuery.validator.messages.email);

// same as url, but TLD is optional
jQuery.validator
    .addMethod(
        "url2",
        function(value, element, param) {
            return this.optional(element)
            || /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)*(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i
            .test(value);
        }, jQuery.validator.messages.url);

jQuery.validator.addMethod("datelessthanequaltoday", function(value, element) {
    var choseDate = getDateFromFormat(value, "yyyy-MM-dd");
    return value ? choseDate <= new Date() : true;
}, "");

jQuery.validator.addMethod("required_group", function(value, element) {
    return $("input.required_group:filled").length;
}, "Please fill out at least one of these fields.");

jQuery.validator.addMethod("required_select_group", function(value, element) {
    return $("select.required_select_group option").length;
}, "Please select at least one option for these fields.");

jQuery.validator.addMethod("date", function(value, element, param) {
    return this.optional(element) || getDateFromFormat(value,param);
});

jQuery.validator.addMethod("custome_regex", function(value, element, params) {
	
    params[0] = (params[0] == '') ? new RegExp(params[0]) : params[0];
	
    return this.optional(element) || params[0].test(value);
});

jQuery.validator.addMethod("number", function(value, element, param) {
    return this.optional(element) || dhis2.validation.isNumber(value);
});

jQuery.validator.addMethod("integer", function(value, element, param) {
    return this.optional(element) || dhis2.validation.isInt(value);
});

jQuery.validator.addMethod("positive_integer", function(value, element, param) {
    return this.optional(element) || dhis2.validation.isPositiveInt(value);
});

jQuery.validator.addMethod("negative_integer", function(value, element, param) {
    return this.optional(element) || dhis2.validation.isNegativeInt(value);
});

jQuery.validator.addMethod("zero_positive_int", function(value, element, param) {
    return this.optional(element) || dhis2.validation.isZeroOrPositiveInt(value);
});

jQuery.validator.addMethod("coordinate", function(value, element, param) {
    return this.optional(element) || dhis2.validation.isCoordinate(value);
});

// Support method for date
//Parse a string and convert it to a Date object.
//If string cannot be parsed, return null.
//Avoids regular expressions to be more portable.
function getDateFromFormat(val, format) {
    val = val + "";
    format = format + "";
    var i_val = 0;
    var i_format = 0;
    var c = "";
    var token = "";
    var token2 = "";
    var x, y;
    var year = new Date().getFullYear();
    var month = 1;
    var date = 1;
    var hh = 0;
    var mm = 0;
    var ss = 0;
    var ampm = "";
    while (i_format < format.length) {
        // Get next token from format string
        c = format.charAt(i_format);
        token = "";
        while ((format.charAt(i_format) == c) && (i_format < format.length)) {
            token += format.charAt(i_format++);
        }
        // Extract contents of value based on format token
        if (token == "yyyy" || token == "yy" || token == "y") {
            if (token == "yyyy") {
                x = 4;
                y = 4;
            }
            if (token == "yy") {
                x = 2;
                y = 2;
            }
            if (token == "y") {
                x = 2;
                y = 4;
            }
            year = getInt(val, i_val, x, y);
            if (year == null) {
                return null;
            }
            i_val += year.length;
            if (year.length == 2) {
                if (year > 70) {
                    year = 1900 + (year - 0);
                } else {
                    year = 2000 + (year - 0);
                }
            }
        } else if (token == "MMM" || token == "NNN") {
            month = 0;
            var names = (token == "MMM" ? (Date.monthNames
                .concat(Date.monthAbbreviations)) : Date.monthAbbreviations);
            for ( var i = 0; i < names.length; i++) {
                var month_name = names[i];
                if (val.substring(i_val, i_val + month_name.length)
                    .toLowerCase() == month_name.toLowerCase()) {
                    month = (i % 12) + 1;
                    i_val += month_name.length;
                    break;
                }
            }
            if ((month < 1) || (month > 12)) {
                return null;
            }
        } else if (token == "EE" || token == "E") {
            var names = (token == "EE" ? Date.dayNames : Date.dayAbbreviations);
            for ( var i = 0; i < names.length; i++) {
                var day_name = names[i];
                if (val.substring(i_val, i_val + day_name.length).toLowerCase() == day_name
                    .toLowerCase()) {
                    i_val += day_name.length;
                    break;
                }
            }
        } else if (token == "MM" || token == "M") {
            month = getInt(val, i_val, token.length, 2);
            if (month == null || (month < 1) || (month > 12)) {
                return null;
            }
            i_val += month.length;
        } else if (token == "dd" || token == "d") {
            date = getInt(val, i_val, token.length, 2);
            if (date == null || (date < 1) || (date > 31)) {
                return null;
            }
            i_val += date.length;
        } else if (token == "hh" || token == "h") {
            hh = getInt(val, i_val, token.length, 2);
            if (hh == null || (hh < 1) || (hh > 12)) {
                return null;
            }
            i_val += hh.length;
        } else if (token == "HH" || token == "H") {
            hh = getInt(val, i_val, token.length, 2);
            if (hh == null || (hh < 0) || (hh > 23)) {
                return null;
            }
            i_val += hh.length;
        } else if (token == "KK" || token == "K") {
            hh = getInt(val, i_val, token.length, 2);
            if (hh == null || (hh < 0) || (hh > 11)) {
                return null;
            }
            i_val += hh.length;
            hh++;
        } else if (token == "kk" || token == "k") {
            hh = getInt(val, i_val, token.length, 2);
            if (hh == null || (hh < 1) || (hh > 24)) {
                return null;
            }
            i_val += hh.length;
            hh--;
        } else if (token == "mm" || token == "m") {
            mm = getInt(val, i_val, token.length, 2);
            if (mm == null || (mm < 0) || (mm > 59)) {
                return null;
            }
            i_val += mm.length;
        } else if (token == "ss" || token == "s") {
            ss = getInt(val, i_val, token.length, 2);
            if (ss == null || (ss < 0) || (ss > 59)) {
                return null;
            }
            i_val += ss.length;
        } else if (token == "a") {
            if (val.substring(i_val, i_val + 2).toLowerCase() == "am") {
                ampm = "AM";
            } else if (val.substring(i_val, i_val + 2).toLowerCase() == "pm") {
                ampm = "PM";
            } else {
                return null;
            }
            i_val += 2;
        } else {
            if (val.substring(i_val, i_val + token.length) != token) {
                return null;
            } else {
                i_val += token.length;
            }
        }
    }
    // If there are any trailing characters left in the value, it doesn't match
    if (i_val != val.length) {
        return null;
    }
    // Is date valid for month?
    if (month == 2) {
        // Check for leap year
        if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) { // leap
            // year
            if (date > 29) {
                return null;
            }
        } else {
            if (date > 28) {
                return null;
            }
        }
    }
    if ((month == 4) || (month == 6) || (month == 9) || (month == 11)) {
        if (date > 30) {
            return null;
        }
    }
    // Correct hours value
    if (hh < 12 && ampm == "PM") {
        hh = hh - 0 + 12;
    } else if (hh > 11 && ampm == "AM") {
        hh -= 12;
    }
    return new Date(year, month - 1, date, hh, mm, ss);
}
//------------------------------------------------------------------
//Utility functions for parsing in getDateFromFormat()
//------------------------------------------------------------------
function isInteger(val) {
    var digits="1234567890";
    for (var i=0; i < val.length; i++) {
        if (digits.indexOf(val.charAt(i))==-1) {
            return false;
        }
    }
    return true;
}
function getInt(str,i,minlength,maxlength) {
    for (var x=maxlength; x>=minlength; x--) {
        var token=str.substring(i,i+x);
        if (token.length < minlength) {
            return null;
        }
        if (isInteger(token)) {
            return token;
        }
    }
    return null;
}

function validatorFormat( text ) // Custom code
{
    return $.validator.format( text );
}

jQuery( document ).ready( function() { // Custom code
	
	if ( typeof( validationMessage ) !== "undefined"  ) // From messages.vm
	{
		$.validator.setMessages( validationMessage );
	}
} );