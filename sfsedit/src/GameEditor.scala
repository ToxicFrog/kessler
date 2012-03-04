import java.io.File
import java.util.Scanner
import ksp.Game

object GameEditor extends DefaultTextUI {
  var backed_up = false
  var dirty = false
  var filename = ""
  var timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@hh-mm-ss").format(new java.util.Date())

  var game: ksp.Game = null
  var selected: Seq[ksp.Vessel] = Seq.empty[ksp.Vessel]
  var stack: List[Seq[ksp.Vessel]] = List.empty[Seq[ksp.Vessel]]
  
  override def prompt = "\n(%d) ".format(selected.length)

  register(LoadCommand)     // load a file
  register(RevertCommand)   // revert to version on disk
  register(SaveCommand)     // save file to disk

  register(SelectCommand)   // select all vessels matching P
  register(PushCommand)     // push the current selection
  register(PopCommand)      // pop the current selection
  register(ListCommand)     // list the current selection
  register(AddCommand)      // add all vessels matching P to the selection
  register(RemoveCommand)   // remove all vessels matching P from the selection
  register(ChooseCommand)   // choose a subset of the selection

  register(CleanCommand)    // do some automatic cleanup of the savegame
  register(DeleteCommand)   // delete objects from the game
  register(SetCommand)      // set an object or subobject property
  register(GetCommand)      // display an object or subobject property
  register(OrbitCommand)    // move an object to another orbit

  register(CheckedExitCommand)     // quit the editor
  /*
  register(OrbitCommand)    // edit the orbits of the selected objects
  */
  
  def checkDirty = !dirty || askYN("All unsaved changes will be lost. Are you sure?")
  
  def loadGame(name: String) {
    println("Loading '" + name + "'...")
    game = Game.fromFile(name)
    filename = name
    backed_up = false
    dirty = false
    timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@hh-mm-ss").format(new java.util.Date())
    runCommand("select all")
  }

  type Filter = ksp.Vessel => Boolean
  type Op = (String, String) => Boolean
  def input2filter(in: Scanner) = {
    var p: Filter = (_ => true)

    def concat(first: Filter, second: Filter): Filter = (obj => first(obj) && second(obj))
    def not(first: Filter): Filter = (obj => !first(obj))
    def hasprop(key: String): Filter = (obj => obj.asObject.hasProperty(key))
    def comparer(key: String, op: Op, value: String): Filter = (
      obj => op(obj.asObject.getProperty(key), value)
    )
    
    while (in.hasNext) {
      val (key,invert) = in.next match {
        case k if k startsWith "!" => (k.slice(1, k.length), true)
        case k => (k, false)
      }

      // special cases like 'all' and 'debris'
      if (specials contains key) {
        if (invert)
          p = concat(p, not(specials(key)))
        else
          p = concat(p, specials(key))

      // parse 'key op value' condition
      } else {
        val (op,invert) = in.next match {
          case o if o startsWith "!" => (o.slice(1, o.length), true)
          case o => (o, false)
        }

        val value = in.findInLine("((?!&&).)+").trim
        // skip '&&'
        if (in.hasNext) in.next

        if (invert)
          p = concat(concat(p, hasprop(key)), not(comparer(key, compares(op), value)))
        else
          p = concat(concat(p, hasprop(key)), comparer(key, compares(op), value))
      }
    }
    p
  }

  private val specials = Map[String, Filter](
    "landed" -> (_.isLanded),
    "debris" -> (_.isDebris),
    "invert" -> (obj => !isSelected(obj)),
    "all" -> (_ => true)
  )

  private val compares = Map[String, Op](
    "="  -> ((x,y) => x == y),
    "==" -> ((x,y) => x == y),
    ">"  -> ((x,y) => try { x.toDouble > y.toDouble } catch { case _ => x > y }),
    "<"  -> ((x,y) => try { x.toDouble < y.toDouble } catch { case _ => x < y }),
    "~"  -> ((x,y) => x contains y),
    "/"  -> ((x,y) => y.r.findFirstMatchIn(x).isDefined)
  )
  
  def isSelected(obj: ksp.Vessel) = selected exists (_.asObject == obj.asObject)
  def select(p: Filter) {
    selected = game.vessels.filter (p(_))
  }

  def delete(v: ksp.Vessel) {
    dirty = true
    game.asObject.deleteChild("VESSEL", v.asObject)
  }
  
  protected object LoadCommand extends Command("load") {
    override def describe = "load a saved game from disk"
    override def help = """
      Usage: load <file>

      Loads a new game from <file>. The currently loaded game, if any, will be discarded, and
      any unsaved changes will be lost.
    """

    override def run(in: Scanner) {
      if (checkDirty) {
        if (!in.hasNext) {
          HelpCommand.run("load")
        } else {
          loadGame(in.next)
        }
      }
    }
  }
  
