package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.ByteBuffer
import org.scalatest.junit.JUnitSuite
import scala.collection.mutable.ArrayBuffer
import TestConversions._
import java.nio.file.NoSuchFileException
import java.nio.file.FileAlreadyExistsException

class DirectorySuite extends JUnitSuite {
  @Test
  def testLocateExistingEmptyDirectoryPath() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createDirectory("b")
    val c = b.createDirectory("c")
    
    val f = r.locateDir("a", "b", "c")
    assert(f.isDefined)
    assert(f.get eq c)
  }
  
  @Test
  def testLocateExistingNotEmptyDirectoryPath() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createDirectory("b")
    val c = b.createDirectory("c")
    val d1 = c.createDirectory("d1")
    val d2 = c.createNewFile("d2")
    
    val f = r.locateDir("a", "b", "c")
    assert(f.isDefined)
    assert(f.get eq c)
  }
  
  @Test
  def testLocateExistingFilePath() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createDirectory("b")
    val c = b.createNewFile("c")
    
    val f = r.locateFile("a", "b", "c")
    assert(f.isDefined)
    assert(f.get eq c)
  }
  
  @Test
  def testLocateInvalidPath() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createNewFile("b")
    
    val node = r.locate(new SomeOrNone[Node], "a", "b", "c")
    assert(node.isEmpty)
    
    val file = r.locate(new SomeOrNone[File], "a", "b", "c")
    assert(file.isEmpty)
    
    val dir = r.locate(new SomeOrNone[Directory], "a", "b", "c")
    assert(dir.isEmpty)
  }
  
  @Test
  def testLocateEmptyPath() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createNewFile("b")
    
    val f = a.locate(new SomeOrNone[Node])
    assert(f.isEmpty)
  }
  
  @Test
  def testCreateDirectoryFromSingleNameWhenItDoesntExist() = {
    val r = newTestFS().rootNode
    
    val d = r.createDirectory("d")
    
    assert(d.name == "d")
    assert(d.parent eq r)
  }
  
  @Test
  def testCreateDirectoryFromPathWhenItDoesntExist() = {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    
    val d = r.createDirectory("a", "b")
    
    assert(d.name == "b")
    assert(d.parent eq a)
  }
  
  @Test
  def testCreateDirectoryFromSingleNameWhenItAlreadyExists() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createDirectory("b")
    
    val d = a.createDirectory("b")
    
    assert(d eq b)
  }
  
  @Test
  def testCreateDirectoryFromPathWhenItAlreadyExists() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createDirectory("b")
    
    val d = r.createDirectory("a", "b")
    
    assert(d eq b)
  }
  
  @Test
  def testCreateDirectoryFromSingleNameWhenFileAlreadyExists() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createFile("b")
    
    intercept[FileAlreadyExistsException] {
      val d = a.createDirectory("b")
    }
  }
  
  @Test
  def testCreateDirectoryFromPathWhenPathDoesntExist() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    
    intercept[NoSuchFileException] {
      r.createDirectory("a", "b", "c")
  	}
  }
  
  @Test
  def testCreateDirectoryFromPathWhenFileInThePath() {
    val r = newTestFS().rootNode
    val a = r.createDirectory("a")
    val b = a.createFile("b")

    intercept[FileAlreadyExistsException] {
    	r.createDirectory("a", "b", "c")
    }
  }
  
}
