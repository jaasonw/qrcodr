package io.github.jaasonw.qrcodr

import org.junit.Assert.assertEquals
import org.junit.Test

class LinksTest {
  @Test
  fun extractsUrlFromText() {
    assertEquals("https://a.com/x?y=1", extractLink("Check this out: https://a.com/x?y=1."))
    assertEquals("plain text", extractLink("  plain text "))
  }

  @Test
  fun stripsTrackers() {
    assertEquals("https://a.com/p?id=3#top", stripTrackers("https://a.com/p?utm_source=x&id=3&fbclid=abc#top"))
    assertEquals("https://a.com/p", stripTrackers("https://a.com/p?utm_medium=y&gclid=z"))
    assertEquals("https://youtu.be/abc?t=42", stripTrackers("https://youtu.be/abc?si=XyZ&t=42"))
    assertEquals("https://www.youtube.com/watch?v=abc", stripTrackers("https://www.youtube.com/watch?v=abc&si=q&pp=w"))
    assertEquals("https://x.com/u/status/1", stripTrackers("https://x.com/u/status/1?s=20&t=abc"))
    assertEquals("https://other.com/?si=keep&t=keep", stripTrackers("https://other.com/?si=keep&t=keep"))
    assertEquals("https://a.com/no-query", stripTrackers("https://a.com/no-query"))
  }

  @Test
  fun removesSingleParam() {
    val parts = UrlParts.parse("https://a.com/p?a=1&b=2&c#frag")
    assertEquals(listOf("a=1", "b=2", "c"), parts.params)
    assertEquals("https://a.com/p?a=1&c#frag", parts.without(1).toString())
    assertEquals("https://a.com/p#frag", parts.copy(params = emptyList()).toString())
  }
}
