package kessler

import java.io.File
import java.util.Scanner
import ksp.Game

class GameEditor(var game: Game = null) extends DefaultTextUI {
  var backed_up = false
  var dirty = false
  var filename = ""
  var timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd@hh-mm-ss").format(new java.util.Date())

  type Selection = Seq[ksp.Object]
  var selected: Selection = Seq.empty[ksp.Object]
  var stack: List[Selection] = List.empty[Selection]
  var select_type: String = "VESSEL"
  
  override def prompt = "\n(%d) ".format(selected.length)

  register(LoadCommand)     // load a file
  register(MergeCommand)    // merge in vessels from another save file
  register(RevertCommand)   // revert to version on disk
  register(SaveCommand)     // save file to disk

  register(TypeCommand)     // change which type of object you're editing
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
  register(PilotCommand)    // make an object remote-pilotable

  register(CheckedExitCommand)     // quit the editor

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

  type Filter = ksp.Object => Boolean
  type Op = (String, String) => Boolean

  def defaultFilter(o: ksp.Object): Boolean = select_type == "*" || o.kind == select_type

  def input2filter(in: Scanner) = {
    var p: Filter = defaultFilter _

    def concat(first: Filter, second: Filter): Filter = (obj => first(obj) && second(obj))
    def not(first: Filter): Filter = (obj => !first(obj))
    def comparer(key: String, op: Op, value: String): Filter = (
      _.getParsedProperty(key) exists (op(_, value))
    )

    while (in.hasNext) {
      val (key,invert) = in.next match {
        case k if k startsWith "!" => (k.slice(1, k.length), true)
        case k => (k, false)
      }

      // special cases like 'all' and 'debris'
      if (specials contains key.toLowerCase) {
        if (invert)
          p = concat(p, not(specials(key.toLowerCase)))
        else
          p = concat(p, specials(key.toLowerCase))

      // parse 'key op value' condition
      } else {
        val (op,invert) = in.next match {
          case o if o startsWith "!" => (o.slice(1, o.length), true)
          case o => (o, false)
        }

        val value = in.findInLine("((?!&&).)+").trim

        if (invert)
          p = concat(p, not(comparer(key, compares(op), value)))
        else
          p = concat(p, comparer(key, compares(op), value))
      }

      // skip '&&'
      if (in.hasNext) in.next
    }
    p
  }

  private val specials = Map[String, Filter](
    "all"       -> (_ => true),
    "debris"    -> (obj => ksp.Vessel.isDebris(obj)),
    "ghost"     -> (obj => ksp.Vessel.isGhost(obj)),
    "invert"    -> (obj => !isSelected(obj)),
    "ksc"       -> (obj => obj("landedAt") == "KSC"),
    "landed"    -> (obj => ksp.Vessel.isLanded(obj)),
    "launchpad" -> (obj => obj("landedAt") == "LaunchPad"),
    "nan"       -> (obj => obj.containsNaN)
  )

  private val compares = Map[String, Op](
    "="  -> ((x,y) => x == y),
    "==" -> ((x,y) => x == y),
    ">"  -> ((x,y) => try { x.toDouble > y.toDouble } catch { case _ => x > y }),
    "<"  -> ((x,y) => try { x.toDouble < y.toDouble } catch { case _ => x < y }),
    "~"  -> ((x,y) => x contains y),
    "/"  -> ((x,y) => y.r.findFirstMatchIn(x).isDefined)
  )
  
  def isSelected(obj: ksp.Object) = selected contains obj
  def select(p: Filter) {
    selected = game.asObject.getChildren.filter (p(_))
  }

