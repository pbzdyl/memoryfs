package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.channels.ClosedChannelException
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

class NodeSeekableByteChannelTest {
	@Test
	def testCloseBehavior() {
	  val ch = new NodeSeekableByteChannel(new FileData)
	  
	  assertTrue(ch.isOpen())
	  ch.close()
	  assertFalse(ch.isOpen())
	  
	  def testCheckOpen(op: (() => Unit)) {
	    try {
	      op()
	      fail("ClosedChannelException expected")
	    } catch {
          case e: ClosedChannelException => // expected
	      case _ => fail()
	    }
	  }
	  
	  testCheckOpen { () => ch.size() }
	  testCheckOpen { () => ch.position() }
	  testCheckOpen { () => ch.position(0) }
	  testCheckOpen { () => ch.read(ByteBuffer.allocate(0)) }
	  testCheckOpen { () => ch.write(ByteBuffer.allocate(0)) }
	  testCheckOpen { () => ch.truncate(0) }
	}
	
	@Test
	def testChannelSize() {
	  val data = new FileData
	  data ++= "test bytes".getBytes()
	  val ch = new NodeSeekableByteChannel(data)
	  
	  assertEquals(data.size, ch.size())
	}
	
	@Test
	def testTruncateWithNegativeSize() {
	  val ch = new NodeSeekableByteChannel(new FileData)
	  
	  try {
	    ch.truncate(-1)
	    fail()
	  } catch {
	    case _: IllegalArgumentException => // expected
	    case _ => fail()
	  }
	}
	
	@Test
	def testTruncateWithSizeGreaterThanOrEqualToDataSize() {
	  val data = new FileData
	  val originalData = "test bytes".getBytes()
	  data ++= originalData
	  
	  val ch = new NodeSeekableByteChannel(data)
	  
	  ch.truncate(originalData.size)
	  assertEquals(originalData.size, ch.size())
	  
	  ch.truncate(originalData.size + 1)
	  assertEquals(originalData.size, ch.size())
	}
	
	@Test
	def testTruncateWithSizeLessThanDataSize() {
	  val testString = "test bytes"
	  val data = new FileData ++= testString.getBytes()
	  val expectedData = new FileData() ++= testString.substring(0, testString.length() - 1).getBytes()
	  val newSize = expectedData.size
	  
	  val ch = new NodeSeekableByteChannel(data)
	  
	  ch.truncate(newSize)
	  assertEquals(newSize, ch.size())
	  assertEquals(expectedData, data)
	}
	
	@Test
	def testInitialPositionSetToZero() {
	  val ch = new NodeSeekableByteChannel(new FileData())
	  assertEquals(0, ch.position())
	}
	
	@Test
	def testExceptionWhenNegativePositionProvided() {
	  val ch = new NodeSeekableByteChannel(new FileData())
	  try {
	    ch.position(-1)
	    fail("IllegalArgumentException expected")
	  } catch {
	    case _: IllegalArgumentException => // expected
	    case _ => fail()
	  }
	}
	
	@Test
	def testProvidedPositionIsApplied() {
	  val ch = new NodeSeekableByteChannel(new FileData())
	  
	  ch.position(100)
	  assertEquals(100, ch.position())
	  
	  ch.position(10)
	  assertEquals(10, ch.position())
	  
	  ch.position(0)
	  assertEquals(0, ch.position())
	}
	
	@Test
	def testReadFromEmptyFile() {
	  val ch = new NodeSeekableByteChannel(new FileData())
	  
	  val dst = ByteBuffer.allocate(1)
	  
	  assertEquals(-1, ch.read(dst))
	}
	
	@Test
	def testReadFromNonEmptyFileWithPositionBiggerThanFileSize() {
	  val testBytes = "test bytes".getBytes()
	  val ch = new NodeSeekableByteChannel(new FileData() ++= testBytes)
	  val dst = ByteBuffer.allocate(1)
	  
	  assertEquals(testBytes.size, ch.size())
	  
	  ch.position(ch.size() + 1)
	  
	  assertEquals(-1, ch.read(dst))
	}
	
	@Test
	def testBytesRead() {
	  val testBytes = "test bytes".getBytes()
	  val ch = new NodeSeekableByteChannel(new FileData() ++= testBytes)
	  val dst = ByteBuffer.allocate(ch.size().toInt)
	  
	  assertEquals(testBytes.size, ch.read(dst))
	  
	  for (i <- testBytes.indices) {
	    assertEquals(testBytes(i), dst.get(i))
	  }
	  
	  val dst2 = ByteBuffer.allocate(1)
	  
	  assertEquals(-1, ch.read(dst2))
	}
}
