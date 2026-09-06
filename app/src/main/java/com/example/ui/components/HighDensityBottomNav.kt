package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighDensityOutlineLight

enum class HighDensityNavTab {
    HOME,
    FILES,
    TOOLS,
    ASK_AI
}

@Composable
fun HighDensityBottomNav(
    currentTab: HighDensityNavTab,
    onTabSelected: (HighDensityNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HighDensityNavItem(
                label = "Home",
                icon = Icons.Default.Home,
                selected = currentTab == HighDensityNavTab.HOME,
                onClick = { onTabSelected(HighDensityNavTab.HOME) },
                testTag = "nav_home_tab"
            )

            HighDensityNavItem(
                label = "Files",
                icon = Icons.Default.FolderOpen,
                selected = currentTab == HighDensityNavTab.FILES,
                onClick = { onTabSelected(HighDensityNavTab.FILES) },
                testTag = "nav_files_tab"
            )

            HighDensityNavItem(
                label = "Tools",
                icon = Icons.Default.Build,
                selected = currentTab == HighDensityNavTab.TOOLS,
                onClick = { onTabSelected(HighDensityNavTab.TOOLS) },
                testTag = "nav_tools_tab"
            )

            HighDensityNavItem(
                label = "Ask AI",
                icon = Icons.Default.AutoAwesome,
                selected = currentTab == HighDensityNavTab.ASK_AI,
                onClick = { onTabSelected(HighDensityNavTab.ASK_AI) },
                testTag = "nav_ask_ai_tab"
            )
        }
    }
}

@Composable
private fun HighDensityNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
                    .background(color = primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = inactiveColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) primaryColor else inactiveColor
        )
    }
}
