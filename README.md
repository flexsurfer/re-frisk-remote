# re-frisk remote library

[re-frisk](https://github.com/flexsurfer/re-frisk) remote library for debugging re-frame applications (react native, electron, web) using leiningen re-frisk [plugin](https://github.com/flexsurfer/lein-re-frisk)

[<img src="2016-01-01-starting-clojure-today.jpg" width="100">](https://github.com/flexsurfer/re-frisk)

## Usage

[![Clojars](https://img.shields.io/clojars/v/re-frisk-remote.svg)](https://clojars.org/re-frisk-remote)


Add `[re-frisk-remote "0.4.1"]` to the dev `:dependencies` in your project.clj
                                
run re-frisk after document will be loaded and before any rendering calls, using `enable-re-frisk-remote!` function on the localhost and default port (4567)

```clojure
(:require [re-frisk-remote.core :refer [enable-re-frisk-remote!]])

(defn ^:export run
 []
 (dispatch-sync [:initialize])
 (enable-re-frisk-remote!)
 (reagent/render [simple-example]
                 (js/document.getElementById "app")))
```

Select a different host and port by supplying the host and port number:

```clojure
(enable-re-frisk-remote! {:host "192.168.1.1:8095"})
```

Run re-frisk remote server using leiningen re-frisk [plugin](https://github.com/flexsurfer/lein-re-frisk)

`$ lein re-frisk`

Run an application,
Enjoy!