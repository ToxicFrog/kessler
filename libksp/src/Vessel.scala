package ksp

class Vessel(self: ksp.Object) extends WrappedObject(self) {
  assert(self.kind == "VESSEL")
  
  def isDebris = !(self.getChild("PART", self.getProperty("root").toInt) hasProperty "crew")
  def isImport = self.testProperty("name", """\([^)]+\)( Debris)?$""")
  def isLanded = self.testProperty("sit", """(SPLASHED|LANDED)""")
  
  def root = new Part(self.getChild("PART", self.getProperty("root").toInt))

  /* Two VESSELs are equal if:
     - they have the same name, and
     - they have the same root part
  */
  override def equals(other: Any): Boolean = other match {
    case other: ksp.Vessel => {
      (this eq other) || this.asObject.getProperty("name") == other.asObject.getProperty("name") && this.root == other.root
    }
    case _ => super.equals(other)
  }
}

class Part(self: ksp.Object) extends WrappedObject(self) {
  assert(self.kind == "PART")

  // two parts are == if they have the same UID, disregarding name
  override def equals(other: Any): Boolean = other match {
    case other: Part => (other.asObject.getProperty("uid") == self.getProperty("uid"))
    case _ => super.equals(other)
  }
}
