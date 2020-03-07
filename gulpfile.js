const { src, dest, series, parallel } = require("gulp");
const { exec, spawn } = require("child_process");

const cleancss   = require("gulp-clean-css");
const concat     = require("gulp-concat");
const del        = require("del");
const postcss    = require("gulp-postcss");
const purgecss   = require("gulp-purgecss");
const rename     = require("gulp-rename");
const replace    = require("gulp-replace");

function build_css() {
  return src("resources/public/css/*.css")
    .pipe(purgecss({
      content: ["resources/public/index.html", "src/pondent/core.cljs"],
      defaultExtractor: content => content.match(/\w[\w/:-]*/g) || []
    }))
    .pipe(cleancss({level: {1: {specialComments: false}}}))
    .pipe(concat("style.css"))
    .pipe(rename({ suffix: ".min" }))
    .pipe(dest("docs/css/"));
}

function build_css_dev() {
  return src("resources/css/tailwind.css")
    .pipe(postcss([require("postcss-import"),
                   require("tailwindcss")]))
    .pipe(src("resources/css/github.css"))
    .pipe(concat("style.css"))
    .pipe(dest("resources/public/css/"));
}

function clean_files() {
  return del(["docs"]);
}

function compile_js(cb) {
  exec("clojure -A:fig:release", function (err, stdout, stderr) {
    cb(err);
  });
}

function compile_js_dev(cb) {
  spawn("clojure", ["-A:fig:remote"], { stdio: "inherit" }).on("close", cb);
}

function move_html() {
  return src("resources/public/index.html")
    .pipe(replace(/cljs\-out\/.*?js/, "js/app.min.js"))
    .pipe(replace(/<link href.*"text\/css">/sm,
                  "<link href=\"css/style.min.css\" rel=\"stylesheet\" type=\"text/css\">"))
    .pipe(dest("docs/"));
}

exports.dev = series(build_css_dev, compile_js_dev);
exports.release = series(clean_files, parallel(compile_js,
                                               series(build_css_dev, build_css),
                                               move_html));

exports.build_css = build_css;
exports.build_css_dev = build_css_dev;
exports.clean_files = clean_files;
exports.compile_js = compile_js;
exports.move_html = move_html;

exports.default = exports.release;
