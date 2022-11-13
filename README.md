# Gradle

[![](https://jitpack.io/v/zj565061763/compose-switch.svg)](https://jitpack.io/#zj565061763/comopse-switch)

# Demo

![](https://thumbsnap.com/i/NdkgQub4.gif?1025)

```kotlin
FSwitch {
    logMsg { "onCheckedChange: $it" }
}


FSwitch(
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
}


FSwitch(
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
}
```