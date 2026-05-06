package com.sameerasw.canvas.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.sameerasw.canvas.R
import com.sameerasw.canvas.ui.theme.GoogleSansFlexRounded

/**
 * A reusable promotion card for the "Made by Sameera" subreddit community.
 */
@Composable
fun MadebySameeraswCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val redditUrl = "https://www.reddit.com/r/MadebySameerasw/"
    val isDark = isSystemInDarkTheme()
    
    val brandColor = Color(0xFF49FCBB)
    val brandColorDark = Color(0xFF007A54) // Darker tone for light theme
    val accentColor = if (isDark) brandColor else brandColorDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, redditUrl.toUri())
                context.startActivity(intent)
            },
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                // Banner Image
                Image(
                    painter = painterResource(id = R.drawable.madebysameerasw_cover),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Avatar Image (Overlapping)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 36.dp)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .border(4.dp, MaterialTheme.colorScheme.surfaceBright, CircleShape)
                        .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.avatar),
                        contentDescription = "Subreddit Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.promo_reddit_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.promo_reddit_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
