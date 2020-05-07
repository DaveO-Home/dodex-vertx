const fs = require('fs');
const path = require("path");
const bundler = "react-fusebox";
const spa = "spa-react";
let mocha = ["mocha"];
let distDir = "main/resources/static/dist_test/";
let base = "http://localhost:8087/dist_test/" + bundler;
let startupHtml = "http://localhost:8087/dist_test/react-fusebox/appl/testapp_dev.html";
try {
    fs.unlinkSync(path.join(__dirname, "karma.bootstrap2.js"));
} catch (e) {
    console.info(e.message);
}
/* Getting global value into the window envirnoment */
const data = fs.readFileSync(path.join(__dirname, "karma.bootstrap.js"), 'utf8');
const result = data.replace(/local = false/, "local = " + global.local);
fs.writeFileSync(path.join(__dirname, "karma.bootstrap2.js"), result, "utf8");

if (global.local) {
    distDir = "dist_test/";
    base = "/base/dist_test/" + bundler;
    startupHtml = "dist_test/" + bundler + "/appl/testapp_dev.html";
}
if(global.allure === true) {
    mocha.push("allure");
}

// Karma configuration
module.exports = function (config) {
    // whichBrowser to use from gulp task.
    if (!global.whichBrowser) {
        global.whichBrowser = ["ChromeHeadless"/*, "FirefoxHeadless"*/];
    }

    config.set({
        // base path that will be used to resolve all patterns (eg. files, exclude)
        basePath: "../../",
        // frameworks to use
        // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
        frameworks: ["jasmine-jquery", "jasmine"],
        proxies: {
            /* Integration testing with backend Java Vertx server */
            "/views/": base + "/appl/views/",
            "/templates/": base + "/appl/templates/",
            "/resources/": base + "/appl/resources/",
            "/dodex/data/": base + "/appl/dodex/data/",
            "/dist_test/react-fusebox/appl/assets/": base + "/appl/assets/",
            "/base/dist_test/react-fusebox/appl/": base + "/appl/",
            "/README.md": base + "/README.md",
            "/images/": base + "/images/",
            "/userlogin/unregister": "http://localhost:8087/userlogin/unregister",
            "/userlogin": "http://localhost:8087/userlogin",
            "/app.js": distDir + bundler + "/app.js"
        },
        // list of files / patterns to load in the browser
        files: [
            // Webcomponents for non-Chrome Browsers - used for link tag with import attribute.
            { pattern: spa + "/tests/webcomponents-hi-sd-ce.js", watched: false },
            // Application and Acceptance specs.
            startupHtml,
            // Jasmine tests
            spa + "/tests/unit_tests*.js",
            // spa + "/node_modules/promise-polyfill/promise.js",
            // Looking for changes to the client bundle
            { pattern: distDir + bundler + "/app.js", included: false, watched: true },
            { pattern: distDir + bundler + "/**/*.*", included: false, watched: false },
            { pattern: spa + "/package.json", watched: false, included: false },
            // Jasmine/Loader tests and starts Karma
            spa + "/devl/karma.bootstrap2.js"
        ],
        bowerPackages: [
        ],
        plugins: [
            "karma-chrome-launcher",
            "karma-firefox-launcher",
            "karma-opera-launcher",
            "karma-jasmine",
            "karma-jasmine-jquery",
            "karma-mocha-reporter",
            "karma-allure-reporter"
        ],
        /* Karma uses <link href="/base/appl/testapp_dev.html" rel="import"> -- you will need webcomponents polyfill to use browsers other than Chrome.
         * This test demo will work with Chrome/ChromeHeadless by default - Webcomponents included above, so FirefoxHeadless should work also. 
         * Other browsers may work with tdd.
         */
        browsers: global.whichBrowser,
        customLaunchers: {
            FirefoxHeadless: {
                base: "Firefox",
                flags: ["--headless"]
            }
        },
        browserNoActivityTimeout: 0,
        exclude: [
        ],
        preprocessors: {
        },
        reporters: mocha,
        allureReport: {
            reportDir: "./spa-react/devl/allure-results", // By default files will be save in the base dir
            useBrowserName: true // add browser name to report and classes names
        },
        port: 9876,
        colors: true,
        // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
        logLevel: config.LOG_WARN,
        autoWatch: true,
        autoWatchBatchDelay: 10000,  // Waiting for Vertx to rebuild when executing "gulp tdd"
        // Continuous Integration mode
        singleRun: false,
        loggers: [{
            type: "console"
        }
        ],
        client: {
            captureConsole: true,
            clearContext: false,
            runInParent: true,
            useIframe: true,
        },
        // how many browser should be started simultaneous
        concurrency: 10 // Infinity
    });
};
