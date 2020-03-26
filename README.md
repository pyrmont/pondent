# Pondent

Pondent is a client-side utility for posting Markdown-formatted files to GitHub.
You can use it [in your browser][app] right now.

[app]: https://pyrmont.github.io/pondent/

## Rationale

A static site is a great way to make your site safer and more performant.
Combine it with GitHub and you have a version-controlled directory you can
access from anywhere.

The problem is how to update it?

Pondent provides a client-side utility for adding Markdown-formatted posts to a
GitHub repository in a format perfect for static site generators like Jekyll and
Hugo.

## Installation

Pondent runs in your browser and requires no installation. However, follow these
instructions and you can run it locally or on your own server.

Note that if you do want to run it yourself, you'll need to register a personal
access token. The Pondent application will not be able to authenticate if run on
a server other than pyrmont.github.io.

### Requirements

Pondent is a client-side JavaScript application written in ClojureScript. To
compile it, you'll need:

- Clojure
- Node.js

### Setup

```console
$ git clone git@github.com:pyrmont/pondent.git
$ cd pondent
$ npm install
```

### Compilation

Pondent uses Figwheel to compile your ClojureScript. The `gulpfile.js` included
in the project root provides a `dev` task that will compile the CSS and
ClojureScript code, kick off an instance of Figwheel and start a watch on the
CSS files in `resources/css/`.

By default, Gulp will start Figwheel using the aliases `fig` and `build` that
are defined in the `deps.edn` file. To use an alias other than `build` (e.g.
`test`), pass the build name as a parameter to the `dev` task.

For development:

```console
$ gulp dev     # Use gulp dev --[buildname] for alternate aliases
```

For production:

```console
$ gulp release
```

## Limitations

Pondent does not support the editing of existing files.

## Bugs

Found a bug? I'd love to know about it. The best way is to report them in the
[Issues section][ghi] on GitHub.

[ghi]: https://github.com/pyrmont/pondent/issues

## Licence

Pondent is released into the public domain. See [LICENSE][lc] for more details.

[lc]: https://github.com/pyrmont/pondent/blob/master/LICENSE
