package com.jiwei.collection

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private val ink = Color(0xffe6f2f1)
private val secondary = Color(0xffb7cccd)
private val glass = Color(0xff253c46)
private val mist = Color(0xff152934)
private val categories = listOf("日出", "日落", "星空", "城市", "山川", "海边", "延时", "其他")

data class Spot(val id:String = UUID.randomUUID().toString(), val name:String="", val address:String="", val category:String="山川", val lat:String="", val lon:String="", val time:String="", val tips:String="", val images:List<String> = emptyList(), val favorite:Boolean=false) {
 fun json() = JSONObject().put("id",id).put("name",name).put("address",address).put("category",category).put("lat",lat).put("lon",lon).put("time",time).put("tips",tips).put("images",JSONArray(images)).put("favorite",favorite)
 companion object { fun from(j:JSONObject):Spot { val a=j.optJSONArray("images") ?: JSONArray(); return Spot(j.optString("id"),j.optString("name"),j.optString("address"),j.optString("category"),j.optString("lat"),j.optString("lon"),j.optString("time"),j.optString("tips"),(0 until a.length()).map { a.getString(it) },j.optBoolean("favorite")) } }
}
class MainActivity:ComponentActivity() { private val incomingLink = mutableStateOf<String?>(null)
 override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); setIntent(intent); if(intent.action==Intent.ACTION_VIEW) incomingLink.value=intent.dataString }
 override fun onCreate(savedInstanceState:Bundle?) { if(intent?.action==Intent.ACTION_VIEW) incomingLink.value=intent.dataString; super.onCreate(savedInstanceState); window.statusBarColor=android.graphics.Color.rgb(15,29,39); window.navigationBarColor=android.graphics.Color.rgb(15,29,39); window.decorView.systemUiVisibility=0; setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xffb4dedd), onPrimary=Color(0xff102a32), surface=mist, onSurface=ink, onSurfaceVariant=secondary, secondaryContainer=Color(0xff486b70), onSecondaryContainer=ink, outline=Color(0xff78989b), surfaceContainerHigh=Color(0xff233c47))) { CompositionLocalProvider(LocalContentColor provides ink) { App(incomingLink.value) { incomingLink.value=null } } } } } }

