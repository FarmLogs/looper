# looper

A Clojure library that provides retries around clj-http http calls.

## Rationale

Shit's brittle.
TODO: expand on this

## Usage

If you are using clj-http already, you can just add a dependency on looper:

```clj
[com.farmlogs/looper "0.1.0-SNAPSHOT"]
```

Then turn this:

```clj
(ns whatever
  (:require [clj-http.client :as client]))

(defn do-stuff [body]
  (client/post "http://somewhere" {:content-type :json :body body}))
```

into:

```clj
(ns whatever
  (:require [looper.client :as client]))

(defn do-stuff [body]
  (client/post "http://somewhere" {:content-type :json :body body}))
```

and get retries around that `post` call.

### What it's doing

This uses the retry functionality from the [Failsafe] library (via
[diehard]) to wrap the clj-http call. By default, it will retry 10
times, with a backoff strategy of: 10ms, 40ms, 160ms, 640ms, 2000ms, 2000ms...

It will only retry if an exception was thrown, and: the exception has
no `:status` in its `ex-data` or that `:status` is 5xx.

On each retry, it will log a message of the form:

```clj
(log/warn exception "failed-to-<method>" {:url url :attempt num-attempts :elapsed time-elapsed-since-first-call})
```

So the above example will result in messages like:

```
WARN: failed-to-post {:url http://somewhere, :attempt 1, :elapsed 10}
WARN: <stacktrace>
```

It will try to use Timbre, then tools.logging, then fall back to
`println` if neither of those are available. It brings in no logging
dependencies.

### Overriding defaults

You can override the retry settings by adding a `:looper/options` map
to the options map you pass to the get/post/etc function:

```clj
(client/post "http://somewhere" {:content-type :json
                                 :body body
                                 :looper/options {:max-retries 3}})
```

## TODO

* Add docstrings
* More docs on what options are avalable
* Add metrics(?)
* More testing

## License

Copyright © 2017 FarmLogs

Distributed under the Eclipse Public License either version 2.0 or (at
your option) any later version.

[Failsafe]:
[diehard]: 
