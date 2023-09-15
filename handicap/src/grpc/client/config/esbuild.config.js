const esbuild = require("esbuild");
const { htmlPlugin } = require("@craftamap/esbuild-plugin-html");
const copyStaticFiles = require('esbuild-copy-static-files')
const isProduction = process.argv[2] === "true";
// Clean up if webpack was used previously
if (isProduction) {
    import("del").then(del => {
        (async () => {
            await del.deleteAsync(["../../main/resources/static/dist/*"], { force: true, dryRun: false });
            build();
        })();
    });
} else {
    build();
}

function build() {
    const options = {
        entryPoints: ["js/client.js"],
        entryNames: isProduction ? "dist/main-[hash]" : "dist/main",
        bundle: true,
        metafile: true,
        outdir: "../../main/resources/static",
        platform: "browser",
        target: "esnext",
        minify: isProduction,
        loader: {
            ".svg": "file",
        },
        plugins: [
            htmlPlugin({
                files: [
                    {
                        entryPoints: [
                            "js/client.js",
                        ],
                        filename: "handicap.html",
                        findRelatedOutputFiles: true,
                        htmlTemplate: "html/index.template.html",
                    },
                ]
            }),
            copyStaticFiles({
                src: "./static",
                dest: "../../main/resources/static",
                dereference: true,
                errorOnExist: false,
                preserveTimestamps: true,
                recursive: false,
              })
        ]
    }

    esbuild.build(options).catch(() => process.exit(1));
}