package com.sd.demo.compose_switch

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_switch.ui.theme.AppTheme
import com.sd.lib.compose.swich.FSwitch
import com.sd.lib.compose.swich.FSwitchBackground
import com.sd.lib.compose.swich.FSwitchThumb

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Content()
                }
            }
        }
    }
}

@Composable
fun Content() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SampleDefault()
        SampleCustom1()
        SampleCustom2()
    }
}

@Composable
private fun SampleDefault() {
    var checked by remember { mutableStateOf(true) }
    FSwitch(
        checked = checked,
        interactiveMode = true,
    ) {
        logMsg { "onCheckedChange: $it" }
        checked = it
    }
}

@Composable
private fun SampleCustom1() {
    var checked by remember { mutableStateOf(false) }
    FSwitch(
        checked = checked,
        interactiveMode = true,
        background = {
            FSwitchBackground(
                progress = it,
                colorChecked = Color.Red,
                shape = RoundedCornerShape(5.dp),
            )
        },
        thumb = {
            FSwitchThumb(shape = RoundedCornerShape(5.dp))
        },
    ) {
        logMsg { "onCheckedChange: $it" }
        checked = it
    }
}

@Composable
private fun SampleCustom2() {
    var checked by remember { mutableStateOf(false) }
    FSwitch(
        checked = checked,
        interactiveMode = true,
        background = {
            FSwitchBackground(
                modifier = Modifier.fillMaxHeight(it.coerceAtLeast(0.2f)),
                progress = it,
                shape = RoundedCornerShape(5.dp)
            )
        },
        thumb = {
            Card(
                modifier = Modifier
                    .aspectRatio(1f, true)
                    .padding(start = 0.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                shape = RoundedCornerShape(5.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(Dp.Hairline, Color(0xFFE3E3E3))
            ) {}
        },
    ) {
        logMsg { "onCheckedChange: $it" }
        checked = it
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FSwitch-demo", block())
}