package kessler

import ksp._
import scala.actors._

class KesslerClient(command: String, arg: String) extends Actor {

  import java.util.Properties
  import java.io.{File,FileInputStream,FileWriter}
  import scala.actors.remote.Node
  import scala.actors.remote.RemoteActor._
  import KesslerDaemon.{PutCommand, GetCommand, Success, Error}

  val config = new Properties(); config.load(new FileInputStream("kessler/client_config.txt"))
  val host = config.getProperty("host", "localhost")
  val port = config.getProperty("port", "8988").toInt
  val pass = config.getProperty("password", "")
  val allow_debris = config.getProperty("allow_debris", "all")

  def act() {
    println("Connecting to " + host + ":" + port + "...")
    val server = select(Node(host, port), 'kesslerd)
    server ! 'Connect

    (command match {
      case "put" => putGame(server, arg)
      case "get" => getGame(server, arg)
    }) match {
      case Success(msg) => println(msg)
      case Error(msg) => println("Error: " + msg)
    }
  }

  def putGame(server: AbstractActor, filename: String) = {
    println("Uploading " + filename + " to server for merge...")
    server !? new PutCommand(pass, io.Source.fromFile(filename).mkString)
  }

  def getGame(server: AbstractActor, filename: String) = {
    println("Scanning KSP directory for parts...")
    val parts = listParts
    println("Requesting new save file from server...")
    server !? new GetCommand(pass, Game.fromFile(filename).mkString, parts) match {
      case Success(msg) => {
        val game = Game.fromString(msg)

        /* remove debris that is not pilotable */
        if (allow_debris == "none" || allow_debris == "only_controllable") {
          game.asObject.getChildren("VESSEL").filter {
            _.getChild("ORBIT").getProperty("OBJ") == "0"
          } foreach {
            game.asObject.deleteChild(_)
          }
        }

        /* remove objects with names ending in "Debris" */
        if (allow_debris == "none" || allow_debris == "only_named") {
          game.asObject.getChildren("VESSEL").filter {
            _.getProperty("name").endsWith("Debris")
          } foreach {
            game.asObject.deleteChild(_)
          }
        }

        safeSave(filename, Game.fromString(msg))
        Success("Game received successfully: " + msg.length + " bytes.")
      }
      case x: Any => x
    }
  }

  def safeSave(file: String, game: Game) {
    /* update elapsed-time value in downloaded save to match local save so
       orbits are correct */
    try {
      val UT = Game.fromFile(file).asObject.getProperty("UT")
      println("Updating elapsed time in save file to " + UT)
      game.asObject.setProperty("UT", UT)
    } catch {
      case e: Exception => println("Warning: couldn't set elapsed-time in merged save: " + e.getMessage)
    }

    val timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@HH.mm.ss").format(new java.util.Date())
    println("Saving to " + file)
    game.save(file + ".tmp")

    new File(file).renameTo(new File(file + "." + timestamp))
    new File(file + ".tmp").renameTo(new File(file))
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
