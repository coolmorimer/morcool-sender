package ru.morcool.sender;
import android.content.Context;import android.graphics.*;import android.view.View;
public class UiIcon extends View {
 final Paint paint=new Paint(3);final int type,color;
 public UiIcon(Context c,int type,int color){super(c);this.type=type;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();float scale=Math.min(getWidth(),getHeight())/24f;canvas.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2);canvas.scale(scale,scale);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);Path p=new Path();
  if(type==0){p.moveTo(3,4);p.lineTo(21,12);p.lineTo(3,20);p.lineTo(6,12);p.close();canvas.drawPath(p,paint);canvas.drawLine(6,12,21,12,paint);}
  if(type==1){canvas.drawCircle(9,7,3,paint);canvas.drawArc(3,12,15,23,185,170,false,paint);canvas.drawArc(13,4,21,12,-75,150,false,paint);canvas.drawArc(15,12,23,23,-90,90,false,paint);}
  if(type==2){for(int i=0;i<3;i++){canvas.drawCircle(4,6+i*6,.7f,paint);canvas.drawLine(9,6+i*6,21,6+i*6,paint);}}
  if(type==3){canvas.drawCircle(12,12,9,paint);canvas.drawCircle(12,9,3,paint);canvas.drawArc(5,13,19,25,200,140,false,paint);}
  if(type==4){p.moveTo(7,9);p.lineTo(12,14);p.lineTo(17,9);canvas.drawPath(p,paint);}
  canvas.restore();
 }
}
