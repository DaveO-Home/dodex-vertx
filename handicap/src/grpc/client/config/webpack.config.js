const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const { CleanWebpackPlugin } = require("clean-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");
const del = import("del");

let isProduction = false;
const publicPath = process.env.PUBLIC_PATH ? process.env.PUBLIC_PATH : "./dist";

let cleanOptions = {
    verbose: false,
    dry: false
}

module.exports = (env, argv) => {
    isProduction = argv.mode === "production";
    let stripCode = {};
    if (isProduction) {
        stripCode = {
            loader: "webpack-strip-block",
            options: {
                start: "develblock:start",
                end: "develblock:end"
            }
        }
    }
    
    return {
        mode: argv.mode,
        // watch: false,
        watch: argv.watch === "true",
        watchOptions: {
            ignored: /node_modules/
        },
        resolve: {
            symlinks: false,
            cacheWithContext: false,
            alias: {
                bootcss: "../node_modules/bootstrap/dist/css/bootstrap.min.css",
                sitecss: "../css/site.css",
                bootstrap: "bootstrap/dist/js/bootstrap.min.js"
            },
            modules: [
                path.resolve("../"),
                "node_modules",
            ]
        },
        context: path.resolve(__dirname, "../"),
        entry: {
            main: "./js/client.js"
        },
        target: "web",
        output: {
            filename: isProduction ? "[name].[chunkhash].js" : "[name].js",
            chunkFilename: isProduction ? "chunk.[chunkhash].js" : "[name].chunk.js",
            path: path.resolve(__dirname, "../../../main/resources/static/dist"),
            publicPath: publicPath
        },
        module: {
            rules: [
                {
                    test: /\.(css|sass|scss)$/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        {
                            loader: "css-loader",
                        },
                        {
                            loader: "sass-loader"
                        }
                    ],
                    type: "javascript/auto"
                }
            ]
        },
        plugins: [
            new webpack.DefinePlugin({
                "process.env": {
                    NODE_ENV: isProduction ? '"production"' : '"development"',
                    PUBLIC_PATH: '""'
                }
            }),
            new CleanWebpackPlugin(cleanOptions),
            new MiniCssExtractPlugin({
                filename: !isProduction
                    ? "main.css" : "[name].[contenthash].css",
                chunkFilename: !isProduction
                    ? "[name].[id].css" : "[name].[id].[contenthash].css"
            }),
            new HtmlWebpackPlugin({
                filename: path.resolve("../../main/resources/static/handicap.html"),
                template: "./html/index.template.html",
                inject: true,
                minify: {
                    removeComments: true,
                    collapseWhitespace: false,
                    removeAttributeQuotes: true
                },
                chunksSortMode: "auto" // "dependency"
            }),
            new CopyPlugin({
                patterns: [
                  { from: path.resolve("./static"), to: path.resolve("../../main/resources/static/") },
                ],
              }),
        ],
        devtool: isProduction ? "cheap-module-source-map" : "inline-source-map",
        performance: {
            hints: false,
        },
        stats: "minimal", // minimal, none, normal, verbose, errors-only
        cache: isProduction ? true : false,
        optimization: isProduction ? {
            minimize: true,
            minimizer: [
                new TerserPlugin({
                    extractComments: "some",
                    parallel: true,
                }),
            ],
            moduleIds: "deterministic",
            splitChunks: {
                chunks: "all",
            },
            runtimeChunk: "single",
        } : {},
    }
};
