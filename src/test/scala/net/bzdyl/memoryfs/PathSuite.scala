package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.ByteBuffer
import org.scalatest.junit.JUnitSuite
import scala.collection.mutable.ArrayBuffer
import TestConversions._
import java.nio.file.Paths
import java.util.NoSuchElementException

class PathSuite extends JUnitSuite {
  val fs = new MemoryFileSystemProvider().getFileSystem(null)
  val fs2 = new MemoryFileSystemProvider().getFileSystem(null)
  
  private def path(p: String) = new MFSPath(fs, p)
  private def path2(p: String) = new MFSPath(fs2, p)
  private def npath(p: String) = Paths.get(p)

  @Test
  def testNormalize() {
    val p1 = path("/a/b/../c/d/./e/./../f/g/./h/i/j/k/l/../.././m")
    
    assert(p1.normalize().toString === "/a/c/d/f/g/h/i/j/m")
    
    val p2 = path("../../a/b/c")
    
    assert(p2.normalize().toString === "../../a/b/c")
    
    val p3 = path("/../../a/b/c")
    
    assert(p3.normalize().toString === "/a/b/c")
  }
  
  @Test
  def testToAbsolutePath() {
    fs.currentDir = path("/a/b/c")
    val abs = path("/x/y/z")
    
    assert(abs.toAbsolutePath() eq abs)
    
    fs.currentDir = fs.rootPath
    
    val rel1 = path("a/b/c")
    
    assert(rel1.toAbsolutePath().toString === "/a/b/c")
    
    fs.currentDir = path("/a/b/c")
    val rel2 = path("d/e/f")
    
    assert(rel2.toAbsolutePath().toString === "/a/b/c/d/e/f")
  }
  
  @Test
  def testRelativize() {
    val a1 = path("/a/b/c/d")
    val a2 = path("/a/b/c/d")
    
    assert(a1.relativize(a2) === path(""))
    
    val b1 = path("/a/b/c/d")
    val b2 = path("/a/b")
    
    assert(b1.relativize(b2) === path("../.."))
    
    val c1 = path("/a/b")
    val c2 = path("/a/b/c/d")
    
    assert(c1.relativize(c2) === path("c/d"))
    
    val d1 = path("/a/b/c/d")
    val d2 = path("/x/y/z")
    
    assert(d1.relativize(d2) === path("../../../../x/y/z"))
    
    val e1 = path("/a/b")
    val e2 = path("/a/b/./../c/../d")
    
    assert(e1.relativize(e2) === path("./../c/../d"))
    
    val f1 = path("/a/b")
    val f2 = path("c/d")
    
    intercept[IllegalArgumentException] {
      f1.relativize(f2)
    }
    
    intercept[IllegalArgumentException] {
      f2.relativize(f1)
    }
    
    val g1 = path("/a/b")
    val g2 = path2("/a/b")
    
    intercept[IllegalArgumentException] {
      g1.relativize(g2)
    }
  }
  
  @Test
  def testResolve() {
    val p1 = path("/a/b/c")
    
    assert(p1.resolve("d/e/f") === path("/a/b/c/d/e/f"))
    
    val p2 = path("/x/y/z")
    
    assert(p1.resolve(p2) eq p2)
    
    assert(p1.resolve("") eq p1)
  }
  
  @Test
  def testResolveSibling() {
    val p1 = path("/a/b/c")
    
    assert(p1.resolveSibling("d/e") === path("/a/b/d/e"))
    
    val p2 = path("/x/y/z")
    
    assert(p1.resolveSibling(p2) eq p2)
    
    val p3 = path("a")
    val p4 = path("b")
    
    assert(p3.resolveSibling(p4) eq p4)
  }
  
