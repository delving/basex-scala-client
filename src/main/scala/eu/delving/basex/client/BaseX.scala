package eu.delving.basex.client

import org.basex.BaseXServer
import java.io.{File, ByteArrayInputStream}
import org.basex.server.ClientSession
import org.basex.core.cmd.{Rename, Delete}
import xml.Node

/**
 * BaseX Client wrapper for Scala
 *
 * TODO support remote connection
 * TODO query with limits
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
class BaseX(host: String, port: Int, eport: Int, user: String, pass: String, useQueryCache: Boolean = false) extends Implicits {

  private var server: BaseXServer = null

  /**
   * Starts an embedded BaseX server
   * @param dataDirectory the data directory on disk. Leave empty to use BaseX default.
   */
  def start(dataDirectory: Option[String] = None) {
    if(dataDirectory.isDefined) {
      val d = new File(dataDirectory.get)
      if(!d.exists()) {
        val created = d.mkdirs()
        if(!created) throw new RuntimeException("Failed to create data directory for BaseX " + dataDirectory)
      }
      System.setProperty("org.basex.path", d.getAbsolutePath)
    }
    server = new BaseXServer("-e%s".format(eport), "-p%s".format(port))
  }

  /**
   * Stops an embedded BaseX server
   */
  def stop() {
    BaseXServer.stop(port, eport)
  }

  /**
   * Starts a new session for the specified database
   * @param database the name of the database to connect to
   * @param block the code to run in the context of the session
   */
  def withSession[T](database: String)(block: ClientSession => T): T = {
    withSession {
      session =>
        session.execute("open " + database)
        block(session)
    }
  }


  /**
   * Executes code in the context of a session, provided a database is already open.
   * Depending on the configuration, may use a [[eu.delving.basex.client.Implicits.StreamingClientSession]]
   * @param block the code to run in the context of the session
   */
  def withSession[T](block: ClientSession => T): T = {
    val session = if(useQueryCache) {
      new ClientSession(host, port, user, pass)
    } else {
      new StreamingClientSession(host, port, user, pass)
    }
    try {
      block(session)
    } finally {
      session.close()
    }
  }

  /**
   * Executes code in the context of specific query results
   * @param database the name of the database
   * @param query the query to execute
   * @param block the code to execute in the context of the query results
   */
  def withQuery[T](database: String, query: String)(block: RichClientQuery => T) = {
    withSession {
      session =>
        session.execute("open " + database)
        val q = session.query(query)
        try {
          block(q)
        } finally {
          q.close()
        }

    }
  }

  def createDatabase(name: String) {
    withSession {
      session => session.execute("create db " + name)
    }
  }

  def openDatabase(name: String) {
    withSession {
      session => session.execute("open " + name)
    }
  }


  def dropDatabase(name: String) {
    withSession {
      session => session.execute("drop db " + name)
    }
  }

  def alter(db: String, newDb: String) {
    withSession {
      session => session.execute("alter db %s %s".format(db, newDb))
    }
  }

  def add(database: String, path: String, document: String) {
    withSession {
      session =>
        session.execute("open " + database)
        session.add(path, new ByteArrayInputStream(document.getBytes("utf-8")))
    }
  }

  def replace(database: String, path: String, document: String) {
    withSession {
      session =>
        session.execute("open " + database)
        session.replace(path, new ByteArrayInputStream(document.getBytes("utf-8")))
    }
  }

  def rename(database: String, path: String, newPath: String) {
    withSession {
      session =>
        session.execute("open " + database)
        session.execute(new Rename(path, newPath))
    }
  }

  def delete(database: String, path: String) {
    withSession {
      session =>
        session.execute("open " + database)
        session.execute(new Delete(path))
    }
  }

  def query(database: String, query: String): List[String] = {
    withSession {
      session =>
        session.execute("open " + database)
        val q = session.query(query)
        val r = q.toList
        q.close()
        r
    }
  }

  def fetchRaw(database: String, path: String): Option[String] = {
    withSession {
      session => session.query("""db:open("%s", "%s")""".format(database, path)).toList.headOption
    }
  }

  def fetch(database: String, path: String): Option[Node] = fetchRaw(database, path).map(scala.xml.XML.loadString(_))

}
