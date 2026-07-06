package com.example.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RobloxClass

@Composable
fun RobloxClassIcon(
    className: RobloxClass,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp
) {
    Image(
        painter = painterResource(id = className.vanillaIconRes()),
        contentDescription = className.name,
        modifier = modifier
            .size(iconSize)
            .aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}

private fun RobloxClass.vanillaIconRes(): Int {
    return when (this) {
        RobloxClass.ScreenGui -> R.drawable.vanilla_instance_screengui
        RobloxClass.Folder -> R.drawable.vanilla_instance_folder
        RobloxClass.Frame -> R.drawable.vanilla_instance_frame
        RobloxClass.TextLabel -> R.drawable.vanilla_instance_textlabel
        RobloxClass.TextButton -> R.drawable.vanilla_instance_textbutton
        RobloxClass.ImageLabel -> R.drawable.vanilla_instance_imagelabel
        RobloxClass.ImageButton -> R.drawable.vanilla_instance_imagebutton
        RobloxClass.ScrollingFrame -> R.drawable.vanilla_instance_scrollingframe
        RobloxClass.ViewportFrame -> R.drawable.vanilla_instance_viewportframe
        RobloxClass.Path2D -> R.drawable.vanilla_instance_path2d
        RobloxClass.UIListLayout -> R.drawable.vanilla_instance_uilistlayout
        RobloxClass.UIGridLayout -> R.drawable.vanilla_instance_uigridlayout
        RobloxClass.UIPadding -> R.drawable.vanilla_instance_uipadding
        RobloxClass.UICorner -> R.drawable.vanilla_instance_uicorner
        RobloxClass.UIStroke -> R.drawable.vanilla_instance_uistroke
        RobloxClass.UIGradient -> R.drawable.vanilla_instance_uigradient
        RobloxClass.UIScale -> R.drawable.vanilla_instance_uiscale
        RobloxClass.UIAspectRatioConstraint -> R.drawable.vanilla_instance_uiaspectratio
        RobloxClass.LocalScript -> R.drawable.vanilla_instance_localscript
        RobloxClass.ModuleScript -> R.drawable.vanilla_instance_modulescript
        RobloxClass.UIShadow -> R.drawable.vanilla_instance_uishadow
    }
}
