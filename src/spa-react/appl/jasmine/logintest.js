import { login } from "../js/login";
export default function (Start, Helpers, ReactDOM, React, StartC, LoginC, timer) {
    /*
     * Test Form validation and submission.
     */
    describe("Popup Login Form", () => {
        let modal;
        let closeButton;
        let nameObject;
        let pwrdObject;
        let pwrdObject2;
        let loginObject;
        let loginButton;
        let checkbox;
        let form;

        beforeAll(done => {
            // Making sure there's a clean starting point
            sessionStorage.removeItem("credentials");
            ReactDOM.render(
                <LoginC />,
                document.getElementById("nav-login"),
                function handleClick(e) {
                    if($(".login:first").html() === "Log Out") {
                        login(null, true)(e);
                    } else {
                        Start["div .login click"](e);
                    }
                }
            );

            loginObject = $("small .login");
            loginObject.click();

            Start.base = true;
            // Note: if page does not refresh, increase the timer time.
            // Using RxJs instead of Promise.
            const numbers = timer(50, 50);
            const observable = numbers.subscribe(timer => {
               modal = $("#modalTemplate");
               form = modal.find("form:first");

                if ((modal.length === 1 && form.length === 1) || timer === 50) {
                    nameObject = $("#inputUsername");
                    pwrdObject = $("#inputPassword");
                    pwrdObject2 = $("#inputPassword2");
                    checkbox = $("#newLogin");
                    observable.unsubscribe();
                    done();
                }
            });
        });

        it("Login form - verify modal with login loaded", function (done) {
            expect(modal[0]).toBeInDOM();
            expect(nameObject[0]).toExist();

            closeButton = $(".close-modal:first");
            done();
        });

        it("Login form - verify required fields", function (done) {
            expect(nameObject[0].validity.valueMissing).toBe(true);
            expect(pwrdObject[0].validity.valueMissing).toBe(true);
            done();
        });

        it("Login form - verify user name pattern mismatch", function (done) {
            nameObject.val("abcd"); // too short
            expect(nameObject[0].validity.patternMismatch).toBe(true);
            nameObject.val("1234567890123456789012345"); // too long
            expect(nameObject[0].validity.patternMismatch).toBe(true);
            nameObject.val("abcde");
            expect(nameObject[0].validity.patternMismatch).toBe(false);
            done();
        });

        it("Login form - verify password pattern mismatch", function (done) {
            pwrdObject.val("Secret01"); // no special character
            expect(pwrdObject[0].validity.patternMismatch).toBe(true);
            pwrdObject.val("Secret1"); // too short
            expect(pwrdObject[0].validity.patternMismatch).toBe(true);
            pwrdObject.val("1234567890123456789012341234567890123456789012345"); // too long
            expect(pwrdObject[0].validity.patternMismatch).toBe(true);
            pwrdObject.val("secret$1");
            expect(pwrdObject[0].validity.patternMismatch).toBe(false);
            done();
        });
        /* Testing Java backend can only happen in remote testing */
        if(!window._local) {
            it("Login form - Make sure test user removed", function (done) {
                loginButton = $(".modal .submit-login");
                
                if(loginButton.length === 0) {
                    done();
                    return;
                }

                sessionStorage.removeItem("credentials");
                sessionStorage.setItem("credentials", `{"name":"abcde", "password":"945973053"}`)

                login(loginButton, false)("DELETE", loginButton, "/userlogin/unregister").then(data => {
                        const credentials = sessionStorage.getItem("credentials");
                        expect(data.status > -1).toBe(true);
                        expect(credentials).toBe(null);
                        done();
                });
            });

            it("Login form - verify java vertx backend routing", function (done) {
                loginButton.click();
                let credentials;
                let badUser;
                let isValid; 
                
                const numbers = timer(75, 50);
                const observable = numbers.subscribe(timer => {
                    badUser = pwrdObject[0].validity.valueMissing === true;
                    isValid = pwrdObject[0].checkValidity(); 
                    credentials = sessionStorage.getItem("credentials");
                    // Trying to login with invalid user
                    if ((!isValid && !credentials && badUser) || timer === 75) {
                        expect(badUser).toBe(true);
                        expect(credentials).toEqual(null);
                        expect(isValid).toBe(false);
                        observable.unsubscribe();
                        done();
                    }
                });
            });

            it("Login form - Create New User, click checkbox & fillout form", done => {
                let numbers = timer(100, 30);
                let observable = numbers.subscribe(timer => {
                    modal = $("#modalTemplate");
                    form = modal.find("form:first");
                    if(form.length === 1 || timer === 40) {
                        checkbox = $("#newLogin");
                        checkbox.click();
                        nameObject = $("#inputUsername");
                        nameObject.val("abcde");
                        pwrdObject = $("#inputPassword");
                        pwrdObject.val("secret$1");
                        pwrdObject2 = $("#inputPassword2");
                        pwrdObject2.val(pwrdObject.val());
                        expect(nameObject.length === 1).toBe(true);
                        expect(checkbox.prop("checked") === true).toBe(true);
                        expect(pwrdObject.val()).toBe(pwrdObject2.val());
                        observable.unsubscribe();
                        done();
                    }
                });
            });

            it("Login form - Create New User, submit & verify new user", done => {
                const badUser = pwrdObject2[0].validity.valueMissing === true;
                const isValid = pwrdObject2[0].checkValidity();
                let returnValue;
                
                loginButton.click();

                let numbers = timer(100, 100);
                let observable = numbers.subscribe(timer => {
                    returnValue = sessionStorage.getItem("credentials");
                    
                    if ((isValid && returnValue !== null && !badUser) || timer === 90) {
                        expect(returnValue).not.toEqual(null);
                        expect(badUser).toBe(false);
                        expect(isValid).toBe(true);
                        observable.unsubscribe();
                        done();
                    }
                });
            });

            it("Login form - Log Out User", function (done) {
                let html = $("small .login").html();
                expect(html).toBe("Log Out");

                loginObject.on( "click", function(e) {
                    if($(".login:first").html() === "Log Out") {
                        login(null, true)(e);
                    } else {
                        Start["div .login click"](e);
                    }
                });

                loginObject.click();

                let numbers = timer(100, 50);
                let observable = numbers.subscribe(timer => {
                    let credentials = sessionStorage.getItem("credentials");
                    html = $("small .login").html();
                    if (html === "Log In" || timer === 50) {
                        expect(html).toBe("Log In");
                        expect(credentials).toEqual(null);
                        observable.unsubscribe();
                        done();
                    }
                });
            });

            it("Login form - Reopen Login", function (done) {
                loginObject.click();
                let numbers = timer(50, 50);
                let observable = numbers.subscribe(timer => {
                    modal = $("#modalTemplate");

                    if ((modal.hasClass("show") && timer > 15) || timer === 50) { // Give modal a chance to open
                        expect(modal[0]).toBeVisible();
                        observable.unsubscribe();
                        done();
                    }
                });
            });

            it("Login form - Remove added User", function (done) { // note: unregister is not a component
                sessionStorage.removeItem("credentials");
                sessionStorage.setItem("credentials", `{"name":"abcde", "password":"945973053"}`);
                login(loginButton, false)("DELETE", loginButton, "/userlogin/unregister").then(data => {
                    const credentials = sessionStorage.getItem("credentials");
                    expect(data.status > 0).toBe(true);
                    expect(credentials).toBe(null);
                    done();
                });
            });
        }

        it("Login form - verify cancel and removed from DOM", function (done) {
            expect(modal[0]).toExist();
            
            closeButton = $(".close-modal");
            closeButton.click();
            
            const numbers = timer(50, 50);
            const observable = numbers.subscribe(timer => {
                if ((modal.hasClass("show") === false && timer > 15) || timer === 50) { // give modal a chance to close
                    expect(modal[0]).not.toBeVisible();
                    expect(modal[0]).not.toBeInDOM();
                    observable.unsubscribe();
                    done();
                }
            });
        });
    });
}
