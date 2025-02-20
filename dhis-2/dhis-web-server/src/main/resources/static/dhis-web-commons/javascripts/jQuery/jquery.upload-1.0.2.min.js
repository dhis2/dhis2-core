/*
 * jQuery.upload v1.0.2
 *
 * Copyright (c) 2010 lagos
 * Dual licensed under the MIT and GPL licenses.
 *
 * http://lagoscript.org
 */
(function(b){function m(e){return b.map(n(e),function(d){return'<input type="hidden" name="'+d.name+'" value="'+d.value+'"/>'}).join("")}function n(e){function d(c,f){a.push({name:c,value:f})}if(b.isArray(e))return e;var a=[];if(typeof e==="object")b.each(e,function(c){b.isArray(this)?b.each(this,function(){d(c,this)}):d(c,b.isFunction(this)?this():this)});else typeof e==="string"&&b.each(e.split("&"),function(){var c=b.map(this.split("="),function(f){return decodeURIComponent(f.replace(/\+/g," "))});
d(c[0],c[1])});return a}function o(e,d){var a;a=b(e).contents().get(0);if(b.isXMLDoc(a)||a.XMLDocument)return a.XMLDocument||a;a=b(a).find("body").html();switch(d){case "xml":a=a;if(window.DOMParser)a=(new DOMParser).parseFromString(a,"application/xml");else{var c=new ActiveXObject("Microsoft.XMLDOM");c.async=false;c.loadXML(a);a=c}break;case "json":a=window.eval("("+a+")");break}return a}var p=0;b.fn.upload=function(e,d,a,c){var f=this,g,j,h;h="jquery_upload"+ ++p;var k=b('<iframe name="'+h+'" style="position:absolute;top:-9999px" />').appendTo("body"),
i='<form target="'+h+'" method="post" enctype="multipart/form-data" />';if(b.isFunction(d)){c=a;a=d;d={}}j=b("input:checkbox",this);h=b("input:checked",this);i=f.wrapAll(i).parent("form").attr("action",e);j.removeAttr("checked");h.attr("checked",true);g=(g=m(d))?b(g).appendTo(i):null;i.submit(function(){k.load(function(){var l=o(this,c),q=b("input:checked",f);i.after(f).remove();j.removeAttr("checked");q.attr("checked",true);g&&g.remove();setTimeout(function(){k.remove();c==="script"&&b.globalEval(l);
a&&a.call(f,l)},0)})}).submit();return this}})(jQuery);
