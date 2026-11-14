package io.github.jaasonw.qrcodr

private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

private val trackerParams =
    setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
        "twclid", "ttclid", "li_fat_id", "mc_cid", "mc_eid", "igshid", "igsh", "_ga", "_gl",
        "_hsenc", "_hsmi", "mkt_tok", "oly_anon_id", "oly_enc_id", "vero_id", "wickedid",
        "ref_src", "ref_url",
    )

// Short generic names are only trackers on these hosts; elsewhere they may carry meaning.
private val hostTrackerParams =
    mapOf(
        "youtube.com" to setOf("si", "pp", "feature"),
        "youtu.be" to setOf("si"),
        "spotify.com" to setOf("si", "context"),
        "twitter.com" to setOf("s", "t"),
        "x.com" to setOf("s", "t"),
        "instagram.com" to setOf("img_index"),
        "linkedin.com" to setOf("rcm", "trk"),
        "reddit.com" to setOf("share_id", "rdt"),
        "tiktok.com" to setOf("is_from_webapp", "sender_device", "_r", "_t"),
        "amazon.com" to setOf("ref", "ref_", "pd_rd_w", "pd_rd_r", "pd_rd_wg", "pf_rd_p", "pf_rd_r", "content-id"),
    )

/** Picks the first http(s) URL out of shared text, or returns the trimmed text if there is none. */
fun extractLink(text: String): String =
    urlRegex.find(text)?.value?.trimEnd('.', ',', ')', '!', '?', '"', '\'') ?: text.trim()

/** Removes known tracking query parameters. Fragment and remaining parameter order are kept. */
fun stripTrackers(url: String): String {
  val hashAt = url.indexOf('#')
  val fragment = if (hashAt >= 0) url.substring(hashAt) else ""
  val beforeFragment = if (hashAt >= 0) url.substring(0, hashAt) else url
  val queryAt = beforeFragment.indexOf('?')
  if (queryAt < 0) return url

  val base = beforeFragment.substring(0, queryAt)
  val host = base.substringAfter("://").substringBefore('/').substringAfterLast('@').substringBefore(':').lowercase()
  val hostSpecific = hostTrackerParams.entries.filter { host == it.key || host.endsWith("." + it.key) }.flatMap { it.value }

  val kept =
      beforeFragment.substring(queryAt + 1).split('&').filter { param ->
        val name = param.substringBefore('=').lowercase()
        param.isNotEmpty() && !name.startsWith("utm_") && name !in trackerParams && name !in hostSpecific
      }
  return base + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + fragment
}
