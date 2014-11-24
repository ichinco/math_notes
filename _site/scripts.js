$(document).ready(function() {
    console.log($('.render-math'));
    $.each($('.render-math'), function(i, elt) {
        katex.render($(elt).text(), elt);
    });
});
