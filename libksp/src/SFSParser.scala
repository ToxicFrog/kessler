package ksp.sfs

import collection.mutable.LinkedHashMap

/*
 * Parser for KSP savegames (.sfs files).
 *
 * A savegame is basically a recursive multimap; it consists of a sequence of entries, each one of which is
 * either:
 *  - a comment, C99 style (// to newline)
 *  - a value, in the form key ' = ' value; value extends until the end of the line, and key does NOT have
 *    to be unique!
 *  - a block, of the form name '{' sfs-data '}'
 * Keys and blocks don't have to be unique; a typical savegame will have many CREW and VESSEL blocks.
 * A VESSEL block, in turn, contains an ORBIT block holding orbital characteristics, and one or more PART
 * blocks containing information about the vessel's structure.
 */
object Parser extends scala.util.parsing.combinator.RegexParsers {
  private def key       = """[^\s]+""".r
  private def value     = """= [^\n]*""".r
  private def blockname = """\p{Upper}+""".r

  private def S: Parser[ksp.Game] = phrase(sfs(new ksp.Game()))

  private def sfs[T <: ksp.Object](o: T): Parser[T] = (entry(o) *) ~> success(o)

  private def entry(o: ksp.Object) = comment(o) | keyvalue(o) | block(o)

  private def comment(o: ksp.Object) = "//[^\n]*".r ~> success(o)

  private def keyvalue(o: ksp.Object) = key ~ value ^^ {
    case ~(k,v) => o.addProperty(k, v drop 2)
  }

  private def block(o: ksp.Object) = (blockname <~ "{") >> {
    name => sfs(new ksp.Object()) <~ "}" ^^ { obj => o.addChild(name, obj) }
  }

  def parseString(reader: String) = parse(S, reader) match {
    case Success(content, _) => content
    case fail: NoSuccess => throw new RuntimeException(fail.toString())
  }
}
