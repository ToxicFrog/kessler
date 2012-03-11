package ksp

import java.io.{File, FileWriter}


/* Superclass for types that wrap an in-game SFS object */


/*
 * KSP savegame class.
 *
 * A savegame consists of:
 * - a multimap of properties (we'll probably just store this as a list)
 * - a list of crew
 * - a list of vessels
 *
 * In addition, for forwards compatibility, we need to store a list of all blocks of unknown type so that if,
 * say, STATION blocks get added in .15, the library passes them through safely.
 *
 * For writing out the save, we dump properties, then crew, then vessels, then unknown blocks.
 */
object Game {
  def fromFile(file: String) = new Game(SFSParser.parseString("GAME", io.Source.fromFile(file).mkString))
  def fromFile(file: File) = SFSParser.parseString("GAME", io.Source.fromFile(file).mkString)
  def fromString(string: String) = new Game(SFSParser.parseString("GAME", string))
}

class Game(self: Object) extends WrappedObject(self) {
  def mkString = "// KSP Flight State\n// Edited by libKSP\n\n" + self.mkString

  def save(filename: String) {
    val fout = new FileWriter(filename)
    fout.write(mkString)
    fout.close()
  }

  def vessels = self.getChildren("VESSEL") map {
    new Vessel(_)
  }

  // create a new game with only vessels that contain the listed parts
  def filter(parts: Set[String]): ksp.Game = {
    val result = self.copy
    
    result.getChildren("VESSEL").filter {
      vessel => vessel.getChildren("PART").contains(
        (part: ksp.Object) => !(parts contains part.getProperty("name"))
      )
    } foreach {
      result.deleteChild(_)
    }

    new Game(result)
  }
  
  // merge all of other's ships into us, skipping any that we already have
  def merge(other: Game): Game = {
    val result = self.copy

    /*
    * Merging in a vessel is slightly more involved than just copying the underlying object
    * into our set of child objects - we also need to bring the crew over, if any, and remap
    * the crew indexes in this vessel.
    * Note that this is a destructive operation, so it creates a copy first.
    */
    def mergeVessel(v: Object) = {
      v.getChildren("PART").filter(_.hasProperty("crew")).foreach { part =>
        part.getProperties("crew").zipWithIndex.foreach {
          case (crew, i) => {
            mergeObject(other.asObject.getChild("CREW", crew.toInt))
            part.setProperty("crew", i, (result.children("CREW").length-1).toString)
          }
        }
      }

      result.addChild("VESSEL", v)
      v
    }

    def mergeObject(other: Object) {
      result.addChild(other.kind, other.copy)
    }

    other.asObject.getChildren("VESSEL") filterNot {
      /* The comparison here needs to be slightly more involved than "do we already contain
       * this vessel", since it might have fragmented in one save file, in which case we need
       * to mark not just the original but also all debris as duplicates.
       * More problematically, if the root part is gone but there is still some debris left
       * over from it, we need to mark this vessel as a duplicate - basically, a vessel is
       * a duplicate if ANY of its parts exist in the current game.
       */
      _.getChildren("PART").exists(result contains WrappedObject(_))
    } map {
      v => mergeVessel(v.copy)
    }

    new Game(result)
  }
}
