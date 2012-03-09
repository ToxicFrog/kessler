package kessler

import ksp._
import scala.actors._

class KesslerClient(host: String, port: Int, command: String, arg: String, pass: String) extends Actor {
  import scala.actors.remote.Node
  import scala.actors.remote.RemoteActor._
  import KesslerDaemon.{SendCommand,GetCommand,Success,Error}

  def act() {
    val server = select(Node(host, port), 'kesslerd)
    println("Connected to server: " + server)

    (command match {
      case "send" => sendGame(server, arg)
      case "get" => getGame(server, arg)
    }) match {
      case Success(msg) => println(msg)
      case Error(msg) => println("Error: " + msg)
    }
  }

  def sendGame(server: AbstractActor, filename: String) = {
    println("Uploading " + filename + " to server for merge...")
    server !? new SendCommand(pass, io.Source.fromFile(filename).mkString)
  }

  def getGame(server: AbstractActor, filename: String) = {
    println("Scanning KSP directory for parts...")
    println("Requesting new save file from server...")
    println("Writing new save...")
    val parts = Set[String]()
    server !? new GetCommand(pass, parts) match {
      case Success(msg) => Success("game received successfully: " + msg.length + " bytes.")
      case x: Any => x
    }
  }
}

object KesslerClient {
  def main(args: Array[String]) {
    val host = args(0)
    val port = args(1).toInt
    val command = args(2)
    val arg = args(3)
    val pass = if (args.length > 4) args(4) else null

    new KesslerClient(host, port, command, arg, pass).start()
  }
}
