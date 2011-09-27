package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.channels.ClosedChannelException
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer
import org.scalatest.junit.JUnitSuite
import org.scalatest.mock.MockitoSugar
import TestConversions._

class NodeSeekableByteSuite extends JUnitSuite with MockitoSugar {
  @Test
  def testCloseBehavior() {
    val ch = new FileDataSeekableByteChannel(new FileData)

    assert(ch.isOpen())
    ch.close()
    assert(!ch.isOpen())

    intercept[ClosedChannelException] {
      ch.size()
    }

    intercept[ClosedChannelException] {
      ch.position()
    }

    intercept[ClosedChannelException] {
      ch.position(0)
    }

    intercept[ClosedChannelException] {
      ch.read(ByteBuffer.allocate(0))
    }

    intercept[ClosedChannelException] {
      ch.write(ByteBuffer.allocate(0))
    }

    intercept[ClosedChannelException] {
      ch.truncate(0)
    }
  }

  @Test
  def testInitialPositionSetToZero() {
    val ch = new FileDataSeekableByteChannel(new FileData())
    assert(ch.position === 0)
  }

  @Test
  def testExceptionWhenNegativePositionProvided() {
    val ch = new FileDataSeekableByteChannel(new FileData())
    intercept[IllegalArgumentException] {
      ch.position(-1)
    }
  }

  @Test
  def testProvidedPositionIsApplied() {
    val ch = new FileDataSeekableByteChannel(new FileData())

    ch.position(100)
    assert(ch.position() === 100)

    ch.position(10)
    assert(ch.position() === 10)

    ch.position(0)
    assert(ch.position() === 0)
  }
  
  @Test
  def testPositionIncrementedCorrectlyWhenSomeDataRead() {
    val testData = testBytes("abcxzy")
    val fileData = new FileData ++= testData
    val ch = new FileDataSeekableByteChannel(fileData)
    val dst = ByteBuffer.allocate(testData.size)
    
    assert(ch.read(dst) === testData.size)
    
    assert(ch.position() === testData.size)
    assert(toArrayBuffer(dst) === testData)
  }
  
  @Test
  def testPositionNotIncrementedWhenNoDataRead() {
    val testData = testBytes("abcxyz")
    val fileData = new FileData ++= testData
    val ch = new FileDataSeekableByteChannel(fileData)
    val dst = ByteBuffer.allocate(1)
    // fill the buffer
    dst.put(0.toByte)
    
    assert(ch.position() === 0)
    assert(ch.read(dst) === 0)
    assert(ch.position() === 0)
  }

  @Test
  def testPositionNotIncrementedWhenEOFReceived() {
    val fileData = new FileData
    val ch = new FileDataSeekableByteChannel(fileData)
    val dst = ByteBuffer.allocate(1)
    
    assert(ch.position() === 0)
    assert(ch.read(dst) === -1)
    assert(ch.position() === 0)
  }
  
  @Test
  def testReadWhenByteBufferSmallerThanAvailableData() {
    val bufferSize = 3
    val testData = testBytes("abcxyz")
    val expectedData = testData.take(bufferSize)
    val dst = ByteBuffer.allocate(bufferSize)
    val ch = new FileDataSeekableByteChannel(new FileData ++= testData)
    
    assert(ch.read(dst) === bufferSize)
    assert(toArrayBuffer(dst) === expectedData)
    assert(ch.position() === bufferSize)
  }
  
  @Test
  def testCorrectPositionUsedForFileDataRead() {
    val position = 3
    val testData = testBytes("abcxyz")
    val expectedData = testData.drop(position)
    val dst = ByteBuffer.allocate(expectedData.size)
    val ch = new FileDataSeekableByteChannel(new FileData ++= testData)
    
    ch.position(position)
    assert(ch.read(dst) === expectedData.size)
    assert(toArrayBuffer(dst) === expectedData)
    assert(ch.position() === testData.size)
  }
  
  @Test
  def testZeroPositionIncrementedCorrectlyWhenSomeDataWritten() {
    val testData = testBytes("abcxzy")
    val fileData = new FileData
    val src: ByteBuffer = testData
    src.flip()
    val ch = new FileDataSeekableByteChannel(fileData)
    
    assert(ch.write(src) === testData.size)
    
    assert(ch.position() === testData.size)
    assert(fileData === testData)
  }
  
  @Test
  def testNonZeroPositionIncrementedCorrectlyWhenSomeDataWritten() {
    val position = 4
    val initialData = testBytes("abcdefg")
    val testData = testBytes("12345")
    val fileData = new FileData ++= initialData
    val src: ByteBuffer = testData
    src.flip()
    val ch = new FileDataSeekableByteChannel(fileData)
    
    ch.position(4)
    assert(ch.position() === position)
    assert(ch.write(src) === testData.size)
    assert(ch.position() === position + testData.size)
  }
  
  @Test
  def testPositionNotIncrementedWhenNoDataWritten() {
    val testData = testBytes("abcxyz")
    val fileData = new FileData
    val ch = new FileDataSeekableByteChannel(fileData)
    val src = ByteBuffer.allocate(1)
    src.flip()
    
    assert(ch.position() === 0)
    assert(ch.write(src) === 0)
    assert(ch.position() === 0)
  }

  @Test
  def testCorrectPositionUsedForFileDataWrite() {
    val position = 3
    val testData = testBytes("abcdef")
    val expectedData = ArrayBuffer.fill[Byte](position)(0) ++= testData
    val src: ByteBuffer = testData
    src.flip()
    val fd = new FileData
    val ch = new FileDataSeekableByteChannel(fd)
    
    ch.position(position)
    assert(ch.write(src) === testData.size)
    assert(fd === expectedData)
    assert(ch.position() === position + testData.size)
  }
}
