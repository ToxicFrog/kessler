package ksp

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
object SFSParser extends scala.util.parsing.combinator.RegexParsers {
  private def key       = """[^=\s]+""".r
  private def value     = """= *[^\n]*""".r
  private def blockname = """\p{Upper}+""".r

  private def S(kind: String) = phrase(sfs(new ksp.Object(kind)))

  private def sfs(o: ksp.Object): Parser[ksp.Object] = (entry(o) *) ~> success(o)

  private def entry(o: ksp.Object) = comment(o) | keyvalue(o) | block(o)

  private def comment(o: ksp.Object) = "//[^\n]*".r ~> success(o)

  private def keyvalue(o: ksp.Object) = key ~ value ^^ {
    case ~(k, v) => o.addProperty(k, v.dropWhile(c => c == '=' || c == ' ').trim)
  }

  private def block(o: ksp.Object) = (blockname <~ "{") >> {
    name => sfs(new ksp.Object(name)) <~ "}" ^^ {
      obj => o.addChild(name, obj)
    }
  }

  def parseString(kind: String, reader: String) = parse(S(kind), reader) match {
    case Success(content, _) => content
    case fail: NoSuccess => throw new RuntimeException(fail.toString)
  }
}