@Composable fun App(incomingLink:String?, consumeLink:()->Unit) {
 val context=LocalContext.current
 val file=remember { File(context.filesDir,"spots.json") }
 val spots=remember { mutableStateListOf<Spot>().apply { try { if(file.exists()) { val a=JSONArray(file.readText()); addAll((0 until a.length()).map { Spot.from(a.getJSONObject(it)) }) } } catch (_:Exception) {} } }
 fun save() { val temp=File(context.filesDir,"spots.tmp"); temp.writeText(JSONArray(spots.map { it.json() }).toString()); if (!temp.renameTo(file)) { file.writeText(temp.readText()); temp.delete() } }
 var screen by remember { mutableStateOf("home") }; var selected by remember { mutableStateOf<String?>(null) }; var editing by remember { mutableStateOf<Spot?>(null) }; var query by remember { mutableStateOf("") }; var filter by remember { mutableStateOf("全部") }; var favorites by remember { mutableStateOf(false) }
 val current=spots.firstOrNull { it.id==selected }
 var showImport by remember { mutableStateOf(false) }
 var importText by remember { mutableStateOf("") }
 var importError by remember { mutableStateOf("") }
 var preview by remember { mutableStateOf<Spot?>(null) }
 LaunchedEffect(incomingLink) { if (incomingLink != null) { importText=incomingLink; preview=null; importError=""; showImport=true; consumeLink() } }
 if(showImport) AlertDialog(onDismissRequest={showImport=false;preview=null}, title={Text(if(preview==null) "导入机位" else "确认导入")}, text={
  if(preview==null) Column { Text("粘贴收到的机位链接，查看内容后再导入。链接仅包含文字和已有坐标，不包含照片。",fontSize=15.sp); Spacer(Modifier.height(10.dp)); OutlinedTextField(value=importText,onValueChange={ if(it.length <= 18048) importText=it;importError=""},label={Text("机位链接")},modifier=Modifier.fillMaxWidth(),maxLines=4); if(importError.isNotEmpty()) Text(importError,color=Color(0xffffb4ab),fontSize=14.sp) }
  else Column(Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState())) { Text(preview!!.name,fontSize=19.sp,fontWeight=FontWeight.Medium); Spacer(Modifier.height(8.dp)); Text(preview!!.address.ifBlank { "未填写地点" }); Text("题材：${preview!!.category}"); if(preview!!.time.isNotBlank()) Text("最佳时间：${preview!!.time}"); if(preview!!.tips.isNotBlank()) Text("拍摄提示：${preview!!.tips}"); Spacer(Modifier.height(8.dp)); Text("导入后保存为新的本地机位，不包含样片。",fontSize=14.sp,color=secondary) }
 },confirmButton={TextButton(onClick={ if(preview==null) { try { preview=SpotLinks.parse(importText);importError="" } catch (_:Exception) { importError="链接无效或已损坏，请检查后重试" } } else { val imported=preview!!;spots.add(imported);save();selected=imported.id;screen="detail";showImport=false;preview=null;importText="" } }) { Text(if(preview==null) "查看内容" else "导入") }},dismissButton={TextButton(onClick={showImport=false;preview=null}) {Text("取消")}})
 Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xff101d2a),Color(0xff203d49),Color(0xff122631))))) {
  AnimatedContent(targetState=screen, transitionSpec={ (fadeIn(tween(260)) + slideInHorizontally(tween(300)) { it / 12 }) togetherWith (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { -it / 18 }) }, label="page transition") { page ->
  when(page) {
   "home" -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=20.dp)) {
    Spacer(Modifier.height(16.dp)); Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("FIELD NOTES  /  机位手记",fontSize=13.sp,letterSpacing=2.sp,color=secondary); Spacer(Modifier.height(6.dp)); Text("机位合集",fontSize=30.sp,fontWeight=FontWeight.Light,color=ink); Text("把值得等待的风景，留在这里。",fontSize=15.sp,color=secondary) }; Column(horizontalAlignment=Alignment.End) { TextButton(onClick={editing=null;screen="edit"}) { Text("＋ 新建",fontSize=17.sp) }; TextButton(onClick={importText="";importError="";preview=null;showImport=true}) { Text("导入链接",fontSize=15.sp) } } }
    Spacer(Modifier.height(20.dp)); OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("搜索地点、名称或题材",fontSize=16.sp)},shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=glass.copy(alpha=.80f),focusedContainerColor=glass.copy(alpha=.95f)))
    Spacer(Modifier.height(14.dp)); Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { (listOf("全部","收藏")+categories).forEach { c -> val active=if(c=="收藏") favorites else !favorites && filter==c; Surface(onClick={ if(c=="收藏") favorites=true else { favorites=false;filter=c } },shape=RoundedCornerShape(50),color=if(active) Color(0xff617f82) else glass.copy(alpha=.80f)) { Text(c,Modifier.padding(horizontal=15.dp,vertical=8.dp),fontSize=15.sp,color=if(active) Color.White else ink) } } }
    Spacer(Modifier.height(17.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(if(favorites) "收藏的机位" else "我的机位",fontSize=16.sp,fontWeight=FontWeight.Medium); Text("${spots.size} 个记录",fontSize=14.sp,color=secondary) }; Spacer(Modifier.height(12.dp))
    val shown=spots.asReversed().filter { (!favorites || it.favorite) && (favorites || filter=="全部" || it.category==filter) && (query.isBlank() || listOf(it.name,it.address,it.category).any { x->x.contains(query,true) }) }
    if(shown.isEmpty()) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("◌",fontSize=42.sp,color=Color(0xff9ab2b2)); Text(if(spots.isEmpty()) "从一处风景开始" else "没有找到相关机位",fontSize=16.sp,color=ink); Spacer(Modifier.height(8.dp)); Text(if(spots.isEmpty()) "添加样片、位置和拍摄灵感" else "试试其他关键词或分类",fontSize=15.sp,color=secondary); if(spots.isEmpty()) TextButton(onClick={ editing=null;screen="edit" }) { Text("创建第一个机位 →") } } } }
    else LazyVerticalGrid(columns=GridCells.Fixed(2),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) { items(shown,key={it.id}) { spot -> SpotCard(spot) {selected=spot.id;screen="detail"} } }
   }
   "edit" -> Editor(editing, onBack={screen=if(selected!=null && editing!=null) "detail" else "home"}, onSave={spot -> val index=spots.indexOfFirst{it.id==spot.id}; if(index>=0) spots[index]=spot else spots.add(spot); save();selected=spot.id;screen="detail"})
   "detail" -> if(current!=null) Detail(current,onBack={selected=null;screen="home"},onEdit={editing=current;screen="edit"},onDelete={spots.remove(current);save();selected=null;screen="home"},onFavorite={val i=spots.indexOf(current);spots[i]=current.copy(favorite=!current.favorite);save()}) else LaunchedEffect(Unit) {screen="home"}
  }
  }
  var showSplash by remember { mutableStateOf(true) }
  LaunchedEffect(Unit) {
   val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
   if (scale > 0f) delay(1350)
   showSplash=false
  }
  AnimatedVisibility(visible=showSplash, exit=fadeOut(tween(360))) { LaunchIntro() }
 }
}

