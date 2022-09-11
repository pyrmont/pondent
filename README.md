# Pondent

Pondent is a client-side utility for posting Markdown-formatted files to GitHub.
You can use it [in your browser][app] right now.

[app]: https://pyrmont.github.io/pondent/

## Rationale

A static site is a great way to make your site safer and more performant.
Combine it with GitHub and you have a version-controlled directory you can
access from anywhere.

The problem is how to update it? Pondent provides a client-side utility for
adding Markdown-formatted posts to a GitHub repository in a format perfect for
static site generators like Jekyll and Hugo.

## Usage

Using Pondent is as easy as 1-2-3!

1. Visit the [URL][app]
2. Enter your GitHub repository settings
3. Authorise with GitHub

You're now ready to post!

## Self-Hosting

Pondent is a client-side JavaScript application written in ClojureScript. To host
it locally or on your own server, you need to compile it to a JavaScript file that
your browser can run.

Note that if you do run it yourself, you'll need to register a personal access
token. The Pondent application will not be able to authenticate if run on a
server other than pyrmont.github.io.

### Compilation

Pondent uses Figwheel to compile your ClojureScript.

In development, Figwheel runs a local web server and recompiles on the fly:

```console
$ clojure -M:fig:dev
```

For production, Figwheel compiles your code into a single JavaScript file
you host on your own server:

```console
$ clojure -M:fig:release
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
