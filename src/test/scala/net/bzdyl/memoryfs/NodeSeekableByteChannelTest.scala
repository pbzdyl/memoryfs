package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.channels.ClosedChannelException
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer
import org.scalatest.junit.JUnitSuite

class NodeSeekableByteSuite extends JUnitSuite {
  @Test
  def testCloseBehavior() {
    val ch = new NodeSeekableByteChannel(new FileData)

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
    val ch = new NodeSeekableByteChannel(new FileData())
    assert(ch.position === 0)
  }

  @Test
  def testExceptionWhenNegativePositionProvided() {
    val ch = new NodeSeekableByteChannel(new FileData())
    intercept[IllegalArgumentException] {
      ch.position(-1)
    }
  }

  @Test
  def testProvidedPositionIsApplied() {
    val ch = new NodeSeekableByteChannel(new FileData())

    ch.position(100)
    assert(ch.position() === 100)

    ch.position(10)
    assert(ch.position() === 10)

    ch.position(0)
    assert(ch.position() === 0)
  }

}
