package ksp

class Orbit(self: ksp.Object) extends WrappedObject(self) {}

object Orbit {
  case class Body(id: Int, radius: Double, names: Set[String]) {}

  val bodies = Seq(// radius is in game units (meters), not km
    Body(0,  65.4e6, Set("Kerbol", "Sol", "Sun")),      // radius 65,400 km
    Body(1, 600.0e3, Set("Kerbin", "Kearth", "Earth")), // radius 600km
    Body(2, 200.0e3, Set("Mun", "Muna", "Moon"))        // radius 200km
  )

  def getBody(id: Int) = bodies find (_.id == id)

  def getBody(name: String) = bodies find (_.names contains name)
}
