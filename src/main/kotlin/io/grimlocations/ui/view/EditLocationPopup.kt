package io.grimlocations.ui.view

import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.grimlocations.constant.APP_ICON
import io.grimlocations.data.dto.LocationDTO
import io.grimlocations.util.extension.closeIfOpen
import kotlinx.coroutines.ExperimentalCoroutinesApi

private val TEXT_FIELD_WIDTH = 480.dp

@ExperimentalFoundationApi
@ExperimentalCoroutinesApi
@Composable
private fun EditLocationPopup(
    location: LocationDTO,
    onOkClicked: (LocationDTO) -> Unit,
    onCancelClicked: (() -> Unit)?,
) {
    val locName = remember { mutableStateOf(location.name) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = locName.value,
                onValueChange = { locName.value = it },
                label = {
                    Text("Name", style = TextStyle(fontSize = 15.sp))
                },
                singleLine = true,
                modifier = Modifier.width(TEXT_FIELD_WIDTH)
            )
            Spacer(Modifier.height(20.dp))
            CoordianteRow(location)
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (onCancelClicked != null) {
                    Button(
                        onClick = onCancelClicked,
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Button(
                    onClick = { onOkClicked(location.copy(name = locName.value)) },
                ) {
                    Text("Ok")
                }
            }
        }
    }
}

@Composable
private fun CoordianteRow(location: LocationDTO) {

    val labelColor = MaterialTheme.colors.onSurface.let {
        val isLightColors = MaterialTheme.colors.isLight
        remember {
            val offset = if (isLightColors) .3f else -.3f
            it.copy(red = it.red + offset, blue = it.blue + offset, green = it.green + offset)
        }
    }

    val textBoxWidth = 150.dp
    val textFieldHeight = 56.dp
    val spacerWidth = 15.dp
    val enabled = true
    val readonly = true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextField(
            value = location.coordinate.coordinate1,
            readOnly = readonly,
            enabled = enabled,
            onValueChange = {},
            singleLine = true,
            label = {
                Text(
                    "Coordinate One",
                    color = labelColor
                )
            },
            modifier = Modifier.width(textBoxWidth).height(textFieldHeight)
        )
        Spacer(modifier = Modifier.width(spacerWidth))
        TextField(
            value = location.coordinate.coordinate2,
            readOnly = readonly,
            enabled = enabled,
            onValueChange = {},
            singleLine = true,
            label = {
                Text(
                    "Coordinate Two",
                    color = labelColor
                )
            },
            modifier = Modifier.width(textBoxWidth).height(textFieldHeight)
        )
        Spacer(modifier = Modifier.width(spacerWidth))
        TextField(
            value = location.coordinate.coordinate3,
            readOnly = readonly,
            enabled = enabled,
            onValueChange = {},
            singleLine = true,
            label = {
                Text(
                    "Coordinate Three",
                    color = labelColor
                )
            },
            modifier = Modifier.width(textBoxWidth).height(textFieldHeight)
        )
    }
}

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
fun openEditLocationPopup(
    location: LocationDTO,
    onOpen: (AppWindow) -> Unit,
    onCancelClicked: (AppWindow) -> Unit,
    onOkClicked: (AppWindow, LocationDTO) -> Unit,
) {
    lateinit var window: AppWindow

    Window(
        title = "Grim Locations",
        icon = APP_ICON,
        size = IntSize(600, 275),
        onDismissRequest = {
            onCancelClicked(window)
        }
    ) {
        window = LocalAppWindow.current

        remember { onOpen(window) }

        GrimLocationsTheme {
            EditLocationPopup(
                location = location,
                onCancelClicked = {
                    onCancelClicked(window)
                    window.closeIfOpen()
                },
                onOkClicked = { loc ->
                    onOkClicked(window, loc)
                    window.closeIfOpen()
                }
            )
        }
    }
}
