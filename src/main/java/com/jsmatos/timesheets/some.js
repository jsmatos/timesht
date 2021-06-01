var code = "your script code here";
var script = document.createElement("script");
script.setAttribute("type", "text/javascript");
script.appendChild(document.createTextNode(code));
document.body.appendChild(script);

javascript:(function(){var n=Date.now();var s=document.createElement("script");s.setAttribute("id",n);s.setAttribute("src","http://127.0.0.1:3000/timeshiit?title="+document.title+"&id="+n);document.body.appendChild(s);})();
