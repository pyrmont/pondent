const { src, dest, series, parallel } = require("gulp");

const cleancss = require("gulp-clean-css");
const concat   = require("gulp-concat");
const del      = require("del");
const exec     = require("child_process").exec;
const purgecss = require("gulp-purgecss");
const rename   = require("gulp-rename");
const replace  = require("gulp-replace");

function build_css() {
  return src("resources/public/css/*.css")
    .pipe(purgecss({
      content: ["resources/public/index.html", "src/pondent/core.cljs"],
      defaultExtractor: content => content.match(/\w[\w/:-]*/g) || []
    }))
    .pipe(cleancss())
    .pipe(concat("style.css"))
    .pipe(rename({ suffix: ".min" }))
    .pipe(dest("docs/css/"));
}

function clean_files() {
  return del(["docs"]);
}

function compile_js(cb) {
  exec("clojure -A:fig:release", function (err, stdout, stderr) {
    cb(err);
  });
}
function move_html() {
  return src("resources/public/index.html")
    .pipe(replace(/cljs\-out\/.*?js/, "js/app.min.js"))
    .pipe(replace(/<link href.*"text\/css">/sm,
                  "<link href=\"css/style.min.css\" rel=\"stylesheet\" type=\"text/css\">"))
    .pipe(dest("docs/"));
}

exports.build_css = build_css;
exports.clean_files = clean_files;
exports.compile_js = compile_js;
exports.move_html = move_html;

exports.default = series(clean_files, parallel(compile_js, build_css, move_html));
