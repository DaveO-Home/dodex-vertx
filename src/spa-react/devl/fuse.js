/*eslint quotes: [2, "double", {"avoidEscape": true, "allowTemplateLiterals": true}]*/
/*eslint no-unused-vars: 0 no-console: 0*/
/*eslint-env es6*/
const path = require("path");
const { fusebox, sparky } = require("fuse-box");
const { pluginStripCode } = require("../appl/js/plugin/StripCode");
let isProduction = process.env.NODE_ENV === "production";
let distDir;
let local = false;
let config = {};

class Context {
    getConfig() {
        return fusebox(config);
    }
}

const run = function (mode, configure, debug, cb) {
    // if ("-d" === process.argv[3] || debug) {
    if (process.argv.includes("-d", 2) || debug) {
        console.log(configure);
    }
    if (process.argv.includes("-l", 2) || process.argv.includes("-local", 2)) {
        local = true;
    }

    // local means the test applications will not be in the vertx classpath and is only used for local javascript frontend development/testing
    if(local) {
        distDir = mode === "prod" ? path.join(__dirname, "../../dist/react-fusebox") :
            mode === "preview" ? path.join(__dirname, "../../dist/react-fusebox") : path.join(__dirname, "../../dist_test/react-fusebox");
    } 
    // remote(default) puts the application in the vertx classpath and can be accessed in the resulting vertx verticle.
    else {
        distDir = mode === "prod" ? path.join(__dirname, "../../main/resources/static/dist/react-fusebox") :
            mode === "preview" ? path.join(__dirname, "../../main/resources/static/dist/react-fusebox") : path.join(__dirname, "../../main/resources/static/dist_test/react-fusebox");
    }
    
    isProduction = mode !== "test";
    config = configure;

    if (mode !== "test") {
        addStripCodePlugin(config);
    }

    const { task, src, exec, rm } = sparky(Context);

    task("clean", context => {
        rm(distDir);
    });

    task("cleanTest", context => {
        rm(path.join(__dirname, "../../main/resources/static/dist_test"));
    });

    task("cache", context => {
        rm(`${__dirname}/.cache`);
    });

    task("copy", async context => {
        try {
            await src(path.join(__dirname, "../appl/views/**/**.*"))
                .dest(`${distDir}/appl`, "appl")
                .exec();
            await src(path.join(__dirname, `../appl/templates/**/**.*`))
                .dest(`${distDir}/appl`, "appl")
                .exec();
            await src(path.join(__dirname, `../appl/assets/**/**.*`))
                .dest(`${distDir}/appl`, "appl")
                .exec();
            await src(path.join(__dirname, `../images/*.*`))
                .dest(`${distDir}/images`, "images")
                .exec();
            await src(path.join(__dirname, `../appl/app_bootstrap.html`))
                .dest(`${distDir}/appl`, "appl")
                .exec();
            await src(path.join(__dirname, `../README.md`))
                .dest(`${distDir}/..`, "spa-react")
                .exec();
            await src(path.join(__dirname, `../README.md`))
                .dest(`${distDir}/`, "spa-react")
                .exec();
            await src(path.join(__dirname, `../index.html`))
                    .dest(`${distDir}/`, "spa-react")
                    .exec();
            await src(path.join(__dirname, `../appl/dodex/data/**/**.*`))
                .dest(`${distDir}/appl`, "appl")
                .exec();
        } catch (e) { console.error(e); }
    });

    task("copytable", async context => {
        await src(path.join(__dirname, "../appl/css/table.css"))
            .dest(`${distDir}`, "appl")
            .exec();
    });

    task("copyhello", async context => {
        await src(path.join(__dirname, "../appl/css/hello*.css"))
            .dest(`${distDir}`, "appl")
            .exec();
    });

    task("test", async context => {
        await exec("clean");
        await exec("cache");
        await exec("copy");
        await exec("copytable");
        await exec("copyhello");
        const fuse = context.getConfig();
               
        const { onComplete } = await fuse.runDev(
            { bundles: { 
                distRoot: distDir,
                app: "app.js" 
            } 
        });

        onComplete(output => {
            if (typeof cb === "function") {
                if (!config.cache.FTL) { // We may be doing tdd (gulp development)
                    setTimeout(function () { // The build finishes before resources are completed.
                        cb();
                    }, 500);
                } else {
                    cb(); // restart gulp task, tests will start
                }
            }
        });
    });

    task("preview", async context => {
        await exec("clean");
        await exec("copy");
        await exec("copytable");
        await exec("copyhello");
        const fuse = context.getConfig();
        
        const { onComplete } = await fuse.runProd({ 
            uglify: false,
            bundles: { 
                    distRoot: distDir,
                    app: "app.js",
                    vendor: "vendor.js"
                }
            });

        onComplete(output => {
            if (typeof cb === "function") {
                if (!config.cache.FTL) { // We may be doing tdd (gulp development)
                    setTimeout(function () { // The build finishes before resources are completed.
                        cb();
                    }, 500);
                } else {
                    cb(); // restart gulp task, tests will start
                }
            }
        });
    });

    task("prod", async context => {
        await exec("clean");
        await exec("cleanTest");
        await exec("copy");
        await exec("copytable");
        await exec("copyhello");
        const fuse = context.getConfig();
     
        const { onComplete } = await fuse.runProd({ 
            uglify: true,
            bundles: { distRoot: distDir,
                app: { path: "app.$hash.js" },
                vendor: { path: "vendor.$hash.js" }
            }
        });
        
        onComplete(output => {
            if (typeof cb === "function") {
                if (!config.cache.FTL) { // We may be doing tdd (gulp development)
                    setTimeout(function () { // The build finishes before resources are completed.
                        cb();
                    }, 500);
                } else {
                    cb(); // restart gulp task, tests will start
                }
            }
        });
    });

    task("default", context => {
        switch (mode) {
            case "preview":
                exec("preview");
                break;
            case "prod":
                exec("prod");
                break;
            case "copy":
                exec("copy");
                exec("copytable");
                exec("copyhello");
                break;
            case "test":
            default:
                exec("test");
                break;
        }
    });
};

function addStripCodePlugin(config) {
    const whichFiles = /(\/js\/.*\.js|index(\.js|\.ts)|entry(\.js|\.ts))/;
    const startComment = "develblock:start";
    const endComment = "develblock:end";
    try {
        config.plugins.push(pluginStripCode(whichFiles, { "start": startComment, "end": endComment }));
    } catch (e) {
        console.error(e);
        process.exit(-1);
    }
}

module.exports = run;
