package ksp

import collection.mutable.{LinkedHashMap,Buffer}

class Object {
  val properties = new LinkedHashMap[String, Buffer[String]]()
  val children = new LinkedHashMap[String, Buffer[Object]]()

  /*
   * get(key) => first value for that key
   * getAll(key) => list of values for that key
   * set(key, value) => sets first value for that key
   * set(key, oldval, newval) => sets first matching value
   * set(key, index, newval) => sets nth value
   * delete(key, val)
   * delete(key, index)
   * delete(key)
   * addProperty(key, val)
   * addChild(key, val)
   */
  def getProperty(key: String) = getProperties(key).head
  def getProperties(key: String) = properties(key)
  
  
  def addProperty(key: String,  value: String) {
    if (properties contains key) {
      properties(key) += value
    } else {
      properties += ((key, Buffer(value)))
    }
  }

  def getChild(key: String): Object = getChildren(key).head
  def getChildren(key: String): Seq[Object] = children(key)

  def addChild(key: String, child: Object) {
    if (children contains key) {
      children(key) += child
    } else {
      children += ((key, Buffer(child)))
    }
  }
  
  def deleteChild(key: String, child: Object) {
    val cs = children(key)
    cs.remove(cs.indexOf(child))
  }

  def mkString: String = mkString("")
  def mkString(indent: String): String = {
    val sb = new StringBuilder()
    properties foreach {
      case (k, vs) => vs.addString(sb, indent + k + " = ", "\n" + indent + k + " = ", "\n")
    }
    children foreach {
      case (k, vs) => vs foreach {
        sb ++= indent ++= k ++= " {\n" ++= _.mkString(indent + "\t") ++= indent ++= "}\n"
      }
    }
    sb.toString()
  }
}

/*
 * KSP savegame class.
 *
 * A savegame consists of:
 * - a multimap of properties (we'll probably just store this as a list)
 * - a list of crew
 * - a list of vessels
 *
 * In addition, for forwards compatibility, we need to store a list of all blocks of unknown type so that if,
 * say, STATION blocks get added in .15, the library passes them through safely.
 *
 * For writing out the save, we dump properties, then crew, then vessels, then unknown blocks.
 */
object Game {
  def fromFile(file: String) = sfs.Parser.parseString(io.Source.fromFile(file).mkString)
  def fromString(string: String) = sfs.Parser.parseString(string)
}

class Game extends Object {
}
