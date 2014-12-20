$(document).ready(function() {
    $.each($('.render-math'), function(i, elt) {
    	try {
        	katex.render($(elt).text(), elt);
	} catch (err) {
		console.log(err, $(elt).text());
	}
    });
});
