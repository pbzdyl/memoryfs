package net.bzdyl.memoryfs

import org.junit.Test
import org.junit.Assert._
import java.nio.ByteBuffer
import org.scalatest.junit.JUnitSuite
import scala.collection.mutable.ArrayBuffer
import TestConversions._
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NotDirectoryException
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption._
import java.nio.file.DirectoryNotEmptyException
import org.junit.Ignore

class MemoryFileSystemSuite extends JUnitSuite {
  @Test
  def testCreateDirectory() {
    val mfsp = new MemoryFileSystemProvider
    val mfs = mfsp.getFileSystem(null)

    val f = mfs.getPath("/a")
//    mfs.createDirectory(f, null)

//    mfs.rootNode.locateChild("a") match {
//      case Some(dir: Directory) => // passed
//      case Some(notDir)         => fail
//      case None                 => fail
//    }
  }

  @Test
  def testCreateDirectoryWhenDirectoryExists() {
//    val mfsp = new MemoryFileSystemProvider
//    val mfs = mfsp.getFileSystem(null)
//
//    val f = mfs.getPath("/a")
//    mfs.createDirectory(f, null)
//
//    intercept[FileAlreadyExistsException] {
//      mfs.createDirectory(f, null)
//    }
  }
  
  @Test
  def testCreateDirectoryWhenParentIsAFile() {
//    val mfsp = new MemoryFileSystemProvider
//    val mfs = mfsp.getFileSystem(null)
//
//    val f = mfs.getPath("/a")
//    val d = f.resolve("b")
//    mfs.createNewFileData(f, null)
//
//    intercept[NotDirectoryException] {
//      mfs.createDirectory(d, null)
//    }
  }
  
  @Test
  def testMoveNotExistingPathToNotExistingPath() {
    val mfs = newTestFS()
    
    val from = mfs.getPath("/a/b/c")
    val to = mfs.getPath("/x/y/z")
    
    intercept[NoSuchFileException] {
      mfs.move(from, to)
    }
  }
  
  @Test
  def testMoveExistingFileToNotExistingPath() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    val moved = mfs.rootNode.locateFile("b")
    
