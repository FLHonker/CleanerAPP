package com.liuyan.cleaner;

/**
 * Created by Frank Liu on 2018-03-20.
 */

import android.os.Bundle;
import android.app.Dialog;
import android.app.Activity;
import android.view.*;
import android.widget.NumberPicker;
import android.widget.Button;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.Toast;

public class planClean extends Dialog {

    private Activity context;
    private NumberPicker setTime_np;
    private View.OnClickListener mClickListener;
    private Button cancel_btn, ok_btn;
    //定义定时的初始值
    private int initTime = 20;

    public planClean(Activity context){
        super(context);
        this.context = context;
    }

    public planClean(Activity context, View.OnClickListener clickListener) {
        super(context);
        this.context = context;
        this.mClickListener = clickListener;
    }

    public int getInitTime(){
        return initTime;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_timer_dialog);
        // 根据id在布局中找到控件对象
        setTime_np = (NumberPicker)findViewById(R.id.setTimer_np);
        cancel_btn = (Button)findViewById(R.id.set_cancel);
        ok_btn = (Button)findViewById(R.id.set_ok);

        // 为按钮绑定点击事件监听器
        cancel_btn.setOnClickListener(mClickListener);
        ok_btn.setOnClickListener(mClickListener);

        //设置np的最小值和最大值
        setTime_np.setMinValue(0);
        setTime_np.setMaxValue(60);
        //设置np的当前值
        setTime_np.setValue(initTime);
        setTime_np.setOnValueChangedListener(new OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // TODO Auto-generated method stub
                initTime = newVal;
//                showSelectedTime();
            }
        });

         /*
         * 获取圣诞框的窗口对象及参数对象以修改对话框的布局设置, 可以直接调用getWindow(),表示获得这个Activity的Window
         * 对象,这样这可以以同样的方式改变这个Activity的属性.
         */
        Window dialogWindow = this.getWindow();

        WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        // p.height = (int) (d.getHeight() * 0.6); // 高度设置为屏幕的0.6
        p.width = (int)(d.getWidth() * 0.6);   // 宽度设置为屏幕的0.8
        dialogWindow.setAttributes(p);
    }

}