  protected object RevertCommand extends Command("revert") {
    override def describe = "revert to the last saved version"
    override def help = """
      Usage: revert
      
      Reloads the game from disk. All changes since your last 'save' or 'load' will be lost.
      This is equivalent to 'load <file>' where <file> is the name of the currently loaded file.
    """
    
    override def run(in: Scanner) {
      if (checkDirty) {
        LoadCommand.run(filename)
      }
    }
  }
  
  protected object SaveCommand extends Command("save") {
    override def describe = "save changes made to the game"
    override def help = """
      Usage: save

      Saves any changes you have made, overwriting the original file. If the file has not yet been
      backed up, backs it up first (by appending the current date and time to the filename).
    """

    override def run(in: Scanner) {
      if (!backed_up) {
        val backupname = filename + "." + timestamp
        println("Creating backup " + backupname)
        new File(filename).renameTo(new File(backupname))
        backed_up = true
      }
      
      println("Saving...")
      game.save(filename)
      dirty = false
    }
  }
  
  protected object CleanCommand extends Command("clean") {
    override def describe = "automatically delete objects from the game"
    override def help = """
      Usage: clean <filters>

      Selects all objects matching <filters> and deletes them. The current selection is left unchanged
      (except, of course, that any objects deleted by clean will vanish from the selection). This is
      basically equivalent to:

        push
        select <filters>
        delete
        pop

      For detailed instructions on filters, see 'help select'.
    """

    override def run(in: Scanner) {
      PushCommand.run(in)
      select(input2filter(in))
      selected foreach (delete _)
      PopCommand.run(in)
    }
  }

  protected object SelectCommand extends Command("select") {
    override def describe = "select all objects meeting a condition"
    override def help = """
      Usage: select <filter> [&& <filter>...]

      Searches the game for all objects matching <filter>, and selects them. This replaces the
      current selection; if you want to add or remove objects from the current selection, try
      'add', 'remove', or 'choose'.

      <filter> is either a predefined filter name (see below) or an expression in the form
      <name> <operator> <value>. <name> is any property name an object can have (objects which
      do not have that property are considered not to match the filter). <value> is any text.
      <operator> is any of the following operators:

        =     equality
        >     greater than
        <     less than
        ~     contains (eg, 'name ~ Station' would match all objects with Station in the name)
        /     regex match (eg, 'name / ^Station' would match all objects with names starting with Station)

      Additionally, any of these can be prefixed with ! to invert the sense of the operator; for
      example, 'name !~ Station' would match all objects with names that do NOT contain "Station".

      In addition to these operators, there are a few one-word filters included for convenience:

        landed    matches all objects which are landed or splashed down
        debris    matches all debris objects (objects without crew)
        invert    matches all objects not currently selected (selected items are deselected and vice versa)
        all       matches all objects

      Like the operators, these can be prefixed with ! to invert them.

      Finally, filters can be chained together using '&&'. For example, the command:

        select !debris && name ~ Station

      Would select all non-debris objects with "Station" somewhere in the name.
    """

    override def run(in: Scanner) {
      select(input2filter(in))
      ListCommand.run("")
    }
  }

  protected object ListCommand extends Command("list") {
    override def describe = "list selected objects"
    override def help = """
      Usage: list

      Lists all currently selected objects, with names and ID numbers.
    """

    override def run(in: Scanner) {
      selected.zipWithIndex.foreach {
        case (v, n) => println(n + "\t" + v.asObject.getProperty("name"))
      }
    }
  }

  protected object PushCommand extends Command("push") {
    override def describe = "save the current selection for later retrieval with 'pop'"
    override def help = """
      Usage: push

      Saves the current selection to an internal stack. The 'pop' command can be used to
      retrieve it later. Note that this saves the SELECTION, not the objects; if you delete
      or edit objects after 'push'ing, a later 'pop' will not revert your edits or restore
      deleted objects.
    """

    override def run(in: Scanner) {
      stack = selected :: stack
    }
  }
  
  protected object PopCommand extends Command("pop") {
    override def describe = "restore a selection previously saved with 'push'"
    override def help = """
      Usage: pop

      Restores the selection most recently saved with 'push'. See 'help push' for details on
      what is and is not restored.
    """

    override def run(in: Scanner) {
      if (stack.isEmpty) {
        println("No saved selection to restore!")
      } else {
        select(obj => stack.head contains obj)
        stack = stack.tail
      }
    }
  }
  
  protected object ChooseCommand extends Command("choose") {
    override def describe = "pick specific objects from the selection"
    override def help = """
      Usage: choose <index> [index...]

      Chooses some objects from the selection, by number (the numbers are displayed by
      "show" or whenever the selection changes). <index> can either be a single number
      (in which case it selects exactly that object), or a range 'low-high', in which
      case it selects all objects numbered between low and high INCLUSIVE.

      For example, the command 'choose 0-5 8 9 14-17' would select objects 0,1,2,3,4,5,
      8,9,14,15,16,17.
    """
  }
  
