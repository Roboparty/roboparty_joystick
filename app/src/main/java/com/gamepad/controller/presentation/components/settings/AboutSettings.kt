package com.gamepad.controller.presentation.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 关于设置
 */
@Composable
fun AboutSettings(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "关于",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8F9FA))
                .padding(vertical = 4.dp)
        ) {
            AboutItem(label = "名称", value = "Robot Controller")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFEEEEEE))
            AboutItem(label = "产品型号", value = "RC-1000")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFEEEEEE))
            AboutItem(label = "机器人版本", value = "v1.0.0")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFEEEEEE))
            AboutItem(label = "语言", value = "简体中文")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFEEEEEE))
            AboutNavigationItem(label = "隐私协议", onClick = { /* TODO */ })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFFEEEEEE))
            AboutItem(label = "国家/城市", value = "中国/北京")
        }
    }
}

@Composable
private fun AboutItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF1A1A2E)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun AboutNavigationItem(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF1A1A2E)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFCCCCCC)
        )
    }
}