    assert(removed.isEmpty)
    assert(moved.nonEmpty)
    assert(moved.get.bytes === a.bytes)
  }
  
  @Test
  def testMoveExistingFileToExistingFileDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    val b = mfs.rootNode.createNewFile("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
      mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateFile("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateFile("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveExistingFileToExistingFileReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    mfs.rootNode.createNewFile("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to, REPLACE_EXISTING)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    val moved = mfs.rootNode.locateFile("b")
    assert(removed.isEmpty)
    assert(moved.nonEmpty)
    assert(moved.get.bytes === a.bytes)
  }
  
  @Test
  def testMoveExistingFileToEmptyDirDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    val b = mfs.rootNode.createNewDirectory("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
      mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateFile("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveExistingFileToEmptyDirReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    val b = mfs.rootNode.createNewDirectory("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to, REPLACE_EXISTING)
    
    val moved = mfs.rootNode.locateFile("b")
    assert(moved.nonEmpty)
    assert(moved.get.bytes === a.bytes)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    assert(removed.isEmpty)
  }
  
  @Test
  def testMoveExistingFileToNonEmptyDirDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    val b = mfs.rootNode.createNewDirectory("b")
    b.createNewFile("c")
    assert(b.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
      mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateFile("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveExistingFileToNonEmptyDirReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    val b = mfs.rootNode.createNewDirectory("b")
    b.createNewFile("c")
    assert(b.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[DirectoryNotEmptyException] {
      mfs.move(from, to, REPLACE_EXISTING)
    }
    
    val notMoved = mfs.rootNode.locateFile("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveEmptyDirToEmptyDirDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
      mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveEmptyDirToEmptyDirReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to, REPLACE_EXISTING)
    
    val moved = mfs.rootNode.locateDir("b")
    assert(moved.nonEmpty)
    assert(moved.get.entries eq a.entries)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    assert(removed.isEmpty)
  }
  
  @Test
  def testMoveEmptyDirToNonEmptyDirDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    b.createFile("c")
    assert(b.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
      mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveEmptyDirToNonEmptyDirReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    b.createFile("c")
    assert(b.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[DirectoryNotEmptyException] {
      mfs.move(from, to, REPLACE_EXISTING)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveEmptyDirToNotExistingPath() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to)
    
    val moved = mfs.rootNode.locateDir("b")
    assert(moved.nonEmpty)
    assert(moved.get.entries eq a.entries)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    assert(removed.isEmpty)
  }
  
  @Test
  def testMoveNonEmptyDirToNotExistingPath() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val c = a.createNewFile("c")
    assert(a.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to)
    
    val b = mfs.rootNode.locateDir("b")
    
    assert(b.nonEmpty)
    assert(b.get.entries === a.entries)
    val movedChild = b.get.locateFile("c")
    assert(movedChild.nonEmpty)
    assert(movedChild.get eq c)
    assert(movedChild.get.bytes eq c.bytes)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    assert(removed.isEmpty)
  }
  
  @Test
  def testMoveNonEmptyDirToEmptyDirDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    val c = a.createNewFile("c")
    assert(a.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
	  mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    assert(notMoved.get.entries eq a.entries)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveNonEmptyDirToEmptyDirReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    val c = a.createNewFile("c")
    assert(a.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to, REPLACE_EXISTING)
    
    val moved = mfs.rootNode.locateDir("b")
    assert(moved.nonEmpty)
    assert(moved.get.entries === a.entries)
    val movedChild = moved.get.locateFile("c")
    assert(movedChild.nonEmpty)
    assert(movedChild.get eq c)
    assert(movedChild.get.bytes eq c.bytes)
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    assert(removed.isEmpty)
  }
  
  @Test
  def testMoveNonEmptyDirToNonEmptyDirDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    val c = a.createNewFile("c")
    assert(a.nonEmpty)
    val d = b.createNewFile("d")
    assert(b.nonEmpty)
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
	  mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    assert(notMoved.get.entries eq a.entries)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
    assert(notReplaced.get.entries eq b.entries)
  }
  
  @Test
  def testMoveNonEmptyDirToNonEmptyDirReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewDirectory("b")
    val c = a.createNewFile("c")
    assert(a.nonEmpty)
    val d = b.createNewFile("d")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[DirectoryNotEmptyException] {
      mfs.move(from, to, REPLACE_EXISTING)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateDir("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveExistingFileToTheSamePath() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewFile("a")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/a")
    
    mfs.move(from, to)
    
    val theSame = mfs.rootNode.locateFile("a")
    assert(theSame.nonEmpty)
    assert(theSame.get eq a)
  }
  
  @Test
  def testMoveDirToTheSamePath() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/a")
    
    mfs.move(from, to)
    
    val theSame = mfs.rootNode.locateDir("a")
    assert(theSame.nonEmpty)
    assert(theSame.get eq a)
  }
  
  @Test
  def testMoveDirToExistingFileDontReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewFile("b")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    intercept[FileAlreadyExistsException] {
      mfs.move(from, to)
    }
    
    val notMoved = mfs.rootNode.locateDir("a")
    assert(notMoved.nonEmpty)
    assert(notMoved.get eq a)
    
    val notReplaced = mfs.rootNode.locateFile("b")
    assert(notReplaced.nonEmpty)
    assert(notReplaced.get eq b)
  }
  
  @Test
  def testMoveDirToExistingFileReplace() {
    val mfs = newTestFS()
    val a = mfs.rootNode.createNewDirectory("a")
    val b = mfs.rootNode.createNewFile("b")
    val c = a.createFile("c")
    
    val from = mfs.getPath("/a")
    val to = mfs.getPath("/b")
    
    mfs.move(from, to, REPLACE_EXISTING)
    
    val moved = mfs.rootNode.locateDir("b")
    assert(moved.nonEmpty)
    assert(moved.get.entries === a.entries)
    
    val movedChild = moved.get.locateFile("c")
    assert(movedChild.nonEmpty)
    assert(movedChild.get eq c)
    assert(movedChild.get.bytes eq c.bytes) 
    
    val removed = mfs.rootNode.locate(NodeOrNone, "a")
    assert(removed.isEmpty)
  }
}
