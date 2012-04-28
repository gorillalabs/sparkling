jQuery.fn.toc = function () {
  if(this.length === 0)
    return;

  var listStack = [ $("<ul class='nav nav-list' />")];
  listStack[0].appendTo(this);

  Array.prototype.last = function() { return this[this.length - 1]};

  var level = 2;
  $(document).ready(function() {
    $(":header").each(function(index, el) {

      if(parseInt(el.tagName[1]) === 1)
        return;

      var text = $(el).text();

      var anchor = text.replace(/[^a-zA-Z 0-9]+/g,'').replace(/\s/g, "_").toLowerCase();
      $(el).attr('id', anchor);

      var currentLevel = parseInt(el.tagName[1]);

      if(currentLevel > level) {
        var nextLevelList = $("<ul class='nav nav-list'/>");
        nextLevelList.appendTo(listStack.last().children("li").last());
        listStack.push(nextLevelList);
      } else if(currentLevel < level) {
        listStack.pop();
      }

      level = currentLevel;
      var li = $("<li />");

      $("<a />").text(text).attr('href', "#" + anchor).appendTo(li);
      li.appendTo(listStack.last());
    });
  });
};
// $($(".highlight")[2]).text().match(/(.*)\.(.*)/)
// "AMQP::Channel.direct".match(/(.*)\.(.*)/)
// "AMQP::Channel#direct".match(/(.*)#(.*)/)
jQuery.fn.yardLink = function () {
  var class_method = /(.*)\.(.*)/;
  var instance_method = /(.*)#(.*)/;
};

$(document).ready(function() {
  $(".well.sidebar-nav").toc();
});