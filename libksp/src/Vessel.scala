package ksp

class Vessel(self: ksp.Object) extends WrappedObject(self) {
  def isDebris = !(self.getChild("PART", self.getProperty("root").toInt) hasProperty "crew")
  def isImport = self.testProperty("name", """\([^)]+\)( Debris)?$""")
  def isLanded = self.testProperty("sit", """(SPLASHED|LANDED)""")
}
