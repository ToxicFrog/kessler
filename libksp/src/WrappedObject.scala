package ksp

class WrappedObject(self: Object) {
  def asObject = self

  override def hashCode = self.hashCode

  override def equals(x: Any) = x match {
    case x: WrappedObject => this.equals(x)
    case x => super.equals(x)
  }

  def equals(x: WrappedObject) = {
    x.asObject == self
  }

  def parseProperty(prop: String): (Seq[WrappedObject], String, Int) = {
    def hasPrefix(prop: String) = """^[^:,]+(,(\d+|\*))?:""".r.findFirstMatchIn(prop).isDefined
    def splitPrefix(prop: String) = {
      val groups = """^([^:,]+)(?:,(\d+|\*))?:(.*)""".r.findFirstMatchIn(prop).get.subgroups

      (groups(0), groups(1), groups(2))
    }
    def parsePrefix(objs: Seq[WrappedObject], key: String): (Seq[WrappedObject], String) = {
      val (kind, index, tail): (String, String, String) = splitPrefix(key)

      (index match {
        case "*" => objs.flatMap(_.asObject.getChildren(kind)).map(new WrappedObject(_))
        case null => objs.map(_.asObject.getChild(kind)).map(new WrappedObject(_))
        case n: String => objs.map(_.asObject.getChild(kind, n.toInt)).map(new WrappedObject(_))
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

  def setParsedProperty(prop: String, value: String) {
    val (objs, key, index) = parseProperty(prop)

    objs foreach {
      _.asObject.setProperty(key, index, value)
    }
  }

  def getParsedProperty(prop: String): Seq[String] = {
    val (objs, key, index) = parseProperty(prop)

    objs filter (_.asObject.hasProperty(key)) map (_.asObject.getProperty(key, index))
  }
}