  @Test
  def testStartsWith() {
    val p1 = path("/a/b/c")
    val p2 = path("/a/b")
    val p3 = path("/a/b/c/d")
    val p4 = path("a/b/c")
    val p5 = path("a/b")
    val p6 = path("a/b/c/d")
    val p7 = path2("/a/b")
    
    assert(p1.startsWith(p2))
    assert(!p1.startsWith(p3))
    assert(p3.startsWith(p1))
    assert(!p1.startsWith(p4))
    assert(!p4.startsWith(p1))
    assert(p4.startsWith(p5))
    assert(!p4.startsWith(p6))
    assert(p6.startsWith(p4))
    assert(!p1.startsWith(p7))
  }
  
  @Test
  def testEndsWith() {
    val p1 = path("/a/b/c")
    val p2 = path("/a/b")
    val p3 = path("/a/b/c/d")
    val p4 = path("a/b/c")
    val p5 = path("a/b")
    val p6 = path("a/b/c/d")
    val p7 = path("b/c")
    val p8 = path2("b/c")
    
    assert(p1.endsWith(p1))
    assert(!p1.endsWith(p2))
    assert(!p1.endsWith(p3))
    assert(p1.endsWith(p4))
    assert(!p1.endsWith(p5))
    assert(!p1.endsWith(p6))
    assert(p1.endsWith(p7))
    
    assert(!p4.endsWith(p1))
    assert(!p4.endsWith(p2))
    assert(!p4.endsWith(p3))
    assert(p4.endsWith(p4))
    assert(!p4.endsWith(p5))
    assert(!p4.endsWith(p6))
    assert(p4.endsWith(p7))
    assert(!p4.endsWith(p8))
  }
  
  @Test
  def testGetFileName() {
    val p1 = path("/a/b/c/d")
    assert(p1.getFileName() === path("d"))
    
    val p2 = path("a/b/c/d")
    assert(p2.getFileName() === path("d"))
    
    val p3 = path("../a/b")
    assert(p3.getFileName() === path("b"))
    
    val p4 = path("a/..")
    assert(p4.getFileName() === path(".."))
    
    val p5 = path(".")
    assert(p5.getFileName() === path("."))
    
    val p6 = path("a/.")
    assert(p6.getFileName() === path("."))
    
    val p7 = path("a/./b")
    assert(p7.getFileName() === path("b"))
    
    val p8 = path("")
    assert(p8.getFileName() === null)
  }
  
  @Test
  def testGetRoot() {
    assert(path("/").getRoot() === path("/"))
    
    assert(path("/a/b").getRoot() === path("/"))
    
    assert(path("/a/../b/../c").getRoot() === path("/"))
    
    assert(path("a/b/c").getRoot() === null)
    
    assert(path("a/../b").getRoot() === null)
    
    assert(path("../a/b").getRoot() === null)
    
    assert(path("./a/b").getRoot() === null)
  }
  
  @Test
  def testCompareTo() {
    val p1 = path("/a/b/c")
    val p2 = path("/a/b/c/d")
    val p3 = path("/a/b/z")
    val p4 = path("")
    val p5 = path("..")
    val p6 = path("a/b/c")
    val p7 = path("0/a")
    val other = npath("/a/b/c")
    
    assert(p1.compareTo(p2) === p1.toString().compareTo(p2.toString()))
    assert(p1.compareTo(p3) === p1.toString().compareTo(p3.toString()))
    assert(p1.compareTo(p4) === p1.toString().compareTo(p4.toString()))
    assert(p1.compareTo(p5) === p1.toString().compareTo(p5.toString()))
    assert(p1.compareTo(p6) === p1.toString().compareTo(p6.toString()))
    assert(p1.compareTo(p7) === p1.toString().compareTo(p7.toString()))
    
    intercept[ClassCastException] {
      p1.compareTo(other)
    }
  }
  
  @Test
  def testEquals() {
    val p1 = path("/a/b/c")
    val p2 = path("/a/b/c")
    val p3 = path("/a/./b/c")
    val p4 = path("a/b/c")
    val p5 = path("a/b/c")
    val p6 = path("a/B/c")
    val p7 = path2("/a/b/c")
    val other = npath("a/b/c")
    
    assert(p1.equals(p2))
    assert(p1.equals(p2))
    assert(!p1.equals(p3))
    assert(!p1.equals(p4))
    assert(p4.equals(p5))
    assert(!p4.equals(p6))
    assert(!p4.equals(other))
    assert(!p1.equals(p7))
  }
  