  def delete(v: ksp.Object) {
    dirty = true
    game.asObject.deleteChild(v)
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

  protected object MergeCommand extends Command("merge") {
    override def describe = "merge all vessels in another save file into this one"
    override def help = """
      Usage: merge <filename>

      Attempts to merge the contents of another save file into this one. Duplicate
      vessels will not be merged (duplicates include debris originally part of
      duplicate vessels). Any crewed vessels merged in will have their crew merged
      in as well, but other (read: dead) crewmembers will not be merged.

      After the merge completes, all of the vessels added by the merge will be selected,
      allowing you to easily edit them.
    """

    override def run(in: Scanner) {
      val oldgame = game
      game = game.merge(Game.fromFile(in.next))
      dirty = true
      select(o => defaultFilter(o) && !oldgame.asObject.contains(o))
      ListCommand.run("")
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

  protected object TypeCommand extends Command("type") {
    override def describe = "choose what type of game objects to edit"
    override def help = """
      Usage: type [type]
      
      This command applies a global filter on the save file based on the type of each
      object. Only objects matching the filter are selectable. By default the setting
      for [type] is VESSEL, meaning that only vessels (both pilotable and debris) are
      selectable.
      
      If run without arguments, displays the current type filter. If run with the
      argument '*' (without the quotes), disables filtering so that all objects in
      the save file can be selected at once - this is usually a VERY BAD IDEA, as
      editing commands that make sense on one object type are rarely useful on others.
      
      At present the only types of objects stored at the top level of save files are
      CREW and VESSEL.
      
      This does not affect your ability to edit subobjects; for example, if the setting
      is VESSEL, you will still be able to edit the ORBIT and PART blocks stored inside
      the selected VESSEL objects.
      
      Examples:
      
        type VESSEL         restrict selections to VESSEL objects (the default)
        type CREW           restrict selections to CREW objects
        type *              enable simultaneous selection of all objects
    """
    
    override def run(in: Scanner) {
      if (!in.hasNext) {
        println("Current type filter: " + select_type)
      } else {
        select_type = in.next
        SelectCommand.run("all")
      }
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
      <name> <operator> <value>.

      <name> is a property name (optionally prefixed with a subobject type and index; see
      'help set' for details). <value> is any text. <op> is one of the following:

        =     equality
        >     greater than
        <     less than
        ~     contains (eg, 'name ~ Station' would match all objects with Station in the name)
        /     regex match (eg, 'name / ^Station' would match all objects with names starting with Station)

      Additionally, any of these can be prefixed with ! to invert the sense of the operator; for
      example, 'name !~ Station' would match all objects with names that do NOT contain "Station".

      In addition to these operators, there are a few one-word filters included for convenience:

        all       all objects
        debris    all debris objects (objects not player-controllable)
        ghost     all ghost ships (ships with orbital information but no parts, created by some staging bugs)
        invert    all objects not currently selected (selected items are deselected and vice versa)
        ksc       all objects on the ground around KSC, but not actually on the launchpad
        landed    matches all objects which are landed or splashed down
        launchpad all objects on the KSC launchpad itself
        nan       all objects potentially containing NaN errors

      Like the operators, these can be prefixed with ! to invert them.

      Finally, filters can be chained together using '&&'. For example, the command:

        select !debris && name ~ Station

      Would select all non-debris objects with "Station" somewhere in the name, and the command:

        select PART,*:mass > 4 && sit = ORBITING

      Would select all orbiting objects which contain at least one part with a mass exceeding 4.
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
        case (obj, n) => printf("%4d  %s\n", n, obj.getProperty("name"))
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

      For example, the command 'choose 0-5 8 9 14-17' would select objects 0, 1, 2, 3,
      4, 5, 8, 9, 14, 15, 16, and 17.
    """
    
    override def run(in: Scanner) {
      def parseIndex(index: String) = {
        val groups = """(\d+)(?:-(\d+))?""".r.findFirstMatchIn(index).get.subgroups
        
        groups(1) match {
          case null => Set(groups(0).toInt)
          case x: String => Range(groups(0).toInt, groups(1).toInt + 1)
        }
      }
      def parseIndexes: Set[Int] = {
        in.hasNext match {
          case true => val index = in.next; parseIndexes ++ parseIndex(index)
          case false => Set.empty[Int]
        }
      }

      val chosen = parseIndexes map (selected(_))
      select(chosen contains _)
      ListCommand.run("")
    }
  }
  
  protected object AddCommand extends Command("add") {
    override def describe = "add objects to the selection"
    override def help = """
      Usage: add <condition>

      Adds all objects meeting the filter to the current selection. See 'help select'
      for details on filters.
    """
    
    override def run(in: Scanner) {
      val p = input2filter(in)
      
      select(obj => isSelected(obj) || p(obj))
      ListCommand.run("")
    }
  }
  
  protected object RemoveCommand extends Command("remove") {
    override def describe = "remove objects from the selection"
    override def help = """
      Usage: remove <filter>

      Removes all objects meeting the filter from the current selection. See 'help select'
      for details on filters.
    """
    
    override def run(in: Scanner) {
      val p = input2filter(in)
      
      select(obj => isSelected(obj) && !p(obj))
      ListCommand.run("")
    }
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

      <value> is any text; if omitted, the property will be made blank.

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
      val (key,value) = (in.next, if (in.hasNextLine) in.nextLine.trim else "")

      dirty = true
      selected foreach (_.setParsedProperty(key, value))
    }
  }
  
  protected object GetCommand extends Command("get") {
    override def describe = "display object or subobject properties"
    override def help = """
      Usage: get <property> [<property...]
      
      Displays one or more object properties for each selected object.
      
      Property naming rules are the same as for set (see 'help set').
    """
    
    override def run(in: Scanner) {
      import collection.JavaConversions._
      val keys = in.toSeq

      selected foreach { obj =>
        println(obj.getProperty("name"))
        keys foreach { key =>
          obj.getParsedProperty(key) foreach {
            value => printf("%20s  %s\n", key, value)
          }
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
      'orbit Kerbin 100' will give you an altitude of 100km and a semi-major axis of 700km.

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

    private def applyOrbit(body: ksp.Orbit.Body, SMA: Double, INC: Double) {
      SetCommand.run("sit ORBITING")
      SetCommand.run("landed False")
      SetCommand.run("splashed False")
      SetCommand.run("landedAt")
      SetCommand.run("ORBIT:REF " + body.id)
      SetCommand.run("ORBIT:ECC 0.0")
      SetCommand.run("ORBIT:SMA " + SMA)
      SetCommand.run("ORBIT:INC " + INC)
    }
    
    private def listBodies() {
      println("No such orbitable body '" + name + "'. Known bodies:")
      println("")
      ksp.Orbit.bodies foreach {
        b => printf("    %4d  %s\n", b.id, b.names.mkString(", "))
      }
      println("")
      println("Note: names are case sensitive!")
    }

    override def run(in: Scanner) {
      if (in.hasNextInt) {
        ksp.Orbit.getBody(in.nextInt)
      } else {
        ksp.Orbit.getBody(in.next)
      } match {
        case None => listBodies()
        case Some(body: ksp.Orbit.Body) =>
          val SMA = if (in.hasNext)
            in.next.toDouble * 1000.0 + body.radius
          else
            100000.0 + body.radius

          val INC = if (in.hasNext)
            in.next.toDouble
          else
            0.0
          
          applyOrbit(body, SMA, INC)
      }
    }
  }
  
  protected object PilotCommand extends Command("pilot") {
    override def describe = "make a discarded stage pilotable again"
    override def help = """
      Usage: pilot <stages>

      Makes all selected objects pilotable, as though they were primary rocket stages.
      This can also be used to restage already-pilotable objects (see below), but using
      it in this manner will completely destroy your existing stage order, so it is
      recommended not to use this on already-pilotable objects unless you REALLY,
      REALLY want to restage the whole thing from scratch.

      If <stages> is specified and is a number, it will attempt to create up to that
      many additional stages, for you to restage in flight. It cannot create more than
      one stage per part in the rocket. This feature is HIGHLY EXPERIMENTAL and makes
      no guarantee that the resulting staging is sensible; it is up to you to review
      it in-flight and reorganize the staging so that it makes sense. While doing this,
      bear in mind that detached stages usually have the decoupler that detached them
      as their root node; firing this decoupler again may have unfortunate consequences
      like the entire rocket erasing itself from existence.

      If you do not request additional stages, all parts will be placed into stage 0.
    """

    /* To make an object pilotable, there's a few things we need to do.
     * In order for it to show up at the tracking center, ORBIT:OBJ needs to be 0.
     * Furthermore, all parts need to be marked as connected and attached, or we won't be
     * able to control the rocket.
     * Finally, for staging to work properly, stg needs to be set to the number of the last
     * stage activated, and istg needs to be adjusted for each part - it looks like setting
     * istg = qstg for each part should do this.
     * 
     * Notes on staging
     * 
     * stg is the last stage activated, so if your rocket has stages 0-4 and you put it on
     * the pad, stg=5.
     * 
     * PART:sidx is the index within the stage of the part.
     * PART:sqor always seems to == PART:istg at launch
     * PART:dstg ("design stage") is how many parts away from root the part is, I think
     * PART:istg is what stage the part is actually in
     *
     * We can't just create empty stages for the user to restage into, because the game will
     * automatically collapse empty stages
     */
    override def run(in: Scanner) {
      val max_stage = if (in.hasNextInt) in.nextInt else 0

      selected foreach { obj =>
        val root = obj.getChild("PART", obj.getProperty("root").toInt)
        var stage = 0
        obj.getChildren("PART").foreach { part =>
          part.setProperty("attached", "True")
          part.setProperty("connected", "True")
          if (part != root) {
            part.setProperty("istg", stage.toString)
            if (stage < max_stage) {
              stage = stage + 1
            }
          }
        }
        obj.setProperty("stg", max_stage.toString)
        obj.getChild("ORBIT").setProperty("OBJ", "0")
        root.setProperty("istg", max_stage.toString)
      }
      dirty = true
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

}

object GameEditor extends GameEditor(null) {
  def main(args:Array[String]) {
    println("Welcome to the KSP Save File Editor")
    println("Type 'help' for a list of commands, or 'help <command>' for information on a specific command.")

    if (args.length > 0) try {
      runCommand("load " + args(0))
    } catch {
      case e: Exception => {
        println("Error loading " + args(0) + ": " + e.getMessage)
        println("Not loading any game - use 'load <filename>' before doing anything else!")
      }
    }

    run()
  }
}
