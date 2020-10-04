<!--- http://dillinger.io/ --->
# sqrl-server-demo


This JEE app is the demo for the [sqrl-server-base](https://github.com/sqrlserverjava/sqrl-server-base)  and [sqrl-server-atmosphere](https://github.com/sqrlserverjava/sqrl-server-atmosphere) libraries.

The app uses spring data so no external DB or config is necessary.
You can run the demo app locally by:
1. Checkout the sqrlexample project:  `git clone https://github.com/ajdaley/sqrl-server-example.git`
1. Move into the directory: `cd sqrl-server-example`
1. Start the server with maven: `mvn spring-boot:run`
1. Install a native SQRL client such as sqrl.exe from [grc.com](https://www.grc.com/dev/sqrl.exe) - mobile clients will not work when running on localhost
1. Open https://127.0.0.1:8443/login in Chrome, IE, Edge, Firefox
1. Bypass the certificate warning (unavoidable on localhost)
1. H2 Console present at https://localhost:8443/h2-console (leave password empty)

**Requirements**

Java8

**Note**

Currently depends on change in forked sqrl-server-base repo: https://github.com/ajdaley/sqrl-server-base/commit/49aaf623847d2aacf517ff6c49cbc68fd734337d

#### Reporting Issues
See [CONTRIBUTING.md](https://github.com/sqrlserverjava/sqrl-server-example/blob/master/CONTRIBUTING.md)