  @Test
  def testGetName() {
    val p1 = path("/a/b/../c/./d/e")
    assert(p1.getName(0) === path("a"))
    assert(p1.getName(1) === path("b"))
    assert(p1.getName(2) === path(".."))
    assert(p1.getName(3) === path("c"))
    assert(p1.getName(4) === path("."))
    assert(p1.getName(5) === path("d"))
    assert(p1.getName(6) === path("e"))
    
    intercept[IllegalArgumentException] {
      p1.getName(-1)
    }
    
    intercept[IllegalArgumentException] {
      p1.getName(7)
    }
    
    val p2 = path("/")
    intercept[IllegalArgumentException] {
      p2.getName(0)
    }
    
    val p3 = path(".")
    assert(p3.getName(0) === path("."))
    intercept[IllegalArgumentException] {
      p3.getName(1)
    }
    
    val p4 = path("..")
    assert(p4.getName(0) === path(".."))
    intercept[IllegalArgumentException] {
      p4.getName(1)
    }
    
    val p5 = path("")
    intercept[IllegalArgumentException] {
      p5.getName(0)
    }
  }
  
  @Test
  def testGetParent() {
    assert(path("/a/b/c").getParent() === path("/a/b"))
    
    assert(path("a/b/c").getParent() === path("a/b"))
    
    assert(path("/").getParent() === null)
    
    assert(path("").getParent() === null)
    
    assert(path("a").getParent() === null)
    
    assert(path("/a").getParent() === path("/"))
    
    assert(path(".").getParent() === null)
    
    assert(path("./a").getParent() === path("."))
    
    assert(path("./a/b").getParent() === path("./a"))
    
    assert(path("..").getParent() === null)
    
    assert(path("../a").getParent() === path(".."))
    
    assert(path("../a/b").getParent() === path("../a"))
  }
  
  @Test
  def testIterator() {
    val i1 = path("/a/b/../c/./d").iterator()
    
    assert(i1.hasNext())
    assert(i1.next() === path("a"))
    assert(i1.hasNext())
    assert(i1.next() === path("b"))
    assert(i1.hasNext())
    assert(i1.next() === path(".."))
    assert(i1.hasNext())
    assert(i1.next() === path("c"))
    assert(i1.hasNext())
    assert(i1.next() === path("."))
    assert(i1.hasNext())
    assert(i1.next() === path("d"))
    assert(!i1.hasNext())
    intercept[NoSuchElementException] {
      i1.next()
    }
    
    val i2 = path("/").iterator()
    
    assert(!i2.hasNext())
    intercept[NoSuchElementException] {
      i2.next()
    }
    
    val i3 = path("").iterator()
    
    assert(!i3.hasNext())
    intercept[NoSuchElementException] {
      i3.next()
    }
  }
  
  @Test
  def testSubpath() {
    val p = path("/a/b/../c/./d")
    
    assert(p.subpath(0, 6) === path("a/b/../c/./d"))
    assert(p.subpath(1, 5) === path("b/../c/."))
    assert(p.subpath(0, 1) === path("a"))
    assert(p.subpath(1, 2) === path("b"))
    assert(p.subpath(5, 6) === path("d"))
    assert(p.subpath(2, 3) === path(".."))
    
    intercept[IllegalArgumentException] {
      p.subpath(-1, 1)
    }
    
    intercept[IllegalArgumentException] {
      p.subpath(0, 7)
    }
    
    intercept[IllegalArgumentException] {
      p.subpath(3, 1)
    }
  }
  
  @Test
  def testToFile() {
    intercept[UnsupportedOperationException] {
      path("/a/b/c").toFile()
    }
  }
  
  @Test
  def testToRealPath() {
    val p = path("/a/b/c/../d/../../e/./f/././g/h")
    assert(p.toRealPath() === p.normalize())
  }
}
