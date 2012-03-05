package ksp

import collection.mutable.{LinkedHashMap, Buffer}
import util.matching.Regex

class Object(val kind: String) {
  val properties = new LinkedHashMap[String, Buffer[String]]()
  val children = new LinkedHashMap[String, Buffer[Object]]()

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



  // contains is transitive; we contain this object if we or any of our subobjects
  // contain it.
  def contains(child: Object): Boolean = children exists {
    case (key, objs) => objs.contains(child) || objs.exists(_ contains child)
  }
  
  def contains(child: WrappedObject): Boolean = children exists {
    case (key, objs) => objs.exists(child == _) || objs.exists(_ contains child)
  }

  /*
   * Property tests
   */
  def hasProperty(key: String) = properties contains key

  def testProperty(key: String, test: String): Boolean = testProperty(key, test.r)

  def testProperty(key: String, test: Regex) = test.findFirstMatchIn(getProperty(key)).isDefined

  /*
   * Property getters
   */
  def getProperty(key: String, n: Int = 0) = getProperties(key)(n)

  def getProperties(key: String) = properties(key)

  def getParsedProperty(prop: String): Seq[String] = {
    val (objs, key, index) = parseProperty(prop)

    objs filter (_.hasProperty(key)) map (_.getProperty(key, index))
  }

  /*
   * Property setters
   */
  def setProperty(key: String, value: String) {
    setProperty(key, 0, value)
  }

  def setProperty(key: String, n: Int, value: String) {
    properties(key).update(n, value)
  }

  def setParsedProperty(prop: String, value: String) {
    val (objs, key, index) = parseProperty(prop)

    objs foreach {
      _.setProperty(key, index, value)
    }
  }

  def addProperty(key: String, value: String) {
    if (properties contains key) {
      properties(key) += value
    } else {
      properties += ((key, Buffer(value)))
    }
  }

  /*
   * Child getters
   */
  def getChild(key: String, n: Int = 0) = getChildren(key)(n)

  def getChildren(key: String): Seq[Object] = children.getOrElse(key, Seq.empty[Object])

  def getChildren: Seq[Object] = children.toSeq.map(_._2).flatten

  /*
   * Child setters
   */
  def addChild(key: String, child: Object) {
    if (children contains key) {
      children(key) += child
    } else {
      children += ((key, Buffer(child)))
    }
  }

  def deleteChild(child: Object) {
    deleteChild(child.kind, child)
  }
  
  private def deleteChild(key: String, child: Object) {
    val cs = children(key)
    cs.remove(cs.indexOf(child))
  }



  private def parseProperty(prop: String): (Seq[Object], String, Int) = {
    def hasPrefix(prop: String) = """^[^:,]+(,(\d+|\*))?:""".r.findFirstMatchIn(prop).isDefined
    def splitPrefix(prop: String) = {
      val groups = """^([^:,]+)(?:,(\d+|\*))?:(.*)""".r.findFirstMatchIn(prop).get.subgroups

      (groups(0), groups(1), groups(2))
    }
    def parsePrefix(objs: Seq[Object], key: String): (Seq[Object], String) = {
      val (kind, index, tail): (String, String, String) = splitPrefix(key)

      (index match {
        case "*" => objs.flatMap(_.getChildren(kind))
        case null => objs.map(_.getChild(kind))
        case n: String => objs.map(_.getChild(kind, n.toInt))
      }, tail)
    }
    def hasSuffix(key: String) = """.+,\d+$""".r.findFirstMatchIn(key).isDefined
    def parseSuffix(key: String) = {
      val groups = """(.+),(\d+)$""".r.findFirstMatchIn(key).get.subgroups

      (groups(0), groups(1).toInt)
    }

    var (objs, key) = (Seq(this), prop)
    var index = 0

    while (hasPrefix(key))
      parsePrefix(objs, key) match {
        case (o, k) => objs = o; key = k;
      }

    if (hasSuffix(key))
      parseSuffix(key) match {
        case (k, i) => key = k; index = i;
      }

    (objs, key, index)
  }


}
