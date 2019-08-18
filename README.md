<!--- http://dillinger.io/ --->
# sqrl-server-demo


This JEE app is the demo for the [sqrl-server-base](https://github.com/sqrlserverjava/sqrl-server-base)  and [sqrl-server-atmosphere](https://github.com/sqrlserverjava/sqrl-server-atmosphere) libraries.  This demo app is running at https://sqrljava.tech:20000/sqrlexample/login

You can run the demo app locally by:
1. Checkout the sqrlexample project:  `git clone https://github.com/sqrlserverjava/sqrl-server-example.git`
1. Move into the directory: `cd sqrl-server-example`
1. Start the server with jetty: `mvn jetty:run`
1. Install a native SQRL client such as sqrl.exe from [grc.com](https://www.grc.com/dev/sqrl.exe) - mobile clients will not work when running on localhost
1. Open https://127.0.0.1/sqrlexample/ in Chrome, IE, or Edge (firefox does not work on localhost for some reason)
1. Bypass the certificate warning (unavoidable on localhost)

You can also run the maven jetty command in debug mode in eclipse (or your favorite IDE) to step through the code, etc

**Requirements**

Requires running with Java8!!!
Requires a DB backend (see persistence.xml). This can be quickly setup with e.g. Docker and MySQL Workbench
```bash
docker pull mysql
docker run -p 3306:3306 --name some-mysql -e MYSQL_ROOT_PASSWORD=my-secret-pw -d mysql:latest // root user
// could then connect e.g. MySQL Workbench to that instance with root user
```
**Running with iOS client**

You could potentially get this working with an iOS client. Steps would be:
* Turn on personal hotspot on iPhone
* Connect iPhone to mac
* Network -> iPhone USB -> Get IP Address
* Update IP addresses in ***jetty-http.xml***, ***jetty-ssl.xml***, and ***login.jsp***
* Replace localhost request above with new ip

#### Reporting Issues
See [CONTRIBUTING.md](https://github.com/sqrlserverjava/sqrl-server-example/blob/master/CONTRIBUTING.md)