@Composable fun LaunchIntro() {
 val iconScale=remember { Animatable(.84f) }
 val opacity=remember { Animatable(0f) }
 LaunchedEffect(Unit) { opacity.animateTo(1f,tween(350)); iconScale.animateTo(1f,tween(700,easing=FastOutSlowInEasing)) }
 Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xff101d2a),Color(0xff254954),Color(0xff10222c)))),contentAlignment=Alignment.Center) {
  Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.graphicsLayer { alpha=opacity.value }) {
   Image(painterResource(com.jiwei.collection.R.drawable.app_icon),contentDescription=null,modifier=Modifier.size(116.dp).graphicsLayer { scaleX=iconScale.value; scaleY=iconScale.value }.clip(RoundedCornerShape(32.dp)))
   Spacer(Modifier.height(22.dp)); Text("机位合集",fontSize=25.sp,fontWeight=FontWeight.Medium,letterSpacing=3.sp,color=ink)
   Spacer(Modifier.height(8.dp)); Text("把风景，留在这里",fontSize=16.sp,color=secondary,letterSpacing=1.sp)
  }
 }
}

@Composable fun Glass(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) { Column(modifier.clip(RoundedCornerShape(22.dp)).background(glass.copy(alpha=.82f)).border(1.dp,Color(0xff9cc6c6).copy(alpha=.24f),RoundedCornerShape(22.dp)).padding(16.dp),content=content) }
@Composable fun SpotCard(s:Spot,onClick:()->Unit) { Column(Modifier.clip(RoundedCornerShape(20.dp)).background(glass.copy(alpha=.88f)).border(1.dp,Color(0xff9cc6c6).copy(alpha=.24f),RoundedCornerShape(20.dp)).clickable(onClick=onClick)) { if(s.images.isNotEmpty()) AsyncImage(model=File(s.images.first()),contentDescription=s.name,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(155.dp)) else Box(Modifier.fillMaxWidth().height(155.dp).background(Brush.linearGradient(listOf(Color(0xff315762),Color(0xff203841)))),contentAlignment=Alignment.Center) { Text("风景待收藏",fontSize=15.sp,color=Color.White) }; Column(Modifier.padding(12.dp)) { Text(s.name,fontSize=17.sp,fontWeight=FontWeight.Medium,maxLines=1); Spacer(Modifier.height(4.dp)); Text(s.address.ifBlank { "未填写地点" },fontSize=14.sp,color=secondary,maxLines=1); Spacer(Modifier.height(9.dp)); Text("${s.category}  ${if(s.favorite) "  ♥" else ""}",fontSize=14.sp,color=Color(0xffb4dedd)) } } }

