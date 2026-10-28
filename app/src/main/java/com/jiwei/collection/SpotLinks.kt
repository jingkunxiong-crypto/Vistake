package com.jiwei.collection

import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** A self-contained, versioned text-only link. It never contains private image paths. */
object SpotLinks {
 private const val MAX_LINK_LENGTH = 16000
 private const val MAX_DOCUMENT_BYTES = 32768
 fun create(spot: Spot): String {
  val json = JSONObject().put("v", 1).put("name", spot.name).put("address", spot.address)
   .put("category", spot.category).put("lat", spot.lat).put("lon", spot.lon)
   .put("time", spot.time).put("tips", spot.tips).toString().toByteArray(Charsets.UTF_8)
  require(json.size <= MAX_DOCUMENT_BYTES) { "机位文字太长，无法生成链接" }
  val buffer = ByteArrayOutputStream()
  GZIPOutputStream(buffer).use { it.write(json) }
  val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray())
  return "jiweicollection://spot/v1?data=$encoded".also {
   require(it.length <= MAX_LINK_LENGTH) { "机位文字太长，无法生成链接" }; parse(it)
  }
 }
 fun parse(input: String): Spot {
  require(input.length <= MAX_LINK_LENGTH + 2048) { "链接过长" }
  val link = Regex("jiweicollection://spot/v1\\?data=[A-Za-z0-9_-]+", RegexOption.IGNORE_CASE).find(input)?.value
   ?: throw IllegalArgumentException("未找到有效的机位链接")
  require(link.length <= MAX_LINK_LENGTH) { "链接过长" }
  val uri = Uri.parse(link)
  require(uri.scheme == "jiweicollection" && uri.host == "spot" && uri.path == "/v1") { "不支持的链接格式" }
  val data = Base64.getUrlDecoder().decode(uri.getQueryParameter("data") ?: throw IllegalArgumentException("链接缺少内容"))
  val out = ByteArrayOutputStream()
  GZIPInputStream(ByteArrayInputStream(data)).use { stream ->
   val buf = ByteArray(4096)
   while (true) { val count = stream.read(buf); if (count < 0) break; require(out.size() + count <= MAX_DOCUMENT_BYTES) { "链接内容过大" }; out.write(buf, 0, count) }
  }
  val json = JSONObject(out.toString("UTF-8"))
  require(json.optInt("v") == 1) { "不支持的链接版本" }
  fun text(key: String, max: Int): String = json.optString(key, "").also { require(it.length <= max) { "机位信息过长" } }
  val name = text("name", 200).trim(); require(name.isNotEmpty()) { "机位名称为空" }
  val lat=text("lat",32); val lon=text("lon",32)
  if (lat.isNotBlank() || lon.isNotBlank()) {
   val a=lat.toDoubleOrNull(); val b=lon.toDoubleOrNull()
   require(a != null && b != null && a in -90.0..90.0 && b in -180.0..180.0) { "坐标无效" }
  }
  return Spot(name=name,address=text("address",1000),category=text("category",50).ifBlank { "其他" },lat=lat,lon=lon,time=text("time",1000),tips=text("tips",12000))
 }
}
