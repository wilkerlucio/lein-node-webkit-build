# lein-node-webkit-build

A Leiningen plugin to generate builds for node-webkit applications.

This project was inspired on the great [grunt-node-webkit-build](https://github.com/mllrsohn/grunt-node-webkit-builder).

## Installation

You can install this plugin by adding it on your plugins list on your `project.clj`.

```clojure
:plugins [["lein-node-webkit-build", "0.1.5"]]
```

## Usage

This project will help you to get from a folder containing a node-webkit application

For a minimum setup, this is what you need to add on your `project.clj`:

```clojure
:node-webkit-build {:root "public"}
```

And then you can run the build with:

```
lein node-webkit-build
```

This will lookup at the folder `public` for your app, will build it up and generate
releases for all available platforms (it will automatic download the nescessary files
for that).

The following options are available to customize the build:

```clojure
{ :root "" ; your node-webkit app root directory
  :name nil ; use this to override the application name
  :version nil ; use this to override the application version
  :osx {
    :icon nil ; point to an .icns icon file to be used on the generated mac osx build
  }
  :platforms #{:osx :osx64 :win :linux32 :linux64} ; select which platforms to generate the build
  :nw-version :latest ; the node-webkit version to be used
  :output "releases" ; output directory for the generated builds
  :disable-developer-toolbar true ; this will update your package.json to remove the developer toolbar
  :use-lein-project-version true ; update the project version using your leiningen project version
  :tmp-path (path-join "tmp" "nw-build")} ; temporary path to place intermediate build files
```

This project still very young and all feedback will be great to improve it.

## License

Released under the MIT License: http://www.opensource.org/licenses/mit-license.php
