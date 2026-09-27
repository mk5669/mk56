package com.hassan.greedycat
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import com.google.android.material.button.MaterialButton

class MainActivity:Activity(){
 private val requestCode=501
 private lateinit var status:TextView
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,30,28,24);setBackgroundColor(Color.rgb(247,248,252))}
  val logo=TextView(this).apply{text="🐱";textSize=42f}
  val title=TextView(this).apply{text="GreedyCat";textSize=30f}
  val owner=TextView(this).apply{text="حسن منصور  •  نسخة جديدة من الصفر";textSize=14f;setTextColor(Color.rgb(79,70,229))}
  val desc=TextView(this).apply{text="مراقبة إحصائية للشاشة مع لوحة عائمة فوق Zaina Live.\nتقرأ النتائج الظاهرة وتعرض ملخصاً حياً؛ لا توجد ضمانات.";textSize=16f;setTextColor(Color.rgb(55,65,81));setPadding(0,28,0,20)}
  status=TextView(this).apply{text="● جاهز";textSize=16f;setTextColor(Color.rgb(22,163,74));setPadding(0,10,0,18)}
  val start=MaterialButton(this).apply{text="بدء المراقبة";isAllCaps=false;setOnClickListener{begin()}}
  val stop=MaterialButton(this).apply{text="إيقاف المراقبة";isAllCaps=false;setOnClickListener{stopService(Intent(this@MainActivity,CaptureService::class.java));status.text="● تم الإيقاف"}}
  root.addView(logo);root.addView(title);root.addView(owner);root.addView(desc);root.addView(status)
  root.addView(start,LinearLayout.LayoutParams(-1,58).apply{bottomMargin=12});root.addView(stop,LinearLayout.LayoutParams(-1,58));setContentView(root)
 }
 private fun begin(){if(!Settings.canDrawOverlays(this)){status.text="● امنح إذن الظهور فوق التطبيقات";startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)));return};startActivityForResult(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent(),requestCode)}
 override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r!=requestCode)return;if(c!=RESULT_OK||d==null){status.text="● لم يبدأ تسجيل الشاشة";return};startForegroundService(Intent(this,CaptureService::class.java).apply{putExtra(CaptureService.EXTRA_RESULT_CODE,c);putExtra(CaptureService.EXTRA_DATA,d)});status.text="● المراقبة تعمل — افتح Zaina Live";moveTaskToBack(true)}
}
