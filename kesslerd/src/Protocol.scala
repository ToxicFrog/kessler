package kessler

object Protocol {
  class Exception(message: String) extends java.lang.Exception(message);

  abstract case class Message() {
    import java.util.zip.{Inflater,Deflater}
    import java.io.ByteArrayOutputStream

    var _payload: Array[Byte] = Array[Byte]()

    def payload: String = {
      val inflater = new Inflater()
      inflater.setInput(_payload)

      val os = new ByteArrayOutputStream(_payload.length)
      val buf = new Array[Byte](10240)

      while (!inflater.finished) {
        os.write(buf, 0, inflater.inflate(buf))
      }

      os.close()

      new String(os.toByteArray, "UTF-8")
    }

    def payload_=(raw: String) {
      val bytes = raw.getBytes("UTF-8")

      val deflater = new Deflater(Deflater.BEST_COMPRESSION)
      deflater.setInput(bytes)
      deflater.finish()

      val os = new ByteArrayOutputStream(bytes.length)
      val buf = new Array[Byte](10240)

      while (!deflater.finished) {
        os.write(buf, 0, deflater.deflate(buf))
      }

      os.close()
      _payload = os.toByteArray
    }
  }

  abstract case class Command() extends Message;
  case class ConnectCommand(pass: String, version: Int) extends Command
  case class PutCommand(pass: String) extends Command;
  case class GetCommand(pass: String) extends Command;

  abstract case class Reply() extends Message;
  case class Success(msg: String) extends Reply;
  case class Error(msg: String) extends Reply;
}
