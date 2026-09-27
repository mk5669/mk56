package com.hassan.greedycat
import kotlin.math.roundToInt
data class GameStats(val count:Int,val average:Double,val median:Double,val below2Percent:Int,val last:Double?,val signal:String)
class StatisticsEngine{
 private val values=ArrayDeque<Double>()
 @Synchronized fun add(v:Double):GameStats{if(v in 1.0..10000.0&&(values.isEmpty()||kotlin.math.abs(values.last()-v)>0.0001)){values.addLast(v);while(values.size>60)values.removeFirst()};return snapshot()}
 @Synchronized fun snapshot():GameStats{val a=values.toList();if(a.isEmpty())return GameStats(0,0.0,0.0,0,null,"بانتظار نتيجة واضحة");val s=a.sorted();val med=if(s.size%2==0)(s[s.size/2-1]+s[s.size/2])/2 else s[s.size/2];val low=(a.count{it<2.0}*100.0/a.size).roundToInt();val recent=a.takeLast(minOf(8,a.size)).average();val signal=when{a.size<5->"تجميع البيانات: "+(5-a.size)+" جولات";recent<1.8->"المتوسط الأخير منخفض";recent>3.0->"المتوسط الأخير مرتفع";else->"النمط الأخير متقارب"};return GameStats(a.size,a.average(),med,low,a.last(),signal)}
}
