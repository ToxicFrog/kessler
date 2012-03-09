package kessler

import ksp._
import scala.actors._

/**
 * A daemon for merging save files and replying with the merged files.
 *
 * Operations are:
 * - update <content> - merge this content into the master save file
 * - get - get a copy of the merged file
 * - get <list> - get a copy of the merged file, excluding rockets that require parts not in <list>
 */
class KesslerDaemon(port: Int, pass: String, games: List[String]) extends Actor {
  import scala.actors.remote.RemoteActor._
  import KesslerDaemon.{SendCommand,GetCommand,Success,Error}

  var game = loadFile(games)
  
  def loadFile(names: List[String]): Game = {
    if (names.isEmpty) {
      println("Error: couldn't find any games to load. Exiting.")
      System.exit(1)
    } else {
      print("Looking for " + names.head + ":")
    }
    
    try {
      val game = Game.fromFile(names.head)
      println(" loaded " + names.head)
      game
    } catch {
      case e: Exception => println(" failed (" + e.getMessage + ")"); loadFile(names.tail)
    }
  }

  override def exceptionHandler = {
    case e: Exception => e.printStackTrace()
  }
  
  def act() {
    alive(port)
    register('kesslerd, this)
    println("Kessler daemon running on port " + port)

    loop {
      react {
        case SendCommand(pass, save) => if (doAuth(pass)) doSend(save);
        case GetCommand(pass, parts) => if (doAuth(pass)) doGet(parts);
      }
    }
  }

  def doAuth(pass: String) = {
    if (this.pass != null && this.pass == pass) {
      true
    } else {
      reply(Error("invalid password"))
      false
    }
  }

  def doSend(save: String) {
    var count = game.asObject.children.values.foldLeft(0)((total, buf) => total + buf.length)

    println("Merging in new save.")
    game = game.merge(Game.fromString(save))

    count = game.asObject.children.values.foldLeft(0)((total, buf) => total + buf.length) - count
    
    reply(Success(count + " objects merged."))

    // FIXME: save game
  }
  
  def doGet(parts: Set[String]) {
    println("Game request from " + sender)
    println("Available parts: " + parts.mkString(", "))

    val filtered = game.filter(parts)
    println("Created save containing "
      + filtered.asObject.getChildren("CREW").length
      + " crew and "
      + filtered.asObject.getChildren("VESSEL").length
      + " vessels.")
    reply(Success(filtered.mkString))
  }
}

object KesslerDaemon {
  abstract case class Command();
  case class SendCommand(pass: String, game: String) extends Command;
  case class GetCommand(pass: String, exclude: Set[String]) extends Command;

  abstract case class Reply();
  case class Success(msg: String) extends Reply;
  case class Error(msg: String) extends Reply;

  val default_games = List("kessler/merged.sfs", "saves/default/persistent.sfs")

  def main(args: Array[String]) {
    val port = if (args.length > 0) args(0).toInt else 8988
    val pass = if (args.length > 1) args(1) else null
    val games = if (args.length > 2) args.drop(2).toList else default_games

    new KesslerDaemon(port, pass, games).start()
  }
}



object Test {
  def main(args: Array[String]) {
    KesslerDaemon.main(Array("8988", "password", "toxicfrog.sfs"))
    KesslerClient.main(Array("localhost", "8988", "send", "airconswitch.sfs", "password"))
    KesslerClient.main(Array("localhost", "8988", "send", "airconswitch.sfs", "spiders"))
    KesslerClient.main(Array("localhost", "8988", "get", "kdtest.sfs", "spiders"))
    KesslerClient.main(Array("localhost", "8988", "get", "kdtest2.sfs", "password"))
    KesslerClient.main(Array("localhost", "8988", "send", "bentai.sfs"))
    KesslerClient.main(Array("localhost", "8988", "get", "kdtest3.sfs"))
  }
}
