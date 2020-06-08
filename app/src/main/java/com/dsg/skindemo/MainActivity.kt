package com.dsg.skindemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            changeSkin(getPath())
        }
        button2.setOnClickListener {
            startActivity(Intent(this, Main2Activity::class.java))
        }
    }
}
