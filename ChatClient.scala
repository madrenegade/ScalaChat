import scala.swing._
import event.ButtonClicked
import scala.actors._
import remote._
import RemoteActor._
import scala.swing.event.EditDone

object ChatClient extends SimpleSwingApplication with Actor {

  scala.actors.Debug.level = 3

  var port = 1235

  override def startup(args: Array[String]) {
    port = args match {
      case Array(portString) => portString.toInt
      case _ => 1235
    }

    alive(port)
    register('chatClient, this)
    start

    super.startup(args)
  }

  val messageArea = new TextArea(20, 50) { editable = false }
  val inputField = new TextField { columns = 40 }
  var username = ""

  def top = new MainFrame {

    title = "ScalaChat"
    preferredSize = new java.awt.Dimension(640, 400)

    val inputField = new TextField { columns = 40 }
    val sendButton = new Button { text = "Senden" }

    contents = new FlowPanel {
      border = Swing.EmptyBorder(5, 5, 5, 5)
      contents += messageArea
      contents += inputField
      contents += sendButton
    }

    listenTo(inputField)
    listenTo(sendButton)

    reactions += {
      case EditDone(`inputField`) => inputField.text.trim match {
        case "" =>
        case msg => {
          ChatClient ! "OUT:" + msg
          this.inputField.text = ""
        }
        this.inputField.text = ""
      }
      case ButtonClicked(`sendButton`) => inputField.text.trim match {
        case "" =>
        case msg => {
          ChatClient ! "OUT:" + msg
          this.inputField.text = ""
        }
        this.inputField.text = ""
      }
    }
  }

  val loginMessage = """OUT:LOGIN:(\w+)""".r
  val loginSucceeded = "SERVER:LOGIN_SUCCEEDED".r
  val sendMessage = """OUT:([\w\s,.]+)""".r

  var server: AbstractActor = null

  def act {
    loop {
      react {
        case loginMessage(username) => {
          this.username = username
          server = select(Node("127.0.0.1", 1234), 'chatServer)
          server ! "LOGIN:%s:%d".format(username, port)
        }
        case loginSucceeded() => messageArea.append("Login erfolgreich\n")
        case sendMessage(message) => server ! "SEND:%s:%s".format(username, message)
        case message: String => messageArea.append("%s\n".format(message))
        case unknown => println("Unknown input: " + unknown)
      }
    }
  }
}
