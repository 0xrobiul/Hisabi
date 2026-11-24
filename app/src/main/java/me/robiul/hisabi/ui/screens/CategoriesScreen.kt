package me.robiul.hisabi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.robiul.hisabi.data.local.CategoryEntity
import me.robiul.hisabi.data.local.CategoryType
import me.robiul.hisabi.ui.viewmodel.LedgerUiState
import me.robiul.hisabi.util.AvailableIconOptions
import me.robiul.hisabi.util.iconForKey

private val CategorySwatchHexes = listOf(
    0xFFFF7849L, 0xFF38BDF8L, 0xFFA78BFAL, 0xFFF59E0BL,
    0xFFF43F5EL, 0xFF6366F1L, 0xFF14B8A6L, 0xFF36C995L,
    0xFFEC4899L, 0xFF4ADE80L,
)

@Composable
fun CategoriesScreen(
    state: LedgerUiState,
    onNavigateBack: () -> Unit,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (Long) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(CategoryType.EXPENSE) }
    var showEditor by remember { mutableStateOf(false) }
    var editingCat by remember { mutableStateOf<CategoryEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val list = if (selectedTab == CategoryType.EXPENSE) state.expenseCategories else state.incomeCategories

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Category?") },
            text = { Text("Remove \"${cat.name}\"? Existing transactions keep their category name snapshot.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    if (showEditor) {
        CategoryEditorDialog(
            existing = editingCat,
            defaultType = selectedTab,
            nextSortOrder = list.size + 1,
            onDismiss = {
                showEditor = false
                editingCat = null
            },
            onSave = { cat ->
                onSaveCategory(cat)
                showEditor = false
                editingCat = null
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        editingCat = null
                        showEditor = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(44.dp).testTag("categories_add_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", maxLines = 1, softWrap = false)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryType.entries.forEach { t ->
                    FilterChip(
                        selected = selectedTab == t,
                        onClick = { selectedTab = t },
                        label = { Text("${t.label} Categories", maxLines = 1, softWrap = false) },
                    )
                }
            }
        }

        items(list, key = { it.id }) { cat ->
            val color = Color(cat.colorHex)
            val rowShape = RoundedCornerShape(18.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, rowShape)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconForKey(cat.iconKey), contentDescription = cat.name, tint = color, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                    val subs = cat.subcategories()
                    Text(
                        text = if (subs.isEmpty()) "No subcategories" else subs.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        editingCat = cat
                        showEditor = true
                    },
                ) { Icon(Icons.Rounded.Edit, contentDescription = "Edit ${cat.name}") }
                IconButton(onClick = { pendingDelete = cat }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${cat.name}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    existing: CategoryEntity?,
    defaultType: CategoryType,
    nextSortOrder: Int,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var nameTouched by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(existing?.type ?: defaultType) }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "others") }
    var colorHex by remember { mutableLongStateOf(existing?.colorHex ?: CategorySwatchHexes.first()) }
    var subcategoriesCsv by remember { mutableStateOf(existing?.subcategoriesCsv ?: "") }

    val nameError = if (nameTouched && name.trim().isEmpty()) "Category name is required" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Category" else "Edit Category") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (existing == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.label, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        nameTouched = true
                        name = it
                    },
                    label = { Text("Category Name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { msg -> { Text(msg) } },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                )
                OutlinedTextField(
                    value = subcategoriesCsv,
                    onValueChange = { subcategoriesCsv = it },
                    label = { Text("Subcategories (comma separated)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategorySwatchHexes.forEach { hex ->
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex },
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AvailableIconOptions.forEach { opt ->
                        FilterChip(
                            selected = iconKey == opt.key,
                            onClick = { iconKey = opt.key },
                            label = { Icon(opt.icon, contentDescription = opt.label, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameTouched = true
                    if (name.trim().isEmpty()) return@Button
                    onSave(
                        CategoryEntity(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            type = type,
                            iconKey = iconKey,
                            colorHex = colorHex,
                            subcategoriesCsv = subcategoriesCsv.split(",")
                                .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","),
                            isDefault = existing?.isDefault ?: false,
                            sortOrder = existing?.sortOrder ?: nextSortOrder,
                        )
                    )
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("category_save_button"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
