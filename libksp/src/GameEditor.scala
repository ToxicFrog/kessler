package ksp

object GameEditor {
  def main(args: Array[String]) {
    val game = Game.fromFile(args(0))

    cleanAuto(game)
    println(game.mkString)
  }

  def clean(game: Game, name: String, key: String, value: Regex) {
    game.getChildren(name) filterNot {
      v => value.findFirstMatchIn(v.getProperty(key)).isEmpty
    } foreach {
      game.deleteChild(name, _)
    }
  }

  def cleanAuto(game: Game) {
    cleanDebris(game)
    cleanImportedFlights(game)
    cleanDeadCrew(game)
  }

  def cleanDebris(game: Game, only_imported: Boolean) {
    if (only_imported) {
      clean(game, "VESSEL", "name", """\([^)]+\) Debris$""".r)
    } else {
      clean(game, "VESSEL", "name", """Debris$""".r)
    }
  }

  def cleanFlights(game: Game, only_imported: Boolean) {
    if (only_imported) {
      clean(game, "VESSEL", "name", """\([^)]+\)$""".r)
    } else {
      clean(game, "VESSEL", "name", """.""".r)
    }
  }

  def cleanDeadCrew(game: Game) {

  }
}
