package com.example.ui.editor

import android.app.Application
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
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao())
    }

    // Projects list from DB
    val savedProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project metadata
    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow("New Project")
    val currentProjectName: StateFlow<String> = _currentProjectName.asStateFlow()

    private val _currentProjectDescription = MutableStateFlow("Designed on Mobile")
    val currentProjectDescription: StateFlow<String> = _currentProjectDescription.asStateFlow()

    private val _screenWidth = MutableStateFlow(1280)
    val screenWidth: StateFlow<Int> = _screenWidth.asStateFlow()

    private val _screenHeight = MutableStateFlow(720)
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

    private val _showGrid = MutableStateFlow(true)
    val showGrid: StateFlow<Boolean> = _showGrid.asStateFlow()

    private val _snapToGrid = MutableStateFlow(true)
    val snapToGrid: StateFlow<Boolean> = _snapToGrid.asStateFlow()

    private val _gridSize = MutableStateFlow(10)
    val gridSize: StateFlow<Int> = _gridSize.asStateFlow()

    private val _devicePreviewType = MutableStateFlow("Phone 16:9") // "Phone 16:9", "Phone 20:9", "Tablet", "Desktop"
    val devicePreviewType: StateFlow<String> = _devicePreviewType.asStateFlow()

    private val _language = MutableStateFlow("vi") // "en" or "vi"
    val language: StateFlow<String> = _language.asStateFlow()

    fun toggleLanguage() {
        _language.value = if (_language.value == "en") "vi" else "en"
    }

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<RobloxObject>()
    private val redoStack = mutableListOf<RobloxObject>()

    // Clipboard
    private var clipboardObject: RobloxObject? = null
    private var clipboardProperty: Pair<String, Any>? = null

    // Autosave job
    private var autosaveJob: Job? = null

    // Helper: Find selected object in tree
    val selectedObject: StateFlow<RobloxObject?> = combine(_rootObject, _selectedObjectId) { root, selectedId ->
        if (selectedId == null) null else findObjectById(root, selectedId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createNewProject(name: String, templateName: String) {
        saveHistoryState()
        val templateRoot = ProjectTemplates.createTemplateByName(templateName)
        
        _currentProjectId.value = UUID.randomUUID().toString().replace("-", "").take(8)
        _currentProjectName.value = name
        _currentProjectDescription.value = "Template: $templateName"
        _rootObject.value = templateRoot
        _selectedObjectId.value = templateRoot.id // Auto-select root
        
        saveProjectToLocal()
    }

    fun loadProject(entity: ProjectEntity) {
        saveHistoryState()
        _currentProjectId.value = entity.id
        _currentProjectName.value = entity.name
        _currentProjectDescription.value = entity.description
        _screenWidth.value = entity.screenWidth
        _screenHeight.value = entity.screenHeight
        
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
            val screen = json.optJSONObject("screenSize")
            val width = screen?.optInt("width", 1280) ?: 1280
            val height = screen?.optInt("height", 720) ?: 720
            
            val objectsArray = json.getJSONArray("objects")
            if (objectsArray.length() > 0) {
                saveHistoryState()
                val root = JSONObjectToRobloxObject(objectsArray.getJSONObject(0))
                
                _currentProjectId.value = UUID.randomUUID().toString().replace("-", "").take(8)
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
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prevState = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_rootObject.value)
            _rootObject.value = prevState
            triggerAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_rootObject.value)
            _rootObject.value = nextState
            triggerAutosave()
        }
    }

    fun selectObject(id: String?) {
        _selectedObjectId.value = id
    }

    // --- Custom User Settings ---
    private val _useSingleDragMode = MutableStateFlow(false)
    val useSingleDragMode: StateFlow<Boolean> = _useSingleDragMode.asStateFlow()

    private val _isTopbarVisible = MutableStateFlow(true)
    val isTopbarVisible: StateFlow<Boolean> = _isTopbarVisible.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    fun setUseSingleDragMode(enabled: Boolean) {
        _useSingleDragMode.value = enabled
    }

    fun setTopbarVisible(visible: Boolean) {
        _isTopbarVisible.value = visible
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun setShowGrid(show: Boolean) {
        _showGrid.value = show
    }

    fun setSnapToGrid(snap: Boolean) {
        _snapToGrid.value = snap
    }

    fun setGridSize(size: Int) {
        _gridSize.value = size
    }

    fun setDevicePreview(device: String) {
        _devicePreviewType.value = device
        when (device) {
            "Phone 16:9" -> { _screenWidth.value = 1280; _screenHeight.value = 720 }
            "Phone 20:9" -> { _screenWidth.value = 1600; _screenHeight.value = 720 }
            "Tablet" -> { _screenWidth.value = 1024; _screenHeight.value = 768 }
            "Desktop" -> { _screenWidth.value = 1920; _screenHeight.value = 1080 }
        }
    }

    // --- Object Manipulation ---

    fun insertObject(className: RobloxClass) {
        val parentId = _selectedObjectId.value ?: _rootObject.value.id
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
        saveHistoryState()
        _rootObject.value = updateObjectInTree(_rootObject.value, id) { old ->
            val updated = old.properties.toMutableMap()
            updated[propName] = value
            old.copy(properties = updated)
        }
        triggerAutosave()
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
            val jsonStr = exportProjectAsJson()
            val entity = ProjectEntity(
                id = pid,
                name = _currentProjectName.value,
                description = _currentProjectDescription.value,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
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
        val newId = "obj_" + UUID.randomUUID().toString().replace("-", "").take(8)
        val newChildren = original.children.map { copyObjectWithNewIds(it) }
        return original.copy(
            id = newId,
            name = "${original.name}_Copy",
            children = newChildren
        )
    }
}
