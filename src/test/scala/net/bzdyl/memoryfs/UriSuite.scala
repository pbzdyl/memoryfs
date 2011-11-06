package net.bzdyl.memoryfs
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import java.net.URI

class UriSuite extends JUnitSuite {
	@Test
	def test() {
	  val uri = new URI("memory:///test1?a=1&b=2");
	  
	  println("scheme " + uri.getScheme())
	  println("authority " + uri.getAuthority())
	  println("fragment " + uri.getFragment())
	  println("host " + uri.getHost())
	  println("path " + uri.getPath())
	  println("port " + uri.getPort())
	  println("query " + uri.getQuery())
	  println("raw authority " + uri.getRawAuthority())
	  println("raw fragment " + uri.getRawFragment())
	  println("raw path " + uri.getRawPath())
	  println("raw query " + uri.getRawQuery())
	  println("raw scheme specific part " + uri.getRawSchemeSpecificPart())
	  println("raw user info " + uri.getRawUserInfo())
	  println("user info " + uri.getUserInfo())
	}
}