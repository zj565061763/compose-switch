# Gradle

[![](https://jitpack.io/v/zj565061763/compose-switch.svg)](https://jitpack.io/#zj565061763/comopse-switch)

# Demo
![](https://thumbsnap.com/i/KNsmBcg9.gif?1025)

```kotlin
@Composable
fun Content() {
    Box(Modifier.fillMaxSize()) {
        FSwitch {
            Log.i(TAG, "onCheckedChange: $it")
        }
    }
}
```