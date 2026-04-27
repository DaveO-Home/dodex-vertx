const esbuild = require("esbuild");
const isProduction = process.argv[2] === "true";

build();

function build() {
    const options = {
        entryPoints: ["js/dodex/monitor.js"],
        entryNames: "main",
        bundle: true,
        metafile: true,
        outdir: "../../../../src/main/resources/static/monitor",
        platform: "browser",
        target: "esnext",
        minify: isProduction,
        loader: {
            ".svg": "file",
        },
    }

    esbuild.build(options).catch(() => process.exit(1));
}
