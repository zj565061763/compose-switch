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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_switch.ui.theme.AppTheme
import com.sd.lib.compose.swich.SwitchDefaultBackground
import com.sd.lib.compose.swich.SwitchDefaultThumb
import com.sd.lib.compose.swich.FSwitch
import com.sd.lib.compose.swich.rememberSwitchState

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Content()
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
      .statusBarsPadding()
      .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    SampleDefault()
    SampleCustom1()
    SampleCustom2()
  }
}

@Composable
private fun SampleDefault() {
  var checked by remember { mutableStateOf(false) }
  val state = rememberSwitchState(checked) {
    logMsg { "onCheckedChange: $it" }
    checked = it
  }

  FSwitch(state = state)
}

@Composable
private fun SampleCustom1() {
  var checked by remember { mutableStateOf(true) }
  val state = rememberSwitchState(checked) { checked = it }

  FSwitch(
    state = state,
    background = {
      SwitchDefaultBackground(
        progress = state.progress,
        colorChecked = Color.Red,
        shape = RoundedCornerShape(5.dp),
      )
    },
    thumb = {
      SwitchDefaultThumb(shape = RoundedCornerShape(5.dp))
    },
  )
}

@Composable
private fun SampleCustom2() {
  var checked by remember { mutableStateOf(false) }
  val state = rememberSwitchState(checked) { checked = it }

  FSwitch(
    state = state,
    background = {
      SwitchDefaultBackground(
        modifier = Modifier.fillMaxHeight(state.progress.coerceAtLeast(0.2f)),
        progress = state.progress,
        shape = RoundedCornerShape(5.dp)
      )
    },
    thumb = {
      Card(
        modifier = Modifier
          .aspectRatio(1f, true)
          .padding(start = 0.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(Dp.Hairline, Color(0xFFE3E3E3))
      ) {}
    },
  )
}

@Preview
@Composable
private fun Preview() {
  Content()
}

inline fun logMsg(block: () -> String) {
  Log.i("sd-demo", block())
}