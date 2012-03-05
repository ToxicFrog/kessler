package ksp

class WrappedObject(self: Object) {
  def asObject = self

  override def hashCode = self.hashCode

  // subclass T should override equals(Any) and define it on (other: T) if it wants custom
  // equality
  override def equals(other: Any): Boolean = other match {
    case other: Object => this == WrappedObject(other)
    case other: WrappedObject => self == other.asObject
    case _ => super.equals(other)
  }

}

object WrappedObject {
  def apply(self: Object): WrappedObject = self.kind match {
    case "VESSEL" => new Vessel(self)
    case "PART"   => new Part(self)
    case "GAME"   => new Game(self)
    case _        => new WrappedObject(self)
  }
}
