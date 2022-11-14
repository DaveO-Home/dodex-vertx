let teetimeDate;
setTimeout(function() {
    teetimeDate = document.getElementById("teetime").value.substring(0, 9);
  }, 1000);

const validateForm = function(isValid, isCourse=false, isScore=false) {
    let status = 0;
    try {
      const inputs = Array.prototype.slice.call(document.getElementById("pin").parentElement.parentElement.querySelectorAll("input")); //  form
      const lengths = [];
      
      inputs.forEach(function (v, i, a) { lengths[i] = v.value.trim().length; })
      for (let i = 0; i < inputs.length; i++) {
        inputs[i].setCustomValidity("");
      }
      
      if (lengths.every(length => length < 1)) {
        inputs[0].setCustomValidity("Please enter data for required field");
        isValid = false;
      } else if (lengths.slice(0, 2).every(length => length < 1) && lengths[2] !== 0) {
        isValid = false;
        inputs[1].setCustomValidity("Please enter data for required field");
      } else if (lengths[0] === 0 && lengths[1] !== 0 && lengths[2] === 0) {
        isValid = false;
        inputs[2].setCustomValidity("Please enter data for required field");
      } else if (course.value.length === 0 && lengths[0] === 0) {
        isValid = false;
        course.setCustomValidity("Please enter data for required field");
      }
      let isEmptyPin = false;
      for (let i = 0; i < inputs.length; i++) {
        if (inputs[i].validity.patternMismatch && inputs[i].id === "pin" && lengths[0] !== 0) {
          inputs[0].setCustomValidity("Pin - first two characters must be alpha with with remaining 4-6 alphanumeric");
          isValid = false;
        } else if (inputs[i].validity.patternMismatch && inputs[i].id === "fname") {
          inputs[1].setCustomValidity("First Name - enter betweeen 3-20 characters");
          isEmptyPin = true;
          isValid = false;
        } else if (inputs[i].validity.patternMismatch && inputs[i].id === "lname") {
          inputs[2].setCustomValidity("Last Name - enter betweeen 4-40 characters");
          isEmptyPin = true;
          isValid = false;
        } else if (lengths[0] === 0) {
          isEmptyPin = true;
        }
      }

      if(isCourse) {
        const course = document.querySelector("#course");
        const rating = document.querySelector("#rating");
        const slope = document.querySelector("#slope");
        const par = document.querySelector("#par");
        course.setCustomValidity("");
        rating.setCustomValidity("");
        slope.setCustomValidity("");
        par.setCustomValidity("");
        if(typeof window.cmd === "undefined") {
          course.setCustomValidity("Please login before adding course tees");
          isValid = false;
        } else if(course.validity.patternMismatch || course.value.length === 0) {
          course.setCustomValidity("Please select or enter a course name");
          isValid = false;
        } else if(rating.validity.patternMismatch || rating.value.length === 0) {
          rating.setCustomValidity("Rating has form 99.9 (30.0-79.9)");
          isValid = false;
        } else if(slope.validity.patternMismatch || slope.value.length === 0 || slope.value < 55 || slope.value > 155) {
          slope.setCustomValidity("Slope has form 999 (55-155)");
          isValid = false;
        } else if(par.validity.patternMismatch || par.value.length === 0) {
          par.setCustomValidity("Par has form 99");
          isValid = false;
        }
      }

      const teetimeEle = document.getElementById("teetime");
      const teetimePattern = teetimeEle.getAttribute("pattern");
      const isTeeTime = new RegExp(teetimePattern).test(teetimeEle.value);

      if(isScore) {
        const total = document.querySelector("#total-score");
        const adjusted = document.querySelector("#adjusted-score");
        total.setCustomValidity("");
        adjusted.setCustomValidity("");
        if(total.validity.patternMismatch || total.value.length === 0
               || total.value < 25 || total.value > 200) {
          total.setCustomValidity("Total Score has form 99(9)");
          isValid = false;
        } else if(adjusted.validity.patternMismatch || adjusted.value.length === 0
               || adjusted.value < 25 || adjusted.value > 200) {
          adjusted.setCustomValidity("Adjusted Score has form 99(9)");
          isValid = false;
        } else if(adjusted.value > total.value) {
          adjusted.setCustomValidity("Adjusted Score must be <= Total Score");
          isValid = false;
        } else if(!isTeeTime) {
          teetimeEle.setCustomValidity("Tee time is required for score");
          isValid = false;
        }
      }

      if (isValid && !isTeeTime && isEmptyPin) {
        inputs[1].setCustomValidity("To login with golfer's name, you must also include a course, tee-time date and tee played")
        isValid = false;
      }

      return isValid;
    } catch (err) {
      return false;
      throw err;
    }
}

const popupMessage = function(message, type) {
    const alert = document.querySelector(".alert");
    alert.querySelector(".replace").innerHTML=message;
    alert.removeAttribute("hidden");
    const messageEle = document.querySelector("#message");
    const popupEle = document.getElementById("popupMessage");
    messageEle.classList.add(type);
    popupEle.style.display = "block";
    setTimeout(function(){
      alert.setAttribute("hidden","");
      messageEle.classList.remove(type);
      popupEle.style.display = "none";
    }, 5000);
}

// see; https://www.delftstack.com/howto/javascript/encode-html-entities-in-javascript/
String.prototype.toHtmlEntities = function () {
  return this.replace(/./gm, (s) => {
    // return "&#" + s.charCodeAt(0) + ";";
    return (s.match(/[a-z0-9\s]+/i)) ? s : "&#" + s.charCodeAt(0) + ";";
  });
};

String.fromHtmlEntities = function (string) {
  return (string + "").replace(/&#\d+;/gm, (s) => {
    return String.fromCharCode(s.match(/\d+/gm)[0]);
  })
};

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export { validateForm, popupMessage, sleep };