package ksp

import collection.mutable.{LinkedHashMap,Buffer}
import util.matching.Regex

class Object {
  val properties = new LinkedHashMap[String, Buffer[String]]()
  val children = new LinkedHashMap[String, Buffer[Object]]()

  def asObject = this
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
  def hasProperty(key: String) = properties contains key

  def getProperty(key: String, n: Int = 0) = getProperties(key)(n)
  def getProperties(key: String) = properties(key)

  def testProperty(key: String, test: String): Boolean = testProperty(key, test.r)
  def testProperty(key: String, test: Regex) = test.findFirstMatchIn(getProperty(key)).isDefined

  def addProperty(key: String,  value: String) {
    if (properties contains key) {
      properties(key) += value
    } else {
      properties += ((key, Buffer(value)))
    }
  }

  def getChild(key: String, n: Int = 0) = getChildren(key)(n)
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
