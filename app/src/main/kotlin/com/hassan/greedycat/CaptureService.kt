package com.hassan.greedycat
import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.math.roundToInt

class CaptureService:Service(){
 companion object{const val EXTRA_RESULT_CODE="result_code";const val EXTRA_DATA="data";const val ACTION_STOP="com.hassan.greedycat.STOP";private const val CHANNEL="greedycat_monitor"}
 private var projection:MediaProjection?=null
 private var display:android.hardware.display.VirtualDisplay?=null
 private var reader:ImageReader?=null
 private var overlay:Overlay?=null
 private var lastScan=0L
 private val worker=Executors.newSingleThreadExecutor()
 private val recognizer by lazy{TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)}
 private val stats=StatisticsEngine()
 private val pattern=Pattern.compile("(?<!\\d)(\\d{1,5}(?:[.,]\\d{1,2})?)(?:\\s*x)?",Pattern.CASE_INSENSITIVE)
 override fun onCreate(){super.onCreate();channel();overlay=Overlay();startForeground(9001,notification())}
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{
  if(i?.action==ACTION_STOP){stopSelf();return START_NOT_STICKY}
  if(projection==null){val code=i?.getIntExtra(EXTRA_RESULT_CODE,Activity.RESULT_CANCELED)?:return START_NOT_STICKY;val data=i.getParcelableExtra(EXTRA_DATA,Intent::class.java)?:return START_NOT_STICKY;startCapture(code,data)}
  return START_STICKY
 }
 private fun startCapture(code:Int,data:Intent){
  projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(code,data);if(projection==null){stopSelf();return}
  val dm=resources.displayMetrics
  reader=ImageReader.newInstance(dm.widthPixels,dm.heightPixels,PixelFormat.RGBA_8888,2)
  display=projection!!.createVirtualDisplay("GreedyCatMonitor",dm.widthPixels,dm.heightPixels,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null)
  projection!!.registerCallback(object:MediaProjection.Callback(){override fun onStop(){stopSelf()}},Handler(Looper.getMainLooper()))
  reader!!.setOnImageAvailableListener({src->{val now=SystemClock.elapsedRealtime();if(now-lastScan<1500){src.acquireLatestImage()?.close();return@setOnImageAvailableListener};lastScan=now;val image=src.acquireLatestImage()?:return@setOnImageAvailableListener;worker.execute{analyze(image)} }},Handler(Looper.getMainLooper()))
  overlay?.show();overlay?.render(stats.snapshot(),"متصل — أبحث عن النتائج")
 }
 private fun analyze(image:Image){
  var bitmap:Bitmap?=null;var cropped:Bitmap?=null
  try{
   val p=image.planes[0];val rowPixels=p.rowStride/p.pixelStride
   bitmap=Bitmap.createBitmap(rowPixels,image.height,Bitmap.Config.ARGB_8888);p.buffer.rewind();bitmap.copyPixelsFromBuffer(p.buffer)
   cropped=if(rowPixels!=image.width)Bitmap.createBitmap(bitmap,0,0,image.width,image.height)else bitmap
   val frame = cropped ?: throw IllegalStateException("frame unavailable")
   recognizer.process(InputImage.fromBitmap(frame,0)).addOnSuccessListener{result->
    val candidates=mutableListOf<Double>()
    result.textBlocks.forEach{b->val m=pattern.matcher(b.text.replace(',','.'));while(m.find())m.group(1)?.toDoubleOrNull()?.let{if(it in 1.0..10000.0)candidates.add(it)}}
    val chosen=candidates.firstOrNull()
    if(chosen!=null)overlay?.render(stats.add(chosen),"نتيجة مرصودة: x"+fmt(chosen)) else overlay?.render(stats.snapshot(),"أبحث عن نتيجة...")
   }.addOnFailureListener{overlay?.render(stats.snapshot(),"قراءة الشاشة مستمرة")}.addOnCompleteListener{if(cropped!==bitmap)cropped?.recycle();bitmap?.recycle()}
  }catch(_:Throwable){overlay?.render(stats.snapshot(),"تعذر تحليل الإطار");if(cropped!==bitmap)cropped?.recycle();bitmap?.recycle()}
  finally{image.close()}
 }
 private fun fmt(v:Double)=if(v%1.0==0.0)v.toInt().toString() else "%.2f".format(v)
 private inner class Overlay{
  private val wm=getSystemService(WINDOW_SERVICE) as WindowManager
  private val box=LinearLayout(this@CaptureService).apply{orientation=LinearLayout.VERTICAL;setPadding(18,14,18,12);background=GradientDrawable().apply{setColor(Color.argb(238,15,23,42));cornerRadius=26f;setStroke(2,Color.argb(150,99,102,241))}}
  private val title=TextView(this@CaptureService).apply{text="🐱 GreedyCat  •  حسن منصور";textSize=17f;setTextColor(Color.WHITE)}
  private val body=TextView(this@CaptureService).apply{textSize=14f;setTextColor(Color.rgb(229,231,235));setPadding(0,8,0,8)}
  private var params:WindowManager.LayoutParams?=null
  fun show(){
   if(box.parent!=null||!Settings.canDrawOverlays(this@CaptureService))return
   val row=LinearLayout(this@CaptureService)
   val min=Button(this@CaptureService).apply{text="تصغير";isAllCaps=false};val stop=Button(this@CaptureService).apply{text="إغلاق";isAllCaps=false}
   row.addView(min,LinearLayout.LayoutParams(0,-2,1f));row.addView(stop,LinearLayout.LayoutParams(0,-2,1f));box.removeAllViews();box.addView(title);box.addView(body);box.addView(row);min.setOnClickListener{compact()};stop.setOnClickListener{stopSelf()}
   val p=WindowManager.LayoutParams(dp(300),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP or Gravity.START;p.x=dp(12);p.y=dp(100);params=p
   var sx=0f;var sy=0f;var ox=0;var oy=0
   title.setOnTouchListener{_,e->when(e.actionMasked){MotionEvent.ACTION_DOWN->{sx=e.rawX;sy=e.rawY;ox=p.x;oy=p.y;true};MotionEvent.ACTION_MOVE->{p.x=ox+(e.rawX-sx).roundToInt();p.y=oy+(e.rawY-sy).roundToInt();try{wm.updateViewLayout(box,p)}catch(_:Throwable){};true};else->true}}
   wm.addView(box,p)
  }
  private fun compact(){box.removeAllViews();box.addView(TextView(this@CaptureService).apply{text="🐱 GC";textSize=16f;setTextColor(Color.WHITE);setPadding(16,10,16,10);setOnClickListener{expand()}});params?.let{it.width=dp(70);try{wm.updateViewLayout(box,it)}catch(_:Throwable){}}}
  private fun expand(){box.removeAllViews();box.addView(title);box.addView(body);val row=LinearLayout(this@CaptureService);row.addView(Button(this@CaptureService).apply{text="تصغير";isAllCaps=false;setOnClickListener{compact()}},LinearLayout.LayoutParams(0,-2,1f));row.addView(Button(this@CaptureService).apply{text="إغلاق";isAllCaps=false;setOnClickListener{stopSelf()}},LinearLayout.LayoutParams(0,-2,1f));box.addView(row);params?.let{it.width=dp(300);try{wm.updateViewLayout(box,it)}catch(_:Throwable){}}}
  fun render(s:GameStats,state:String){body.post{val last=s.last?.let{"x"+fmt(it)}?:"—";val avg=if(s.count==0)"—"else"%.2f".format(s.average);val med=if(s.count==0)"—"else"%.2f".format(s.median);body.text="● "+state+"\nالجولات: "+s.count+"\nآخر نتيجة: "+last+"\nالمتوسط: "+avg+"x\nالوسيط: "+med+"x\nأقل من x2: "+s.below2Percent+"%\nالإشارة: "+s.signal+"\n\nالتوقع إحصائي فقط — ليس ضماناً."}}
  fun remove(){try{if(box.parent!=null)wm.removeView(box)}catch(_:Throwable){}}
  private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
 }
 private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"GreedyCat",NotificationManager.IMPORTANCE_LOW))}
 private fun notification():Notification{val pi=PendingIntent.getService(this,7001,Intent(this,CaptureService::class.java).apply{action=ACTION_STOP},PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);return Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_logo).setContentTitle("GreedyCat — حسن منصور").setContentText("المراقبة تعمل").addAction(Notification.Action.Builder(null,"إغلاق",pi).build()).setOngoing(true).build()}
 override fun onDestroy(){overlay?.remove();display?.release();reader?.close();projection?.stop();worker.shutdownNow();recognizer.close();super.onDestroy()}
 override fun onBind(intent:Intent?)=null
}
