package net.bzdyl.memoryfs
import scala.collection.mutable.Buffer
import scala.collection.GenTraversableOnce
import scala.collection.script.Message
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

trait ReadWriteLockBuffer[A] extends Buffer[A] with ReadWriteLockOps {
  abstract override def length: Int = withReadLock {
    super.length
  }

  abstract override def iterator: Iterator[A] = withReadLock {
    super.iterator
  }

  abstract override def apply(n: Int): A = withReadLock {
    super.apply(n)
  }

  override def +(elem: A): Self = withWriteLock {
    super.+(elem)
  }

  abstract override def +=(elem: A): this.type = withWriteLock[this.type] {
    super.+=(elem)
  }

  override def ++(xs: GenTraversableOnce[A]): Self = withWriteLock {
    super.++(xs)
  }

  override def ++=(xs: TraversableOnce[A]): this.type = withWriteLock[this.type] {
    super.++=(xs)
  }

  override def append(elems: A*): Unit = withWriteLock {
    super.++=(elems)
  }

  override def appendAll(xs: TraversableOnce[A]): Unit = withWriteLock {
    super.appendAll(xs)
  }

  abstract override def +=:(elem: A): this.type = withWriteLock[this.type] {
    super.+=:(elem)
  }

  override def ++=:(xs: TraversableOnce[A]): this.type = withWriteLock[this.type] { super.++=:(xs) }

  override def prepend(elems: A*): Unit = prependAll(elems)

  override def prependAll(xs: TraversableOnce[A]): Unit = withWriteLock {
    super.prependAll(xs)
  }

  override def insert(n: Int, elems: A*): Unit = withWriteLock {
    super.insertAll(n, elems)
  }

  abstract override def insertAll(n: Int, xs: Traversable[A]): Unit = withWriteLock {
     super.insertAll(n, xs)
  }

  abstract override def update(n: Int, newelem: A): Unit = withWriteLock {
    super.update(n, newelem)
  }

  abstract override def remove(n: Int): A = withWriteLock {
    super.remove(n)
  }

  abstract override def clear(): Unit = withWriteLock {
    super.clear
  }

  override def <<(cmd: Message[A]): Unit = withWriteLock {
    super.<<(cmd)
  }

  override def clone(): Self = withReadLock {
    super.clone()
  }

  override def hashCode(): Int = withReadLock {
    super.hashCode()
  }
}
