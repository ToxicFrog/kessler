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
      val line = ask(prompt)
      if (line == null) { println(""); System.exit(0) }
      if (line.trim.length > 0) runCommand(line)
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


