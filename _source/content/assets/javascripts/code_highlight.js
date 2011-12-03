$(document).ready(function() {
  $('pre').each(function() {
    var $this = $(this),
        $code = $this.text();

    $this.empty();

    var myCodeMirror = CodeMirror(this, {
        value: $code,
        mode: 'ruby',
        lineNumbers: true,
        readOnly: true
    });

  });
});



