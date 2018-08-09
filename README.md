# Kubernetes Gossip Sidecar


![Travis CI Build](https://travis-ci.org/NiftySoft/k8s-gossip-sidecar.svg?branch=master)
![Codacy Badge](https://api.codacy.com/project/badge/Grade/090f054b569a4074864f3a9e260850b8)
![Codacy Badge](https://api.codacy.com/project/badge/Coverage/090f054b569a4074864f3a9e260850b8)
![MIT Licensed](https://img.shields.io/badge/license-MIT-blue.svg)


A lightweight Kubernetes sidecar for performing slow-sync on an in-memory key-value
store.

### But Why?

Not all cluster data requires strong write consistency and high-availability. Sometimes you
just want to sync information with peers without taxing cluster resources. This sidecar is
built for those use-cases.

1. Provides a key-value store for syncing arbitrary data (as long as it is a UTF-8 string).
1. Exposes GET and PUT HTTP endpoints for ease-of-use.
1. Uses gossip-protocols to perform two-way sync with a random peer.
1. Built on Netty, a lightweight non-blocking networking library for Java.

### How does it work?

The sidecar is installed as a headless Kubernetes service. During sync, each node looks up
its list of peers from the service and initiates two-way sync with a random peer. Peers 
overwrite stale data with fresher information from their peers.

While the gossip protocol used is [guaranteed](http://disi.unitn.it/~montreso/ds/papers/montresor17.pdf) 
to converge within a logarithmic number of rounds, if you require strict convergence SLAs
a Gossip Sidecar may not be the right tool for your use-case.
