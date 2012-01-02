package net.bzdyl.memoryfs

import org.junit.Test
import TestConversions._
import org.scalatest.junit.JUnitSuite

class TreePrinterSuite extends JUnitSuite {
  @Test
  def test() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = r.createDirectory("b")
    
    a.createFile("a1")
    a.createFile("a2")
    
    b.createFile("b1")
    
    println(TreePrinter.printTree(r))
  }
}
