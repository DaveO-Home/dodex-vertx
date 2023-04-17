import routerTest from "./routertest";
import domTest from "./domtest";
import toolsTest from "./toolstest";
import contactTest from "./contacttest";
import loginTest from "./logintest";
import dodexTest from "./dodextest";
import inputTest from "./inputtest";
import Start from "../js/controller/start";
import Helpers from "../js/utils/helpers";
import React, { useEffect } from "react";
import { createRoot } from 'react-dom/client';
import StartC, { getStartComp } from "../components/StartC";
import { getPdfComp } from "../components/PdfC";
import Tools, { getToolsComp } from "../components/ToolsC";
import LoginC from "../components/LoginC";
import Menulinks, { Dodexlink } from "../Menulinks";
import { timer } from "rxjs";
import dodex from "dodex";
import input from "dodex-input";
import mess from "dodex-mess";
import { act } from "@testing-library/react";
import ErrorBoundary from "../components/ErrorBoundary";
// import { render, fireEvent, cleanup, waitForElement, waitFor } from 'react-testing-library'

export default function (App) {
    if(!window.main) {
        const container = document.getElementById("main_container");
        window.main = createRoot(container);
    }

    describe("Application Integration suite - AppTest", () => {
        beforeAll(() => {
            karmaDisplay();
            
            spyOn(App, "loadController").and.callThrough();
            spyOn(App, "renderTools").and.callThrough();
            spyOn(Helpers, "isResolved").and.callThrough();
            sessionStorage.removeItem("credentials");  // fake login to test table & dodex
            sessionStorage.setItem("credentials", `{"name":"abcde", "password":"945973053"}`);

            if(!window.rootRoot) {
                const rootContainer = document.getElementById("root");
                window.rootRoot = createRoot(rootContainer);
            }
            act(() => {
                window.rootRoot.render(
                    <Menulinks />
                );
            });
        }, 5000);

        afterAll(() => {
            $("body").empty();
            window.parent.scrollTo(0, 0);
        }, 3000);

        it("Is Welcome Page Loaded", done => {
            /*
             * Loading Welcome page.
             */
            getStartComp().then((StartComp) => {
                act(() => {
                    window.main.render(
                        <StartComp/>
                    );
                });
               const numbers = timer(50, 50);
               const observable = numbers.subscribe(timer => {
                   if (App.controllers["Start"] !== null || timer === 25) {
                       expect(App.controllers["Start"]).not.toBeUndefined();
                       expect(document.querySelector("#main_container span").children.length > 3).toBe(true);
                       domTest("index", document.querySelector("#main_container"));
                       observable.unsubscribe();
                       done();
                    }
                });
            });
        });

        it("Is Pdf Loaded", done => {
                const PdfComp = getPdfComp;
                act(() => {
                    window.main.render(
                        <PdfComp/>
                    );
                });
               const numbers = timer(50, 50);
               const observable = numbers.subscribe(timer => {
                   if ($("#main_container").find("#data[src$=\"Test.pdf\"]") !== null || timer === 50) {
                       expect(document.querySelector("#main_container").children.length === 1).toBe(true);
                       domTest("pdf", document.querySelector("#main_container"));
                       observable.unsubscribe();
                       done();
                    }
                });
        });

        it("Is Tools Table Loaded", done => {
            getToolsComp().then((ToolsComp) => {
                act(() => {
                    window.main.render(
                        <ToolsComp />
                    );
                });
               const numbers = timer(50, 50);
               const observable = numbers.subscribe(timer => {
                   if (App.controllers["Table"] !== null || timer === 25) {
                       expect(App.loadController).toHaveBeenCalled();
                       expect(App.renderTools.calls.count()).toEqual(1);
                       expect(App.controllers["Table"]).not.toBeUndefined();
                       expect(document.getElementById("main_container").querySelector("#tools").children.length > 1).toBe(true);
                       domTest("tools", document.querySelector("#main_container"));
                       observable.unsubscribe();
                       done();
                   }
               });
            });
        });

        routerTest("pdf", timer);
        routerTest("table", timer);

        // Executing here makes sure the tests are run in sequence.
        // Spec to test if page data changes on select change event.
        toolsTest(Tools, Helpers, React, timer);
        // Form Validation
        contactTest(timer, React);
        // Verify modal form
        loginTest(Start, React, LoginC, timer);
//        // Test dodex
        dodexTest(dodex, input, mess, getAdditionalContent(), Start, timer, React, createRoot, Dodexlink);
//        // Test dodex input
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
    let url = "http://localhost:8089/dist_test/react-fusebox/appl/testapp_dev.html";
    if(window._local) {
        url = "/base/dist_test/react-fusebox/appl/app_bootstrap.html";
    }
    const originalConsoleError = console.error;
    console.error = (...args) => {
      const firstArg = args[0];
      if (
        typeof args[0] === 'string' &&
        (args[0].startsWith(
          "Warning: An update to %s inside a test was not wrapped in act(...)"
        ))
      ) {
        return;
      }
      originalConsoleError.apply(console, args);
    };

//    $("body").load(url, function () {
//        if(!window.rootRoot) {
//            const rootContainer = document.getElementById("root");
//            window.rootRoot = createRoot(rootContainer);
//        }
//        act(() => {
//            window.rootRoot.render(
//                <Menulinks />
//            );
//        });
//
//        if(!window.loginRoot) {
//            const loginContainer = document.querySelector("nav-login");
//            window.loginRoot = createRoot(loginContainer);
//        }
//        act(() => {
//            window.loginRoot.render(
//                <LoginC />
//            );
//        });
//
//        if(!window.dodexRoot) {
//            const dodexContainer = document.querySelector(".dodex--ico");
//            window.dodexRoot = createRoot(dodexContainer);
//        }
//        act(() => {
//            window.dodexRoot.render(
//                <Dodexlink />
//            );
//        });
//    });
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
