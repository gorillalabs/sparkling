
$(document).ready(function() {
  var $toc = $('#toc');

  function format (title) {
    var $title = $(title),
        txt = $title.text(),
        id = $title.attr('id');
    return "<li> <a href=#" + id + ">" + txt + "</a></li>";
  }
  // return;

  if($toc.length) {
    var $h3s = $('.span9 :header');
    var titles = $h3s.map(function () {
      return format(this);
    }).get().join('');
    $toc.html(titles);
  }

  $("#toc").affix();

});