@Composable fun Editor(original:Spot?,onBack:()->Unit,onSave:(Spot)->Unit) {
 val ctx=LocalContext.current; val initial=remember(original) { original ?: Spot() }
 var name by remember { mutableStateOf(initial.name) }; var address by remember { mutableStateOf(initial.address) }; var category by remember { mutableStateOf(initial.category) }; var lat by remember { mutableStateOf(initial.lat) }; var lon by remember { mutableStateOf(initial.lon) }; var time by remember { mutableStateOf(initial.time) }; var tips by remember { mutableStateOf(initial.tips) }; val images=remember { mutableStateListOf<String>().apply { addAll(initial.images) } }; var error by remember { mutableStateOf("") }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> uris.forEach { uri -> try { val dir=File(ctx.filesDir,"photos").apply { mkdirs() }; val out=File(dir,"${UUID.randomUUID()}.image"); ctx.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }; if(out.exists() && out.length()>0) images.add(out.absolutePath) } catch (_:Exception) { error="部分图片导入失败，请重试" } } }
 LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { TextButton(onClick=onBack) {Text("‹ 返回")}; Text(if(original==null) "记录新机位" else "编辑机位",Modifier.weight(1f),fontSize=20.sp); TextButton(onClick={ val a=lat.toDoubleOrNull();val b=lon.toDoubleOrNull(); if(name.isBlank()) error="请填写机位名称" else if((lat.isNotBlank() || lon.isNotBlank()) && (a==null || b==null || a !in -90.0..90.0 || b !in -180.0..180.0)) error="请填写有效的经纬度（纬度 -90～90，经度 -180～180）" else { onSave(initial.copy(name=name.trim(),address=address.trim(),category=category,lat=lat.trim(),lon=lon.trim(),time=time.trim(),tips=tips.trim(),images=images.toList())) } }) { Text("保存") } } }
  if(error.isNotBlank()) item { Text(error,color=Color(0xffffb4ab),fontSize=15.sp) }
  item { Glass(Modifier.fillMaxWidth()) { Text("样片",fontSize=16.sp); Spacer(Modifier.height(8.dp)); Text("从相册选择多张照片，原始文件将保存在本机",fontSize=14.sp,color=secondary); Spacer(Modifier.height(12.dp)); Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { images.forEach { path -> Box { AsyncImage(File(path),null,Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop); Text("×",Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha=.5f),RoundedCornerShape(50)).clickable { images.remove(path) }.padding(horizontal=6.dp),color=Color.White) } }; Surface(onClick={picker.launch("image/*")},shape=RoundedCornerShape(12.dp),color=Color(0xff34545a)) { Box(Modifier.size(82.dp),contentAlignment=Alignment.Center) { Text("＋\n添加",fontSize=16.sp,lineHeight=21.sp) } } } } }
  item { Glass(Modifier.fillMaxWidth()) { Field("机位名称 *",name,{name=it}); Field("详细地址 / 地标",address,{ if (address != it) { address=it; lat=""; lon="" } }); Spacer(Modifier.height(12.dp)); Text("拍摄题材",fontSize=15.sp,color=secondary); Spacer(Modifier.height(8.dp)); Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) { categories.forEach { c-> FilterChip(selected=category==c,onClick={category=c},label={Text(c,fontSize=15.sp)}) } } } }
  item { Glass(Modifier.fillMaxWidth()) { Text("地图位置",fontSize=16.sp); Spacer(Modifier.height(8.dp)); Text(if(address.isBlank()) "先填写详细地址或地标，即可在高德地图搜索。" else "在高德地图中搜索：$address",fontSize=15.sp,color=secondary); Spacer(Modifier.height(8.dp)); TextButton(onClick={ openAmapSearch(ctx,address) },enabled=address.isNotBlank()) { Text("打开高德地图搜索 ↗") }; Text("搜索结果不会自动回填，请将准确地点名称填写到上方。",fontSize=14.sp,color=secondary) } }
  item { Glass(Modifier.fillMaxWidth()) { Field("最佳拍摄时间",time,{time=it}); Field("拍摄小贴士",tips,{tips=it}) } }
  item { Spacer(Modifier.height(20.dp)) }
 }
}
@Composable fun Field(label:String,value:String,onChange:(String)->Unit,number:Boolean=false) { OutlinedTextField(value,onChange,Modifier.fillMaxWidth().padding(top=9.dp),label={Text(label,fontSize=15.sp)},shape=RoundedCornerShape(14.dp),keyboardOptions=KeyboardOptions(keyboardType=if(number) KeyboardType.Decimal else KeyboardType.Text),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xff19323d).copy(alpha=.65f))) }
@Composable fun Detail(s:Spot,onBack:()->Unit,onEdit:()->Unit,onDelete:()->Unit,onFavorite:()->Unit) {
 val ctx=LocalContext.current; var confirm by remember { mutableStateOf(false) }; var share by remember { mutableStateOf(false) }
 val coordinate=if(s.lat.isNotBlank() && s.lon.isNotBlank()) "${s.lat}, ${s.lon}" else "按地点名称搜索"
 Column(Modifier.fillMaxSize().statusBarsPadding()) {
  LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   item { Box(Modifier.fillMaxWidth().height(300.dp).background(Brush.verticalGradient(listOf(Color(0xff315762),Color(0xff1b3440))))) { if(s.images.isNotEmpty()) AsyncImage(File(s.images.first()),s.name,Modifier.fillMaxSize(),contentScale=ContentScale.Crop); Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween) { Surface(onClick=onBack,shape=RoundedCornerShape(50),color=Color(0xff19323d).copy(alpha=.85f)) { Text("‹ 返回",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontSize=16.sp) }; Surface(onClick=onEdit,shape=RoundedCornerShape(50),color=Color(0xff19323d).copy(alpha=.85f)) { Text("编辑",Modifier.padding(horizontal=14.dp,vertical=8.dp),fontSize=16.sp) } } } }
   item { Glass(Modifier.fillMaxWidth().padding(horizontal=18.dp)) { Text("${s.category}  /  机位手记",fontSize=14.sp,color=secondary,letterSpacing=1.sp); Spacer(Modifier.height(7.dp)); Text(s.name,fontSize=25.sp,fontWeight=FontWeight.Light); Spacer(Modifier.height(6.dp)); Text(s.address.ifBlank { "未填写地点" },fontSize=16.sp,color=secondary); Spacer(Modifier.height(18.dp)); HorizontalDivider(color=Color(0xff607d80)); Spacer(Modifier.height(14.dp)); Text("位置坐标",fontSize=15.sp,color=secondary); Spacer(Modifier.height(5.dp)); Text(coordinate,fontSize=17.sp); if(s.address.isNotBlank() || s.name.isNotBlank()) { TextButton(onClick={ openAmapSearch(ctx,s.address.ifBlank { s.name }) }) { Text("在高德地图搜索 ↗",fontSize=15.sp) } }; Spacer(Modifier.height(8.dp)); Text("最佳拍摄时间",fontSize=15.sp,color=secondary); Spacer(Modifier.height(5.dp)); Text(s.time.ifBlank { "暂未记录" },fontSize=16.sp); Spacer(Modifier.height(15.dp)); Text("拍摄小贴士",fontSize=15.sp,color=secondary); Spacer(Modifier.height(5.dp)); Text(s.tips.ifBlank { "暂未记录" },fontSize=16.sp,lineHeight=22.sp) } }
   if(s.images.isNotEmpty()) item { Glass(Modifier.fillMaxWidth().padding(horizontal=18.dp)) { Text("样片集  ·  ${s.images.size}",fontSize=17.sp); Spacer(Modifier.height(12.dp)); Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { s.images.forEach { AsyncImage(File(it),"机位样片",Modifier.size(140.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop) } } } }
   item { TextButton(onClick={confirm=true},modifier=Modifier.padding(start=18.dp)) { Text("删除机位",color=Color(0xffffb4ab),fontSize=15.sp) } }
  }
  Surface(color=Color(0xff1d3540).copy(alpha=.96f),shadowElevation=3.dp) { Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceEvenly) { TextButton(onClick=onFavorite) { Text(if(s.favorite) "♥ 已收藏" else "♡ 收藏") }; Button(onClick={share=true},shape=RoundedCornerShape(14.dp)) { Text("分享机位") } } }
 }
 if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("删除这个机位？")},text={Text("该机位记录将从本机移除。")},confirmButton={TextButton(onClick={confirm=false;onDelete()}) { Text("删除") }},dismissButton={TextButton(onClick={confirm=false}) {Text("取消")}})
 if(share) AlertDialog(onDismissRequest={share=false},title={Text("分享机位链接")},text={Column { Text(s.name,fontWeight=FontWeight.Medium); Text(s.address,fontSize=16.sp); Spacer(Modifier.height(8.dp)); Text("链接包含机位文字与已有坐标，不包含样片。接收方需使用机位合集导入。",fontSize=15.sp,color=secondary) }},confirmButton={TextButton(onClick={ try { val link=SpotLinks.create(s); val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"机位合集 · ${s.name}\n$link"); ctx.startActivity(Intent.createChooser(intent,"分享机位链接")); share=false } catch (e:Exception) { android.widget.Toast.makeText(ctx,e.message ?: "生成链接失败",android.widget.Toast.LENGTH_LONG).show() } }) {Text("分享链接")}},dismissButton={TextButton(onClick={ try { val link=SpotLinks.create(s); (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("机位链接",link)); android.widget.Toast.makeText(ctx,"链接已复制",android.widget.Toast.LENGTH_SHORT).show();share=false } catch (e:Exception) { android.widget.Toast.makeText(ctx,e.message ?: "生成链接失败",android.widget.Toast.LENGTH_LONG).show() } }) {Text("复制链接")}})
}


private fun openAmapSearch(context:android.content.Context, keyword:String) {
 val uri=Uri.Builder().scheme("androidamap").authority("poi").appendQueryParameter("sourceApplication","机位合集").appendQueryParameter("keywords",keyword).appendQueryParameter("dev","0").build()
 val intent=Intent(Intent.ACTION_VIEW,uri).setPackage("com.autonavi.minimap")
 try { context.startActivity(intent) } catch (_:android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context,"未找到高德地图，请先安装高德地图",android.widget.Toast.LENGTH_LONG).show() }
}



