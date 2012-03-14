package kessler

import ksp._
import actors._
import java.io.PrintStream

class KesslerClient(command: String, arg: String) extends Actor {

  import java.util.Properties
  import java.io.{File,FileInputStream}
  import actors.remote.Node
  import actors.remote.RemoteActor._
  import KesslerDaemon.{PutCommand, GetCommand, ConnectCommand, Success, Error}

  val VERSION = 12031300;

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
    println("Connecting to " + config('host) + ":" + config.port + "...")

    send(10000, ConnectCommand(config('pass), VERSION))
    
    command match {
      case "put" => putGame()
      case "get" => getGame()
      case other => die("Invalid command: " + other)
    }

    System.exit(0)
  }
  
  def send(timeout: Long, message: KesslerDaemon.Command): String = {
    server !? (timeout, message) match {
      case Some(Success(msg)) => msg
      case Some(Error(msg)) => die("Error from server: " + msg)
      case None => die("Error: timeout in communication with server.")
      case other => die("Invalid message from server: " + other)
    }
  }

  def putGame() {
    println("Uploading " + arg + " to server for merge...")
    println(send(60000, PutCommand(config('pass), io.Source.fromFile(arg).mkString)))
  }

  def logRejects(game: Game, parts: Set[String]) {
    val fout = new PrintStream(config('log_rejects))

    game.asObject.getChildren("VESSEL").foreach { v =>
      val missing = v.getChildren("PART").map(_.getProperty("name")).filterNot(parts contains _)

      if (!missing.isEmpty) {
        fout.println("Rejecting vessel " + v.getProperty("name") + " - requires the following parts:")
        missing foreach { name =>
          fout.println("\t" + name)
        }
      }
    }
    
    fout.close()
  }
  
  def getGame() {
    println("Requesting new save file from server...")
    val localGame = Game.fromFile(arg)
    var remoteGame = Game.fromString(send(60000, GetCommand(config('pass))))

    println("Scanning KSP directory for parts...")
    val parts = listParts

    if (config.filter != null) {
      remoteGame = filterGame(remoteGame, config.filter)
    }

    println("Merging save...")
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
    println("Setting timestamp in merged game to " + UT)
    newGame.asObject.setProperty("UT", UT)

    safeSave(arg, newGame)
  }

  def filterGame(game: Game, filters: Seq[String]) = {
    import kessler.GameEditor
    val editor = new GameEditor(game)

    filters foreach { filter =>
      try {
        println("Applying filter '" + filter + "'")
        editor.runCommand("clean " + filter)
      } catch {
        case e: Exception => println("Error applying filter '" + filter + "': " + e.getMessage)
      }
    }

    editor.game
  }

  def safeSave(filename: String, game: Game) {
    val timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@HH.mm.ss").format(new java.util.Date())
    println("Saving to " + filename)

    if (!new File(filename).renameTo(new File(filename + "." + timestamp)))
      println("Warning: couldn't create backup of save file.")

    game.save(filename)
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
