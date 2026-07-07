package com.example.ui.editor

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsPrefs = application.getSharedPreferences("vanilla_editor_settings", Context.MODE_PRIVATE)
    private val repository: ProjectRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao())
    }

    // Projects list from DB
    val savedProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project metadata
    private val _currentProjectId = MutableStateFlow<String?>(createProjectId())
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow("New Project")
    val currentProjectName: StateFlow<String> = _currentProjectName.asStateFlow()

    private val _currentProjectDescription = MutableStateFlow("Designed on Mobile")
    val currentProjectDescription: StateFlow<String> = _currentProjectDescription.asStateFlow()

    private val initialDevicePreview = "Roblox Scale"
    private val initialScreenSize = screenSizeForDevice(initialDevicePreview)

    private val _screenWidth = MutableStateFlow(initialScreenSize.first)
    val screenWidth: StateFlow<Int> = _screenWidth.asStateFlow()

    private val _screenHeight = MutableStateFlow(initialScreenSize.second)
    val screenHeight: StateFlow<Int> = _screenHeight.asStateFlow()

    // Roblox Object Hierarchy Root
    private val _rootObject = MutableStateFlow<RobloxObject>(ProjectTemplates.createEmptyProject())
    val rootObject: StateFlow<RobloxObject> = _rootObject.asStateFlow()

    // Editor Status & Navigation States
    private val _selectedObjectId = MutableStateFlow<String?>(null)
    val selectedObjectId: StateFlow<String?> = _selectedObjectId.asStateFlow()

    private val _activeScriptId = MutableStateFlow<String?>(null)
    val activeScriptId: StateFlow<String?> = _activeScriptId.asStateFlow()

    fun openScriptEditor(id: String) {
        _activeScriptId.value = id
    }

    fun closeScriptEditor() {
        _activeScriptId.value = null
    }

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _showGrid = MutableStateFlow(settingsPrefs.getBoolean("showGrid", true))
    val showGrid: StateFlow<Boolean> = _showGrid.asStateFlow()

    private val _snapToGrid = MutableStateFlow(settingsPrefs.getBoolean("snapToGrid", true))
    val snapToGrid: StateFlow<Boolean> = _snapToGrid.asStateFlow()

    private val _gridSize = MutableStateFlow(settingsPrefs.getInt("gridSize", 10).coerceIn(4, 64))
    val gridSize: StateFlow<Int> = _gridSize.asStateFlow()

    private val _devicePreviewType = MutableStateFlow(initialDevicePreview)
    val devicePreviewType: StateFlow<String> = _devicePreviewType.asStateFlow()

    private val _language = MutableStateFlow(settingsPrefs.getString("language", "vi") ?: "vi") // "en" or "vi"
    val language: StateFlow<String> = _language.asStateFlow()

    fun toggleLanguage() {
        val next = if (_language.value == "en") "vi" else "en"
        _language.value = next
        settingsPrefs.edit().putString("language", next).apply()
    }

    fun setLanguage(language: String) {
        val next = if (language == "en") "en" else "vi"
        _language.value = next
        settingsPrefs.edit().putString("language", next).apply()
    }

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<RobloxObject>()
    private val redoStack = mutableListOf<RobloxObject>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Clipboard
    private var clipboardObject: RobloxObject? = null
    private var clipboardProperty: Pair<String, Any>? = null

    // Autosave job
    private var autosaveJob: Job? = null
    private var lastTransformHistoryAt = 0L

    // Helper: Find selected object in tree
    val selectedObject: StateFlow<RobloxObject?> = combine(_rootObject, _selectedObjectId) { root, selectedId ->
        if (selectedId == null) null else findObjectById(root, selectedId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createNewProject(name: String, templateName: String) {
        saveHistoryState()
        val templateRoot = ProjectTemplates.createTemplateByName(templateName)
        
        _currentProjectId.value = createProjectId()
        _currentProjectName.value = name
        _currentProjectDescription.value = "Template: $templateName"
        _rootObject.value = templateRoot
        _selectedObjectId.value = templateRoot.id // Auto-select root
        
        saveProjectToLocal()
    }

    fun loadProject(entity: ProjectEntity) {
        saveHistoryState()
        val (width, height) = screenSizeForDevice("Roblox Scale")
        _currentProjectId.value = entity.id
        _currentProjectName.value = entity.name
        _currentProjectDescription.value = entity.description
        _screenWidth.value = width
        _screenHeight.value = height
        
        try {
            val json = JSONObject(entity.rootJson)
            val root = JSONObjectToRobloxObject(json)
            _rootObject.value = root
            _selectedObjectId.value = root.id
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importProjectFromJson(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val name = json.optString("projectName", "Imported Project")
            val (width, height) = screenSizeForDevice("Roblox Scale")
            
            val objectsArray = json.getJSONArray("objects")
            if (objectsArray.length() > 0) {
                saveHistoryState()
                val root = JSONObjectToRobloxObject(objectsArray.getJSONObject(0))
                
                _currentProjectId.value = createProjectId()
                _currentProjectName.value = name
                _currentProjectDescription.value = "Imported JSON project"
                _screenWidth.value = width
                _screenHeight.value = height
                _rootObject.value = root
                _selectedObjectId.value = root.id
                
                saveProjectToLocal()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportProjectAsJson(): String {
        val json = JSONObject()
        json.put("projectName", _currentProjectName.value)
        json.put("version", "1.0.0")
        
        val screen = JSONObject()
        screen.put("width", _screenWidth.value)
        screen.put("height", _screenHeight.value)
        json.put("screenSize", screen)
        
        val objectsArray = org.json.JSONArray()
        objectsArray.put(_rootObject.value.toJSONObject())
        json.put("objects", objectsArray)
        
        return json.toString(2)
    }

    // --- State Operations ---

    private fun saveHistoryState() {
        if (undoStack.size >= 30) {
            undoStack.removeAt(0)
        }
        undoStack.add(_rootObject.value)
        redoStack.clear()
        updateHistoryAvailability()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prevState = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_rootObject.value)
            _rootObject.value = prevState
            updateHistoryAvailability()
            triggerAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_rootObject.value)
            _rootObject.value = nextState
            updateHistoryAvailability()
            triggerAutosave()
        }
    }

    private fun updateHistoryAvailability() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun selectObject(id: String?) {
        _selectedObjectId.value = id
    }

    // --- Custom User Settings ---
    private val _useSingleDragMode = MutableStateFlow(settingsPrefs.getBoolean("useSingleDragMode", false))
    val useSingleDragMode: StateFlow<Boolean> = _useSingleDragMode.asStateFlow()

    private val _isTopbarVisible = MutableStateFlow(settingsPrefs.getBoolean("isTopbarVisible", true))
    val isTopbarVisible: StateFlow<Boolean> = _isTopbarVisible.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _uiScalePercent = MutableStateFlow(settingsPrefs.getInt("uiScalePercent", 40).coerceIn(40, 140))
    val uiScalePercent: StateFlow<Int> = _uiScalePercent.asStateFlow()

    private val _studioTheme = MutableStateFlow(settingsPrefs.getString("studioTheme", "Studio Dark") ?: "Studio Dark")
    val studioTheme: StateFlow<String> = _studioTheme.asStateFlow()

    private val _arrowControlsEnabled = MutableStateFlow(settingsPrefs.getBoolean("arrowControlsEnabled", true))
    val arrowControlsEnabled: StateFlow<Boolean> = _arrowControlsEnabled.asStateFlow()

    private val _arrowStepPx = MutableStateFlow(settingsPrefs.getInt("arrowStepPx", 5).coerceIn(1, 64))
    val arrowStepPx: StateFlow<Int> = _arrowStepPx.asStateFlow()

    fun setUseSingleDragMode(enabled: Boolean) {
        _useSingleDragMode.value = enabled
        settingsPrefs.edit().putBoolean("useSingleDragMode", enabled).apply()
    }

    fun setTopbarVisible(visible: Boolean) {
        _isTopbarVisible.value = visible
        settingsPrefs.edit().putBoolean("isTopbarVisible", visible).apply()
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setUiScalePercent(percent: Int) {
        val next = percent.coerceIn(40, 140)
        _uiScalePercent.value = next
        settingsPrefs.edit().putInt("uiScalePercent", next).apply()
    }

    fun setStudioTheme(theme: String) {
        _studioTheme.value = theme
        settingsPrefs.edit().putString("studioTheme", theme).apply()
    }

    fun setArrowControlsEnabled(enabled: Boolean) {
        _arrowControlsEnabled.value = enabled
        settingsPrefs.edit().putBoolean("arrowControlsEnabled", enabled).apply()
    }

    fun setArrowStepPx(step: Int) {
        val next = step.coerceIn(1, 64)
        _arrowStepPx.value = next
        settingsPrefs.edit().putInt("arrowStepPx", next).apply()
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun setShowGrid(show: Boolean) {
        _showGrid.value = show
        settingsPrefs.edit().putBoolean("showGrid", show).apply()
    }

    fun setSnapToGrid(snap: Boolean) {
        _snapToGrid.value = snap
        settingsPrefs.edit().putBoolean("snapToGrid", snap).apply()
    }

    fun setGridSize(size: Int) {
        val next = size.coerceIn(4, 64)
        _gridSize.value = next
        settingsPrefs.edit().putInt("gridSize", next).apply()
    }

    fun setDevicePreview(device: String) {
        val normalizedDevice = "Roblox Scale"
        _devicePreviewType.value = normalizedDevice
        settingsPrefs.edit().putString("devicePreview", normalizedDevice).apply()
        val (width, height) = screenSizeForDevice(normalizedDevice)
        _screenWidth.value = width
        _screenHeight.value = height
        triggerAutosave()
    }

    // --- Object Manipulation ---

    fun insertObject(className: RobloxClass) {
        val parentId = _selectedObjectId.value ?: _rootObject.value.id
        insertObjectInto(parentId, className)
    }

    fun insertObjectInto(parentId: String, className: RobloxClass) {
        val newObj = createDefaultObject(className)
        
        saveHistoryState()
        _rootObject.value = insertObjectInTree(_rootObject.value, parentId, newObj)
        _selectedObjectId.value = newObj.id // Select newly created object
        triggerAutosave()
    }

    fun deleteObject(id: String) {
        if (id == _rootObject.value.id) return // Cannot delete top level ScreenGui
        saveHistoryState()
        _rootObject.value = deleteObjectFromTree(_rootObject.value, id)
        if (_selectedObjectId.value == id) {
            _selectedObjectId.value = _rootObject.value.id
        }
        triggerAutosave()
    }

    fun duplicateObject(id: String) {
        if (id == _rootObject.value.id) return
        val original = findObjectById(_rootObject.value, id) ?: return
        val parent = findParentInTree(_rootObject.value, id) ?: return
        
        saveHistoryState()
        val duplicate = copyObjectWithNewIds(original)
        _rootObject.value = insertObjectInTree(_rootObject.value, parent.id, duplicate)
        _selectedObjectId.value = duplicate.id
        triggerAutosave()
    }

    fun renameObject(id: String, newName: String) {
        if (newName.isBlank()) return
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, id) { old ->
            old.copy(name = newName)
        }
        triggerAutosave()
    }

    fun updateProperty(id: String, propName: String, value: Any) {
        if (propName == "Name" && value is String) {
            renameObject(id, value)
            return
        }
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, id) { old ->
            val updated = old.properties.toMutableMap()
            updated[propName] = value
            old.copy(properties = updated)
        }
        triggerAutosave()
    }

    fun updateTransform(id: String, position: UDim2, size: UDim2) {
        val now = System.currentTimeMillis()
        if (now - lastTransformHistoryAt > 350L) {
            saveHistoryState()
            lastTransformHistoryAt = now
        }
        val rootSnapshot = _rootObject.value
        val parent = findParentInTree(rootSnapshot, id)
        val (parentW, parentH) = if (parent == null || parent.id == rootSnapshot.id) {
            _screenWidth.value.toFloat() to _screenHeight.value.toFloat()
        } else {
            resolveAbsoluteObjectSize(rootSnapshot, parent)
        }

        _rootObject.value = updateObjectInTree(rootSnapshot, id) { old ->
            val updated = old.properties.toMutableMap()
            val oldSize = old.properties["Size"] as? UDim2
            val currentAnchor = old.properties["AnchorPoint"] as? Vector2 ?: Vector2(0f, 0f)
            val movingWithoutResize = oldSize == size
            val aligned = if (movingWithoutResize) {
                resolveAutoAlignment(position, size, currentAnchor, parentW, parentH)
            } else {
                position to currentAnchor
            }

            updated["Position"] = aligned.first
            updated["Size"] = size
            updated["AnchorPoint"] = aligned.second
            old.copy(properties = updated)
        }
        triggerAutosave()
    }

    fun nudgeSelectedObject(deltaX: Int, deltaY: Int) {
        val id = _selectedObjectId.value ?: return
        val obj = findObjectById(_rootObject.value, id) ?: return
        val pos = obj.properties["Position"] as? UDim2 ?: return
        val size = obj.properties["Size"] as? UDim2 ?: return
        updateTransform(id, pos.copy(offsetX = pos.offsetX + deltaX, offsetY = pos.offsetY + deltaY), size)
    }

    fun resizeSelectedObject(deltaWidth: Int, deltaHeight: Int) {
        val id = _selectedObjectId.value ?: return
        val obj = findObjectById(_rootObject.value, id) ?: return
        val pos = obj.properties["Position"] as? UDim2 ?: return
        val size = obj.properties["Size"] as? UDim2 ?: return
        val updatedSize = size.copy(
            offsetX = (size.offsetX + deltaWidth).coerceAtLeast(10),
            offsetY = (size.offsetY + deltaHeight).coerceAtLeast(10)
        )
        updateTransform(id, pos, updatedSize)
    }

    fun reparentObject(id: String, targetParentId: String): Boolean {
        if (id == _rootObject.value.id || id == targetParentId) return false
        val movingObject = findObjectById(_rootObject.value, id) ?: return false
        if (containsObject(movingObject, targetParentId)) return false
        val currentParent = findParentInTree(_rootObject.value, id) ?: return false
        if (currentParent.id == targetParentId) return false
        if (findObjectById(_rootObject.value, targetParentId) == null) return false

        saveHistoryState()
        val withoutMovingObject = deleteObjectFromTree(_rootObject.value, id)
        _rootObject.value = insertObjectInTree(withoutMovingObject, targetParentId, movingObject)
        _selectedObjectId.value = id
        triggerAutosave()
        return true
    }

    fun moveObjectInHierarchy(id: String, moveUp: Boolean) {
        if (id == _rootObject.value.id) return
        val parent = findParentInTree(_rootObject.value, id) ?: return
        saveHistoryState()
        _rootObject.value = reorderChildren(_rootObject.value, parent.id, id, moveUp)
        triggerAutosave()
    }

    // --- Clipboard Operations ---

    fun copyObject(id: String) {
        clipboardObject = findObjectById(_rootObject.value, id)
    }

    fun pasteObject(targetParentId: String) {
        val toPaste = clipboardObject ?: return
        saveHistoryState()
        val duplicate = copyObjectWithNewIds(toPaste)
        _rootObject.value = insertObjectInTree(_rootObject.value, targetParentId, duplicate)
        _selectedObjectId.value = duplicate.id
        triggerAutosave()
    }

    fun copyProperty(propName: String, value: Any) {
        clipboardProperty = Pair(propName, value)
    }

    fun pasteProperty(targetId: String) {
        val (propName, value) = clipboardProperty ?: return
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, targetId) { old ->
            val updated = old.properties.toMutableMap()
            updated[propName] = value
            old.copy(properties = updated)
        }
        triggerAutosave()
    }

    // --- Utility Operations (Responsive Design) ---

    fun convertOffsetToScale(id: String) {
        val obj = findObjectById(_rootObject.value, id) ?: return
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, id) { old ->
            val updated = old.properties.toMutableMap()
            val parent = findParentInTree(_rootObject.value, id)
            
            // Extrapolate parent pixel boundaries (assume screen width/height for root-level Frame)
            val parentW = if (parent == null || parent.id == _rootObject.value.id) _screenWidth.value.toFloat() else 400f
            val parentH = if (parent == null || parent.id == _rootObject.value.id) _screenHeight.value.toFloat() else 300f

            val currentPos = old.properties["Position"] as? UDim2
            if (currentPos != null) {
                val newScaleX = currentPos.scaleX + (currentPos.offsetX / parentW)
                val newScaleY = currentPos.scaleY + (currentPos.offsetY / parentH)
                updated["Position"] = UDim2(newScaleX, 0, newScaleY, 0)
            }

            val currentSize = old.properties["Size"] as? UDim2
            if (currentSize != null) {
                val newScaleW = currentSize.scaleX + (currentSize.offsetX / parentW)
                val newScaleH = currentSize.scaleY + (currentSize.offsetY / parentH)
                updated["Size"] = UDim2(newScaleW, 0, newScaleH, 0)
            }

            old.copy(properties = updated)
        }
        triggerAutosave()
    }

    fun convertScaleToOffset(id: String) {
        val obj = findObjectById(_rootObject.value, id) ?: return
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, id) { old ->
            val updated = old.properties.toMutableMap()
            val parent = findParentInTree(_rootObject.value, id)
            val parentW = if (parent == null || parent.id == _rootObject.value.id) _screenWidth.value.toFloat() else 400f
            val parentH = if (parent == null || parent.id == _rootObject.value.id) _screenHeight.value.toFloat() else 300f

            val currentPos = old.properties["Position"] as? UDim2
            if (currentPos != null) {
                val newOffsetX = (currentPos.scaleX * parentW + currentPos.offsetX).toInt()
                val newOffsetY = (currentPos.scaleY * parentH + currentPos.offsetY).toInt()
                updated["Position"] = UDim2(0f, newOffsetX, 0f, newOffsetY)
            }

            val currentSize = old.properties["Size"] as? UDim2
            if (currentSize != null) {
                val newOffsetW = (currentSize.scaleX * parentW + currentSize.offsetX).toInt()
                val newOffsetH = (currentSize.scaleY * parentH + currentSize.offsetY).toInt()
                updated["Size"] = UDim2(0f, newOffsetW, 0f, newOffsetH)
            }

            old.copy(properties = updated)
        }
        triggerAutosave()
    }

    fun applyAnchorPreset(id: String, preset: String) {
        val obj = findObjectById(_rootObject.value, id) ?: return
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, id) { old ->
            val updated = old.properties.toMutableMap()
            
            val (anchor, pos) = when (preset) {
                "Top Left" -> Pair(Vector2(0f, 0f), UDim2(0f, 0, 0f, 0))
                "Top Center" -> Pair(Vector2(0.5f, 0f), UDim2(0.5f, 0, 0f, 0))
                "Center" -> Pair(Vector2(0.5f, 0.5f), UDim2(0.5f, 0, 0.5f, 0))
                "Bottom Right" -> Pair(Vector2(1f, 1f), UDim2(1f, 0, 1f, 0))
                "Top Right" -> Pair(Vector2(1f, 0f), UDim2(1f, 0, 0f, 0))
                "Bottom Left" -> Pair(Vector2(0f, 1f), UDim2(0f, 0, 1f, 0))
                else -> Pair(Vector2(0f, 0f), UDim2(0f, 0, 0f, 0))
            }
            
            updated["AnchorPoint"] = anchor
            updated["Position"] = pos
            old.copy(properties = updated)
        }
        triggerAutosave()
    }

    // --- Autosave & DB Operations ---

    private fun triggerAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1000) // Debounce autosave for 1 second
            saveProjectToLocal()
        }
    }

    fun saveProjectToLocal() {
        val pid = _currentProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = repository.getProjectById(pid)
            val jsonStr = exportProjectAsJson()
            val entity = ProjectEntity(
                id = pid,
                name = _currentProjectName.value,
                description = _currentProjectDescription.value,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                screenWidth = _screenWidth.value,
                screenHeight = _screenHeight.value,
                rootJson = jsonStr
            )
            repository.insertProject(entity)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProjectById(id)
            if (_currentProjectId.value == id) {
                _currentProjectId.value = null
            }
        }
    }

    // --- Private Helper Functions ---
    // Change to public to allow use in MainWorkspace for ScriptEditor
    fun findObjectById(root: RobloxObject, id: String): RobloxObject? {
        if (root.id == id) return root
        for (child in root.children) {
            val found = findObjectById(child, id)
            if (found != null) return found
        }
        return null
    }

    private fun findParentInTree(root: RobloxObject, childId: String): RobloxObject? {
        for (child in root.children) {
            if (child.id == childId) return root
            val parent = findParentInTree(child, childId)
            if (parent != null) return parent
        }
        return null
    }

    private fun containsObject(root: RobloxObject, targetId: String): Boolean {
        if (root.id == targetId) return true
        return root.children.any { containsObject(it, targetId) }
    }

    fun findParentOfObject(root: RobloxObject, childId: String): RobloxObject? {
        return findParentInTree(root, childId)
    }

    private fun resolveAbsoluteObjectSize(root: RobloxObject, obj: RobloxObject): Pair<Float, Float> {
        val parent = findParentInTree(root, obj.id)
        val (parentW, parentH) = if (parent == null || parent.id == root.id) {
            _screenWidth.value.toFloat() to _screenHeight.value.toFloat()
        } else {
            resolveAbsoluteObjectSize(root, parent)
        }
        val size = obj.properties["Size"] as? UDim2 ?: return parentW to parentH
        return ((size.scaleX * parentW) + size.offsetX).coerceAtLeast(1f) to
            ((size.scaleY * parentH) + size.offsetY).coerceAtLeast(1f)
    }

    private fun resolveAutoAlignment(
        position: UDim2,
        size: UDim2,
        anchor: Vector2,
        parentW: Float,
        parentH: Float
    ): Pair<UDim2, Vector2> {
        val w = ((size.scaleX * parentW) + size.offsetX).coerceAtLeast(1f)
        val h = ((size.scaleY * parentH) + size.offsetY).coerceAtLeast(1f)
        val anchorX = (position.scaleX * parentW) + position.offsetX
        val anchorY = (position.scaleY * parentH) + position.offsetY
        val left = anchorX - anchor.x * w
        val top = anchorY - anchor.y * h
        val right = left + w
        val bottom = top + h
        val centerX = left + w / 2f
        val centerY = top + h / 2f
        val threshold = 8f

        val horizontal = when {
            abs(left) <= threshold -> Triple(0f, 0, 0f)
            abs(centerX - parentW / 2f) <= threshold -> Triple(0.5f, 0, 0.5f)
            abs(right - parentW) <= threshold -> Triple(1f, 0, 1f)
            else -> null
        }
        val vertical = when {
            abs(top) <= threshold -> Triple(0f, 0, 0f)
            abs(centerY - parentH / 2f) <= threshold -> Triple(0.5f, 0, 0.5f)
            abs(bottom - parentH) <= threshold -> Triple(1f, 0, 1f)
            else -> null
        }

        val alignedPosition = position.copy(
            scaleX = horizontal?.first ?: position.scaleX,
            offsetX = horizontal?.second ?: position.offsetX,
            scaleY = vertical?.first ?: position.scaleY,
            offsetY = vertical?.second ?: position.offsetY
        )
        val alignedAnchor = anchor.copy(
            x = horizontal?.third ?: anchor.x,
            y = vertical?.third ?: anchor.y
        )
        return alignedPosition to alignedAnchor
    }

    private fun screenSizeForDevice(device: String): Pair<Int, Int> {
        return 1280 to 720
    }

    private fun updateObjectInTree(
        root: RobloxObject,
        targetId: String,
        transform: (RobloxObject) -> RobloxObject
    ): RobloxObject {
        if (root.id == targetId) {
            return transform(root)
        }
        return root.copy(
            children = root.children.map { updateObjectInTree(it, targetId, transform) }
        )
    }

    private fun deleteObjectFromTree(root: RobloxObject, targetId: String): RobloxObject {
        return root.copy(
            children = root.children
                .filter { it.id != targetId }
                .map { deleteObjectFromTree(it, targetId) }
        )
    }

    private fun insertObjectInTree(root: RobloxObject, parentId: String, newObj: RobloxObject): RobloxObject {
        if (root.id == parentId) {
            return root.copy(children = root.children + newObj)
        }
        return root.copy(
            children = root.children.map { insertObjectInTree(it, parentId, newObj) }
        )
    }

    private fun reorderChildren(root: RobloxObject, parentId: String, childId: String, moveUp: Boolean): RobloxObject {
        if (root.id == parentId) {
            val index = root.children.indexOfFirst { it.id == childId }
            if (index == -1) return root
            val newChildren = root.children.toMutableList()
            if (moveUp && index > 0) {
                val temp = newChildren[index]
                newChildren[index] = newChildren[index - 1]
                newChildren[index - 1] = temp
            } else if (!moveUp && index < newChildren.size - 1) {
                val temp = newChildren[index]
                newChildren[index] = newChildren[index + 1]
                newChildren[index + 1] = temp
            }
            return root.copy(children = newChildren)
        }
        return root.copy(
            children = root.children.map { reorderChildren(it, parentId, childId, moveUp) }
        )
    }

    private fun copyObjectWithNewIds(original: RobloxObject): RobloxObject {
        val newId = "obj_" + createProjectId()
        val newChildren = original.children.map { copyObjectWithNewIds(it) }
        return original.copy(
            id = newId,
            name = "${original.name}_Copy",
            children = newChildren
        )
    }

    private fun createProjectId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(8)
    }
}
