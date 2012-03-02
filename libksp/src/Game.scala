package ksp

import java.io.FileWriter

/* Superclass for types that wrap an in-game SFS object */
abstract class WrappedObject(self: Object) {
  def asObject = self
}

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
  def fromFile(file: String) = SFSParser.parseString(io.Source.fromFile(file).mkString)
  def fromString(string: String) = SFSParser.parseString(string)
}

class Game(self: Object) extends WrappedObject(self) {
  def mkString = "// KSP Flight State\n// Edited by libKSP\n\n" + self.mkString

  def save(filename: String) {
    val fout = new FileWriter(filename)
    fout.write(mkString)
    fout.close()
  }

  def vessels = self.getChildren("VESSEL") map { new Vessel(_) }

  def clean(p: (Vessel => Boolean)) {
    vessels.filter(p).foreach {
      v => println("\tD " + v.asObject.getProperty("name")); self.deleteChild("VESSEL", v.asObject)
    }
  }
}

class Vessel(self: ksp.Object) extends WrappedObject(self) {
  def isDebris = !(self.getChild("PART", self.getProperty("root").toInt) hasProperty "crew")
  def isImport = self.testProperty("name", """\([^)]+\)( Debris)?$""")
  def isLanded = self.testProperty("sit", """(SPLASHED|LANDED)""")
}
