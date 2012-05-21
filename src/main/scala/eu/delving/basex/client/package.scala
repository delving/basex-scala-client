package eu.delving.basex.client

import org.basex.core.cmd.Flush
import xml.{XML, Node}
import org.basex.server.{ServerCmd, ClientSession, ClientQuery}
import org.basex.io.in.{DecodingInput, BufferInput}
import org.basex.util.Token
import org.basex.util.list.ByteList
import java.io.{OutputStream, BufferedInputStream}


object `package` extends Implicits

trait Implicits {

  class RichClientQuery(query: ClientQuery) extends Iterator[String] {
    def next(): String = {
      if(query.isInstanceOf[NonCachedClientQuery]) {
        query.asInstanceOf[NonCachedClientQuery].getNext
      } else {
        query.next
      }
    }

    def hasNext: Boolean = query.more()
  }

  class RichClientSession(session: ClientSession) {

    def open(db: String) {
      session.execute("open " + db)
    }

    def find(query: String): Iterator[Node] = {
      session.query(query).map(XML.loadString(_))
    }

    def findRaw(query: String): Iterator[String] = {
      session.query(query)
    }

    def findOne(query: String): Option[Node] = {
      findOneRaw(query).map(XML.loadString(_)).toList.headOption
    }

    def findOneRaw(query: String): Option[String] = {
      session.query(query).toList.headOption
    }

    def setAutoflush(flush: Boolean) {
      if (flush) {
        session.execute("set autoflush true")
      } else {
        session.execute("set autoflush false")
      }
    }

    def flush() {
      session.execute(new Flush())
    }

    def createAttributeIndex() {
      session.execute("create index attribute")
    }

  }

  implicit def withRichClientQuery[A <: ClientQuery](query: A): RichClientQuery = new RichClientQuery(query)

  implicit def withRichClientSession[A <: ClientSession](session: A): RichClientSession = new RichClientSession(session)


  class NonCachedClientSession(val host: String, val port: Int, val user: String, val pass: String) extends ClientSession(host, port, user, pass) {

    def getServerOutput = sout

    def getServerInput = sin

    override def send(s: String) {
      super.send(s)
    }

    override def query(query: String): ClientQuery = {
      new NonCachedClientQuery(query, this, out);
    }
  }

  class NonCachedClientQuery(query: String, session: NonCachedClientSession, os: OutputStream) extends ClientQuery(query, session, os) {

    private def ncs = cs.asInstanceOf[NonCachedClientSession]

    private var streamConsumed: Boolean = false
    private var resultStream: BufferInput = null

    private var lookAhead: Array[Byte] = null

    override def more(): Boolean = {
      initStream()
      !streamConsumed && lookAhead != null
    }

    def getNext = {
      if (more()) {
        val l = lookAhead
        lookAhead = fetchNext()
        Token.string(l)
      } else {
        null
      }
    }

    def fetchNext(): Array[Byte] = {
      if (streamConsumed) return null
      if (!(resultStream.read > 0)) {
        streamConsumed = true
        null
      } else {
        val bl = new ByteList()
        val di: DecodingInput = new DecodingInput(resultStream)
        var b: Int = 0
        while ( {
          b = di.read;
          b
        } != -1 ) bl.add(b)
        pos += 1
        bl.toArray
      }
    }

    def initStream() {
      if(resultStream == null) {
      ncs.getServerOutput.write(ServerCmd.ITER.code)
      ncs.send(id)
      ncs.getServerOutput.flush()
      resultStream = new BufferInput(ncs.getServerInput)
      lookAhead = fetchNext()
      }
    }
  }

}