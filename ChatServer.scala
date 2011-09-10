import scala.actors._
import remote._
import RemoteActor._

object ChatServer extends App with Actor {  
  scala.actors.Debug.level = 3

  val port = 1234
  
  alive(port)
  register('chatServer, this)
  start
  
  println("ChatServer started at port %d".format(port))

  val loginMessage = """LOGIN:(\w+):(\d+)""".r
  val incomingMessage = """SEND:(\w+):(\w+)""".r

  def act {
    loop {
      react {
        case loginMessage(username, port) => registerClient(username, port.toInt)
        case incomingMessage(username, message) => sendToAllClients(username, message)
        case unknown => println("Unknown input")
      }
    }
  }

  var clients: List[AbstractActor] = List()

  def registerClient(username: String, port: Int) {
    println("%s logged in".format(username))
    val client = select(Node("127.0.0.1", port), 'chatClient)
    clients = client :: clients
    client ! "SERVER:LOGIN_SUCCEEDED"
  }

  def sendToAllClients(from: String, message: String) = clients foreach {
    _ ! "%s> %s".format(from, message)
  }
}