  protected object AddCommand extends Command("add") {
    override def describe = "add objects to the selection"
    override def help = """
      Usage: add <condition>

      Adds all objects meeting the condition to the current selection. The condition
      works the same way as 'select'; see 'help select' for details.
    """
  }
  
  protected object RemoveCommand extends Command("remove") {
    override def describe = "remove objects from the selection"
    override def help = """
      Usage: remove <condition>

      Removes all objects meeting the condition from the current selection. The condition
      works the same way as 'select'; see 'help select' for details.
    """
  }
  
  protected object DeleteCommand extends Command("delete") {
    override def describe = "delete objects from the game"
    override def help = """
      Usage: delete
      
      Deletes all currently selected objects from the game, permanently.
    """
    
    override def run(in: Scanner) {
      selected foreach (delete _)
      select(_ => false)
    }
  }
  
  protected object SetCommand extends Command("set") {
    override def describe = "set object or subobject properties"
    override def help = """
      Usage: set <property> <value>

      Set an object property.

      <value> is any text.

      <property> is a property specifier, of the form {type[,index]:}name{,index} - that is to
      say, any number of 'type:' or 'type,index:' prefixes, followed by a property name, optionally
      followed by an index number.

      If a type prefix is specified without an index number, the first subobject of that type is
      edited. If the index number is '*', ALL subobjects of that type are edited.

      For example, the following commands rename all of the selected objects to "Jeb's Joyride", set
      their orbital eccentricity to 0.8, and make the first part super-heavy and everything else
      nearly weightless:

        set name Jeb's Joyride
        set ORBIT:ECC 0.8
        set PART,*:mass 0.01
        set PART,0:mass 1000
    """
    
    override def run(in: Scanner) {
      val (key,value) = (in.next, in.nextLine.trim)

      dirty = true
      selected foreach (_.setParsedProperties(key, value))
    }
  }
  
  protected object GetCommand extends Command("get") {
    override def describe = "display object or subobject properties"
    override def help = """
      Usage: get <property>
      
      Display an object property. If more than one object is selected, displays the property
      for each object, prefixed with the object name.
      
      Property naming rules are the same as for set (see 'help set').
    """
    
    override def run(in: Scanner) {
      val key = in.next
      
      selected foreach { obj =>
        println(obj.asObject.getProperty("name"))
        obj.getParsedProperties(key) foreach {
          value => println("\t" + value)
        }
      }
    }
  }
  
  protected object OrbitCommand extends Command("orbit") {
    override def describe = "move objects into stable orbit"
    override def help = """
      Usage: orbit <body> [altitude [inclination]]

      Teleports the selected objects into orbit around the given body, optionally with some
      fine-tuning of the orbit.

      <body> can be given either as a name ("Kerbol", "Kerbin", or "Mun") or as an object ID
      (0-2).

      Altitude is altitude above the surface (in km), not orbital radius - for example,
      'teleport Kerbin 100' will give you an altitude of 100km and a semi-major axis of
      700km.

      Inclination is given in degrees. An inclination of 0 will give you an equatorial orbit;
      90 will give you a polar orbit. (180 will give you an equatorial orbit in the opposite
      direction.)

      No other modifications of the object are made, so exactly where in its orbit the object
      appears - and what direction it's facing and so forth - will depend on its state before
      it was teleported.

      For example, to move all debris into a nice polar orbit around Kerbin at 250km:

        select debris
        orbit Kerbin 250 90
    """

    override def run(in: Scanner) {
      val bodyname = in.next
      val body = try {
        ksp.Orbit.getBody(bodyname.toInt)
      } catch {
        case _ => ksp.Orbit.getBody(bodyname)
      }
      
      val SMA = if (in.hasNext)
        in.next.toDouble * 1000.0 + body.radius
      else
        100000.0 + body.radius
      
      val INC = if (in.hasNext)
        in.next.toDouble
      else
        0.0
      
      SetCommand.run("ORBIT:REF " + body.id)
      SetCommand.run("ORBIT:ECC 0.0")
      SetCommand.run("ORBIT:SMA " + SMA)
      SetCommand.run("ORBIT:INC " + INC)
    }
  }
  
  protected object CheckedExitCommand extends Command("exit") {
    override def describe = "exit the program"
    override def help = """
      Usage: exit

      Exits the program.
    """

    override def run(in: Scanner) {
      if (checkDirty) {
        System.exit(0)
      }
    }
  }

  def main(args:Array[String]) {
    if (args.length > 0) runCommand("load " + args(0))

    run()
  }
}
