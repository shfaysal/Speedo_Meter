package com.example.speedometer

import android.location.Location
import android.util.Log
import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.cos
import kotlin.math.sin


private val MINOR_INDICATOR_LENGTH = 14.dp
private val MAJOR_INDICATOR_LENGTH = 18.dp
private val INDICATOR_INITIAL_OFFSET = 5.dp

@Composable
fun SpeedometerScreen(
    modifier: Modifier = Modifier,
    currentLocation : LatLng,
    speedKmph : Float = 60f
) {

    val textMeasurer = rememberTextMeasurer()
    var currentSpeed by remember {
        mutableFloatStateOf(speedKmph)
    }



    var lastLocation = LatLng(0.0,0.0)


    val distance = calculateDistance(currentLocation, lastLocation)




    var rotate by remember {
        mutableStateOf(currentSpeed)
    }

    Log.d("SPEED METERRRR","Inside : $speedKmph rotate :  $rotate")
//    LaunchedEffect(key1 = currentLocation) {
//        while (true) {
//            try {
//                delay(2000) // Wait for 2 seconds before updating
//
//                // If lastLocation is not null, calculate the distance
//                if (lastLocation != LatLng(0.0, 0.0)) {
//                    val distance = calculateDistance(currentLocation, lastLocation!!)
//
//                    Log.d("DISTANCE FINAL", "Current: $currentLocation, Last: $lastLocation = Distance: $distance meters")
//                    lastLocation = currentLocation
//                }
//
//                if (lastLocation == LatLng(0.0, 0.0)){
//                    lastLocation = currentLocation
//                }
//                // Now update lastLocation to the current location after calculating distance
////                delay(1000)
////                Log.d("DISTANCE","current : $currentLocation last : $lastLocation = ${calculateDistance(currentLocation, lastLocation)}")
//                rotate += 1f // Update the rotation by 5 degrees (adjust to your needs)
//                if (rotate >= 360f) rotate = 0f // Reset after a full rotation
////                delay(2000) // Delay by 1 second between updates
////                lastLocation = currentLocation
//            } catch (e: TimeoutCancellationException) {
//                Log.d("TimeOut", "$e")
//            }
//        }
//    }

    Column(
        modifier = modifier.padding(top = 100.dp),
    ) {

        Canvas(modifier = modifier
            .padding(90.dp)
            .requiredSize(300.dp)) {
            drawArc(
                color = Color.Red,
                startAngle = 150f,
                sweepAngle = speedKmph,
                useCenter = false,
                style = Stroke(
                    width = 10.0.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            rotate(degrees = speedKmph) {
                drawRect(
                    color = Color.Green,
                    topLeft = center,
                    size = Size(5f, 300f)

                )
            }


            drawLine(
                color = Color.Red,
                start = Offset(0f,0f),
                end = center
            )

            for (angle in 300 downTo 60 step 2) {
                val speed = 300 - angle


                val startOffset = pointOfCircle(
                    thetaInDegrees = angle.toDouble(),
                    radius = size.height/2 - INDICATOR_INITIAL_OFFSET.toPx() ,
                    cX = center.x,
                    cY = center.y
                )


                if(speed%20 == 0) {
                    val endOffset = pointOfCircle(
                        thetaInDegrees = angle.toDouble(),
                        radius = size.height/2 - MAJOR_INDICATOR_LENGTH.toPx(),
                        cX = center.x,
                        cY = center.y
                    )

                    if (speed<=120) {
                        val numberOffset = pointOfCircle(
                            thetaInDegrees = angle.toDouble(),
                            radius = size.height/2 - MAJOR_INDICATOR_LENGTH.toPx() - MINOR_INDICATOR_LENGTH.toPx() ,
                            cX = center.x - MINOR_INDICATOR_LENGTH.toPx()/2,
                            cY = center.y - MINOR_INDICATOR_LENGTH.toPx()/2
                        )

//                    drawLine(
//                        color = Color.Yellow,
//                        start = endOffset,
//                        end = center,
//                        strokeWidth = 2.dp.toPx()
//                    )


                        drawText(
                            textMeasurer = textMeasurer,
                            text = speed.toString(),
                            topLeft = numberOffset
                        )
                    } else {

                        val numberOffset = pointOfCircle(
                            thetaInDegrees = angle.toDouble(),
                            radius = size.height/2 - MAJOR_INDICATOR_LENGTH.toPx() - MINOR_INDICATOR_LENGTH.toPx() - MINOR_INDICATOR_LENGTH.toPx()/2 ,
                            cX = center.x - MINOR_INDICATOR_LENGTH.toPx()/2,
                            cY = center.y - MINOR_INDICATOR_LENGTH.toPx()/1.5.toFloat()
                        )

//                    drawLine(
//                        color = Color.Yellow,
//                        start = endOffset,
//                        end = center,
//                        strokeWidth = 2.dp.toPx()
//                    )

                        drawText(
                            textMeasurer = textMeasurer,
                            text = speed.toString(),
                            topLeft = numberOffset
                        )
                    }

                    drawLine(
                        color = Color.Green,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 2.dp.toPx()
                    )
                }else if (speed%10 == 0) {
                    val endOffset = pointOfCircle(
                        thetaInDegrees = angle.toDouble(),
                        radius = size.height/2 - MINOR_INDICATOR_LENGTH.toPx(),
                        cX = center.x,
                        cY = center.y
                    )

                    drawLine(
                        color = Color.Black,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 2.dp.toPx()
                    )
                }else{
                    val endOffset = pointOfCircle(
                        thetaInDegrees = angle.toDouble(),
                        radius = size.height/2 - MINOR_INDICATOR_LENGTH.toPx(),
                        cX = center.x,
                        cY = center.y
                    )

                    drawLine(
                        color = Color.Red,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        Slider(
            value = rotate,
            onValueChange = {rotate = it},
            steps = 360,
            valueRange = 45f..300f
        )


    }




}

fun calculateDistance(currentLocation: LatLng, lastLocation: LatLng): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        currentLocation.latitude, currentLocation.longitude,
        lastLocation.latitude, lastLocation.longitude,
        result
    )
    return result[0] // The result is in meters
}

@Composable
fun SpeedoMeter(
    @FloatRange(from = 0.0, to = 240.0) currentSpeed: Float,
    modifier: Modifier = Modifier
) {

    val textMeasurer = rememberCoroutineScope()
    val textColor = MaterialTheme.colorScheme.surface
    val indicatorColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier,
        onDraw = {
            drawArc(
                color = Color.Red,
                startAngle = 60f,
                sweepAngle = -240f,
                useCenter = true,
                style = Stroke(
                    width = 2.0.dp.toPx()
                )
            )
        }
    )

}

private fun pointOfCircle(
    thetaInDegrees: Double,
    radius: Float,
    cX: Float = 0f,
    cY: Float = 0f
): Offset {
    val x = cX + (radius * sin(Math.toRadians(thetaInDegrees)).toFloat())
    val y = cY + (radius * cos(Math.toRadians(thetaInDegrees)).toFloat())

//    println("$cX and $cY radius = $radius $thetaInDegrees = ($x, $y)")
    return Offset(x,y)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SpeedometerPreview() {
    SpeedometerScreen(Modifier, LatLng(0.0, 0.0), 100f)
}
