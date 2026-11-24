package me.robiul.hisabi.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import me.robiul.hisabi.data.local.AccountType

data class IconOption(val key: String, val label: String, val icon: ImageVector)

val AvailableIconOptions = listOf(
    IconOption("food", "Food", Icons.Rounded.Restaurant),
    IconOption("coffee", "Cha / Cafe", Icons.Rounded.LocalCafe),
    IconOption("groceries", "Groceries", Icons.Rounded.ShoppingCart),
    IconOption("transport", "Transport", Icons.Rounded.DirectionsCar),
    IconOption("metro", "Bus / Metro", Icons.Rounded.DirectionsTransit),
    IconOption("shopping", "Shopping", Icons.Rounded.ShoppingBag),
    IconOption("bills", "Bills", Icons.Rounded.Bolt),
    IconOption("phone", "Recharge", Icons.Rounded.PhoneAndroid),
    IconOption("health", "Health", Icons.Rounded.LocalHospital),
    IconOption("education", "Education", Icons.Rounded.School),
    IconOption("entertainment", "Fun", Icons.Rounded.Movie),
    IconOption("housing", "Housing", Icons.Rounded.Home),
    IconOption("personal", "Personal", Icons.Rounded.Person),
    IconOption("travel", "Travel", Icons.Rounded.Flight),
    IconOption("salary", "Salary", Icons.Rounded.Payments),
    IconOption("freelance", "Freelance", Icons.Rounded.LaptopMac),
    IconOption("business", "Business", Icons.Rounded.BusinessCenter),
    IconOption("investment", "Invest", Icons.AutoMirrored.Rounded.TrendingUp),
    IconOption("gifts", "Gifts", Icons.Rounded.CardGiftcard),
    IconOption("savings", "Savings", Icons.Rounded.Savings),
    IconOption("tuition", "Tuition", Icons.AutoMirrored.Rounded.MenuBook),
    IconOption("subscriptions", "Sub", Icons.Rounded.Subscriptions),
    IconOption("charity", "Family", Icons.Rounded.VolunteerActivism),
    IconOption("wallet", "Wallet", Icons.Rounded.AccountBalanceWallet),
    IconOption("bank", "Bank", Icons.Rounded.AccountBalance),
    IconOption("card", "Card", Icons.Rounded.CreditCard),
    IconOption("mfs", "MFS", Icons.Rounded.Smartphone),
    IconOption("star", "Goal", Icons.Rounded.Star),
    IconOption("others", "Others", Icons.Rounded.Category),
)

fun iconForKey(key: String): ImageVector = when (key) {
    "food" -> Icons.Rounded.Restaurant
    "coffee" -> Icons.Rounded.LocalCafe
    "groceries" -> Icons.Rounded.ShoppingCart
    "transport" -> Icons.Rounded.DirectionsCar
    "metro" -> Icons.Rounded.DirectionsTransit
    "shopping" -> Icons.Rounded.ShoppingBag
    "bills" -> Icons.Rounded.Bolt
    "phone" -> Icons.Rounded.PhoneAndroid
    "health" -> Icons.Rounded.LocalHospital
    "education" -> Icons.Rounded.School
    "entertainment" -> Icons.Rounded.Movie
    "housing" -> Icons.Rounded.Home
    "personal" -> Icons.Rounded.Person
    "travel" -> Icons.Rounded.Flight
    "salary" -> Icons.Rounded.Payments
    "freelance" -> Icons.Rounded.LaptopMac
    "business" -> Icons.Rounded.BusinessCenter
    "investment" -> Icons.AutoMirrored.Rounded.TrendingUp
    "gifts" -> Icons.Rounded.CardGiftcard
    "savings" -> Icons.Rounded.Savings
    "tuition" -> Icons.AutoMirrored.Rounded.MenuBook
    "subscriptions" -> Icons.Rounded.Subscriptions
    "charity" -> Icons.Rounded.VolunteerActivism
    "wallet" -> Icons.Rounded.AccountBalanceWallet
    "bank" -> Icons.Rounded.AccountBalance
    "card" -> Icons.Rounded.CreditCard
    "mfs" -> Icons.Rounded.Smartphone
    "transfer" -> Icons.Rounded.SwapHoriz
    "adjust" -> Icons.Rounded.Tune
    "refund" -> Icons.Rounded.Replay
    "star" -> Icons.Rounded.Star
    else -> Icons.Rounded.Category
}

fun AccountType.icon(): ImageVector = iconForKey(defaultIconKey)
