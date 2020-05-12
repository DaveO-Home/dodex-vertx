import { messageAlert } from "./utils/alert";
let status = "0";
export function login(submitButton, isValidate) {
    const userLogin = new Login();
    status = "0";
    Login.submitButton = submitButton ? submitButton[0] : submitButton;
    Login.userLogin = userLogin;
    if (typeof "".hashCode === "undefined") {
        userLogin.hash();
    }
    if (isValidate) {
        if (submitButton) {
            userLogin.newUser();
        }
        return userLogin.formValidation;
    } else {
        return userLogin.backend;
    }
}

function Login() {
    this.returnData = null;
    this.userLogin = null;
    this.submitButton;
    this.credentials = {};
    this.newLogin = false;
    this.user = async (type, submitButton, url) => {
        let formData = {};
        let userLogin = typeof url !== "undefined" ? url : "/userlogin";
        if (type === "DELETE") {
            let userJson = {};
            if(sessionStorage.getItem("credentials") === undefined) {
                return;
            }
            userJson.name = JSON.parse(sessionStorage.getItem("credentials")).name;
            userJson.password = JSON.parse(sessionStorage.getItem("credentials")).password;
            userLogin = userLogin + "?user=" + userJson.name + "&password=" + userJson.password;
        } else {
            formData = JSON.stringify($(submitButton.parentElement.parentElement).serializeArray());
        }

        await $.ajax({
            type: type,
            url: userLogin,
            data: formData,
            success: (data) => {
                data.credentials = this.credentials;
                Login.returnData = data;
                status = data.status;
                window._status = status;
                if (status === "0" && type !== "DELETE") {
                    sessionStorage.setItem("credentials", JSON.stringify(Login.returnData));
                    $(".login:first").html("Log Out");
                    $(".close-modal:first").click();
                    Login.newLogin = false;
                } 
            },
            dataType: "json",
            contentType: "application/json"
        })
            .fail((err) => {
                console.error("Error:", err, err.status);
                Login.returnData = { err: err.message, status: err.status };
            });
    };
    this.formValidation = async e => {
        const validateForm = isValid => {
            try {
                const inputs = Array.prototype.slice.call(Login.submitButton.parentElement.parentElement.querySelectorAll("input")); //  form
                for (let i = 0;i < inputs.length;i++) {
                    isValid = !inputs[i].checkValidity() ? false : isValid;
                    inputs[i].setCustomValidity("");

                    if (inputs[i].validity.valueMissing && !isValid) {
                        if (status === "-1") {
                            inputs[i].setCustomValidity("User/password combination not found. New?");
                        } else if (status === "-2") {
                            inputs[i].setCustomValidity("Failed, duplicate user id.");
                        } else if (status === "-4") {
                            inputs[i].setCustomValidity("Failed, non-unique?.");
                        } else {
                            inputs[i].setCustomValidity("Please enter data for required field");
                        }
                    } else
                        if (inputs[i].validity.patternMismatch && !isValid && inputs[i].id === "inputPassword") {
                            inputs[i].setCustomValidity("Minimum 8 with at least 1 character 1 number 1 special character");
                        } else if (inputs[i].validity.patternMismatch && !isValid && inputs[i].name === "username") {
                            inputs[i].setCustomValidity("Please enter 5-24 characters");
                        } else if (!isValid && inputs[i].id === "inputPassword2" && inputs[i].hasAttribute("required")) {
                            if (inputs[i].validity.patternMismatch) {
                                inputs[i].setCustomValidity("Minimum 8 with at least 1 character 1 number 1 special character");
                            } else if (inputs[i].value !== inputs[i - 1].value) {
                                inputs[i].setCustomValidity("Passwords do not match");
                            }
                        }
                }
                for (let i = 0;isValid && i < inputs.length;i++) {
                    if (inputs[i].name === "username") {
                        this.credentials.user = inputs[i].value;
                    }
                    if (inputs[i].value &&
                        inputs[i].name === "password") {
                        this.credentials.password = inputs[i].value;
                        inputs[i].value = inputs[i].value.hashCode();
                    }
                }
                return isValid;
            } catch (err) {
                e.preventDefault();
                throw err;
            }
        };
        if ($(".login:first").html() === "Log Out") {
            e.preventDefault();
            await this.user("DELETE", Login.submitButton);
            if (status === "-3") {
                if (typeof $("#top-nav").find(".alert-danger")[0] === "undefined") {
                    messageAlert($("#top-nav"), "<strong>Server:</strong> Session Timeout", "danger");
                }
            }
            sessionStorage.removeItem("credentials");
            $(".login:first").html("Log In");

            return;
        }

        const isValid = validateForm(true) ? true : validateForm(true);

        if (isValid) {
            e.preventDefault();
            sessionStorage.removeItem("credentials");

            if (Login.newLogin) {
                await this.user("PUT", Login.submitButton);
                if (Login.returnData.status === "-2" || Login.returnData.status === "-4") {
                    this.duplicateLogin();
                }
            } else {
                await this.user("GET", Login.submitButton);
                if (Login.returnData.status === "-1") {
                    this.notFound();
                }
            }
        }
    };
    this.hash = () => {
        Object.defineProperty(String.prototype, "hashCode", {
            value: function () {
                var hash = 0, i, chr;
                for (i = 0;i < this.length;i++) {
                    chr = this.charCodeAt(i);
                    hash = ((hash << 5) - hash) + chr;
                    hash |= 0; // Convert to 32bit integer
                }
                return hash;
            }
        });
    };
    this.notFound = () => {
        const password = Login.submitButton.parentElement.parentElement.querySelector("#inputPassword");
        const password2 = Login.submitButton.parentElement.parentElement.querySelector("#inputPassword2");

        password.value = "";
        password2.value = "";
        Login.submitButton.click();
    };
    this.duplicateLogin = () => {
        const name = Login.submitButton.parentElement.parentElement.querySelector("#inputUsername");
        const password = Login.submitButton.parentElement.parentElement.querySelector("#inputPassword");
        const password2 = Login.submitButton.parentElement.parentElement.querySelector("#inputPassword2");

        password.value = "";
        password2.value = "";
        name.value = "";
        Login.submitButton.click();
    };
    this.newUser = () => {
        const checkbox = Login.submitButton.parentElement.parentElement.querySelector("input[name=newLogin]");
        const password2 = Login.submitButton.parentElement.parentElement.querySelector("#inputPassword2");
        
        checkbox.addEventListener("change", function () {
            const lastGroup = $(Login.submitButton.parentElement.parentElement).find(".form-group").last()[0];
            if (this.checked) {
                lastGroup.removeAttribute("hidden");
                password2.setAttribute("required", "true");
                Login.newLogin = true;
            } else {
                lastGroup.setAttribute("hidden", "true");
                password2.removeAttribute("required");
                Login.newLogin = false;
            }
        });
    };
    this.backend = async (type, submitButton, url) => {
        await this.user(type, submitButton, url);
        if(url && url.indexOf("/unregister") > -1) {
            sessionStorage.removeItem("credentials");
        }
        return Login.returnData;
    };
    this.getData = () => {
        return Login.returnData;
    };
}
