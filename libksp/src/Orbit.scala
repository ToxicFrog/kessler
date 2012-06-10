package ksp

class Orbit(self: ksp.Object) extends WrappedObject(self) {}

object Orbit {
  case class Body(id: Int, radius: Double, names: Set[String]) {}

  val bodies = Seq(// radius is in game units (meters), not km
    Body(0,  65.4e6, Set("kerbol", "sol", "sun")),      // radius 65,400 km
    Body(1, 600.0e3, Set("kerbin", "kearth", "earth")), // radius 600km
    Body(2, 200.0e3, Set("mun", "muna", "moon")),       // radius 200km
    Body(3,  60.0e3, Set("minmus", "minimus"))          // radius 60km
  )

  def getBody(id: Int) = bodies find (_.id == id)

  def getBody(name: String) = bodies find (_.names contains (name.toLowerCase))
}
