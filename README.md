# looper

A Clojure library that provides retries around clj-http http calls.

## Rationale

Shit's brittle.
TODO: expand on this

## Usage

If you are using clj-http already, you can just add a dependency on looper:

```clj
[com.farmlogs/looper "0.3.0"]
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

The following options can be overridden:

* `:backoff-ms`: should be a vector of
  `[intial-backoff-in-ms max-backoff-in-ms delay-factor]`. Default is
  `[10 2000 4]`. See the
  [diehard docs](https://sunng87.github.io/diehard/diehard.core.html#var-with-retry)
  for more details.

* `:max-retries`: number of retry attempts before giving up. Default
  is `10`. Set to `nil` to retry forever.

* `:on-failed-attempt`: a fn that will be passed a map of the form:
```
{:result result-of-call
:exception exception
:method method
:url url
:attempt attempt-number
:elapsed time-elapsed-since-first-attempt}
```

The [default fn `log/warns`](src/looper/retry.clj#L14) the exception
along with the method, url, attempt count, and elapsed time.

* `:retry-if`: a fn that will be passed the result and exception (if
  any), and should return truthy if the request should be
  retried. [The default](src/looper/retry.clj#L5) only retries if
  there is an exception and the exception has no status or has a
  status >= 500.

## TODO

* Add docstrings
* Add metrics(?)

## License

Copyright © 2017 FarmLogs

Distributed under the Eclipse Public License either version 2.0 or (at
your option) any later version.

[Failsafe]: https://github.com/jhalterman/failsafe
[diehard]: https://github.com/sunng87/diehard
