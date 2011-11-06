package net.bzdyl.memoryfs

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardOpenOption._
import java.nio.ByteBuffer
import org.junit.Ignore

class FilesSuite extends JUnitSuite {
	@Test
	@Ignore
	def test() {
	  val p = Paths.get("c:/temp/test1/test2/test3/alibaba.txt")
	  
	  val ch = Files.newByteChannel(p, CREATE_NEW, WRITE)
	  val buf = ByteBuffer.wrap(new Array(10))
	  ch.position(2)
	}
}