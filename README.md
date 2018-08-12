# Gossipcar
### Kubernetes-native Volatile Datastore

![GitHub tag](https://img.shields.io/github/tag/niftysoft/k8s-gossipcar.svg)
![MIT Licensed](https://img.shields.io/badge/license-MIT-blue.svg)

![Travis CI Build](https://travis-ci.org/NiftySoft/k8s-gossipcar.svg?branch=master)
![Codacy Badge](https://api.codacy.com/project/badge/Grade/090f054b569a4074864f3a9e260850b8)
![Codacy Badge](https://api.codacy.com/project/badge/Coverage/090f054b569a4074864f3a9e260850b8)




A lightweight Kubernetes sidecar for syncing  an in-memory key-value store between pods.

## But Why?

Not all cluster data requires strong write consistency and high-availability. Sometimes you
just want to sync information with peers without taxing cluster resources. This sidecar is
built for those use-cases.

1. Provides a key-value store for syncing arbitrary data (as long as it is a UTF-8 string).
1. Exposes GET and PUT HTTP endpoints for ease-of-use.
1. Uses gossip-protocols to perform two-way sync with a random peer.
1. Built on Netty, a lightweight non-blocking networking library for Java.

## How does it work?

The sidecar is installed as a headless Kubernetes service. During sync, each node looks up
its list of peers from the service and initiates two-way sync with a random peer. Peers 
overwrite stale data with fresher information from their peers.

While the gossip protocol used is [guaranteed](http://disi.unitn.it/~montreso/ds/papers/montresor17.pdf) 
to converge within a logarithmic number of rounds, if you require strict convergence SLAs
a Gossip Sidecar may not be the right tool for your use-case.

## How can I use it?

Docker images are availble in `.tar` format in [releases](https://github.com/NiftySoft/k8s-gossipcar/releases). 
To deploy on Kubernetes, a starting config is available in [k8s-deploy.yaml](https://github.com/NiftySoft/k8s-gossipcar/blob/master/k8s-deploy.yaml). 
You will probably want to edit the spec to include your own application container, and possibly change 
the `containerPort` to something other than port 80.

Once deployed, another process running on the same pod can access the container by sending HTTP requests
to localhost:80. The following endpoints are available to interact with the key-value store.

## How can I write to Gossipcar?

PUT `http://localhost:80/map?k=<key>`

**REQUEST BODY:**
```
[Octet stream of data to be associated with "key"]
```

**RESPONSE HEADERS:**
```
Content-Length: 0
```

**RESPONSE BODY:**
```
EMPTY
```

**RESPONSE CODES:**
 * **201 CREATED**    - if key did not previously exist in the repo
 * **204 NO CONTENT** - if key existed previously, and was succesfully overwritten
   
 * **422 UNPROCESSABLE ENTITY** - if query parameter k was not present.
   
 * **500 INTERNAL ERROR** - something awful has occurred.


## How can I read from Gossipcar?

GET `http://localhost:80/map?k=<key1>&k=<key2>...`

**RESPONSE HEADERS:**
```
Content-Type: application/octect-stream
Content-Length: [number of bytes in body]
```

**RESPONSE CONTENT:**
```
<key1>=<value1>\n
<key2>=<value2>\n
...
```

**RESPONSE CODES:**
 * **200 OK** - if at least one key was present.
   
 * **404 NOT FOUND**            - If none of the requested keys were present.
 * **422 UNPROCESSABLE ENTITY** - If no query parameter named "k" was present.

**NOTES:**
 * Each key-value pair appears on a separate line, ended by a single '\n' character.
 * Requested keys which are not present are not included in the response body.
 * "<key#>=" will be formatted as a UTF-8 string.
 * "<value#>" will be the octet stream associated with <key#>.
