function browserSupported() {
  return typeof document.body.style.flex == "string" 
      || typeof document.body.style.msFlex == "string" 
      || typeof document.body.style.webkitFlex == "string";
}

function goPswd(ev) {
  if (ev.keyCode && ev.keyCode == 13) {
    var p = document.getElementById("pswd");
    if (p) {
      p.focus();
      return false;
    }
  }
  return true;
}

function getStoredLanguage() {
  return localStorage.getItem("language");
}
function storeLanguage(lng) {
  localStorage.setItem("language", lng);
}

function onSelectLanguage(lng) {
  if (lng) {
    storeLanguage(lng);
    translate(lng);
  }
}

function getSelectedLanguage() {
  var nodes = document.forms["login"]["locale"];
  for (var i = 0; i < nodes.length; i++) {
    var node = nodes.item(i);
    if (node.checked) {
      return node.value;
    }
  }
  return null;
}
function setSelectedLanguage(form) {
  form["locale"].value = getSelectedLanguage();
}

function translate(lng) {
  selectLabel("label-" + lng);

  var dictionary = eval("dictionary" + lng);

  if (dictionary) {
    for ( var id in dictionary) {
      var el = document.getElementById(id);
  
      if (el) {
        if (id == "user") {
          el.placeholder = dictionary[id];
        } else if (id == "pswd") {
          el.placeholder = dictionary[id];
        } else {
          el.textContent = dictionary[id];
        }
      }
    }
  }
}

function selectLabel(id) {
  var elements = document.getElementsByClassName("bee-SignIn-Locale-label");

  for (var i = 0; i < elements.length; i++) {
    elements[i].style.fontWeight = (elements[i].id == id) ? 'bold' : 'normal';
  }
}

function inputValidationStyle() {
  var user = document.getElementsByClassName("bee-SignIn-Input-user");
  var pasw = document.getElementsByClassName("bee-SignIn-Input-password");
  
  user.classList.add('-invalid');
  pasw.classList.add('-invalid');
}

function onload(reqLng) {
  if (browserSupported()) {
    var lng = getStoredLanguage();
    var el = null;

    if (lng) {
      el = document.getElementById(lng);
    }
    if (!el) {
      lng = reqLng;
      el = document.getElementById(lng);
    }

    if (el) {
      el.checked = true;
      translate(lng);
    }

    customizeStyle();
    customizeBackground();

    document.body.className = "bee-ready";

    var u = document.getElementById("user");
    if (u) {
      u.focus();
    }

  } else {
    showSupport();
  }
}

function customizeBackground() {
  var bg = localStorage.getItem("login_bg");

  if (bg) {
    var img = document.getElementById("background");

    if (bg.indexOf("/") >= 0) {
      document.body.style.backgroundImage = "none";

      if (img) {
        img.src = bg;
        img.classList.add("bee-SignIn-has-src");
      }

    } else {
      document.body.style.backgroundImage = bg;

      if (img) {
        img.src = "";
        img.classList.remove("bee-SignIn-has-src");
      }
    }
  }
}

function customizeStyle() {
  var css = localStorage.getItem("login_css");
  if (css) {
    var node = document.createElement("style");
    node.innerHTML = css;
    document.head.appendChild(node);    
  }
}

function showSupport() {
  document.body.className = "bee-not-supported";
  
  var html = 
    "<h3>Your browser is not supported</h3>" +
    "<div>B-NOVO is currently supported only through the following browsers:</div>" +
    "<ul>" +
    '<li><a href="http://www.google.com/chrome">Chrome 30+</a></li>' +
    '<li><a href="http://www.mozilla.com/firefox">Firefox 23+</a></li>' +
    '<li><a href="http://windows.microsoft.com/en-us/internet-explorer/download-ie">Internet Explorer 10+</a></li>' +
    '<li><a href="http://www.opera.com">Opera 17+</a></li>' +
    '<li><a href="http://www.apple.com/safari">Safari 7+</a></li>' +
    "</ul>";
  
  document.body.innerHTML = html;
}