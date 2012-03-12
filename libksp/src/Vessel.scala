package ksp

class Vessel(self: ksp.Object) extends WrappedObject(self) {
  assert(self.kind == "VESSEL")
  
  def root = new Part(self.getChild("PART", self.getProperty("root").toInt))

  /* Two VESSELs are equal if:
     - they have the same name, and
     - they have the same root part
  */
  override def equals(other: Any): Boolean = other match {
    case other: ksp.Vessel => {
      (this eq other) || (this.self.getProperty("lct") == other.asObject.getProperty ("lct")) || (this.self == other.asObject)
    }
    case _ => super.equals(other)
  }
}

object Vessel {
  def isDebris(obj: Object) = if (obj.kind == "VESSEL") {
    obj.getChild("ORBIT").getProperty("OBJ") != "0"
  } else false

  def isLanded(obj: Object): Boolean = if (obj.kind == "VESSEL") {
    obj.testProperty("sit", """(SPLASHED|LANDED)""")
  } else false

  def isGhost(obj: Object): Boolean = if (obj.kind == "VESSEL") {
    obj.getChildren("PART").isEmpty
  } else false
}

class Part(self: ksp.Object) extends WrappedObject(self) {
  assert(self.kind == "PART")

  // two parts are == if they have the same UID, disregarding name
  override def equals(other: Any): Boolean = other match {
    case other: Part => (other.asObject.getProperty("uid") == self.getProperty("uid"))
    case _ => super.equals(other)
  }
}
