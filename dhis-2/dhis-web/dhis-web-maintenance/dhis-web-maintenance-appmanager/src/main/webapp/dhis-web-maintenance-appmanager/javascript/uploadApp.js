/* global jQuery */
jQuery(function autoUpload() {
    var uploadForm = document.getElementById('uploadPackageForm')

    //If settings are not valid the form is not available.
    if (uploadForm === null) { return; }

    uploadForm.addEventListener('change', function(e) {
        var fd = new FormData(document.getElementById('uploadPackageForm'));
        var xhr = new XMLHttpRequest();
        xhr.addEventListener('progress', function(e) {
            var done = e.position || e.loaded, total = e.totalSize || e.total;
            jQuery("#progressbar").show();
            jQuery("#progressbar").progressbar({value: (Math.floor(done / total * 1000) / 10)});
        }, false);
        if (xhr.upload) {
            xhr.upload.onprogress = function(e) {
                var done = e.position || e.loaded, total = e.totalSize || e.total;
                jQuery("#progressbar").show();
                jQuery("#progressbar").progressbar({value: (Math.floor(done / total * 1000) / 10)});
            };
        }
        xhr.onreadystatechange = function(e) {
            if (4 == this.readyState) {
                jQuery(".ui-progressbar-value").html('<div style="text-align:center">Upload complete</div>');
                setTimeout(function(){
                    jQuery(".ui-progressbar-value").html('');
                    jQuery("#progressbar").hide();
                }, 4000);
                setHeaderDelayMessage(JSON.parse(xhr.responseText).message);
                jQuery("#uploadPackageForm")[0].reset();

                setInterval( function() {
                	window.location.reload() }, 1500 );
            }
        };
        xhr.open('post', 'addApp.action', true);
        xhr.send(fd);
    }, false);
});

function deleteApp( appId, appName ) {
  removeItem(appId, appName, i18n_confirm_delete, "deleteApp.action?appName=" + appName);
}
