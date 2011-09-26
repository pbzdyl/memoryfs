package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.ByteBuffer
import org.scalatest.junit.JUnitSuite

class FileDataTest extends JUnitSuite {
	@Test
	def testSize() {
	  val testData = "test bytes".getBytes()
	  val fd = new FileData
	  fd ++= testData
	  
	  assert(fd.size === testData.size)
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
	  val originalData = "test bytes".getBytes()
	  val fd = new FileData
	  fd ++= originalData
	  
	  
	  fd.truncate(originalData.size)
	  assert(fd.size === originalData.size)
	  
	  fd.truncate(originalData.size + 1)
	  assert(fd.size === originalData.size)
	}
	
	@Test
	def testTruncateWithSizeLessThanDataSize() {
	  val testString = "test bytes"
	  val fd = new FileData ++= testString.getBytes()
	  val expectedData = new FileData() ++= testString.substring(0, testString.length() - 1).getBytes()
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
	}
	
	@Test
	def testReadFromNonEmptyFileWithPositionBiggerThanFileSize() {
	  val testBytes = "test bytes".getBytes()
	  val fd = new FileData() ++= testBytes
	  val dst = ByteBuffer.allocate(1)
	  
	  assert(fd.size === testBytes.size)
	  
	  assert(fd.read(fd.size + 1, dst) === -1)
	}
	
	@Test
	def testBytesRead() {
	  val testBytes = "test bytes".getBytes()
	  val fd = new FileData() ++= testBytes
	  val dst = ByteBuffer.allocate(fd.size)
	  
	  assert(fd.read(0, dst) === testBytes.size)
	  
	  for (i <- testBytes.indices) {
	    assert(dst.get(i) === testBytes(i))
	  }
	  
	  val dst2 = ByteBuffer.allocate(1)
	  
	  assert(fd.read(fd.size, dst2) === -1)
	}
}
