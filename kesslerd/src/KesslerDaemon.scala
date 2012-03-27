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

  val VERSION = 12031300;
  
  println("Loading configuration from " + configfile)
  
  private object config extends Properties(new Properties()) {
    load(new FileInputStream(configfile))

    defaults put ("port", "8988")
    defaults put ("save", "kessler/merged.sfs")
    defaults put ("load", "kessler/merged.sfs,saves/default/persistent.sfs")
    defaults put ("filter", "nan,ghost,launchpad")

    def apply(key: String) = getProperty(key)
    def apply(key: Symbol) = getProperty(key name)
    
    def port = this('port).toInt
    def load = this('load).split(",")
    def filter = this('filter).split(",")
  }
  
  var game = loadFile(config.load)

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
    alive(config.port)
    register('kesslerd, this)
    log("Kessler daemon running on port " + config.port + " (protocol version " + VERSION + ")")

    loop {
      react {
        case ConnectCommand(pass, version) => doAuth(pass) && checkVersion(version)
        case PutCommand(pass, save) => if (doAuth(pass)) doPut(save);
        case GetCommand(pass) => if (doAuth(pass)) doGet();
        case other => sender ! Error("Invalid command: " + other)
      }
    }
  }
  
  def checkVersion(version: Int) = {
    println("Performing version check: " + version)
    if (version != VERSION) {
      println("Rejecting connection (client is using mismatched version " + version + ")")
      sender ! Error("Version mismatch: server " + VERSION + ", client " + version)
      false
    } else {
      sender ! Success("Server ready.")
      true
    }
  }

  def doAuth(pass: String) = {
    if (config('pass) == null || config('pass) == pass) {
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
    
    val newGame = if (config.filter != null) {
      filterGame(Game.fromString(save), config.filter)
    } else {
      Game.fromString(save)
    }
    
    game = game.merge(newGame)
    count = game.asObject.children.values.foldLeft(0)((total, buf) => total + buf.length) - count

    log(count + " objects received from client.")
    reply(Success(count + " objects successfully uploaded."))

    safeSave(game)
  }

  def filterGame(game: Game, filters: Seq[String]) = {
    import kessler.GameEditor
    val editor = new GameEditor(game)
    
    filters foreach { filter =>
      try {
        log("Applying filter '" + filter + "'")
        editor.runCommand("clean " + filter)
      } catch {
        case e: Exception => log("Error applying filter '" + filter + "': " + e.getMessage)
      }
    }

    editor.game
  }

  def safeSave(game: Game) {
    game.save(config('save) + ".tmp")
    new File(config('save)).delete()
    new File(config('save) + ".tmp").renameTo(new File(config('save)))
  }
  
  def doGet() {
    log("Sending merged save file containing "
      + game.asObject.getChildren("CREW").length
      + " crew and "
      + game.asObject.getChildren("VESSEL").length
      + " vessels."
    )
    
    reply(Success(game.mkString))
  }
}

object KesslerDaemon {
  abstract case class Command();
  case class ConnectCommand(pass: String, version: Int) extends Command
  case class PutCommand(pass: String, game: String) extends Command;
  case class GetCommand(pass: String) extends Command;

  abstract case class Reply();
  case class Success(msg: String) extends Reply;
  case class Error(msg: String) extends Reply;

  def main(args: Array[String]) {
    val configfile = if (args.length > 0) args(0) else "kessler/client_config.txt"
    new KesslerDaemon(configfile).start()
  }
}
