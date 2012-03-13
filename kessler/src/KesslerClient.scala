package kessler

import ksp._
import actors._

class KesslerClient(command: String, arg: String) extends Actor {

  import java.util.Properties
  import java.io.{File,FileInputStream}
  import actors.remote.Node
  import actors.remote.RemoteActor._
  import KesslerDaemon.{PutCommand, GetCommand, ConnectCommand, Success, Error}

  val VERSION = 12031200;

  val config = new Properties(); config.load(new FileInputStream("kessler/client_config.txt"))
  val host = config.getProperty("host", "localhost")
  val port = config.getProperty("port", "8988").toInt
  val pass = config.getProperty("password", "")
  val allow_debris = config.getProperty("allow_debris", "all")

  val server = select(Node(host, port), 'kesslerd)
  
  override def exceptionHandler = {
    case e: Exception => e.printStackTrace(); die("unhandled exception")
  }
  
  def die(reason: String) = {
    Console.err.println(reason)
    System.exit(1)
    println("Exiting")
    exit()
    ""
  }
  
  def act() {
    println("Connecting to " + host + ":" + port + "...")

    val sid = send(10000, ConnectCommand(pass, VERSION))
    
    command match {
      case "put" => putGame(sid)
      case "get" => getGame(sid)
      case other => die("Invalid command: " + other)
    }
  }
  
  def send(timeout: Long, message: KesslerDaemon.Command): String = {
    server !? (timeout, message) match {
      case Some(Success(msg)) => msg
      case Some(Error(msg)) => die("Error from server: " + msg)
      case None => die("Error: timeout in communication with server.")
      case other => die("Invalid message from server: " + other)
    }
  }

  def putGame(sid: String) {
    println("Uploading " + arg + " to server for merge...")
    println(send(60000, PutCommand(sid, io.Source.fromFile(arg).mkString)))
  }

  def getGame(sid: String) {
    println("Scanning KSP directory for parts...")
    val parts = listParts
    
    println("Requesting new save file from server...")
    val localGame = Game.fromFile(arg)
    val newGame = Game.fromString(send(60000, GetCommand(sid, localGame.mkString, parts)))

    /* remove debris that is not pilotable */
    if (allow_debris == "none" || allow_debris == "only_controllable") {
      tidyGame(newGame, _.getChild("ORBIT").getProperty("OBJ") == "0")
    }

    /* remove objects with names ending in "Debris" */
    if (allow_debris == "none" || allow_debris == "only_named") {
      tidyGame(newGame, _.getProperty("name").endsWith("Debris"))
    }

    /* update elapsed-time value in downloaded save to match local save so
       orbits are correct */
    val UT = localGame.asObject.getProperty("UT")
    println("Setting timestamp in merged game to " + UT)
    newGame.asObject.setProperty("UT", UT)

    safeSave(arg, newGame)
  }
  
  def tidyGame(game: Game, filter: (Object => Boolean)) {
    game.asObject.getChildren("VESSEL").filter {
      filter(_)
    } foreach {
      game.asObject.deleteChild(_)
    }
  }

  def safeSave(file: String, game: Game) {
    val timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@HH.mm.ss").format(new java.util.Date())
    println("Saving to " + file)
    game.save(file + ".tmp")

    new File(file).renameTo(new File(file + "." + timestamp)) && new File(file + ".tmp").renameTo(new File(file))
  }

  def listParts: Set[String] = {
    new File("parts").listFiles.map(
      x => """_""".r.replaceAllIn(Object.fromFile(new File(x, "part.cfg")).getProperty("name"), ".")
    ).toSet
  }
}

object KesslerClient {
  def main(args: Array[String]) {
    val command = args(0)
    val arg = args(1)

    new KesslerClient(command, arg).start()
  }
}
