import java.util.Scanner
import collection.mutable.Map
import annotation.tailrec

class TextUI {
  val commands = Map[String, Command]()
  def prompt = "\n> "
  def missingCommand(name: String) {
    println("No command '" + name + "' - try 'help'.")
  }

  type CommandFunc = (Scanner => Unit)
  type CommandMap = collection.mutable.Map[String, Command]

  class Command(_name: String) {
    var commands: CommandMap = null;
    def name      = _name
    def describe  = ""
    def help      = "No help is available for this command."
    def run(in: Scanner) {
      throw new UnsupportedOperationException(name)
    }
    def run(in: String) {
      run(new Scanner(in))
    }
  }

  def register(c: Command) {
    c.commands = commands
    commands += ((c.name, c))
  }

  @tailrec final def run() {
    try {
      val in = new Scanner(ask(prompt))
      
      if (in.hasNext) runCommand(in)
    } catch {
      case e: Exception => e.printStackTrace()
    }
    run()
  }
  
  def runCommand(in: String) {
    runCommand(new Scanner(in))
  }

  def runCommand(in: Scanner) {
    val command = in.next
    
    if (commands contains command) {
      commands(command).run(in)
    } else {
      missingCommand(command)
    }
  }
  
  def ask(question:String):String = {
    print(question)
    readLine()
  }

  def askYN(question:String):Boolean = {
    val answer = ask(question + " [y/N]:")
    answer.startsWith("y") || answer.startsWith("Y")
  }

  def askAndThen(question:String) = {
    (p:() => Unit) => {
      if(askYN(question)) p()
    }
  }
}

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
