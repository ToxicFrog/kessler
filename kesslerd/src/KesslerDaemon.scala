package kessler

import ksp._
import scala.actors._
import java.io.FileWriter

/**
 * A daemon for merging save files and replying with the merged files.
 *
 * Operations are:
 * - update <content> - merge this content into the master save file
 * - get - get a copy of the merged file
 * - get <list> - get a copy of the merged file, excluding rockets that require parts not in <list>
 */
class KesslerDaemon(configfile: String) extends Actor {
  import scala.actors.remote.RemoteActor._
  import KesslerDaemon.{ConnectCommand,PutCommand,GetCommand,Success,Error}
  import java.util.Properties
  import java.io.{File,FileInputStream}

  val VERSION = 12031200;
  
  println("Loading configuration from " + configfile)
  val config = new Properties(); config.load(new FileInputStream(configfile))
  val port = config.getProperty("port", "8988").toInt
  val pass = config.getProperty("password", "")
  val save = config.getProperty("save", "kessler/merged.sfs")
  val games = config.getProperty("load", "kessler/merged.sfs:saves/default/persistent.sfs").split(':')

  var game = loadFile(games)

  def log(message: String) { println(message) }
  
  def loadFile(names: Seq[String]): Game = {
    if (names.isEmpty) {
      log("Error: couldn't find any games to load. Exiting.")
      System.exit(1)
    } else {
      log("Looking for " + names.head + ":")
    }
    
    try {
      val game = Game.fromFile(names.head)
      log(" loaded " + names.head)
      game
    } catch {
      case e: Exception => log(" failed (" + e.getMessage + ")"); loadFile(names.tail)
    }
  }

  override def exceptionHandler = {
    case e: Exception => e.printStackTrace(); sender ! Error(e.getMessage)
  }

  def act() {
    alive(port)
    register('kesslerd, this)
    log("Kessler daemon running on port " + port + " (protocol version " + VERSION + ")")

    loop {
      react {
        case ConnectCommand(pass, version) => doAuth(pass) && checkVersion(version)
        case PutCommand(pass, save) => if (doAuth(pass)) doPut(save);
        case GetCommand(pass, save, parts) => if (doAuth(pass)) doGet(save, parts);
        case other => sender ! Error("Invalid command: " + other)
      }
    }
  }
  
  def checkVersion(version: Int) = {
    println("Performing version check: " + version)
    if (version != VERSION) {
      sender ! Error("Version mismatch: server " + VERSION + ", client " + version)
      false
    } else {
      sender ! Success("Server ready.")
      true
    }
  }

  def doAuth(pass: String) = {
    if (this.pass == "" || this.pass == pass) {
      log("Command received from " + sender)
      true
    } else {
      log("Rejecting command from " + sender + ": invalid password")
      reply(Error("invalid password"))
      false
    }
  }

  def doPut(save: String) {
    var count = game.asObject.children.values.foldLeft(0)((total, buf) => total + buf.length)

    game = game.merge(Game.fromString(save))
    count = game.asObject.children.values.foldLeft(0)((total, buf) => total + buf.length) - count

    log(count + " objects merged into world.")
    reply(Success(count + " objects merged."))

    safeSave(game)
  }

  def safeSave(game: Game) {
    game.save(save + ".tmp")
    new File(save).delete()
    new File(save + ".tmp").renameTo(new File(save))
  }
  
  def doGet(save: String, parts: Set[String]) {
    log("Creating merged save file: " + parts.count(_ => true) + " parts available.")

    val filtered = game.filter(parts)
    log("Created save containing "
      + filtered.asObject.getChildren("CREW").length
      + " crew and "
      + filtered.asObject.getChildren("VESSEL").length
      + " vessels. (Original: "
      + game.asObject.getChildren("CREW").length
      + "/" + game.asObject.getChildren("VESSEL").length
      + ")"
    )
    
    val merged = Game.fromString(save).merge(filtered)
    
    reply(Success(merged.mkString))
  }
}

object KesslerDaemon {
  abstract case class Command();
  case class ConnectCommand(pass: String, version: Int) extends Command
  case class PutCommand(pass: String, game: String) extends Command;
  case class GetCommand(pass: String, game: String, exclude: Set[String]) extends Command;

  abstract case class Reply();
  case class Success(msg: String) extends Reply;
  case class Error(msg: String) extends Reply;

  def main(args: Array[String]) {
    val configfile = if (args.length > 0) args(0) else "kessler/client_config.txt"
    new KesslerDaemon(configfile).start()
  }
}
