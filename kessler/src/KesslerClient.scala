package kessler

import ksp._
import util.logging._

class KesslerClient extends Logged {

  import java.util.Properties
  import java.io.{File,FileInputStream}
  import actors.remote.Node
  import actors.remote.RemoteActor._
  import KesslerDaemon.{PutCommand, GetCommand, ConnectCommand, Success, Error}

  val VERSION = 12031300;

  // load configuration file - this is a normal Java property file
  private object config extends Properties(new Properties()) {
    load(new FileInputStream("kessler/client_config.txt"))

    defaults put ("host", "localhost")
    defaults put ("port", "8988")
    defaults put ("filter", "nan,ghost,launchpad")
    defaults put ("clean", "nan,ghost")

    def apply(key: String) = getProperty(key)
    def apply(key: Symbol) = getProperty(key name)

    def port = this('port).toInt
    def filter = this('filter).split(",")
    def clean = this('clean).split(",")
  }

  val server = select(Node(config('host), config.port), 'kesslerd)

  // connect to the server - we automatically send a ConnectCommand, which
  // includes an authentication and version check
  // If we timeout or get a version mismatch, send() will call die()
  def connect() {
    log("Connecting to " + config('host) + ":" + config.port + "...")
    send(10000, ConnectCommand(config('pass), VERSION))
  }

  def die(reason: String) = {
    log(reason)
    System.exit(1)
    ""
  }

  def send(timeout: Long, message: KesslerDaemon.Command): String = {
    server !? (timeout, message) match {
      case Some(Success(msg)) => msg
      case Some(Error(msg)) => die("Error from server: " + msg)
      case None => die("Timeout in communication with server.")
      case other => die("Invalid message from server: " + other)
    }
  }

  def syncGame(save: String) {
    putGame(save)
    getGame(save)
  }

  def putGame(save: String) {
    log("Uploading " + save + " to server for merge...")
    log(send(60000, PutCommand(config('pass), io.Source.fromFile(save).mkString)))
  }

  def logRejects(game: Game, parts: Set[String]) {
    game.asObject.getChildren("VESSEL").foreach { v =>
      val missing = v.getChildren("PART").map(_.getProperty("name")).filterNot(parts contains _)

      if (!missing.isEmpty) {
        log("Rejecting vessel " + v.getProperty("name") + " - requires the following parts:")
        missing foreach { name =>
          log("\t" + name)
        }
      }
    }
  }
  
  def getGame(save: String) {
    log("Requesting new save file from server...")
    val localGame = Game.fromFile(save)
    var remoteGame = Game.fromString(send(60000, GetCommand(config('pass))))

    log("Scanning KSP directory for parts...")
    val parts = listParts

    if (config.filter != null) {
      remoteGame = filterGame(remoteGame, config.filter)
    }

    log("Merging save...")
    if (config('log_rejects) != null) {
      logRejects(remoteGame, parts)
    }
    var newGame = localGame.merge(remoteGame.filter(parts))

    if (config.clean != null) {
      newGame = filterGame(newGame, config.clean)
    }

    /* update elapsed-time value in downloaded save to match local save so
       orbits are correct */
    val UT = localGame.asObject.getProperty("UT")
    log("Setting timestamp in merged game to " + UT)
    newGame.asObject.setProperty("UT", UT)

    safeSave(save, newGame)
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

  def safeSave(filename: String, game: Game) {
    val timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@HH.mm.ss").format(new java.util.Date())
    log("Saving to " + filename)

    if (!new File(filename).renameTo(new File(filename + "." + timestamp)))
      log("Warning: couldn't create backup of save file.")

    game.save(filename)
  }

  def listParts: Set[String] = {
    new File("parts").listFiles.filter(
      x => new File(x, "part.cfg").exists
    ).map( x =>
      try {
        """_""".r.replaceAllIn(Object.fromFile(new File(x, "part.cfg")).getProperty("name"), ".")
      } catch {
        case e: Exception => {
          log("Error loading part definition for " + x + ": " + e.getMessage + "; skipping")
          "<corrupt part definition>"
        }
      }
    ).toSet
  }
}

object KesslerClient {
  import java.io.PrintStream

  trait ClientLogger extends Logged {
    val out = new PrintStream("kessler/client_log.txt")

    override def log(s: String) {
      println(s)
      out.println(s)
    }
  }

  def main(args: Array[String]) {
    val client = new KesslerClient with ClientLogger

    try {
      val command = args(0)
      val arg = args(1)

      client.connect()

      command match {
        case "put" => client.putGame(arg)
        case "get" => client.getGame(arg)
        case "sync" => client.syncGame(arg)
        case other => client.die("Invalid command: " + other)
      }
    } catch {
      case e: Exception => {
        client.log("  ---- BEGIN STACK TRACE ----")
        e.printStackTrace(client.out)
        client.log("   ---- END STACK TRACE ----")
        client.log("Unhandled error executing Kessler client!")
        client.log("Please record the above stack trace and report it to the developer as a bug.")
        print("Press enter to continue...")
        readLine()
      }
    }
  }
}
