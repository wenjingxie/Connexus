
$(function () {
    'use strict';
    var url = $('#fileupload').action
    $('#fileupload').fileupload({
        url: url,
        sequentialUploads: true

    })


});
