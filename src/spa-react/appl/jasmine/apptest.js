import routerTest from "./routertest";
import domTest from "./domtest";
import toolsTest from "./toolstest";
import contactTest from "./contacttest";
import loginTest from "./logintest";
import dodexTest from "./dodextest";
import inputTest from "./inputtest";
import Start from "../js/controller/start";
import Helpers from "../js/utils/helpers";
import React from "react";
import ReactDOM from "react-dom";
import StartC, { getStartComp } from "../components/StartC";
import { getPdfComp } from "../components/PdfC";
import ToolsC from "../components/ToolsC";
import LoginC from "../components/LoginC";
import { Dodexlink, Menulinks } from "../Menulinks";
import { timer } from "rxjs";
import dodex from "dodex";
import input from "dodex-input";
import mess from "dodex-mess";

export default function (App) {
    karmaDisplay();

    describe("Application Unit test suite - AppTest", () => {
        beforeAll(() => {
            spyOn(App, "loadController").and.callThrough();
            spyOn(App, "renderTools").and.callThrough();
            spyOn(Helpers, "isResolved").and.callThrough();
            sessionStorage.removeItem("credentials");  // fake login to test table & dodex
            sessionStorage.setItem("credentials", `{"name":"abcde", "password":"945973053"}`);
        }, 5000);

        afterAll(() => {
            $("body").empty();
            window.parent.scrollTo(0, 0);
        }, 3000);

        it("Is Welcome Page Loaded", done => {
            /*
             * Loading Welcome page.
             */
            getStartComp().then(function (StartComp) {
                ReactDOM.render(
                    <StartComp />,
                    document.getElementById("main_container"),
                    // Using the react callback to run the tests
                    function () {
                        expect(App.controllers["Start"]).not.toBeUndefined();
                        expect(document.querySelector("#main_container span").children.length > 3).toBe(true);
                        domTest("index", document.querySelector("#main_container"));
                        done();
                    }
                );
            });
        });

        it("Is Pdf Loaded", done => {
            ReactDOM.render(
                getPdfComp(),
                document.querySelector("#main_container"),
                function () {
                    expect(document.querySelector("#main_container").children.length === 1).toBe(true);

                    domTest("pdf", document.querySelector("#main_container"));
                    done();
                }
            );
        });

        it("Is Tools Table Loaded", done => {
            /*
             * Letting the Router load the appropriate page.
             */
            ReactDOM.render(
                <ToolsC />,
                document.querySelector("#main_container")
            );
            Helpers.getResource(ReactDOM, "main_container", 0, 0)
                .catch(rejected => {
                    fail(`The Tools Page did not load within limited time: ${rejected}`);
                }).then(() => {
                    expect(App.loadController).toHaveBeenCalled();
                    expect(App.renderTools.calls.count()).toEqual(1);
                    expect(App.controllers["Table"]).not.toBeUndefined();
                    expect(document.getElementById("main_container").querySelector("#tools").children.length > 1).toBe(true);
                    domTest("tools", document.querySelector("#main_container"));
                    done();
                });
        });
 
        routerTest("pdf");
        routerTest("table");

        // Executing here makes sure the tests are run in sequence.
        // Spec to test if page data changes on select change event.
        toolsTest(ToolsC, Helpers, ReactDOM, React, timer);
        // Form Validation
        contactTest(timer);
        // Verify modal form
        loginTest(Start, Helpers, ReactDOM, React, StartC, LoginC, timer);
        // Test dodex
        dodexTest(dodex, input, mess, getAdditionalContent(), Start, timer);
        // Test dodex input
        inputTest(dodex, timer);

        jasmine.DEFAULT_TIMEOUT_INTERVAL = 4000;
        __karma__.start();

        if (testOnly) {
            it("Testing only", () => {
                fail("Testing only, build will not proceed");
            });
        }
    });
}

async function karmaDisplay() {
    // Load of test page(without html, head & body) to append to the Karma iframe
    let url = "http://localhost:8087/dist_test/react-fusebox/appl/testapp_dev.html";
    if(window._local) {
        url = "/base/dist_test/react-fusebox/appl/appv_bootstrap.html";
    }
    $("body").load(url, function () {
        ReactDOM.render(
            <Menulinks />,
            document.getElementById("root")
        );
        ReactDOM.render(
            <LoginC />,
            document.getElementById("nav-login")
        );
        ReactDOM.render(
            <Dodexlink />,
            document.querySelector(".dodex--ico")
        );
    });
}

function getAdditionalContent() {
    return {
        cards: {
            card28: {
                tab: "F01999", // Only first 3 characters will show on the tab.
                front: {
                    content: `<h1 style="font-size: 10px;">Friends</h1>
					<address style="width:385px">
						<strong>Charlie Brown</strong> 	111 Ace Ave. Pet Town
						<abbr title="phone"> : </abbr>555 555-1212<br>
						<abbr title="email" class="mr-1"></abbr><a href="mailto:cbrown@pets.com">cbrown@pets.com</a>
					</address>
					`
                },
                back: {
                    content: `<h1 style="font-size: 10px;">More Friends</h1>
					<address style="width:385px">
						<strong>Lucy</strong> 113 Ace Ave. Pet Town
						<abbr title="phone"> : </abbr>555 555-1255<br>
						<abbr title="email" class="mr-1"></abbr><a href="mailto:lucy@pets.com">lucy@pets.com</a>
					</address>
					`
                }
            },
            card29: {
                tab: "F02",
                front: {
                    content: "<h1 style=\"font-size: 14px;\">My New Card Front</h1>"
                },
                back: {
                    content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
                }
            }
        }
    };
}
