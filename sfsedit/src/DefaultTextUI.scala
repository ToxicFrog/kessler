import java.util.Scanner

class DefaultTextUI extends TextUI {
  register(HelpCommand)
  register(ExitCommand)

  protected object HelpCommand extends Command("help") {
    override def describe = "list commands or view command documentation"

    override def help = """
      Usage: help [command]

      Without arguments, lists all available commands and a short description of each.
      If [command] is specified, displays detailed help for that command.
    """

    override def run(in: Scanner) {
      /* No args? List commands. */
      val maxCommandWidth = commands.foldLeft(0)((len, kv) => len.max(kv._1.length)) + 2
      if (!in.hasNext) {
        commands.values.toSeq.sortWith {
          (lhs, rhs) => lhs.name.compare(rhs.name) < 0
        } foreach {
          command => println(("%" + maxCommandWidth + "s  %s").format(command.name, command.describe))
        }
      } else {
        val name = in.next()

        if (commands contains name) {
          println(commands(name).help)
        } else {
          missingCommand(name)
        }
      }
    }
  }

  protected object ExitCommand extends Command("exit") {
    override def describe = "exit the program"

    override def help = """
      Usage: exit

      Exits the program.
    """

    override def run(in: Scanner) {
      System.exit(0)
    }
  }

}
