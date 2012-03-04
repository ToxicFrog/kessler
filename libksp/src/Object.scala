package ksp

import collection.mutable.{LinkedHashMap, Buffer}
import util.matching.Regex

class Object(val kind: String) {
  val properties = new LinkedHashMap[String, Buffer[String]]()
  val children = new LinkedHashMap[String, Buffer[Object]]()

  def asObject = this

  def copy: Object = new Object(kind).copyFrom(this)
  
  def copyFrom(other: Object) = {
    other.properties.foreach {
      case (key, values) => values foreach (v => addProperty(key, v))
    }
    other.children.foreach {
      case (key, values) => values foreach (v => addChild(key, v.copy))
    }
    this
  }

  // contains is transitive; we contain this object if we or any of our subobjects
  // contain it.
  def contains(child: Object): Boolean = children exists {
    case (key, objs) => objs.contains(child) || objs.exists(_ contains child)
  }
  
  def contains(child: WrappedObject): Boolean = children exists {
    case (key, objs) => objs.exists(child == _) || objs.exists(_ contains child)
  }

  def hasProperty(key: String) = properties contains key

  def getProperty(key: String, n: Int = 0) = getProperties(key)(n)

  def getProperties(key: String) = properties(key)

  def setProperty(key: String, value: String) {
    setProperty(key, 0, value)
  }

  def setProperty(key: String, n: Int, value: String) {
    properties(key).update(n, value)
  }

  def testProperty(key: String, test: String): Boolean = testProperty(key, test.r)

  def testProperty(key: String, test: Regex) = test.findFirstMatchIn(getProperty(key)).isDefined

  def addProperty(key: String, value: String) {
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
