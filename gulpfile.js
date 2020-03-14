const { src, dest, series, parallel, watch } = require("gulp");
const { exec, spawn } = require("child_process");

const cleancss   = require("gulp-clean-css");
const concat     = require("gulp-concat");
const del        = require("del");
const postcss    = require("gulp-postcss");
const purgecss   = require("gulp-purgecss");
const rename     = require("gulp-rename");
const replace    = require("gulp-replace");

var watcher;

function build_css() {
  return src("resources/public/css/*.css")
    .pipe(purgecss({
      content: ["resources/public/index.html", "src/pondent/**/*.cljs"],
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
    .pipe(src(["resources/css/github.css",
               "resources/css/symbols.css"]))
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
  var build_name;
  let i = process.argv.indexOf("dev");
  if (i == -1 || process.argv.length == (i + 1) || process.argv[i + 1].slice(0, 2) !== "--") {
    build_name = "build";
  } else {
    build_name = process.argv[i + 1].slice(2);
  }

  spawn("clojure", ["-A:fig:" + build_name], { stdio: "inherit" }).on("close", cb);
}

function move_html() {
  return src("resources/public/index.html")
    .pipe(replace(/cljs\-out\/.*?js/, "js/app.min.js"))
    .pipe(replace(/<link href.*"text\/css">/sm,
                  "<link href=\"css/style.min.css\" rel=\"stylesheet\" type=\"text/css\">"))
    .pipe(dest("docs/"));
}

function stop_watch(cb) {
  watcher.watch.close();
  watcher.cb();
  cb();
}

function watch_css(cb) {
  watcher = { watch: watch("resources/css/*.css", build_css_dev),
              cb: cb };
}

exports.dev = parallel(series(build_css_dev, compile_js_dev, stop_watch),
                       watch_css);
exports.release = series(clean_files, parallel(compile_js,
                                               series(build_css_dev, build_css),
                                               move_html));

exports.build_css = build_css;
exports.build_css_dev = build_css_dev;
exports.clean_files = clean_files;
exports.compile_js = compile_js;
exports.move_html = move_html;

exports.default = exports.release;
