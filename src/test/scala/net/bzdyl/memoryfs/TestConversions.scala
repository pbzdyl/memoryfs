package net.bzdyl.memoryfs
import java.nio.ByteBuffer
import java.nio.Buffer
import scala.collection.mutable.ArrayBuffer

object TestConversions {
	implicit def byteBuffer2ArrayBuffer(src: ByteBuffer): ArrayBuffer[Byte] = {
	  toArrayBuffer(src)
	}
	
	implicit def stringToByteArrayBuffer(src: String): ArrayBuffer[Byte] = {
	  new ArrayBuffer[Byte] ++= src.getBytes()
	}
	
	def testBytes(src: String) = stringToByteArrayBuffer(src)
	
	def toArrayBuffer(src: ByteBuffer): ArrayBuffer[Byte] = {
	  val res = new ArrayBuffer[Byte](src.position())
	  for (i <- 0 until src.position) res += src.get(i)
	  res
	}
	
	implicit def seqToByteBuffer(src: Seq[Byte]): ByteBuffer = {
	  val dst = ByteBuffer.allocate(src.size)
	  src.foreach(b => dst.put(b))
	  dst
	}
	
	def newTestFS() = new MemoryFileSystemProvider().getFileSystem(null)
}