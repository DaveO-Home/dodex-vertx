const esbuild = require("esbuild");
//const { htmlPlugin } = require("@craftamap/esbuild-plugin-html");
//const copyStaticFiles = require('esbuild-copy-static-files')
const isProduction = process.argv[2] === "true";

// Clean up if webpack was used previously
if (isProduction) {
    import("del").then(del => {
        (async () => {
            await del.deleteAsync(["../../../../src/main/resources/static/group/*"], { force: true, dryRun: false });
            build();
        })();
    });
} else {
    build();
}

function build() {
    const options = {
        entryPoints: ["js/dodex/startGroup.js"],
        entryNames: isProduction ? "main.min" : "main",
        bundle: true,
        metafile: true,
        outdir: "../../../../src/main/resources/static/group",
        platform: "browser",
        target: "esnext",
        minify: isProduction,
    }

    esbuild.build(options).catch(() => process.exit(1));
}
