# Pondent

Pondent is a client-side utility for posting Markdown-formatted files to GitHub.
You can use it [in your browser][app] right now.

[app]: https://pyrmont.github.io/pondent/

## Rationale

A static site is a great way to make your site safer and more performant.
Combine it with GitHub and you have a version-controlled you can access from
anywhere.

The problem is how to update it?

Pondent provides a client-side utility for adding Markdown-formatted posts to a
GitHub repository in a format perfect for static site generators like Jekyll and
Hugo.

## Installation

Pondent runs in your browser and requires no installation. However, follow these
instructions and you can run it locally or own your own server.

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

For development:

```console
$ gulp dev
```

For production

```console
$ gulp release
```

## Limitations

Pondent does not support the editing of existing files. It also does not support
image uploading.

## Bugs

Found a bug? I'd love to know about it. The best way is to report them in the
[Issues section][ghi] on GitHub.

[ghi]: https://github.com/pyrmont/pondent/issues

## Licence

Pondent is released into the public domain. See [LICENSE][lc] for more details.

[lc]: https://github.com/pyrmont/pondent/blob/master/LICENSE
