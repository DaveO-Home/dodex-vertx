// eslint-disable-next-line
import "./config";
import Setup from "./utils/setup";
import popper from "popper.js";
import App from "./app";
import Default from "./utils/default";
import "tablesorter/dist/js/extras/jquery.tablesorter.pager.min.js";
import JSONEditor from "jsoneditor/dist/jsoneditor.min.js";
import "../entry";

/* develblock:start */
import apptest from "../jasmine/apptest";
window._bundler = "fusebox";
/* develblock:end */
window.JSONEditor = JSONEditor;
window.Popper = popper;
App.init(Default);
Setup.init();

/* develblock:start */
// Code between the ..start and ..end tags will be removed by the BlockStrip plugin during the production build.
// testit is true if running under Karma - see testapp_dev.html
/* eslint no-unused-vars: ["error", { "args": "none" }] */
new Promise((resolve, reject) => {
    setTimeout(function () {
        resolve();
    }, 500);
}).catch(rejected => {
    fail(`Error ${rejected}`);
}).then(resolved => {
    if (typeof window.testit !== "undefined" && window.testit) {
        // Run acceptance tests. - To run only unit tests, comment the apptest call.
        try {
            apptest(App);
        } catch(e) { 
            // console.error(e.message); 
        }
    }
});
/* develblock:end */
