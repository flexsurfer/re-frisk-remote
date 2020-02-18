# re-frisk remote library

[re-frisk](https://github.com/flexsurfer/re-frisk) remote library for debugging re-frame applications (react native, electron, web) using leiningen re-frisk [plugin](https://github.com/flexsurfer/lein-re-frisk)

[<img src="2016-01-01-starting-clojure-today.jpg" width="100">](https://github.com/flexsurfer/re-frisk)

## Usage

[![Clojars](https://img.shields.io/clojars/v/re-frisk-remote.svg)](https://clojars.org/re-frisk-remote)


NOTE! for shadow-cljs RN projects you can use this libary: https://github.com/flexsurfer/re-frisk-rn

Add `[re-frisk-remote "0.5.10"]` to the dev `:dependencies` in your project.clj
                                
run re-frisk using `enable-re-frisk-remote!` function on the localhost and default port (4567)

```cljs
(:require [re-frisk-remote.core :refer [enable-re-frisk-remote!]])

 (enable-re-frisk-remote!)
```

Or select a different host and port by supplying the host and port number:

```cljs
(enable-re-frisk-remote! {:host "192.168.1.1:8095"})
```

You could also provide options `:enable-re-frisk? false` or `:enable-re-frame-10x? true` to enable/disable sending traces for respective component while re-frame-10x is disabled by default.


Run re-frisk remote server using leiningen re-frisk [plugin](https://github.com/flexsurfer/lein-re-frisk)

`$ lein re-frisk`

Run an application,
Enjoy!
