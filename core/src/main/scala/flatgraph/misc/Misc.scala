package flatgraph.misc

object Misc {
  val reChars = Array('[', ']', '(', ')', '{', '}', '*', '+', '&', '|', '?', '.', ',', '\\', '$')

  def isRegex(pattern: String): Boolean = reChars.exists(pattern.indexOf(_) >= 0)

  extension (i: Int) {
    def toShortSafely: Short = {
      assert(0 <= i && i <= Short.MaxValue, throw new ConversionException(s"cannot safely downcast int with value=$i to short"))
      i.toShort
    }
  }
}

class ConversionException(msg: String) extends RuntimeException(msg)