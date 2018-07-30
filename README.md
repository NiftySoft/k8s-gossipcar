# Kubernetes Gossip Sidecar

![https://opensource.org/licenses/mit-license.php](https://badges.frapsoft.com/os/mit/mit.png?v=103)
![https://gitlab.com/kalexmills/k8s-gossip-sidecar/commits/%{default_branch}](https://gitlab.com/kalexmills/k8s-gossip-sidecar/badges/master/pipeline.svg)
![https://www.codacy.com/app/kalexmills/k8s-gossip-sidecar?utm_source=gitlab.com&amp;utm_medium=referral&amp;utm_content=kalexmills/k8s-gossip-sidecar&amp;utm_campaign=Badge_Grade](https://api.codacy.com/project/badge/Grade/144c0e99b8e843538c6e5c986b7d7941)
![https://app.codacy.com/project/kalexmills/k8s-gossip-sidecar/dashboard](https://img.shields.io/codacy/coverage/144c0e99b8e843538c6e5c986b7d7941.svg)

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
