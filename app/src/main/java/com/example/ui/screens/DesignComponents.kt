package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.LocalCraftPalette

@Composable
fun FloatingBottomActionPill(
  text: String,
  icon: ImageVector,
  onClick: () -> Unit,
  testTagName: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val bg = MaterialTheme.colorScheme.background
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, bg.copy(alpha = 0.86f), bg)
          )
        )
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Button(
      onClick = onClick,
      enabled = enabled,
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 560.dp)
          .heightIn(min = 56.dp)
          .testTag(testTagName),
      shape = RoundedCornerShape(28.dp),
      elevation =
        ButtonDefaults.buttonElevation(
          defaultElevation = 6.dp,
          pressedElevation = 2.dp,
          disabledElevation = 0.dp,
        ),
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Composable
fun CraftLocationTagPill(
  locationName: String,
  modifier: Modifier = Modifier,
  maxLines: Int = 2,
) {
  val craftPalette = LocalCraftPalette.current
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.primaryContainer,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(6.dp)
            .clip(CircleShape)
            .background(craftPalette.brassEyelet)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Outlined.Place,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = locationName,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun IllustratedEmptyState(
  @DrawableRes illustrationRes: Int,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  actionButton: (@Composable () -> Unit)? = null,
) {
  BoxWithConstraints(
    modifier = modifier.padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    val isCompactHeight = maxHeight < 390.dp
    val imageHeight = if (isCompactHeight) 60.dp else 152.dp
    val verticalGap = if (isCompactHeight) 6.dp else 14.dp
    val bottomClearance = if (isCompactHeight) 78.dp else 96.dp

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(top = 6.dp, bottom = bottomClearance),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Image(
          painter = painterResource(id = illustrationRes),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.width(imageHeight * 4f / 3f)
              .height(imageHeight)
              .clip(RoundedCornerShape(22.dp)),
        )
      }
      Spacer(modifier = Modifier.height(verticalGap))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = if (isCompactHeight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionButton != null) {
        Spacer(modifier = Modifier.height(verticalGap))
        actionButton()
      }
    }
  }
}

@Composable
fun AboutAndPrivacyDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 460.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(R.string.about_app_title),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.testTag("about_dialog_title"),
        )
        Text(
          text =
            stringResource(
              R.string.about_app_version,
              BuildConfig.VERSION_NAME,
              BuildConfig.VERSION_CODE,
            ),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(R.string.about_app_summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AboutInfoBlock(
          icon = Icons.Outlined.Lock,
          title = stringResource(R.string.about_privacy_title),
          body = stringResource(R.string.about_privacy_body),
        )

        AboutInfoBlock(
          icon = Icons.Outlined.PhotoCamera,
          title = stringResource(R.string.about_permissions_title),
          body = stringResource(R.string.about_permissions_body),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp).testTag("close_about_dialog_button"),
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      ) {
        Text(text = stringResource(R.string.close_button), style = MaterialTheme.typography.labelLarge)
      }
    },
  )
}

@Composable
private fun AboutInfoBlock(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
