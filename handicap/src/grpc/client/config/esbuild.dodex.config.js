const esbuild = require("esbuild");
//const { htmlPlugin } = require("@craftamap/esbuild-plugin-html");
//const copyStaticFiles = require('esbuild-copy-static-files')
const isProduction = process.argv[2] === "true";

// Clean up if webpack was used previously
if (isProduction) {
    import("del").then(del => {
        (async () => {
            await del.deleteAsync(["../../../../src/main/resources/static/dodex/*"], { force: true, dryRun: false });
            build();
        })();
    });
} else {
    build();
}

function build() {
    const options = {
        entryPoints: ["js/dodex/dodexWithGroup.js"],
        entryNames: isProduction ? "dodex.group" : "dodex.group",
        bundle: true,
        metafile: true,
        outdir: "../../../../src/main/resources/static/dodex",
        platform: "browser",
        target: "esnext",
        minify: isProduction,
        loader: {
            ".svg": "file",
        },
    }

    esbuild.build(options).catch(() => process.exit(1));
}
