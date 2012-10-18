# BaseX Scala Client

This is a client library that simplifies the use of BaseX (http://www.basex.org) in Scala applications.
It does so by providing wrappers that leverage Scala's collection library and other features such as the XML integration.

In order to use the library, add the following dependencies / resolvers to your SBT build:

- dependencies: `"eu.delving" %% "basex-scala-client" % "0.5"`
- resolvers:
  - `"Delving Releases Repository" at "http://development.delving.org:8081/nexus/content/groups/public"`
  - `"Delving Snapshot Repository" at "http://development.delving.org:8081/nexus/content/repositories/snapshots"`

## Usage

The BaseX Scala client adds a few enhancements using implicit conversions, so if you want to make use of it, always use the following import:

    import eu.delving.basex.client._


### Starting an embedded BaseX server

    import eu.delving.basex.client._

    val server = new BaseX("localhost", 1234, 1235, "admin", "admin")
    server.start()

### Creating a connection to a running BaseX server

    import eu.delving.basex.client._

    val server = new BaseX(host = "fooHost", port = 1234, eport = 1235, user = "admin", pass = "secret")


Note: by default, the `ClientSession` will not cache query results coming for the server but stream them directly to your client code (which is useful when the query result is big). If you want to make use of BaseX's default behaviour, create the server connection like this:

    import eu.delving.basex.client._

    val server = new BaseX(host = "fooHost", port = 1234, eport = 1235, user = "admin", pass = "secret", useQueryCache = true)


### Running a query in the context of a session

    import eu.delving.basex.client._
    import scala.xml.Node
    
    val server = new BaseX("localhost", 1234, 1235, "admin", "admin")
    server.start()

    def bookTitles(): String = {
  
      server.withSession("fooDatabase") { session =>
        val bookTitles: Iterator[Node] = session.find("""for $x in doc("books.xml")/bookstore/book
                                                         |where $x/price>30
                                                         |order by $x/title
                                                         |return <title>$x/title</title>""")

       bookTitles.map(t => t.text).mkString(", ")
   
      }

    } 

Note that there are other finder methods, e.g. `findRaw(query)` to retrieve the raw XML string coming from BaseX and `findOne(query)`, `findOneRaw(query)` to retrieve only one match (or None if none is found).

### More examples

Check out the [spec](https://github.com/delving/basex-scala-client/blob/master/src/test/scala/eu/delving/basex/client/BaseXSpec.scala), or have a look directly at the [source](https://github.com/delving/basex-scala-client/tree/master/src/main/scala/eu/delving/basex/client).
    

## Versions

### 18.10.2012 - 0.5

- using BaseX 7.3
- documentation!

### 19.05.2012 - 0.1

- initial release

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2012 Delving B.V (http://www.delving.eu).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
