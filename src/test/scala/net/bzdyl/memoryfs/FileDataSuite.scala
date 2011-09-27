package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.ByteBuffer
import org.scalatest.junit.JUnitSuite
import scala.collection.mutable.ArrayBuffer
import TestConversions._

class FileDataSuite extends JUnitSuite {
	@Test
	def testContentsAndSize() {
	  val testData = testBytes("abcxyz")
	  val fd = new FileData ++= testData
	  
	  assert(fd.size === testData.size)
	  assert(fd === testData)
	}
	
	@Test
	def testTruncateWithNegativeSize() {
	  val fd = new FileData
	  
	  intercept[IllegalArgumentException] {
	    fd.truncate(-1)
	  }
	}
	
	@Test
	def testTruncateWithSizeGreaterThanOrEqualToDataSize() {
	  val originalData = testBytes("abcxyz")
	  val fd = new FileData ++= originalData
	  
	  fd.truncate(originalData.size)
	  assert(fd.size === originalData.size)
	  
	  fd.truncate(originalData.size + 1)
	  assert(fd.size === originalData.size)
	}
	
	@Test
	def testTruncateWithSizeLessThanDataSize() {
	  val testData = testBytes("abcxyz")
	  val fd = new FileData ++= testData
	  val expectedData = testData.dropRight(1)
	  val newSize = expectedData.size
	  
	  fd.truncate(newSize)
	  assert(fd.size === newSize)
	  assert(fd === expectedData)
	}
	
	@Test
	def testReadFromEmptyFile() {
	  val fd = new FileData()
	  
	  val dst = ByteBuffer.allocate(1)
	  
	  assert(fd.read(0, dst) === -1)
	  assert(dst.position() === 0)
	}
	
	@Test
	def testReadFromNonEmptyFileWithPositionBiggerThanFileSize() {
	  val testData = testBytes("abcxyz")
	  val fd = new FileData() ++= testData
	  val dst = ByteBuffer.allocate(1)
	  
	  assert(fd.size === testData.size)
	  assert(fd.read(fd.size + 1, dst) === -1)
	  assert(dst.position() === 0)
	}
	
	@Test
	def testBytesRead() {
	  val testData = testBytes("abcxzy")
	  val fd = new FileData() ++= testData
	  val dst = ByteBuffer.allocate(fd.size)
	  
	  assert(fd.read(0, dst) === testData.size)
	  assert(toArrayBuffer(dst) === testData)
	  
	  val dst2 = ByteBuffer.allocate(1)
	  
	  assert(fd.read(fd.size, dst2) === -1)
	  assert(dst2.position() === 0)
	}
	
	@Test
	def testBytesReadFromCorrectPosition() {
	  val position = 3
	  val testData = testBytes("abcxyz")
	  val expectedData = testData.drop(position)
	  val dst = ByteBuffer.allocate(expectedData.size)
	  val fd = new FileData ++= testData
	  
	  assert(fd.read(position, dst) === expectedData.size)
	  assert(toArrayBuffer(dst) === expectedData)
	}
	
	@Test
	def testWriteSomeDataToEmptyFile() {
	  val testData = testBytes("abcxyz")
	  val src: ByteBuffer = testData
	  src.flip()
	  val fd = new FileData
	  
	  assert(fd.write(0, src) === testData.size)
	  assert(fd.size === testData.size)
	  assert(fd === testData)
	}
	
	@Test
	def testWriteSomeDataWithPositionBiggerThanCurrentFileSize() {
	  val position = 10
	  val testData = testBytes("abcxyz")
	  val src: ByteBuffer = testData
	  src.flip()
	  val expectedData = ArrayBuffer.fill[Byte](position)(0) ++= testData
	  val fd = new FileData
	  
	  assert(fd.write(position, src) === testData.size)
	  assert(fd.size === expectedData.size)
	  assert(fd === expectedData)
	}
	
	@Test
	def testWriteEmptyByteBufferWithPositionBiggerThanCurrentFileSize() {
	  val position = 10
	  val src = ByteBuffer.allocate(1)
	  src.flip()
	  val expectedData = ArrayBuffer.fill[Byte](position)(0)
	  val fd = new FileData
	  
	  assert(fd.write(position, src) === 0)
	  assert(fd.size === expectedData.size)
	  assert(fd === expectedData)
	}
	
	@Test
	def testWriteSomeDataWithPositionInTheMiddleOfTheFile() {
	  val position = 4
	  val initialString = "abcdefghijk"
	  val writtenString = "xyz"
	  val resultString = initialString.substring(0, position) + writtenString + initialString.substring(position + writtenString.length())
	  val initialData = testBytes(initialString)
	  val testData = testBytes(writtenString)
	  val src: ByteBuffer = testData
	  src.flip()
	  val expectedData = testBytes(resultString)
	  val fd = new FileData ++= initialData
	  
	  assert(fd.write(position, src) === testData.size)
	  assert(fd.size === expectedData.size)
	  assert(fd === expectedData)
	}
	
	@Test
	def testWriteSomeDataWithPositionInTheMiddleOfTheFileGrowingTheFile() {
	  val position = 4
	  val initialString = "abcdefg"
	  val writtenString = "pqrstuvwxyz"
	  val resultString = initialString.substring(0, position) + writtenString
	  val initialData = testBytes(initialString)
	  val testData = testBytes(writtenString)
	  val src: ByteBuffer = testData
	  src.flip()
	  val expectedData = testBytes(resultString)
	  val fd = new FileData ++= initialData
	  
	  assert(fd.write(position, src) === testData.size)
	  assert(fd.size === expectedData.size)
	  assert(fd === expectedData)
	}
}
